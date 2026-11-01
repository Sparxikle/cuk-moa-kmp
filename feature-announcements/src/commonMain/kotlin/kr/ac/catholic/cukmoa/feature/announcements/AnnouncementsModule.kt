package kr.ac.catholic.cukmoa.feature.announcements

import org.jetbrains.compose.resources.stringResource
import kr.ac.catholic.cukmoa.core.resources.*
import kr.ac.catholic.cukmoa.core.ui.text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.ui.EmptyState
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState

/** Announcements module: school, LMS and department board announcements in one place. Depends on the server's collector. */
object AnnouncementsModule : FeatureModule {
    override val id = Routes.ANNOUNCEMENTS
    override val title = Res.string.module_announcements
    override val summary = Res.string.module_announcements_summary
    override val icon = Icons.Default.Campaign
    override val needsNetwork = true
    override val homeCards = setOf(HomeCardId.Announcements)

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) = AnnouncementsScreen(student)

    @Composable
    override fun HomeCard(card: HomeCardId, catalog: Catalog, student: StudentState) {
        val vm = LocalAppViewModel.current
        val nav = LocalNavigator.current
        val state by vm.announcements.collectAsStateWithLifecycle()
        Section(
            title = stringResource(Res.string.new_announcements),
            action = stringResource(Res.string.all) to { vm.markAnnouncementsSeen(); nav.open(Routes.ANNOUNCEMENTS) },
        ) {
            if (state.highlights.isEmpty()) EmptyState(state.error?.text() ?: stringResource(Res.string.loading_announcements))
            else state.highlights.forEach { a -> AnnouncementRow(a, seenAt = null) }
        }
    }
}
