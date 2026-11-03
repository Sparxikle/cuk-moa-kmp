package kr.ac.catholic.cukmoa.app.ui

import kr.ac.catholic.cukmoa.core.ui.Choices
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.text
import kr.ac.catholic.cukmoa.core.ui.yearTitle
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import kr.ac.catholic.cukmoa.core.ui.Toggle
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.state.CatalogStatus
import kr.ac.catholic.cukmoa.core.state.StorageStatus
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.AppBackHandler
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipRow
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.core.ui.screenPop
import kr.ac.catholic.cukmoa.logic.usesCatalogTracks
import kr.ac.catholic.cukmoa.model.Catalog
import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.ThemeMode

/** App name (가대모아 = "gathering Catholic University info"). The store/launcher name is also set in AndroidManifest and the desktop config. */
const val APP_NAME = "가대모아"
const val APP_VERSION = "0.2.0"

/** Settings pages. Main lists the categories; each category is its own screen, 전공 opens from 내 정보. */
private enum class SettingsPage(val parent: SettingsPage?) {
    Main(null), Me(Main), Department(Me), Home(Main), Modules(Main), Announcements(Main), Appearance(Main), Language(Main), Data(Main), About(Main);

    val depth: Int get() = generateSequence(parent) { it.parent }.count()
}

/** Settings: a profile card (내 정보) and categories — home screen · features · announcements · appearance · data · about */
@Composable
fun SettingsScreen(catalog: Catalog, student: StudentState, apiBaseUrl: String, onBack: () -> Unit, onRestart: () -> Unit = {}) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    AppBackHandler { page.parent?.let { page = it } ?: onBack() }
    // Moving between pages: screen open/close transition by depth
    AnimatedContent(
        targetState = page,
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.bg),
        transitionSpec = { screenPop(forward = targetState.depth > initialState.depth) },
        label = "settings",
    ) { p -> SettingsPageContent(p, catalog, student, apiBaseUrl, onBack, onRestart) { page = it } }
}

@Composable
private fun SettingsPageContent(
    page: SettingsPage,
    catalog: Catalog,
    student: StudentState,
    apiBaseUrl: String,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    go: (SettingsPage) -> Unit,
) {
    val vm = LocalAppViewModel.current
    val departments by vm.departments.collectAsStateWithLifecycle()
    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val catalogStatus by vm.catalogStatus.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    val majors = departments.findAll(student.departments, student.majors)
    var confirmReset by remember { mutableStateOf(false) }
    // Language picked but not confirmed yet (changing it restarts the app)
    var pendingLanguage by remember { mutableStateOf<Pending?>(null) }
    val back = { page.parent?.let(go) ?: onBack() }
    val majorSummary = majors.mapIndexed { i, d -> "${majorLabel(i)} ${d.name}" }.joinToString(" · ").ifEmpty { stringResource(Res.string.not_chosen) }
    val catalogTracks = usesCatalogTracks(majors.firstOrNull()?.name)
    val tracks = if (catalogTracks) student.trackIds.mapNotNull { id -> catalog.tracks.firstOrNull { it.id == id }?.shortName }
    else student.careerInterests.filter { it in majors.firstOrNull()?.careers.orEmpty() }
    val serverSummary = when (val s = catalogStatus) {
        CatalogStatus.Live -> stringResource(Res.string.connected, apiBaseUrl)
        CatalogStatus.Loading -> stringResource(Res.string.connecting, apiBaseUrl)
        is CatalogStatus.Offline -> stringResource(Res.string.offline_saved, s.error.text())
    }

    // LG Settings: related pages share a card, each with a colored rounded-square icon (white glyph)
    val groups = listOf(
        listOf(
            SettingsEntry(SettingsPage.Home, Icons.Filled.Dashboard, SettingsColors.orange, stringResource(Res.string.home_screen), stringResource(Res.string.cards_on, student.homeCards.size)),
            SettingsEntry(SettingsPage.Modules, Icons.Filled.Extension, SettingsColors.slate, stringResource(Res.string.features), ModuleRegistry.enabled(student).map { stringResource(it.title) }.joinToString(", ")),
        ),
        listOf(SettingsEntry(SettingsPage.Announcements, Icons.Filled.Campaign, SettingsColors.red, stringResource(Res.string.module_announcements), null)),
        listOf(
            SettingsEntry(SettingsPage.Appearance, Icons.Filled.Palette, SettingsColors.purple, stringResource(Res.string.display), themeLabel(student.theme)),
            SettingsEntry(SettingsPage.Language, Icons.Filled.Language, SettingsColors.blue, stringResource(Res.string.language), languageLabel(student.language)),
        ),
        listOf(
            SettingsEntry(SettingsPage.Data, Icons.Filled.Storage, SettingsColors.green, stringResource(Res.string.data), null),
            SettingsEntry(SettingsPage.About, Icons.Filled.Info, SettingsColors.slate, stringResource(Res.string.about), APP_VERSION),
        ),
    )

    when (page) {
        SettingsPage.Main -> ScreenList(title = stringResource(Res.string.settings), onBack = onBack) {
            item(key = "me") {
                Group {
                ListRow(
                    yearTitle(student.grade) + " · " + (majors.firstOrNull()?.name ?: stringResource(Res.string.no_major_chosen)),
                    listOfNotNull(majors.getOrNull(1)?.name, tracks.joinToString(" · ").ifEmpty { null }).joinToString(" · ").ifEmpty { null },
                    modifier = Modifier.contentWidth().padding(vertical = Space.s),
                    leading = {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), contentAlignment = Alignment.Center) {
                            Text("${student.grade}", color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.titleLarge)
                        }
                    },
                    onClick = { go(SettingsPage.Me) },
                )
                }
            }
            groups.forEachIndexed { i, rows ->
                item(key = "group$i") {
                    Group {
                        rows.forEach { e ->
                            ListRow(e.title, e.summary, leading = { SettingsIcon(e.icon, e.color) }, onClick = { go(e.page) })
                        }
                    }
                }
            }
        }

        SettingsPage.Me -> ScreenList(title = stringResource(Res.string.my_info), onBack = back) {
            item(key = "year") {
                Section(stringResource(Res.string.year)) {
                    Choices((1..4).map { yearTitle(it) }, student.grade - 1, { vm.setGrade(it + 1) })
                }
            }
            item(key = "major") {
                Section(stringResource(Res.string.major)) {
                    ListRow(majors.firstOrNull()?.name ?: stringResource(Res.string.not_chosen), majors.getOrNull(1)?.name, chevron = true, onClick = { go(SettingsPage.Department) })
                }
            }
            item(key = "interests") {
                val first = majors.firstOrNull()
                Section(stringResource(Res.string.career_interests)) {
                    if (catalogTracks) ChipRow {
                        catalog.tracks.forEach { t ->
                            val order = student.trackIds.indexOf(t.id)
                            item(key = t.id.name) {
                                FilterChip(if (order >= 0) "${order + 1}. ${t.shortName}" else t.shortName, order >= 0) {
                                    vm.setTracks(if (order >= 0) student.trackIds - t.id else (student.trackIds + t.id).takeLast(2))
                                }
                            }
                        }
                    } else if (first == null || first.careers.isEmpty()) {
                        EmptyState(if (first == null) stringResource(Res.string.jobs_settings_no_major) else stringResource(Res.string.jobs_settings_none, first.name))
                    } else {
                        // Majors without catalog tracks: the 1st major's jobs (names: 대교협, details: CareerNet)
                        val jobs by vm.jobs.collectAsStateWithLifecycle()
                        LaunchedEffect(first.id) { vm.loadJobs(first.id) }
                        JobPicker(first.careers, jobs[first.id], tracks) { vm.setCareerInterests(it) }
                    }
                }
            }
        }

        SettingsPage.Department -> ScreenList(title = stringResource(Res.string.major), onBack = back) {
            item { DepartmentPicker(departments, majors) { vm.setDepartments(it) } }
        }

        SettingsPage.Home -> ScreenList(title = stringResource(Res.string.home_screen), onBack = back) {
            item {
                Group {
                    homeCardInfo.forEach { info ->
                        val provider = ModuleRegistry.all.firstOrNull { info.id in it.homeCards }
                        val moduleOff = provider != null && ModuleRegistry.enabled(student).none { it.id == provider.id }
                        val on = info.id in student.homeCards
                        fun set(v: Boolean) {
                            val next = if (v) student.homeCards + info.id else student.homeCards - info.id
                            vm.setHomeCards(homeCardInfo.map { it.id }.filter { it in next })
                        }
                        ListRow(
                            stringResource(info.title),
                            if (moduleOff) stringResource(Res.string.turn_on_feature_to_use, stringResource(provider!!.title)) else null,
                            leading = { Icon(info.icon, null) },
                            onClick = if (moduleOff) null else ({ set(!on) }),
                            trailing = { Toggle(checked = on, enabled = !moduleOff, onCheckedChange = ::set) },
                        )
                    }
                }
            }
        }

        SettingsPage.Modules -> ScreenList(title = stringResource(Res.string.features), onBack = back, info = stringResource(Res.string.features_platform_note)) {
            item { ModuleList(ModuleRegistry.enabled(student).map { it.id }, vm::setModules) }
        }

        SettingsPage.Announcements -> ScreenList(title = stringResource(Res.string.module_announcements), onBack = back) {
            item {
                Group {
                    if (majors.isEmpty()) ListRow(stringResource(Res.string.my_major_board), stringResource(Res.string.pick_major_for_notices))
                    majors.forEachIndexed { i, d ->
                        ListRow(
                            d.name,
                            majorLabel(i),
                            trailing = { if (d.board != null) Chip(stringResource(Res.string.notices_supported), ChipTone.Accent) else Chip(stringResource(Res.string.in_preparation)) },
                        )
                    }
                    ListRow(stringResource(Res.string.clear_new_badges), onClick = vm::resetAnnouncementsSeen)
                }
            }
        }

        SettingsPage.Appearance -> ScreenList(title = stringResource(Res.string.display), onBack = back) {
            item {
                Section(stringResource(Res.string.theme)) {
                    Choices(ThemeMode.entries.map { themeLabel(it) }, student.theme.ordinal, { vm.setTheme(ThemeMode.entries[it]) })
                }
            }
            // Only on phones that have wallpaper colors (Android 12+)
            if (vm.dynamicScheme != null) item {
                Section(stringResource(Res.string.use_wallpaper_colors)) {
                    ListRow(
                        stringResource(Res.string.use_wallpaper_colors),
                        stringResource(Res.string.use_wallpaper_colors_detail),
                        onClick = { vm.setDynamicColor(!student.dynamicColor) },
                        trailing = { Toggle(checked = student.dynamicColor, onCheckedChange = { vm.setDynamicColor(it) }) },
                    )
                }
            }
        }

        SettingsPage.Language -> ScreenList(title = stringResource(Res.string.language), onBack = back, info = stringResource(Res.string.language_android_note)) {
            item {
                Group {
                    val options = listOf<String?>(null) + LANGUAGE_NAMES.map { it.first }
                    options.forEach { lang ->
                        val selected = student.language == lang
                        ListRow(
                            languageLabel(lang),
                            onClick = { if (!selected) pendingLanguage = Pending(lang) },
                            trailing = { if (selected) Icon(Icons.Default.Check, stringResource(Res.string.selected), tint = MaterialTheme.colorScheme.primary) },
                        )
                    }
                }
            }
        }

        SettingsPage.Data -> ScreenList(title = stringResource(Res.string.data), onBack = back) {
            item {
                Group {
                    ListRow(
                        stringResource(Res.string.server_connection), serverSummary,
                        subtitleMaxLines = 2,
                        trailing = {
                            TextButton(onClick = { vm.refreshCatalog(); vm.refreshAnnouncements(); vm.refreshCalendar() }) { Text(stringResource(Res.string.reconnect)) }
                        },
                    )
                    ListRow(
                        stringResource(Res.string.device_storage),
                        when (storage) {
                            StorageStatus.Ok -> stringResource(Res.string.storage_ok)
                            StorageStatus.Unreadable -> stringResource(Res.string.storage_unreadable)
                            StorageStatus.SaveFailed -> stringResource(Res.string.storage_save_failed)
                        },
                    )
                    ListRow(
                        stringResource(Res.string.clear_records),
                        leading = { Icon(Icons.Outlined.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { confirmReset = true },
                    )
                }
            }
        }

        SettingsPage.About -> ScreenList(title = stringResource(Res.string.about), onBack = back) {
            item {
                Group {
                    ListRow(stringResource(Res.string.version), APP_VERSION)
                    ListRow(stringResource(Res.string.graduation_title), "${catalog.graduation.source.label} · ${catalog.graduation.label}", subtitleMaxLines = 3)
                    ListRow(stringResource(Res.string.courses_senior_records), catalog.source.label + if (catalog.source.isSample) stringResource(Res.string.sample_suffix) else "", subtitleMaxLines = 3)
                    ListRow(stringResource(Res.string.academic_calendar), calendar?.source?.let { stringResource(Res.string.as_of, it.label, it.asOf) } ?: stringResource(Res.string.not_received_yet), subtitleMaxLines = 3)
                    ListRow(stringResource(Res.string.department_list), departments?.source?.label ?: stringResource(Res.string.loading), subtitleMaxLines = 3)
                    ListRow(stringResource(Res.string.module_announcements), stringResource(Res.string.announcements_sources), subtitleMaxLines = 3)
                    ListRow(stringResource(Res.string.font), stringResource(Res.string.font_name))
                }
            }
        }
    }

    pendingLanguage?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingLanguage = null },
            title = { Text(stringResource(Res.string.language_restart_title)) },
            text = { Text(stringResource(Res.string.language_restart_text)) },
            confirmButton = {
                TextButton(onClick = { pendingLanguage = null; vm.setLanguage(p.language); onRestart() }) { Text(stringResource(Res.string.restart)) }
            },
            dismissButton = { TextButton(onClick = { pendingLanguage = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(Res.string.clear_records_q)) },
            text = { Text(stringResource(Res.string.clear_records_warning)) },
            confirmButton = { TextButton(onClick = { vm.resetRecords(); confirmReset = false }) { Text(stringResource(Res.string.clear)) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

private class SettingsEntry(val page: SettingsPage, val icon: ImageVector, val color: Color, val title: String, val summary: String?)

/** LG Settings category icon colors (measured on LG UX 10) */
private object SettingsColors {
    val blue = Color(0xFF2A9DF4)
    val red = Color(0xFFF5594A)
    val orange = Color(0xFFFF9500)
    val purple = Color(0xFF7A3CF0)
    val green = Color(0xFF34C25A)
    val slate = Color(0xFF5F6B76)
}

/** LG Settings icon: 36dp rounded square in the category color with a white glyph */
@Composable
private fun SettingsIcon(icon: ImageVector, color: Color) {
    Box(Modifier.size(36.dp).clip(RoundedCornerShape(percent = 30)).background(color), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(22.dp), tint = Color.White)
    }
}

@Composable
private fun themeLabel(mode: ThemeMode) = stringResource(
    when (mode) {
        ThemeMode.System -> Res.string.theme_system
        ThemeMode.Light -> Res.string.theme_light
        ThemeMode.Dark -> Res.string.theme_dark
    },
)

/** A language choice waiting for confirmation (null language = follow the phone). */
private data class Pending(val language: String?)

/** Each language in its own script, so it can be found whatever the current language is */
private val LANGUAGE_NAMES = listOf("ko" to "한국어", "en" to "English", "zh" to "中文 (简体)", "ja" to "日本語")

@Composable
private fun languageLabel(language: String?): String =
    LANGUAGE_NAMES.firstOrNull { it.first == language }?.second ?: stringResource(Res.string.follow_phone)

