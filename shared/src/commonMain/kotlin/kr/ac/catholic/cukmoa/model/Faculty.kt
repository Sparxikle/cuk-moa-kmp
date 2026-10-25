package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.Serializable

/**
 * One professor. Source: each department website's faculty page (currently the 5 departments of the College of AI, Electronics & Information Convergence).
 * A professor listed in several departments (joint appointment) is merged into one, with all departments in majors.
 * Contact details (phone/email) are not stored in the app; it links to the department website profile (profileUrl).
 */
@Serializable
data class FacultyMember(
    /** School faculty number (persNo) */
    val id: String,
    val name: String,
    val nameEn: String? = null,
    /** Professor · associate professor · assistant professor … */
    val position: String,
    /** Administrative role such as department chair */
    val role: String? = null,
    /** Board codes of the departments listing this professor (csie, ai …) */
    val majors: List<String>,
    /** Affiliated department name */
    val departments: List<String>,
    /** Office location */
    val office: String? = null,
    /** Research areas */
    val field: String? = null,
    /** Research keywords */
    val keywords: List<String> = emptyList(),
    /** Professor bio */
    val intro: String? = null,
    /** Department website profile (contact, education, publications) */
    val profileUrl: String,
)

@Serializable
data class FacultyList(
    val source: DataSource,
    val items: List<FacultyMember>,
)
