package kr.ac.catholic.cukmoa.feature.graduation

import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.runtime.Composable
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

/** Graduation requirements module: completion checklist, credits left per area, comprehensive graduation exam calculation. */
object GraduationModule : FeatureModule {
    override val id = Routes.GRADUATION
    override val title = Res.string.module_graduation
    override val summary = Res.string.module_graduation_summary
    override val icon = Icons.Default.School

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) = GraduationScreen(catalog, student)
}
