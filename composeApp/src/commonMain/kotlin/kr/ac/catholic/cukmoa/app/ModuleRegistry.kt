package kr.ac.catholic.cukmoa.app

import kr.ac.catholic.cukmoa.core.FeatureModule
import kr.ac.catholic.cukmoa.feature.announcements.AnnouncementsModule
import kr.ac.catholic.cukmoa.feature.graduation.GraduationModule
import kr.ac.catholic.cukmoa.feature.professors.ProfessorsModule
import kr.ac.catholic.cukmoa.feature.roadmap.RoadmapModule
import kr.ac.catholic.cukmoa.model.StudentState

/**
 * Feature modules known to the app. Add new modules here with one line.
 * Order = tab order when first enabled.
 */
object ModuleRegistry {
    val all: List<FeatureModule> = listOf(
        RoadmapModule,
        GraduationModule,
        ProfessorsModule,
        AnnouncementsModule,
    )

    /** Bottom tabs are home + up to 4 modules */
    const val MAX_TABS = 4

    fun byId(id: String): FeatureModule? = all.firstOrNull { it.id == id }

    /** Modules the user enabled (all if nothing is stored). IDs not in the current app are skipped. */
    fun enabled(student: StudentState): List<FeatureModule> =
        (student.modules ?: all.map { it.id }).mapNotNull { byId(it) }
}
