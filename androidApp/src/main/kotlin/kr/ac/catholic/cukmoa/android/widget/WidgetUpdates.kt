package kr.ac.catholic.cukmoa.android.widget

import android.content.Context
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kr.ac.catholic.cukmoa.android.BuildConfig
import kr.ac.catholic.cukmoa.api.ApiClient
import kr.ac.catholic.cukmoa.app.SharedPreferencesStore
import kr.ac.catholic.cukmoa.core.state.WidgetSink
import kr.ac.catholic.cukmoa.core.state.refreshWidgetSnapshot
import kr.ac.catholic.cukmoa.logic.decodeWidgetSnapshot
import kr.ac.catholic.cukmoa.logic.encodeWidgetSnapshot
import kr.ac.catholic.cukmoa.logic.kstMoment
import kr.ac.catholic.cukmoa.model.WidgetSnapshot

// Widgets read the snapshot from their own SharedPreferences file. It is written by the app (WidgetSink, on every data
// change) and by WidgetRefreshWorker (hourly, while the app is closed). A second, one-off job wakes the widgets at the
// next class start or end and at midnight, so "next class" and D-days are right without a network call.

private const val PREFS = "cuk-moa-widget"
private const val SNAPSHOT_KEY = "snapshot"
private const val REFRESH_WORK = "widget-refresh"
private const val TICK_WORK = "widget-tick"

object WidgetStore {
    fun read(context: Context): WidgetSnapshot? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(SNAPSHOT_KEY, null)?.let(::decodeWidgetSnapshot)

    fun write(context: Context, json: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(SNAPSHOT_KEY, json) }
        decodeWidgetSnapshot(json)?.let { flow(context).value = it }
    }

    private var current: MutableStateFlow<WidgetSnapshot?>? = null

    /**
     * The snapshot as it changes. Widgets collect this inside their composition: Glance keeps a widget's session
     * running and only recomposes on update(), so data read once in provideGlance would stay stale until the
     * widget is moved or resized.
     */
    @Synchronized
    fun flow(context: Context): MutableStateFlow<WidgetSnapshot?> =
        current ?: MutableStateFlow(read(context.applicationContext)).also { current = it }
}

private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** Every widget of this app, redrawn after the snapshot changes. */
suspend fun updateAllWidgets(context: Context) {
    CalendarWidget().updateAll(context)
    TimetableWidget().updateAll(context)
    AnnouncementsWidget().updateAll(context)
    TodayWidget().updateAll(context)
}

/** The app's side: stores each new snapshot and redraws the widgets. */
class AndroidWidgetSink(context: Context) : WidgetSink {
    private val app = context.applicationContext
    override fun publish(snapshotJson: String) {
        WidgetStore.write(app, snapshotJson)
        widgetScope.launch {
            updateAllWidgets(app)
            WidgetSchedule.scheduleTick(app)
        }
    }
}

object WidgetSchedule {
    /** Hourly refresh from the server while there is a network. Kept if already scheduled. */
    fun ensure(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.HOURS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(REFRESH_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
        scheduleTick(context)
    }

    /** On app start: makes sure the jobs run when widgets are placed (e.g. after an update cleared them). */
    fun ensureIfPlaced(context: Context) {
        val manager = android.appwidget.AppWidgetManager.getInstance(context)
        val placed = listOf(
            CalendarWidgetReceiver::class.java, TimetableWidgetReceiver::class.java,
            AnnouncementsWidgetReceiver::class.java, TodayWidgetReceiver::class.java,
        ).any { manager.getAppWidgetIds(android.content.ComponentName(context, it)).isNotEmpty() }
        if (placed) ensure(context)
    }

    /** Stops both jobs when the last widget is removed. */
    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(REFRESH_WORK)
        WorkManager.getInstance(context).cancelUniqueWork(TICK_WORK)
    }

    /** Wakes the widgets at the next class start or end, or at midnight (Korea time), whichever comes first. */
    fun scheduleTick(context: Context) {
        val now = System.currentTimeMillis()
        val m = kstMoment(now)
        val untilMidnight = (24 * 60 - m.minute).toLong()
        val untilClass = WidgetStore.read(context)?.timetable?.slots.orEmpty()
            .filter { it.weekday == m.weekday }
            .flatMap { listOf(it.startMinute, it.endMinute) }
            .filter { it > m.minute }
            .minOrNull()?.let { (it - m.minute).toLong() }
        // Within the hour before a class the widgets count down ("In 17 min"), so wake every minute then
        val untilCountdown = untilClass?.let { if (it <= 60) 1L else it - 60 }
        // At least a minute, and a few seconds late so the boundary itself has passed
        val delayMs = (minOf(untilMidnight, untilCountdown ?: untilMidnight) * 60_000L - (now % 60_000L) + 5_000L).coerceAtLeast(55_000L)
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(WidgetRefreshWorker.TICK_ONLY to true))
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(TICK_WORK, ExistingWorkPolicy.REPLACE, request)
    }
}

/**
 * Hourly: rebuilds the snapshot from the app's stored data plus fresh calendar and announcements.
 * Tick (TICK_ONLY): only redraws, then schedules the next tick.
 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (!inputData.getBoolean(TICK_ONLY, false)) {
            val snapshot = refreshWidgetSnapshot(SharedPreferencesStore(applicationContext), ApiClient(BuildConfig.API_BASE_URL))
            WidgetStore.write(applicationContext, encodeWidgetSnapshot(snapshot))
        }
        updateAllWidgets(applicationContext)
        WidgetSchedule.scheduleTick(applicationContext)
        return Result.success()
    }

    companion object {
        const val TICK_ONLY = "tickOnly"
    }
}
