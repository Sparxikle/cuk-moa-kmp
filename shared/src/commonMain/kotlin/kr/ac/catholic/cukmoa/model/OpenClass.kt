package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.Serializable

/**
 * One class (분반) opened this semester. Source: the school's 개설과목 조회 (needs a school login, so the export is
 * imported into the server by hand once a semester). The same subject appears once per class number.
 */
@Serializable
data class OpenClass(
    /** 2026 */
    val year: Int,
    /** 1 · 2 = regular semesters; other codes are kept as the school sends them */
    val semester: String,
    /** 학수번호 (sbjtNo) */
    val subjectNo: String,
    /** 분반 (clssNo) */
    val classNo: String,
    /** 과목명 (sbjtKorNm) */
    val name: String,
    /** 개설전공 (sustNm) */
    val department: String,
    /** 1전공 기준 이수구분 (pobtFgNm): 제1전공선택 · 기초교양필수 · 전공기초 … */
    val category: String,
    /** 이수학년 (pobtGrade). Not set for classes open to every year */
    val grade: Int? = null,
    /** 외국어 강의 (orgLangLtYnChk): "B형(영)" … null = Korean */
    val foreignLanguage: String? = null,
    /** 담당 교수 (respProfNm) */
    val professors: List<String> = emptyList(),
    /** 주역량/핵심능력 (corsTemp2Nm) */
    val competency: String? = null,
    /** 수강인원 (tlsnAplyRcnt) */
    val enrolled: Int? = null,
    /** 시간 (pntCnt "4/3" → 4) */
    val hours: Int? = null,
    /** 학점 (pntCnt "4/3" → 3) */
    val credits: Int? = null,
    /** Meeting times (tmTblDesc "화5~6(M306), 목5~6(M306)"). Empty = time not set or online */
    val times: List<ClassTime> = emptyList(),
)

/** One weekly meeting: day "월"…"일", periods start..end (1교시 = 09:00, 1 hour each), room "M306" */
@Serializable
data class ClassTime(
    val day: String,
    val start: Int,
    val end: Int,
    val room: String? = null,
)

/** GET /api/v1/classes */
@Serializable
data class OpenClassList(
    val source: DataSource,
    val year: Int,
    val semester: String,
    val items: List<OpenClass>,
)
