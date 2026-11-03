package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.Serializable

/**
 * A career related to a department. The names come from 한국대학교육협의회 (Department.careers);
 * the description, salary and aptitude from CareerNet's job encyclopedia (career.go.kr) when the server has a key.
 */
@Serializable
data class JobInfo(
    /** Job name as the department list gives it */
    val name: String,
    /** 하는 일 (what the job does) */
    val summary: String? = null,
    /** Typical salary as CareerNet states it (e.g. "4,500만원") */
    val wage: String? = null,
    /** Aptitude type (예: 탐구형) */
    val aptitude: String? = null,
    /** Related jobs */
    val related: String? = null,
    /** CareerNet job page */
    val url: String? = null,
    /**
     * CareerNet job the details come from, when its name differs from [name] (a broader job, e.g. 일러스트레이터
     * for 게임일러스트레이터). null when the names are the same or there are no details.
     */
    val matched: String? = null,
)

/** GET /api/v1/jobs?department=성심/경영학과 */
@Serializable
data class JobList(
    val source: DataSource,
    val department: String,
    val items: List<JobInfo>,
)
