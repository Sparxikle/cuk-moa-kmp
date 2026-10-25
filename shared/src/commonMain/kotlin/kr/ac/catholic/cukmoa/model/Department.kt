package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.Serializable

/**
 * One school department (major). Source: Public Data Portal "National University Department Standard Data" (Korean Council for University Education);
 * the server filters out closed departments and admission units (…계열, …대학, 기타, etc.) and groups the rest by college.
 */
@Serializable
data class Department(
    /** Fixed storage ID: "campus/department name" */
    val id: String,
    /** Department/major name */
    val name: String,
    /** Group name. If there's no college info, the field (공학계열, etc.) or "융복합전공" */
    val college: String,
    /** 성심 · 성의 · 성신 */
    val campus: String,
    /** Board code of a department whose announcements are collected (csie, ai …). null for departments that aren't collected yet */
    val board: String? = null,
    /** Main courses (per-department main courses published by the school) */
    val subjects: List<String> = emptyList(),
    /** Related careers */
    val careers: List<String> = emptyList(),
    /** Admission quota */
    val quota: Int? = null,
    /** Number of graduates (as of the published year) */
    val graduates: Int? = null,
)

@Serializable
data class DepartmentList(
    val source: DataSource,
    val items: List<Department>,
)

/** Departments whose announcements are collected now: department name → board code. Other departments get added in turn. */
val DepartmentBoards: Map<String, String> = linkedMapOf(
    "컴퓨터정보공학부" to "csie",
    "미디어기술콘텐츠학과" to "mtc",
    "데이터사이언스학과" to "datascience",
    "정보통신전자공학부" to "ice",
    "인공지능학과" to "ai",
)

const val AI_COLLEGE = "AI전자정보융합대학"
