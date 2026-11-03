package kr.ac.catholic.cukmoa.app

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import org.jetbrains.compose.resources.StringResource
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kr.ac.catholic.cukmoa.app.ui.HomeScreen
import kr.ac.catholic.cukmoa.app.ui.ModulesScreen
import kr.ac.catholic.cukmoa.app.ui.OnboardingScreen
import kr.ac.catholic.cukmoa.app.ui.SettingsScreen
import kr.ac.catholic.cukmoa.core.ui.AppBackHandler
import kr.ac.catholic.cukmoa.core.ui.screenPop
import kr.ac.catholic.cukmoa.core.ui.tabSlide
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Navigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.state.AppViewModel
import kr.ac.catholic.cukmoa.core.state.Platform
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.CourseSheet
import kr.ac.catholic.cukmoa.core.ui.ProfessorSheet
import kr.ac.catholic.cukmoa.model.ThemeMode

private sealed interface Sheet {
    data class Course(val id: String) : Sheet
    data class Professor(val id: String) : Sheet
}

private data class TabItem(val route: String, val label: StringResource, val icon: ImageVector)

private enum class AppScreen(val depth: Int) { Onboarding(0), Main(1), Settings(2), Modules(2) }

/** Bottom tabs + tab screens. Switching tabs slides left or right according to tab order. */
@Composable
private fun MainTabs(
    catalog: kr.ac.catholic.cukmoa.model.Catalog,
    student: kr.ac.catholic.cukmoa.model.StudentState,
    modules: List<kr.ac.catholic.cukmoa.core.FeatureModule>,
    route: String,
    newCount: Long,
    onTab: (String) -> Unit,
) {
    val tabs = listOf(TabItem(Routes.HOME, Res.string.tab_home, Icons.Default.GridView)) + modules.map { TabItem(it.id, it.title, it.icon) }
    fun index(r: String) = tabs.indexOfFirst { it.route == r }.coerceAtLeast(0)

    Scaffold(
        containerColor = AppTheme.colors.bg,
        bottomBar = { TabBar(tabs, route, newCount, onTab) },
    ) { padding ->
        // Each screen (ScreenList) handles the top inset (status bar) itself → only reserve the bottom tab height.
        AnimatedContent(
            targetState = route,
            modifier = Modifier.fillMaxSize().background(AppTheme.colors.bg).padding(bottom = padding.calculateBottomPadding()),
            transitionSpec = { tabSlide(forward = index(targetState) > index(initialState)) },
            label = "tab",
        ) { r ->
            val module = ModuleRegistry.byId(r)
            if (module == null) HomeScreen(catalog, student, modules) else module.Screen(catalog, student)
        }
    }
}

/**
 * LG UX bottom tab bar (Dialer, Clock): 75dp, equal tabs, a 40dp icon over a 12sp label, no indicator pill.
 * Selection shows only by color — accent and bold for the selected tab, 40% black/white for the others — and the
 * newly selected icon plays a short pop (LG plays a Lottie once; the old icon just goes grey). Colors swap instantly.
 */
@Composable
private fun TabBar(tabs: List<TabItem>, route: String, newCount: Long, onTab: (String) -> Unit) {
    val c = AppTheme.colors
    val off = c.ink.copy(alpha = 0.4f)
    // LG pads tabs 4dp + 8dp; with five tabs (home + 4 modules) the labels need the room
    val crowded = tabs.size > 4
    Row(Modifier.fillMaxWidth().background(c.bg).navigationBarsPadding().height(75.dp).padding(horizontal = 4.dp)) {
        tabs.forEach { t ->
            val selected = route == t.route
            val color = if (selected) c.accent else off
            // Pop only when a tab becomes selected, not for the first tab at start
            val pop = remember { Animatable(1f) }
            var started by remember { mutableStateOf(false) }
            LaunchedEffect(selected) {
                if (selected && started) {
                    pop.snapTo(0.55f)
                    pop.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 260f))
                } else pop.snapTo(1f)
                started = true
            }
            Box(Modifier.weight(1f).fillMaxHeight().padding(horizontal = if (crowded) 1.dp else 4.dp), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.fillMaxWidth().height(61.dp).clip(RoundedCornerShape(25.dp))
                        .selectable(selected = selected, role = Role.Tab, onClick = { onTab(t.route) })
                        .padding(horizontal = if (crowded) 2.dp else 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(36.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }, contentAlignment = Alignment.Center) {
                        if (t.route == Routes.ANNOUNCEMENTS && newCount > 0) {
                            BadgedBox(badge = { Badge { Text("$newCount") } }) { Icon(t.icon, null, Modifier.size(28.dp), tint = color) }
                        } else Icon(t.icon, null, Modifier.size(28.dp), tint = color)
                    }
                    Text(
                        stringResource(t.label),
                        color = color,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            letterSpacing = 0.sp,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

/**
 * App shell: onboarding → bottom tabs (dashboard + enabled modules) → detail sheets.
 * The shell knows nothing about module internals. Tab screens and home cards are drawn by the modules in ModuleRegistry.
 */
@Composable
fun App(platform: Platform, initialRoute: String = Routes.HOME, onDarkTheme: (Boolean) -> Unit = {}, onRestart: () -> Unit = {}) {
    val vm = viewModel { AppViewModel(platform) }
    val student by vm.student.collectAsStateWithLifecycle()
    val dark = when (student.theme) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    // The platform styles the system bars (status bar icon color) from the app's theme, not the system's
    LaunchedEffect(dark) { onDarkTheme(dark) }
    kr.ac.catholic.cukmoa.app.i18n.AppLocale(student.language) {
    AppTheme(dark = dark, dynamic = vm.dynamicScheme?.takeIf { student.dynamicColor }?.invoke(dark)) {
        val catalog by vm.catalog.collectAsStateWithLifecycle()
        val announcements by vm.announcements.collectAsStateWithLifecycle()
        val modules = ModuleRegistry.enabled(student)
        val currentModules by rememberUpdatedState(modules)

        var route by rememberSaveable { mutableStateOf(initialRoute) }
        var sheet by remember { mutableStateOf<Sheet?>(null) }
        var managing by rememberSaveable { mutableStateOf(initialRoute == Routes.MODULES) }
        var settings by rememberSaveable { mutableStateOf(initialRoute == Routes.SETTINGS) }

        val navigator = remember {
            fun enabled(id: String) = currentModules.any { it.id == id }
            Navigator(
                open = { r ->
                    if (r == Routes.HOME || enabled(r)) {
                        route = r
                        if (r == Routes.ANNOUNCEMENTS) vm.markAnnouncementsSeen()
                    } else managing = true // Module turned off → prompt to turn it on
                },
                openCourse = { sheet = Sheet.Course(it) },
                openProfessor = { sheet = Sheet.Professor(it) },
                openRoadmap = { track ->
                    vm.requestRoadmap(track)
                    if (enabled(Routes.ROADMAP)) route = Routes.ROADMAP else managing = true
                },
                openSettings = { settings = true },
                manageModules = { managing = true },
                isEnabled = ::enabled,
            )
        }

        // Only the background color shows during the brief moment while the bundled catalog is read.
        val c = catalog ?: return@AppTheme Box(Modifier.fillMaxSize().background(AppTheme.colors.bg))

        CompositionLocalProvider(LocalNavigator provides navigator, LocalAppViewModel provides vm) {
            // Screen to show now. Depth decides the open/close direction: onboarding 0 → main 1 → settings/modules 2
            val screen = when {
                !student.onboarded -> AppScreen.Onboarding
                settings -> AppScreen.Settings
                managing -> AppScreen.Modules
                else -> AppScreen.Main
            }
            // Paint the background so the window's white backdrop doesn't show through while resizing
            AnimatedContent(
                targetState = screen,
                modifier = Modifier.fillMaxSize().background(AppTheme.colors.bg),
                transitionSpec = { screenPop(forward = targetState.depth >= initialState.depth) },
                label = "screen",
            ) { current ->
                when (current) {
                    AppScreen.Onboarding ->
                        OnboardingScreen(catalog = c, student = student, editing = false, startAtCards = false, onClose = {})
                    AppScreen.Settings ->
                        SettingsScreen(c, student, platform.apiBaseUrl, onBack = { settings = false }, onRestart = onRestart)
                    AppScreen.Modules -> {
                        AppBackHandler { managing = false }
                        ModulesScreen(modules.map { it.id }, vm::setModules, onBack = { managing = false })
                    }
                    AppScreen.Main -> {
                        // Back: on another tab, go home; on home, close the app (system default)
                        AppBackHandler(enabled = route != Routes.HOME && sheet == null) { route = Routes.HOME }
                        // If the current tab's module was turned off, go home
                        if (route != Routes.HOME && modules.none { it.id == route }) route = Routes.HOME
                        MainTabs(c, student, modules, route, announcements.newCount, onTab = { navigator.open(it) })
                    }
                }
            }

            when (val s = sheet) {
                is Sheet.Course -> CourseSheet(c, student, s.id, onDismiss = { sheet = null })
                is Sheet.Professor -> ProfessorSheet(c, s.id, onDismiss = { sheet = null })
                null -> Unit
            }
        }
    }
    }
}
