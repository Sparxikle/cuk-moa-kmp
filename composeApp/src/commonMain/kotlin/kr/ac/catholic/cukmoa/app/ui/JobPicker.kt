package kr.ac.catholic.cukmoa.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalUriHandler
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.CheckCircle
import kr.ac.catholic.cukmoa.core.ui.DetailSheet
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.MoreRow
import kr.ac.catholic.cukmoa.core.ui.SheetHeader
import kr.ac.catholic.cukmoa.model.JobInfo
import kr.ac.catholic.cukmoa.model.JobList
import org.jetbrains.compose.resources.stringResource

/** Rows shown before "view all". */
private const val SHOWN = 5

/**
 * Jobs related to the 1st major, up to 2 picked. The first five with what the job does (CareerNet),
 * plus "아직 모르겠어요"; "모두 보기" opens every job of the department in a sheet.
 * Without details from the server the rows show the job names alone.
 */
@Composable
fun JobPicker(names: List<String>, jobs: JobList?, selected: List<String>, onChange: (List<String>) -> Unit) {
    val info = jobs?.items?.associateBy { it.name }.orEmpty()
    var showAll by remember { mutableStateOf(false) }
    fun toggle(name: String) = onChange(if (name in selected) selected - name else (selected + name).takeLast(2))

    // Picked jobs stay visible even when they come from the full list
    val shown = (names.take(SHOWN) + selected.filter { it in names }).distinct()
    if (showAll) AllJobsSheet(names, info, selected, ::toggle) { showAll = false }

    Column {
        shown.forEach { name -> JobRow(info[name] ?: JobInfo(name), selected.indexOf(name)) { toggle(name) } }
        ListRow(
            stringResource(Res.string.not_sure_yet),
            leading = { CheckCircle(selected.isEmpty()) },
            onClick = { onChange(emptyList()) },
        )
        if (names.size > SHOWN) MoreRow(stringResource(Res.string.view_all_jobs, names.size)) { showAll = true }
    }
}

@Composable
private fun JobRow(job: JobInfo, order: Int, trailing: (@Composable () -> Unit)? = null, onClick: () -> Unit) {
    ListRow(
        if (order >= 0) "${order + 1}. ${job.name}" else job.name,
        // A broader CareerNet job says so first, so its description isn't taken for this exact job
        listOfNotNull(
            job.matched?.let { stringResource(Res.string.job_matched, it) },
            job.summary,
            job.wage?.let { stringResource(Res.string.job_wage, it) },
        ).joinToString(" · ").ifEmpty { null },
        subtitleMaxLines = 2,
        leading = { CheckCircle(order >= 0) },
        trailing = trailing,
        onClick = onClick,
    )
}

/** Every job of the department, as a checklist with descriptions and a link to CareerNet. */
@Composable
private fun AllJobsSheet(names: List<String>, info: Map<String, JobInfo>, selected: List<String>, onToggle: (String) -> Unit, onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    DetailSheet(onDismiss) {
        SheetHeader(stringResource(Res.string.all_jobs_title, names.size), stringResource(Res.string.pick_up_to_two))
        names.forEach { name ->
            val job = info[name] ?: JobInfo(name)
            JobRow(
                job, selected.indexOf(name),
                trailing = job.url?.let { url ->
                    {
                        IconButton(onClick = { runCatching { uri.openUri(url) } }) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, stringResource(Res.string.open_careernet, name), tint = AppTheme.colors.ink2)
                        }
                    }
                },
            ) { onToggle(name) }
        }
    }
}
