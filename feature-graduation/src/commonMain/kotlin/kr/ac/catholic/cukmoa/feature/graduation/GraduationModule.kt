package kr.ac.catholic.cukmoa.feature.graduation

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
    override val title = "졸업"
    override val summary = "이수 과목을 체크하면 영역별 남은 학점과 졸업시험 과목 수를 바로 다시 계산"
    override val icon = Icons.Default.School

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) = GraduationScreen(catalog, student)
}
