package app.orariunimi

import org.json.JSONArray
import org.json.JSONObject

/** Small, process-safe description of the visible page and its parents. No lesson data is saved. */
internal data class NavigationBookmark(
    val course: CourseRoute? = null,
    val calendar: CalendarRoute? = null,
    val exam: ExamRoute? = null,
    val appeal: AppealRoute? = null,
    val buildingCode: String? = null,
    val roomCode: String? = null,
    val buildingDay: String? = null,
    val roomDay: String? = null,
    val notificationId: String? = null,
    val notificationFromCenter: Boolean = false,
    val subjectSelection: SubjectRoute? = null,
    val settings: Boolean = false,
    val notificationSettings: Boolean = false,
    val notifications: Boolean = false,
    val agenda: Boolean = false,
    val lessonMonth: Boolean = false,
    val examPlanner: Boolean = false,
    val examPlannerSavedMode: Boolean = false,
    val examSearch: Boolean = false
) {
    val hasPage: Boolean get() = course != null || calendar != null || exam != null ||
        appeal != null || buildingCode != null || notificationId != null || settings ||
        notificationSettings || notifications || agenda || lessonMonth || examPlanner || examSearch ||
        subjectSelection != null

    fun encode(): String = JSONObject().apply {
        course?.let { put("course", JSONObject().put("year", it.year).put("code", it.code)) }
        calendar?.let { put("calendar", JSONObject().put("title", it.title).put("year", it.year)
            .put("kind", it.kind?.name).put("code", it.code).put("name", it.name)
            .put("combined", it.combined)) }
        exam?.let { put("exam", JSONObject().put("year", it.year).put("code", it.code)
            .put("name", it.name).put("kind", it.kind.name).put("window", it.window.name)
            .put("fromCourse", it.fromCourse).put("sources", JSONArray().apply {
                it.sources.forEach { source -> put(JSONObject().put("course", source.courseCode)
                    .put("subject", source.examSubjectCode)) }
            })) }
        appeal?.let { put("appeal", JSONObject().put("course", it.courseCode).put("id", it.id)
            .put("available", it.available)) }
        buildingCode?.let { put("building", it) }
        roomCode?.let { put("room", it) }
        buildingDay?.let { put("buildingDay", it) }
        roomDay?.let { put("roomDay", it) }
        notificationId?.let { put("notification", it) }
        put("notificationFromCenter", notificationFromCenter)
        subjectSelection?.let { put("subjectSelection", JSONObject().put("year", it.year)
            .put("code", it.code).put("name", it.name)) }
        put("settings", settings)
        put("notificationSettings", notificationSettings)
        put("notifications", notifications)
        put("agenda", agenda)
        put("lessonMonth", lessonMonth)
        put("examPlanner", examPlanner)
        put("examPlannerSavedMode", examPlannerSavedMode)
        put("examSearch", examSearch)
    }.toString()

    companion object {
        fun decode(value: String): NavigationBookmark? = runCatching {
            val json = JSONObject(value)
            val course = json.optJSONObject("course")?.let {
                CourseRoute(it.getString("year"), it.getString("code"))
            }
            val calendar = json.optJSONObject("calendar")?.let {
                CalendarRoute(it.getString("title"), it.getString("year"),
                    it.optString("kind").takeIf(String::isNotBlank)?.let(SearchKind::valueOf),
                    it.optString("code"), it.optString("name"), it.optBoolean("combined"))
            }
            val exam = json.optJSONObject("exam")?.let {
                val rawSources = it.optJSONArray("sources") ?: JSONArray()
                val sources = (0 until rawSources.length()).mapNotNull { index ->
                    rawSources.optJSONObject(index)?.let { source ->
                        ExamSubjectSource(source.getString("course"), source.getString("subject"))
                    }
                }
                ExamRoute(it.getString("year"), it.getString("code"), it.getString("name"),
                    SearchKind.valueOf(it.getString("kind")), ExamWindow.valueOf(it.getString("window")),
                    it.optBoolean("fromCourse"), sources)
            }
            val appeal = json.optJSONObject("appeal")?.let {
                AppealRoute(it.getString("course"), it.getString("id"), it.optBoolean("available", true))
            }
            val subjectSelection = json.optJSONObject("subjectSelection")?.let {
                SubjectRoute(it.getString("year"), it.getString("code"), it.getString("name"))
            }
            NavigationBookmark(
                course = course,
                calendar = calendar,
                exam = exam,
                appeal = appeal,
                buildingCode = json.optString("building").takeIf(String::isNotBlank),
                roomCode = json.optString("room").takeIf(String::isNotBlank),
                buildingDay = json.optString("buildingDay").takeIf(String::isNotBlank),
                roomDay = json.optString("roomDay").takeIf(String::isNotBlank),
                notificationId = json.optString("notification").takeIf(String::isNotBlank),
                notificationFromCenter = json.optBoolean("notificationFromCenter"),
                subjectSelection = subjectSelection,
                settings = json.optBoolean("settings"),
                notificationSettings = json.optBoolean("notificationSettings"),
                notifications = json.optBoolean("notifications"),
                agenda = json.optBoolean("agenda"),
                lessonMonth = json.optBoolean("lessonMonth"),
                examPlanner = json.optBoolean("examPlanner"),
                examPlannerSavedMode = json.optBoolean("examPlannerSavedMode"),
                examSearch = json.optBoolean("examSearch")
            )
        }.getOrNull()
    }
}

internal data class CourseRoute(val year: String, val code: String)
internal data class CalendarRoute(
    val title: String, val year: String, val kind: SearchKind?,
    val code: String, val name: String, val combined: Boolean
)
internal data class ExamRoute(
    val year: String, val code: String, val name: String, val kind: SearchKind,
    val window: ExamWindow, val fromCourse: Boolean,
    val sources: List<ExamSubjectSource> = emptyList()
)
internal data class AppealRoute(val courseCode: String, val id: String, val available: Boolean)
internal data class SubjectRoute(val year: String, val code: String, val name: String)
