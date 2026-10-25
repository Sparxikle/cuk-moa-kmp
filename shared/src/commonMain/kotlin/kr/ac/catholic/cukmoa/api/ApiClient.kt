package kr.ac.catholic.cukmoa.api

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kr.ac.catholic.cukmoa.logic.catalogProblems
import kr.ac.catholic.cukmoa.model.AnnouncementMeta
import kr.ac.catholic.cukmoa.model.CalendarResponse
import kr.ac.catholic.cukmoa.model.DepartmentList
import kr.ac.catholic.cukmoa.model.FacultyList
import kr.ac.catholic.cukmoa.model.AnnouncementPage
import kr.ac.catholic.cukmoa.model.AnnouncementQuery
import kr.ac.catholic.cukmoa.model.ApiJson
import kr.ac.catholic.cukmoa.model.Catalog
import kr.ac.catholic.cukmoa.model.Course
import kr.ac.catholic.cukmoa.model.ErrorBody

class ApiException(message: String, val status: Int? = null, val code: String? = null) : Exception(message)

/**
 * API client used by the app. Reads responses with the same models as the server (shared/model).
 * All failures are collected into a single ApiException so screens only need to handle "offline".
 */
class ApiClient(baseUrl: String, timeoutMs: Long = 5_000) {
    private val base = baseUrl.trimEnd('/')

    // The engine is picked automatically from the platform dependency (OkHttp/Darwin/Java).
    private val http = HttpClient {
        expectSuccess = false
        install(ContentNegotiation) { json(ApiJson) }
        install(HttpTimeout) { requestTimeoutMillis = timeoutMs }
    }

    suspend fun catalog(): Catalog {
        val catalog = get<Catalog>("/api/v1/catalog")
        if (catalogProblems(catalog).isNotEmpty()) throw ApiException("서버 데이터 형식이 앱과 맞지 않습니다.")
        return catalog
    }

    suspend fun course(id: String): Course = get("/api/v1/courses/$id")

    suspend fun announcements(q: AnnouncementQuery): AnnouncementPage = get("/api/v1/announcements") {
        if (q.source.isNotEmpty()) parameter("source", q.source.joinToString(","))
        if (q.major.isNotEmpty()) parameter("major", q.major.joinToString(","))
        if (q.category.isNotEmpty()) parameter("category", q.category.joinToString(","))
        if (q.board.isNotEmpty()) parameter("board", q.board.joinToString(","))
        q.q?.takeIf { it.isNotBlank() }?.let { parameter("q", it) }
        q.since?.let { parameter("since", it) }
        parameter("pinnedFirst", q.pinnedFirst)
        parameter("limit", q.limit)
        parameter("offset", q.offset)
    }

    suspend fun announcementMeta(): AnnouncementMeta = get("/api/v1/announcements/meta")

    suspend fun departments(): DepartmentList = get("/api/v1/departments")

    suspend fun professors(majors: List<String> = emptyList()): FacultyList = get("/api/v1/professors") {
        if (majors.isNotEmpty()) parameter("major", majors.joinToString(","))
    }

    suspend fun calendar(campus: String = "성심"): CalendarResponse = get("/api/v1/calendar") { parameter("campus", campus) }

    private suspend inline fun <reified T> get(
        path: String,
        crossinline params: io.ktor.client.request.HttpRequestBuilder.() -> Unit = {},
    ): T {
        val response: HttpResponse = try {
            http.get("$base$path") { params() }
        } catch (e: Exception) {
            throw ApiException(
                if (e::class.simpleName?.contains("Timeout") == true) "서버 응답 시간이 초과되었습니다."
                else "서버에 연결하지 못했습니다.",
            )
        }
        if (!response.status.isSuccess()) {
            val error = runCatching { response.body<ErrorBody>().error }.getOrNull()
            throw ApiException(error?.message ?: "API 요청 실패 (${response.status.value})", response.status.value, error?.code)
        }
        return try {
            response.body()
        } catch (e: Exception) {
            throw ApiException("서버 데이터 형식이 앱과 맞지 않습니다.")
        }
    }
}
