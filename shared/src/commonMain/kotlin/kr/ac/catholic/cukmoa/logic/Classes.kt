package kr.ac.catholic.cukmoa.logic

import kr.ac.catholic.cukmoa.model.BucketId
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Course
import kr.ac.catholic.cukmoa.model.OpenClass
import kr.ac.catholic.cukmoa.model.StudentState
import kr.ac.catholic.cukmoa.model.TakenClass

// Links the semester's open classes (GET /api/v1/classes) to catalog courses and to the student's credits.

/** Names compare without spaces and middle dots ("BMCE 개론" = "BMCE개론"). */
private fun key(name: String) = name.filterNot { it.isWhitespace() || it == '·' || it == 'ㆍ' }.lowercase()

/** Same course regardless of the English-taught "Global …" version (Global Career DESIGN = Career DESIGN). */
fun sameCourseKey(name: String): String = key(name.trim().removePrefix("Global ").removePrefix("global "))

/** Catalog course a class belongs to (same name), if any. Checking such a class checks the catalog course, so credits count once. */
fun Catalog.courseFor(k: OpenClass): Course? = courses.firstOrNull { key(it.name) == key(k.name) }

/** This semester's class numbers (분반) of a catalog course. */
fun List<OpenClass>.forCourse(course: Course): List<OpenClass> = filter { key(it.name) == key(course.name) }

/** All class numbers of one subject merged: one row per 학수번호 in the class picker. */
data class OpenSubject(val subjectNo: String, val classes: List<OpenClass>) {
    val first: OpenClass get() = classes.first()
    val professors: List<String> get() = classes.flatMap { it.professors }.distinct()
}

fun List<OpenClass>.subjects(): List<OpenSubject> = groupBy { it.subjectNo }.map { (no, list) -> OpenSubject(no, list) }

private fun StudentState.majorNames(): List<String> = departments.map { key(it.substringAfter('/')) }

/**
 * Graduation area a class counts toward, from its 1전공 기준 이수구분:
 * liberal categories by name, 전공기초 as 전공기초, the 1st major's 전공 classes as 전공, everything else
 * (other departments' 전공, the 2nd major, 교직 …) as 일반선택.
 */
fun StudentState.bucketOf(k: OpenClass): BucketId = when (k.category) {
    "기초교양필수" -> BucketId.LiberalBasic
    "중핵교양필수", "중핵교양선택필수" -> BucketId.LiberalCore
    "자유선택교양", "교양선택" -> BucketId.LiberalElective
    "전공기초" -> BucketId.MajorBasic
    "제1전공필수", "제1전공선택", "전공필수", "전공선택" ->
        if (majorNames().firstOrNull() == key(k.department)) BucketId.Major else BucketId.General
    else -> BucketId.General
}

fun StudentState.takenClass(k: OpenClass) = TakenClass(
    subjectNo = k.subjectNo,
    name = k.name,
    credits = k.credits ?: 0,
    bucket = bucketOf(k),
    foreignLanguage = k.foreignLanguage != null,
)

fun StudentState.hasTaken(subjectNo: String) = takenClasses.any { it.subjectNo == subjectNo }

/** Foreign-language lecture credits: entered by hand + checked classes taught in a foreign language. */
val StudentState.foreignTotal: Int get() = foreignCredits + takenClasses.filter { it.foreignLanguage }.sumOf { it.credits }

/**
 * A professor as this semester's open classes know them. Until department websites are collected for every
 * department, this is the school-wide professor list; a scraped profile (FacultyMember) adds the details.
 */
data class ClassProfessor(val name: String, val departments: List<String>, val classes: List<OpenClass>)

/** One entry per professor name, by name. Classes without a professor are left out. */
fun List<OpenClass>.professors(): List<ClassProfessor> =
    flatMap { k -> k.professors.map { it to k } }
        .groupBy({ it.first }, { it.second })
        .map { (name, list) -> ClassProfessor(name, list.map { it.department }.distinct(), list.sortedWith(compareBy({ it.name }, { it.classNo }))) }
        .sortedBy { it.name }

/** Scraped profile for a professor name: same name, and a shared department when several have that name. */
fun kr.ac.catholic.cukmoa.model.FacultyList?.profileOf(name: String, departments: List<String>): kr.ac.catholic.cukmoa.model.FacultyMember? {
    val same = this?.items.orEmpty().filter { it.name == name }
    return same.firstOrNull { m -> m.departments.any { d -> departments.any { key(it) == key(d) } } } ?: same.singleOrNull()
}
