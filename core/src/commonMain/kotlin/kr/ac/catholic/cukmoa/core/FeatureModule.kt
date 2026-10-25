package kr.ac.catholic.cukmoa.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.vector.ImageVector
import kr.ac.catholic.cukmoa.core.state.AppViewModel
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TrackId

/**
 * One app feature = one module (announcements, roadmap, graduation requirements, professors …).
 *
 * For a new feature, create a feature-xxx module, implement this interface, then add one line to ModuleRegistry in composeApp.
 * The app shell (tabs, home, module manager) knows nothing about module internals — modules talk to each other only through core.
 *
 * Users choose which modules to enable (onboarding and the module manager). On Android this can later move to Play Feature Delivery,
 * downloading on enable; on iOS, App Store rules mean modules stay bundled in the app and are just toggled.
 */
interface FeatureModule {
    /** Fixed ID used for storage and routing. Changing it breaks users' settings. */
    val id: String

    /** Name shown on the tab */
    val title: String

    /** One-line description on the module picker */
    val summary: String

    val icon: ImageVector

    /** Whether it needs the server (internet) to work. Shown in the module manager. */
    val needsNetwork: Boolean get() = false

    /** Cards this module can add to home (dashboard) */
    val homeCards: Set<HomeCardId> get() = emptySet()

    /** Tab screen */
    @Composable
    fun Screen(catalog: Catalog, student: StudentState)

    /** Draws a card listed in homeCards */
    @Composable
    fun HomeCard(card: HomeCardId, catalog: Catalog, student: StudentState) {}
}

/** Route ID. Same as the module ID. */
object Routes {
    const val HOME = "home"
    const val ANNOUNCEMENTS = "announcements"
    const val ROADMAP = "roadmap"
    const val GRADUATION = "graduation"
    const val PROFESSORS = "professors"

    /** Module manager screen (a shell screen, not a module) */
    const val MODULES = "modules"
}

/** Navigation actions usable from any screen. Navigating to a disabled module makes the shell open the module manager. */
class Navigator(
    val open: (route: String) -> Unit,
    val openCourse: (String) -> Unit,
    val openProfessor: (String) -> Unit,
    val openRoadmap: (TrackId) -> Unit,
    val editProfile: (startAtCards: Boolean) -> Unit,
    val manageModules: () -> Unit,
    /** Whether a module is enabled (e.g. hide the "로드맵" link when the roadmap is off) */
    val isEnabled: (moduleId: String) -> Boolean,
)

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("App 안에서 써야 해요.") }
val LocalAppViewModel = staticCompositionLocalOf<AppViewModel> { error("App 안에서 써야 해요.") }
