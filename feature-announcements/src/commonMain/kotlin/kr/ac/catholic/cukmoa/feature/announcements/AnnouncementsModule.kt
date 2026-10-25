package kr.ac.catholic.cukmoa.feature.announcements

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.LocalNavigator
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.core.theme.AppTheme
import kr.ac.catholic.cukmoa.core.ui.Card
import kr.ac.catholic.cukmoa.core.ui.Group
import kr.ac.catholic.cukmoa.core.ui.Section
import kr.ac.catholic.cukmoa.core.ui.Tone
import kr.ac.catholic.cukmoa.core.ui.Txt
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.HomeCardId
import kr.ac.catholic.cukmoa.model.StudentState

/** Announcements module: school, LMS and department board announcements in one place. Depends on the server's collector. */
object AnnouncementsModule : FeatureModule {
    override val id = Routes.ANNOUNCEMENTS
    override val title = "공지"
    override val summary = "학교·LMS·학과 게시판 18곳의 공지를 내 전공에 맞춰 한곳에서"
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
            title = "새 공지",
            detail = if (state.newCount > 0) "마지막 확인 후 ${state.newCount}개 새 글" else "내 전공 게시판 최신 글",
            action = "전체" to { vm.markAnnouncementsSeen(); nav.open(Routes.ANNOUNCEMENTS) },
        ) {
            if (state.highlights.isEmpty()) {
                Card { Txt(state.error ?: "공지를 불러오는 중이에요.", AppTheme.type.callout, tone = Tone.Ink2) }
            } else Group {
                state.highlights.forEachIndexed { i, a ->
                    AnnouncementRow(a, divider = i < state.highlights.lastIndex, seenAt = null)
                }
            }
        }
    }
}
