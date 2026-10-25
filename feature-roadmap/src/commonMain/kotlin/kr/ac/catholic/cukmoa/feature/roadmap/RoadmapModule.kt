package kr.ac.catholic.cukmoa.feature.roadmap

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.LocalAppViewModel
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

/** Roadmap module: course order from 1st to 4th year taken by seniors in each career track. */
object RoadmapModule : FeatureModule {
    override val id = Routes.ROADMAP
    override val title = "로드맵"
    override val summary = "진로별로 선배들이 몇 학기에 무엇을 들었는지, 학기별 수강 비율과 선배 이야기"
    override val icon = Icons.Default.Route

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) {
        val requested by LocalAppViewModel.current.roadmapTrack.collectAsStateWithLifecycle()
        RoadmapScreen(catalog, student, requested)
    }
}
