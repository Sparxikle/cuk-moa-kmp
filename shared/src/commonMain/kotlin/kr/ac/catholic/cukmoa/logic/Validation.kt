package kr.ac.catholic.cukmoa.logic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.StudentState

// Types are checked by serialization; this checks that "references actually exist".
// The server runs it at startup, tests run it every time. An empty problem list means it passes.

fun catalogProblems(c: Catalog): List<String> {
    val problems = mutableListOf<String>()
    val courses = c.courses.map { it.id }.toSet()
    val professors = c.professors.map { it.id }.toSet()
    val tracks = c.tracks.map { it.id }.toSet()

    if (courses.size != c.courses.size) problems += "duplicate course ids"
    fun course(id: String, where: String) { if (id !in courses) problems += "missing course $id ($where)" }

    c.courses.forEach { co ->
        co.professorIds.forEach { if (it !in professors) problems += "missing professor $it (course ${co.id})" }
        co.prerequisiteIds?.forEach { course(it, "prerequisite of ${co.id}") }
    }
    c.professors.forEach { p -> p.courseIds.forEach { course(it, "professor ${p.id}") } }
    c.tracks.forEach { t -> t.keyCourseIds.forEach { course(it, "track ${t.id}") } }
    c.roadmaps.forEach { r ->
        if (r.trackId !in tracks) problems += "roadmap for unknown track ${r.trackId}"
        r.steps.forEach { s -> s.items.forEach { i -> i.courseId?.let { course(it, "roadmap ${r.trackId} ${s.semester}") } } }
        r.steps.flatMap { it.items }.forEach { if (it.takenRatio !in 0.0..1.0) problems += "takenRatio out of range in ${r.trackId}" }
    }
    c.stories.forEach { s ->
        if (s.trackId !in tracks) problems += "story ${s.id} has unknown track"
        s.courseIds.forEach { course(it, "story ${s.id}") }
    }
    val g = c.graduation
    val plans = g.liberalPlans.map { it.id }.toSet()
    g.liberalPlans.forEach { p -> p.steps.forEach { course(it.courseId, "liberal plan ${p.id}") } }
    if (g.departments.map { it.name }.toSet().size != g.departments.size) problems += "duplicate department rules"
    (g.departments + g.fallback).forEach { d ->
        if (d.liberalPlan !in plans) problems += "unknown liberal plan ${d.liberalPlan} (${d.name})"
        (d.majorBasicCourseIds + d.liberalElectiveCourseIds).forEach { course(it, "rules ${d.name}") }
        d.test?.exam?.subjectCourseIds?.forEach { course(it, "exam subjects ${d.name}") }
        d.test?.groups?.flatMap { it.options }?.let { opts ->
            if (opts.map { it.id }.toSet().size != opts.size) problems += "duplicate test option ids (${d.name})"
            if (opts.any { it.exam } && d.test.exam == null) problems += "exam option without exam rules (${d.name})"
        }
        if (d.major > d.majorDeep || d.majorRequired > d.major) problems += "major credits out of order (${d.name})"
    }
    return problems
}

fun parseCatalog(text: String): Catalog {
    val catalog = ApiJson.decodeFromString(Catalog.serializer(), text)
    val problems = catalogProblems(catalog)
    require(problems.isEmpty()) { "catalog has broken references: ${problems.joinToString()}" }
    return catalog
}

/**
 * Reads the stored student state. Older formats are converted to the latest format.
 * null if unreadable — callers must not overwrite the original and should ask the user to reset.
 */
fun decodeStudentState(text: String): StudentState? = runCatching {
    var obj = Json.parseToJsonElement(text).jsonObject
    val version = (obj["schemaVersion"] as? JsonPrimitive)?.int ?: 1
    if (version > StudentState.CURRENT_SCHEMA_VERSION) return null // Value saved by a newer app
    for (v in version until StudentState.CURRENT_SCHEMA_VERSION) {
        obj = migrations.getValue(v)(obj)
    }
    ApiJson.decodeFromJsonElement(StudentState.serializer(), obj)
}.getOrNull()

fun encodeStudentState(state: StudentState): String = ApiJson.encodeToString(StudentState.serializer(), state)

/** v → v+1 conversions. Add one line here whenever the format changes. */
private val migrations: Map<Int, (JsonObject) -> JsonObject> = mapOf(
    // v1 → v2: single major (major, department) → up to 2 majors (majors, departments)
    1 to { old ->
        val major = (old["major"] as? JsonPrimitive)?.contentOrNull
        val department = (old["department"] as? JsonPrimitive)?.contentOrNull
        JsonObject(
            old - "major" - "department" + mapOf(
                "schemaVersion" to JsonPrimitive(2),
                "majors" to JsonArray(listOfNotNull(major).map(::JsonPrimitive)),
                "departments" to JsonArray(listOfNotNull(department).map(::JsonPrimitive)),
            ),
        )
    },
    // v2 → v3: the single "liberal" bucket split into 기초교양 · 중핵교양 · 교양선택; old liberal credits go to 중핵교양
    2 to { old ->
        val credits = (old["otherCredits"] as? JsonObject).orEmpty()
        val moved = credits - "liberal" + credits["liberal"]?.let { mapOf("liberal-core" to it) }.orEmpty()
        JsonObject(old + mapOf("schemaVersion" to JsonPrimitive(3), "otherCredits" to JsonObject(moved)))
    },
)
