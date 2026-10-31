package kr.ac.catholic.cukmoa.feature.timetable

import kr.ac.catholic.cukmoa.core.resources.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.runtime.Composable
import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.core.Routes
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

/** AI timetable maker: 3 checked timetables from this semester's open classes, graduation status and grades. */
object TimetableModule : FeatureModule {
    override val id = Routes.TIMETABLE
    override val title = Res.string.module_timetable
    override val summary = Res.string.module_timetable_summary
    override val icon = Icons.Default.CalendarViewWeek
    override val needsNetwork = true

    @Composable
    override fun Screen(catalog: Catalog, student: StudentState) = TimetableScreen(catalog, student)
}
