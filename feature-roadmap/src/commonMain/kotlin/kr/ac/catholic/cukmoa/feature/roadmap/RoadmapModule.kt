package kr.ac.catholic.cukmoa.feature.roadmap

import kr.ac.catholic.cukmoa.core.ui.AnimatedIcon
import kr.ac.catholic.cukmoa.core.resources.*
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
    override val title = Res.string.module_roadmap
    override val summary = Res.string.module_roadmap_summary
    override val icon = Icons.Default.Route
    override val animatedIcon = AnimatedIcon.Roadmap

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) {
        val requested by LocalAppViewModel.current.roadmapTrack.collectAsStateWithLifecycle()
        RoadmapScreen(catalog, student, requested)
    }
}
