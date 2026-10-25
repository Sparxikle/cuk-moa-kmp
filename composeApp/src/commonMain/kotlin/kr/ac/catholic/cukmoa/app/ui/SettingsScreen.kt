package kr.ac.catholic.cukmoa.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.state.CatalogStatus
import kr.ac.catholic.cukmoa.core.state.StorageStatus
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.AppBackHandler
import kr.ac.catholic.cukmoa.core.ui.screenPop
import kr.ac.catholic.cukmoa.core.ui.Chip
import kr.ac.catholic.cukmoa.core.ui.ChipTone
import kr.ac.catholic.cukmoa.core.ui.Chips
import kr.ac.catholic.cukmoa.core.ui.FilterChip
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.ScreenList
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.contentWidth
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

/** App name (가대모아 = "gathering Catholic University info"). The store/launcher name is also set in AndroidManifest and the desktop config. */
const val APP_NAME = "가대모아"
const val APP_VERSION = "0.2.0"

private enum class SettingsPage { Main, Department }

/** Settings: my info · dashboard · feature modules · announcements · data · about */
@Composable
fun SettingsScreen(catalog: Catalog, student: StudentState, apiBaseUrl: String, onBack: () -> Unit) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.Main) }
    AppBackHandler { if (page == SettingsPage.Main) onBack() else page = SettingsPage.Main }
    // Settings ↔ major picker: screen open/close transition
    AnimatedContent(
        targetState = page,
        modifier = Modifier.fillMaxSize().background(AppTheme.colors.bg),
        transitionSpec = { screenPop(forward = targetState.ordinal > initialState.ordinal) },
        label = "settings",
    ) { p -> SettingsPageContent(p, catalog, student, apiBaseUrl, onBack) { page = it } }
}

@Composable
private fun SettingsPageContent(
    page: SettingsPage,
    catalog: Catalog,
    student: StudentState,
    apiBaseUrl: String,
    onBack: () -> Unit,
    go: (SettingsPage) -> Unit,
) {

    val vm = LocalAppViewModel.current
    val departments by vm.departments.collectAsStateWithLifecycle()
    val majors = departments.findAll(student.departments, student.majors)

    if (page == SettingsPage.Department) {
        ScreenList(title = "전공", eyebrow = "설정 · 최대 2개", trailing = { BackButton { go(SettingsPage.Main) } }) {
            item {
                Column(Modifier.contentWidth()) {
                    DepartmentPicker(departments, majors) { vm.setDepartments(it) }
                }
            }
        }
        return
    }

    val calendar by vm.calendar.collectAsStateWithLifecycle()
    val catalogStatus by vm.catalogStatus.collectAsStateWithLifecycle()
    val storage by vm.storage.collectAsStateWithLifecycle()
    var pendingGrade by remember { mutableStateOf<Int?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val c = AppTheme.colors

    ScreenList(title = "설정", eyebrow = APP_NAME, trailing = { BackButton(onBack) }) {
        // ---- My info ----
        item {
            Section("내 정보") {
                Group {
                    Column(Modifier.fillMaxWidth().padding16(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        Txt("학년", AppTheme.type.callout)
                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            (1..4).forEach { g ->
                                SegmentedButton(
                                    selected = g == student.grade,
                                    onClick = { if (g != student.grade) pendingGrade = g },
                                    shape = SegmentedButtonDefaults.itemShape(g - 1, 4),
                                ) { Text("${g}학년", style = AppTheme.type.subhead) }
                            }
                        }
                    }
                    ListRow(
                        "전공",
                        majors.mapIndexed { i, d -> "${majorLabel(i)} ${d.name}" }.joinToString(" · ").ifEmpty { "고르지 않음" },
                        chevron = true,
                        onClick = { go(SettingsPage.Department) },
                    )
                    Column(Modifier.fillMaxWidth().padding16(), verticalArrangement = Arrangement.spacedBy(Space.s)) {
                        Txt("관심 진로", AppTheme.type.callout)
                        Txt("두 개까지 · 첫 번째가 추천 기준", AppTheme.type.footnote, tone = Tone.Ink2)
                        Chips {
                            catalog.tracks.forEach { t ->
                                val order = student.trackIds.indexOf(t.id)
                                FilterChip(
                                    if (order >= 0) "${order + 1}. ${t.shortName}" else t.shortName,
                                    selected = order >= 0,
                                ) {
                                    vm.setTracks(if (order >= 0) student.trackIds - t.id else (student.trackIds + t.id).takeLast(2))
                                }
                            }
                        }
                    }
                }
            }
        }

        // ---- Dashboard ----
        item {
            Section("홈 화면", "켠 카드가 위에서부터 이 순서로 나와요.") {
                Group {
                    homeCardInfo.forEachIndexed { i, info ->
                        val provider = ModuleRegistry.all.firstOrNull { info.id in it.homeCards }
                        val moduleOff = provider != null && ModuleRegistry.enabled(student).none { it.id == provider.id }
                        ListRow(
                            info.title,
                            if (moduleOff) "${provider!!.title} 기능을 켜면 쓸 수 있어요" else info.detail,
                            divider = i < homeCardInfo.lastIndex,
                            trailing = {
                                Switch(
                                    checked = info.id in student.homeCards,
                                    enabled = !moduleOff,
                                    onCheckedChange = { on ->
                                        val next = if (on) student.homeCards + info.id else student.homeCards - info.id
                                        vm.setHomeCards(homeCardInfo.map { it.id }.filter { it in next })
                                    },
                                    colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
                                )
                            },
                        )
                    }
                }
            }
        }

        // ---- Feature modules ----
        item {
            Section("기능", "켠 기능만 아래 탭과 홈 카드에 나와요.") {
                ModuleList(ModuleRegistry.enabled(student).map { it.id }, vm::setModules)
            }
        }

        // ---- Announcements ----
        item {
            Section("공지") {
                Group {
                    if (majors.isEmpty()) ListRow("내 전공 게시판", "전공을 고르면 전공 공지를 먼저 보여 드려요")
                    majors.forEachIndexed { i, d ->
                        ListRow(
                            "${majorLabel(i)} 게시판",
                            if (d.board != null) "${d.name} 게시판 공지를 모으고 있어요"
                            else "${d.name} 게시판은 아직 모으지 않아요 (학교 공지는 받을 수 있어요)",
                            trailing = { if (d.board != null) Chip("공지 지원", ChipTone.Accent) else Chip("준비 중") },
                        )
                    }
                    ListRow(
                        "새 공지 표시 지우기",
                        "다음에 공지를 열 때까지 새 글 배지를 표시하지 않아요",
                        divider = false,
                        onClick = vm::resetAnnouncementsSeen,
                    )
                }
            }
        }

        // ---- Data ----
        item {
            Section("데이터") {
                Group {
                    ListRow(
                        "서버 연결",
                        when (val s = catalogStatus) {
                            CatalogStatus.Live -> "연결됨 · $apiBaseUrl"
                            CatalogStatus.Loading -> "연결 중… · $apiBaseUrl"
                            is CatalogStatus.Offline -> "${s.message} · 저장된 데이터로 보여 드려요"
                        },
                        trailing = {
                            Txt("다시 연결", AppTheme.type.subhead, Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                                vm.refreshCatalog(); vm.refreshAnnouncements(); vm.refreshCalendar()
                            }, tone = Tone.Accent)
                        },
                    )
                    ListRow(
                        "기기 저장",
                        when (storage) {
                            StorageStatus.Ok -> "학년·이수 체크·담은 과목은 이 기기에만 저장돼요"
                            StorageStatus.Unreadable -> "저장된 기록을 읽지 못했어요 — 홈 화면에서 새로 저장할 수 있어요"
                            StorageStatus.SaveFailed -> "마지막 변경을 저장하지 못했어요"
                        },
                    )
                    ListRow(
                        "이수 기록 되돌리기",
                        "${student.grade}학년 예시 이수 기록으로 바꿔요 (직접 체크한 내용은 사라져요)",
                        divider = false,
                        onClick = { confirmReset = true },
                    )
                }
            }
        }

        // ---- About ----
        item {
            Section("앱 정보") {
                Group {
                    ListRow("버전", APP_VERSION)
                    ListRow("과목·졸업요건·선배 기록", catalog.source.label + if (catalog.source.isSample) " (예시)" else "")
                    ListRow("학사일정", calendar?.source?.label?.let { "$it · ${calendar?.source?.asOf} 기준" } ?: "예시 일정 (서버 연결 전)")
                    ListRow("학과 목록", departments?.source?.label ?: "불러오는 중")
                    ListRow("공지", "학교·LMS·행사·외부 공모·AI전자정보융합대학 학과 게시판 (서버가 30분마다 수집)")
                    ListRow("서체", "Pretendard (SIL Open Font License)", divider = false)
                }
            }
        }
    }

    pendingGrade?.let { g ->
        AlertDialog(
            onDismissRequest = { pendingGrade = null },
            title = { Text("${g}학년으로 바꿀까요?") },
            text = { Text("이수 체크를 ${g}학년 예시 기록으로 바꿀 수 있어요. 바꾸지 않으면 지금 체크한 기록은 그대로 두고 학년만 바꿔요.") },
            confirmButton = {
                TextButton(onClick = { vm.setGrade(g, resetRecords = true); pendingGrade = null }) { Text("예시 기록으로") }
            },
            dismissButton = {
                TextButton(onClick = { vm.setGrade(g, resetRecords = false); pendingGrade = null }) { Text("학년만 바꾸기") }
            },
        )
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("이수 기록을 되돌릴까요?") },
            text = { Text("직접 체크한 이수·담은 과목이 ${student.grade}학년 예시 기록으로 바뀌어요. 되돌릴 수 없어요.") },
            confirmButton = { TextButton(onClick = { vm.resetRecords(); confirmReset = false }) { Text("되돌리기") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("취소") } },
        )
    }
}

private fun Modifier.padding16() = this.padding(horizontal = Space.l, vertical = Space.m)

@Composable
private fun BackButton(onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(AppTheme.colors.surface).clickable(role = Role.Button, onClickLabel = "뒤로", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로", tint = AppTheme.colors.ink, modifier = Modifier.size(18.dp)) }
}
