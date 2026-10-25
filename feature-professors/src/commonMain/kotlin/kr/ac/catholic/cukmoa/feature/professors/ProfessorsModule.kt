package kr.ac.catholic.cukmoa.feature.professors

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.runtime.Composable
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

/** Professors module: find professors by name, field and course. */
object ProfessorsModule : FeatureModule {
    override val id = Routes.PROFESSORS
    override val title = "교수님"
    override val summary = "관심 분야의 교수님과 담당 과목을 이름·분야·과목으로 검색"
    override val icon = Icons.Default.Group

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) = ProfessorsScreen(catalog, student)
}
