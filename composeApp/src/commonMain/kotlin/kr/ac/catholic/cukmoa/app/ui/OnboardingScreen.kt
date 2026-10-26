package kr.ac.catholic.cukmoa.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kr.ac.catholic.cukmoa.app.ModuleRegistry
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.ui.AppBackHandler
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.theme.Radius
import kr.ac.catholic.cukmoa.core.theme.Space
import kr.ac.catholic.cukmoa.core.ui.BookIcon
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.ListRow
import kr.ac.catholic.cukmoa.core.ui.PrimaryButton
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.core.ui.trackIcon
import kr.ac.catholic.cukmoa.logic.defaultHomeCards
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.Department
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TrackId

private enum class Step { Intro, Grade, Major, Tracks, Modules, Cards }

private val gradeInfo = listOf(
    1 to "학과에서 무엇을 배우는지 알아가는 중",
    2 to "전공 기초를 쌓으며 방향을 고르는 중",
    3 to "진로에 맞춰 과목을 고르는 중",
    4 to "졸업요건을 마무리하는 중",
)

internal data class CardInfo(val id: HomeCardId, val title: String, val detail: String, val icon: ImageVector)

internal val homeCardInfo = listOf(
    CardInfo(HomeCardId.Explore, "진로 둘러보기", "다섯 갈래 진로를 넘겨 보며 비교", Icons.Default.Route),
    CardInfo(HomeCardId.Progress, "졸업까지 남은 학점", "130칸 중 어디까지 채웠는지", Icons.Default.School),
    CardInfo(HomeCardId.Announcements, "새 공지", "학과·학교 공지를 한곳에서", Icons.Default.Campaign),
    CardInfo(HomeCardId.Next, "다음 학기 추천 과목", "관심 진로 선배들이 많이 들은 과목", BookIcon),
    CardInfo(HomeCardId.Saved, "담은 과목", "책갈피로 모아 둔 다음 학기 후보", Icons.Default.Bookmark),
    CardInfo(HomeCardId.Events, "다가오는 학사일정", "시험·신청 기간을 D-day로", Icons.Default.CalendarMonth),
    CardInfo(HomeCardId.Story, "선배 한마디", "같은 진로 선배의 수강 경험", Icons.Default.FormatQuote),
    CardInfo(HomeCardId.Professors, "내 전공 교수님", "내 전공 학과의 교수님과 연구분야", Icons.Default.Group),
)

@Composable
fun OnboardingScreen(catalog: Catalog, student: StudentState, editing: Boolean, startAtCards: Boolean, onClose: () -> Unit) {
    val vm = LocalAppViewModel.current
    val c = AppTheme.colors
    val steps = if (editing) listOf(Step.Grade, Step.Major, Step.Tracks, Step.Modules, Step.Cards) else Step.entries
    var index by remember { mutableIntStateOf(if (startAtCards) steps.indexOf(Step.Cards) else 0) }
    var forward by remember { mutableStateOf(true) }
    var grade by remember { mutableIntStateOf(student.grade) }
    val departments by vm.departments.collectAsStateWithLifecycle()
    var picked by remember { mutableStateOf<List<Department>?>(null) }
    // If nothing has been picked yet, show the stored majors (or board codes)
    val majors = picked ?: departments.findAll(student.departments, student.majors)
    var tracks by remember { mutableStateOf(student.trackIds) }
    var cards by remember { mutableStateOf(student.homeCards) }
    var cardsTouched by remember { mutableStateOf(editing) }
    var modules by remember { mutableStateOf(ModuleRegistry.enabled(student).map { it.id }) }

    val step = steps[index]
    val last = index == steps.lastIndex
    val gradeChanged = grade != student.grade
    fun go(d: Int) { forward = d > 0; index = (index + d).coerceIn(0, steps.lastIndex) }
    fun finish() {
        vm.completeOnboarding(grade, tracks, cards)
        vm.setModules(modules)
        vm.setDepartments(majors)
        onClose()
    }

    AppBackHandler(enabled = index > 0) { go(-1) }

    Column(Modifier.fillMaxSize().background(c.bg).statusBarsPadding().navigationBarsPadding()) {
        // Header: back/close + step indicator
        Row(Modifier.fillMaxWidth().height(52.dp).padding(horizontal = Space.l), verticalAlignment = Alignment.CenterVertically) {
            when {
                index > 0 -> HeaderButton(Icons.AutoMirrored.Filled.ArrowBack, "이전") { go(-1) }
                editing -> HeaderButton(Icons.Default.Close, "닫기", onClose)
                else -> Spacer(Modifier.size(34.dp))
            }
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.Center) {
                val progress = steps.filter { it != Step.Intro }
                if (step != Step.Intro) progress.forEachIndexed { i, s ->
                    Box(
                        Modifier.padding(horizontal = 3.dp).width(28.dp).height(4.dp).clip(RoundedCornerShape(2.dp))
                            .background(if (i <= progress.indexOf(step)) c.accent else c.fillStrong),
                    )
                }
            }
            if (editing && index > 0) HeaderButton(Icons.Default.Close, "닫기", onClose) else Spacer(Modifier.size(34.dp))
        }

        AnimatedContent(
            targetState = step,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val dir = if (forward) 1 else -1
                slideInHorizontally { it * dir } togetherWith slideOutHorizontally { -it * dir }
            },
        ) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = Space.l, vertical = Space.l),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = 560.dp).fillMaxWidth()) {
                    when (s) {
                        Step.Intro -> Intro()
                        Step.Grade -> StepFrame("지금 몇 학년인가요?", "학년에 맞춰 홈에 보일 정보와 추천이 달라져요.") {
                            Group {
                                gradeInfo.forEachIndexed { i, (g, detail) ->
                                    ListRow(
                                        "${g}학년", detail,
                                        divider = i < gradeInfo.lastIndex,
                                        trailing = { if (g == grade) Icon(Icons.Default.Check, "선택됨", tint = c.accent) },
                                        onClick = { grade = g; if (!cardsTouched) cards = defaultHomeCards(g) },
                                    )
                                }
                            }
                            if (gradeChanged || !student.onboarded) {
                                Txt(
                                    if (gradeChanged && student.onboarded) "학년을 바꾸면 이수 체크가 ${grade}학년 예시 기록으로 바뀌어요. 지금까지 체크한 내용은 사라져요."
                                    else "${grade}학년 예시 이수 기록으로 시작해요. 졸업요건 탭에서 직접 체크해 고칠 수 있어요.",
                                    AppTheme.type.footnote,
                                    Modifier.padding(horizontal = Space.xs),
                                    tone = if (gradeChanged && student.onboarded) Tone.Warn else Tone.Ink2,
                                )
                            }
                        }
                        Step.Major -> StepFrame("전공이 무엇인가요?", "단과대학과 상관없이 2개까지 고를 수 있어요 (제1전공 + 복수·부전공). 내 전공 게시판 공지를 먼저 보여 드려요.") {
                            DepartmentPicker(departments, majors) { picked = it }
                        }
                        Step.Tracks -> TracksStep(catalog, tracks, grade) { tracks = it }
                        Step.Modules -> StepFrame("어떤 기능을 쓸까요?", "필요한 기능만 골라 앱을 가볍게 써요. 나중에 설정 → 기능에서 바꿀 수 있어요.") {
                            ModuleList(modules) { modules = it }
                        }
                        Step.Cards -> StepFrame("홈 화면에 무엇을 둘까요?", "고른 정보를 홈에 모아 보여 드려요. 언제든 바꿀 수 있어요.") {
                            Group {
                                homeCardInfo.forEachIndexed { i, info ->
                                    ListRow(
                                        info.title, info.detail,
                                        divider = i < homeCardInfo.lastIndex,
                                        leading = {
                                            Box(Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(c.accentSoft), contentAlignment = Alignment.Center) {
                                                Icon(info.icon, null, tint = c.accent, modifier = Modifier.size(18.dp))
                                            }
                                        },
                                        trailing = {
                                            Switch(
                                                checked = info.id in cards,
                                                onCheckedChange = { on ->
                                                    cardsTouched = true
                                                    val next = if (on) cards + info.id else cards - info.id
                                                    // Display order follows the list order.
                                                    cards = homeCardInfo.map { it.id }.filter { it in next }
                                                },
                                                colors = SwitchDefaults.colors(checkedTrackColor = c.accent),
                                            )
                                        },
                                    )
                                }
                            }
                            if (gradeChanged && student.onboarded) {
                                Txt("완료하면 이수 체크가 ${grade}학년 예시 기록으로 바뀌어요.", AppTheme.type.footnote, tone = Tone.Warn)
                            }
                        }
                    }
                }
            }
        }

        Box(Modifier.fillMaxWidth().padding(horizontal = Space.l, vertical = Space.m), contentAlignment = Alignment.Center) {
            PrimaryButton(
                label = when {
                    step == Step.Intro -> "시작하기"
                    last -> if (editing) "저장" else "내 화면 만들기"
                    else -> "다음"
                },
                onClick = { if (last) finish() else go(1) },
                modifier = Modifier.widthIn(max = 560.dp),
            )
        }
    }
}

@Composable
private fun HeaderButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(34.dp).clip(CircleShape).background(AppTheme.colors.fill).clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = AppTheme.colors.ink, modifier = Modifier.size(16.dp)) }
}

@Composable
private fun StepFrame(title: String, detail: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.xxl)) {
        Column(Modifier.padding(horizontal = Space.xs), verticalArrangement = Arrangement.spacedBy(Space.s)) {
            Txt(title, AppTheme.type.largeTitle.copy(fontSize = 28.sp, lineHeight = 35.sp))
            Txt(detail, AppTheme.type.callout, tone = Tone.Ink2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) { content() }
    }
}

@Composable
private fun Intro() {
    val c = AppTheme.colors
    val points = listOf(
        Triple(Icons.Default.Route, "선배들이 실제로 들은 순서", "진로별로 몇 학기에 무엇을 들었는지"),
        Triple(Icons.Default.School, "졸업까지 남은 학점", "체크할 때마다 바로 다시 계산돼요"),
        Triple(Icons.Default.Campaign, "흩어진 공지를 한곳에", "학교·LMS·학과 게시판 공지를 내 전공에 맞춰"),
        Triple(Icons.Default.Group, "과목과 교수님", "누가 무엇을 가르치는지 한곳에서"),
    )
    Column(Modifier.padding(top = Space.xxl), verticalArrangement = Arrangement.spacedBy(40.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Bookmark, null, tint = c.onAccent, modifier = Modifier.size(22.dp))
            }
            Txt(APP_NAME, AppTheme.type.title)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.m)) {
            Txt("흩어진 학교 정보를\n내 전공에 맞춰\n한곳에", AppTheme.type.largeTitle.copy(fontSize = 34.sp, lineHeight = 42.sp))
            Txt("컴퓨터정보공학부 학생을 위한 수강·진로 길잡이예요.", AppTheme.type.body, tone = Tone.Ink2)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.xl)) {
            points.forEach { (icon, title, detail) ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.l), verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = c.accent, modifier = Modifier.size(26.dp))
                    Column {
                        Txt(title, AppTheme.type.headline)
                        Txt(detail, AppTheme.type.footnote, tone = Tone.Ink2)
                    }
                }
            }
        }
        Txt(
            "졸업요건은 2026학년도 입학자 기준(학사제도 안내)이에요. 다른 학번은 본인 입학년도 기준과 다를 수 있어요.",
            AppTheme.type.caption,
            tone = Tone.Ink3,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TracksStep(catalog: Catalog, selected: List<TrackId>, grade: Int, onChange: (List<TrackId>) -> Unit) {
    val c = AppTheme.colors
    StepFrame(
        "어떤 일에 관심이 있나요?",
        if (grade == 1) "아직 몰라도 괜찮아요. 둘러보면서 바꿀 수 있어요." else "두 개까지 고를 수 있어요. 첫 번째가 추천 기준이 돼요.",
    ) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(Space.m), verticalArrangement = Arrangement.spacedBy(Space.m), maxItemsInEachRow = 2) {
            catalog.tracks.forEach { t ->
                val order = selected.indexOf(t.id)
                TrackTile(
                    selected = order >= 0,
                    background = c.surface,
                    onClick = { onChange(if (order >= 0) selected - t.id else (selected + t.id).takeLast(2)) },
                    modifier = Modifier.weight(1f),
                ) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Icon(trackIcon(t.id), null, tint = if (order >= 0) c.accent else c.ink2, modifier = Modifier.size(24.dp))
                        if (order >= 0) Box(Modifier.size(22.dp).clip(CircleShape).background(c.accent), contentAlignment = Alignment.Center) {
                            Txt("${order + 1}", AppTheme.type.caption, tone = Tone.OnAccent)
                        }
                    }
                    Txt(t.name, AppTheme.type.headline)
                    Txt(t.summary, AppTheme.type.footnote, tone = Tone.Ink2)
                }
            }
            TrackTile(selected = selected.isEmpty(), background = c.fill, onClick = { onChange(emptyList()) }, modifier = Modifier.weight(1f)) {
                Txt("아직 모르겠어요", AppTheme.type.headline)
                Txt("학교 권장 순서로 추천하고, 진로 둘러보기를 먼저 보여 드려요.", AppTheme.type.footnote, tone = Tone.Ink2)
            }
        }
    }
}

@Composable
private fun TrackTile(selected: Boolean, background: Color, onClick: () -> Unit, modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .heightIn(min = 150.dp)
            .clip(RoundedCornerShape(Radius.card - 4.dp))
            .background(background)
            .border(2.dp, if (selected) AppTheme.colors.accent else Color.Transparent, RoundedCornerShape(Radius.card - 4.dp))
            .clickable(role = Role.Checkbox, onClick = onClick)
            .padding(Space.l),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) { content() }
}
