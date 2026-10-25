package kr.ac.catholic.cukmoa.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Navigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.state.AppViewModel
import kr.ac.catholic.cukmoa.core.state.Platform
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.CourseSheet
import kr.ac.catholic.cukmoa.core.ui.ProfessorSheet

private sealed interface Sheet {
    data class Course(val id: String) : Sheet
    data class Professor(val id: String) : Sheet
}

private data class TabItem(val route: String, val label: String, val icon: ImageVector)

/**
 * App shell: onboarding → bottom tabs (dashboard + enabled modules) → detail sheets.
 * The shell knows nothing about module internals. Tab screens and home cards are drawn by the modules in ModuleRegistry.
 */
@Composable
fun App(platform: Platform, initialRoute: String = Routes.HOME) {
    val vm = viewModel { AppViewModel(platform) }
    AppTheme {
        val catalog by vm.catalog.collectAsStateWithLifecycle()
        val student by vm.student.collectAsStateWithLifecycle()
        val announcements by vm.announcements.collectAsStateWithLifecycle()
        val modules = ModuleRegistry.enabled(student)
        val currentModules by rememberUpdatedState(modules)

        var route by rememberSaveable { mutableStateOf(initialRoute) }
        var sheet by remember { mutableStateOf<Sheet?>(null) }
        var editing by remember { mutableStateOf<Boolean?>(null) } // null: closed, true: start from the home cards step
        var managing by rememberSaveable { mutableStateOf(initialRoute == Routes.MODULES) }

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
                editProfile = { editing = it },
                manageModules = { managing = true },
                isEnabled = ::enabled,
            )
        }

        // Only the background color shows during the brief moment while the bundled catalog is read.
        val c = catalog ?: return@AppTheme Box(Modifier.fillMaxSize().background(AppTheme.colors.bg))

        CompositionLocalProvider(LocalNavigator provides navigator, LocalAppViewModel provides vm) {
            if (!student.onboarded || editing != null) {
                OnboardingScreen(
                    catalog = c,
                    student = student,
                    editing = student.onboarded,
                    startAtCards = editing == true,
                    onClose = { editing = null },
                )
                return@CompositionLocalProvider
            }
            if (managing) {
                ModulesScreen(modules.map { it.id }, vm::setModules, onBack = { managing = false })
                return@CompositionLocalProvider
            }

            // If the current tab's module was turned off, go home
            if (route != Routes.HOME && modules.none { it.id == route }) route = Routes.HOME

            val tabs = listOf(TabItem(Routes.HOME, "한눈에", Icons.Default.GridView)) +
                modules.map { TabItem(it.id, it.title, it.icon) }

            Scaffold(
                containerColor = AppTheme.colors.bg,
                bottomBar = {
                    NavigationBar(containerColor = AppTheme.colors.surface) {
                        tabs.forEach { t ->
                            NavigationBarItem(
                                selected = route == t.route,
                                onClick = { navigator.open(t.route) },
                                icon = {
                                    if (t.route == Routes.ANNOUNCEMENTS && announcements.newCount > 0) {
                                        BadgedBox(badge = { Badge { Text("${announcements.newCount}") } }) { Icon(t.icon, null) }
                                    } else Icon(t.icon, null)
                                },
                                label = { Text(t.label, style = AppTheme.type.caption) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = AppTheme.colors.accent,
                                    selectedTextColor = AppTheme.colors.accent,
                                    indicatorColor = AppTheme.colors.accentSoft,
                                    unselectedIconColor = AppTheme.colors.ink3,
                                    unselectedTextColor = AppTheme.colors.ink3,
                                ),
                            )
                        }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    val module = ModuleRegistry.byId(route)
                    if (module == null) HomeScreen(c, student, modules) else module.Screen(c, student)
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
