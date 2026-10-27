package kr.ac.catholic.cukmoa.logic

// The catalog's five career tracks (web, backend, AI, security, embedded) and their senior roadmaps were written
// for 컴퓨터정보공학부. Other majors pick interests from their department's 관련 직업 (Department.careers) instead.

private val CATALOG_TRACK_MAJORS = setOf("컴퓨터정보공학부")

/** Whether the catalog's career tracks apply to this 1st major (department name). */
fun usesCatalogTracks(firstMajor: String?): Boolean = firstMajor in CATALOG_TRACK_MAJORS
