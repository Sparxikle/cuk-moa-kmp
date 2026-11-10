package kr.ac.catholic.cukmoa.logic

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.StudentState

/**
 * Backup file (Settings → Data → Export): the student's own records only.
 *
 * ```
 * {"type": "cuk-moa-backup", "version": 1, "exportedAt": "2026-11-10T09:00:00Z",
 *  "student": {"schemaVersion": 3, "grade": 3, "completedCourseIds": [...], ...}}
 * ```
 *
 * Left out: anything that comes from the server (announcement board codes, the announcements "seen" mark) and
 * app settings (theme, language, colors, home cards, modules), which stay as they are on the importing device.
 * "student" keeps its schemaVersion, so files from older app versions go through the same migrations as stored state.
 */
const val BACKUP_TYPE = "cuk-moa-backup"
const val BACKUP_VERSION = 1
const val BACKUP_EXTENSION = "json"

/** StudentState fields written to a backup. Everything else is kept from the device on import. */
private val BACKUP_FIELDS = setOf(
    "schemaVersion",
    "grade",
    "currentSemester",
    "departments",
    "trackIds",
    "careerInterests",
    "completedCourseIds",
    "inProgressCourseIds",
    "savedCourseIds",
    "takenClasses",
    "grades",
    "otherCredits",
    "foreignCredits",
    "exam",
    "graduationChecks",
    "timetable",
)

private val BackupJson = Json(ApiJson) { prettyPrint = true }

fun encodeBackup(state: StudentState, exportedAt: String): String {
    val student = ApiJson.encodeToJsonElement(StudentState.serializer(), state).jsonObject.filterKeys { it in BACKUP_FIELDS }
    val root = JsonObject(
        mapOf(
            "type" to JsonPrimitive(BACKUP_TYPE),
            "version" to JsonPrimitive(BACKUP_VERSION),
            "exportedAt" to JsonPrimitive(exportedAt),
            "student" to JsonObject(student),
        ),
    )
    return BackupJson.encodeToString(JsonObject.serializer(), root)
}

/**
 * The records in [text] applied over [current]: the backup's fields replace the device's, the rest (settings) is kept.
 * null if the file isn't a backup, comes from a newer app, or can't be read.
 */
fun decodeBackup(text: String, current: StudentState): StudentState? = runCatching {
    val root = Json.parseToJsonElement(text).jsonObject
    if ((root["type"] as? JsonPrimitive)?.contentOrNull != BACKUP_TYPE) return null
    if (((root["version"] as? JsonPrimitive)?.int ?: return null) > BACKUP_VERSION) return null
    // Older records are brought to the latest format first, then only the record fields are taken
    val records = migrateStudentJson(root["student"] as? JsonObject ?: return null)?.filterKeys { it in BACKUP_FIELDS } ?: return null
    val base = ApiJson.encodeToJsonElement(StudentState.serializer(), current).jsonObject
    val imported = ApiJson.decodeFromJsonElement(StudentState.serializer(), JsonObject(base + records))
    imported.copy(
        onboarded = true,
        // Board codes come from the server: cleared here and filled in again from the department list
        majors = emptyList(),
        announcementsSeenAt = current.announcementsSeenAt,
        homeCards = current.homeCards,
        modules = current.modules,
        theme = current.theme,
        language = current.language,
        dynamicColor = current.dynamicColor,
    )
}.getOrNull()

/** Suggested file name: cuk-moa-backup-2026-11-10 (the extension is added by the save dialog) */
fun backupFileName(exportedAt: String): String = "$BACKUP_TYPE-${exportedAt.take(10)}"
