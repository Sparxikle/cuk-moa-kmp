package kr.ac.catholic.cukmoa.model

import kotlinx.serialization.Serializable

/**
 * Major roadmap officially published by the department (department website course/roadmap.do).
 * One table cell = one course. Keeps the track (major area), semester and marks (graduation requirement, recommended, etc.) together.
 * Departments that publish only an image instead of a table have empty entries and only imageUrl.
 */
@Serializable
data class MajorRoadmap(
    /** Department board code (csie, ai …) */
    val major: String,
    val department: String,
    /** Roadmap page on the department website */
    val url: String,
    /** Image address when there's no table, only an image */
    val imageUrl: String? = null,
    /** Mark abbreviation → meaning (e.g. "졸" → "졸업요건과목") */
    val legend: Map<String, String> = emptyMap(),
    val entries: List<MajorRoadmapEntry> = emptyList(),
    /** Note about the data (e.g. data transcribed from an image) */
    val note: String? = null,
)

@Serializable
data class MajorRoadmapEntry(
    /** Track / major area (기초역량 과정, 핵심과정, 미디어공학트랙 …) */
    val track: String,
    val semester: SemesterKey? = null,
    /** Course name (exactly as written on the department website) */
    val name: String,
    /** Meanings of the marks (e.g. ["졸업요건과목"]) */
    val marks: List<String> = emptyList(),
    /** Departments that give only the year, not the semester (정보통신전자공학부), store just the year */
    val year: Int? = null,
) {
    /** Year (from the semester if there is one) */
    val yearLevel: Int get() = semester?.year ?: year ?: 0

    /** For sorting and grouping: 4-2 → 42, 4th year → 40 */
    val order: Int get() = yearLevel * 10 + (semester?.term ?: 0)

    /** Display: "3학년 1학기" or "3학년" */
    val periodLabel: String get() = semester?.label ?: "${yearLevel}학년"
}

@Serializable
data class MajorRoadmapList(
    val source: DataSource,
    val items: List<MajorRoadmap>,
)

/** For comparing course names: removes differences in spacing, middle dots and Roman numerals */
fun courseKey(name: String): String = name
    .replace(Regex("\\s+"), "")
    .replace('·', '.').replace('ㆍ', '.').replace('・', '.')
    .replace("Ⅰ", "I").replace("Ⅱ", "II").replace("Ⅲ", "III").replace("Ⅳ", "IV").replace("Ⅴ", "V")
    .lowercase()
