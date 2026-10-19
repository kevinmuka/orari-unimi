package app.orariunimi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.time.LocalDate

data class ExamNotificationBaseline(
    val selectionKey: String,
    val appeals: List<ExamAppeal>,
    val missingCounts: Map<String, Int> = emptyMap(),
    val consecutiveEmptyResponses: Int = 0,
    val sourceUpdatedAtMillis: Long = 0
)

class ExamNotificationBaselineStore(context: Context) {
    private val directory = File(context.filesDir, "notifications")
    private val file = File(directory, "exam-baseline.json")

    fun read(): ExamNotificationBaseline? = synchronized(lock) {
        if (!file.isFile) return@synchronized null
        runCatching {
            val root = JSONObject(file.readText())
            val appeals = root.optJSONArray("appeals") ?: JSONArray()
            val missing = root.optJSONObject("missingCounts") ?: JSONObject()
            val hasSourceTimestamp = root.has("sourceUpdatedAtMillis")
            ExamNotificationBaseline(
                selectionKey = root.optString("selectionKey"),
                appeals = (0 until appeals.length()).mapNotNull { appeals.optJSONObject(it)?.toAppeal() },
                // An old counter may have been advanced twice by the same cached response.
                missingCounts = if (hasSourceTimestamp) missing.keys().asSequence()
                    .associateWith { missing.optInt(it) } else emptyMap(),
                consecutiveEmptyResponses = if (hasSourceTimestamp)
                    root.optInt("consecutiveEmptyResponses", 0) else 0,
                sourceUpdatedAtMillis = root.optLong("sourceUpdatedAtMillis", 0)
            )
        }.getOrNull()
    }

    fun write(baseline: ExamNotificationBaseline) = synchronized(lock) {
        directory.mkdirs()
        val appeals = JSONArray().apply { baseline.appeals.forEach { put(it.toJson()) } }
        val missing = JSONObject().apply {
            baseline.missingCounts.forEach { (key, value) -> put(key, value) }
        }
        val root = JSONObject()
            .put("selectionKey", baseline.selectionKey)
            .put("appeals", appeals)
            .put("missingCounts", missing)
            .put("consecutiveEmptyResponses", baseline.consecutiveEmptyResponses)
            .put("sourceUpdatedAtMillis", baseline.sourceUpdatedAtMillis)
        val temporary = File(directory, "exam-baseline.tmp")
        temporary.writeText(root.toString())
        if (!temporary.renameTo(file)) {
            temporary.copyTo(file, overwrite = true)
            temporary.delete()
        }
    }

    fun clear() = synchronized(lock) { file.delete() }

    private fun ExamAppeal.toJson() = JSONObject()
        .put("id", id).put("courseCode", courseCode).put("courseName", courseName)
        .put("subjectCode", subjectCode).put("subjectPortalCode", subjectPortalCode)
        .put("subjectName", subjectName).put("date", date.toString()).put("time", time)
        .put("location", location).put("teacher", teacher).put("surnameFrom", surnameFrom)
        .put("surnameTo", surnameTo).put("testType", testType).put("appealType", appealType)
        .put("registrationOpen", registrationOpen?.toString().orEmpty())
        .put("registrationClose", registrationClose?.toString().orEmpty())

    private fun JSONObject.toAppeal(): ExamAppeal? = runCatching {
        ExamAppeal(
            id = getString("id"),
            courseCode = getString("courseCode"),
            courseName = optString("courseName"),
            subjectCode = optString("subjectCode"),
            subjectPortalCode = optString("subjectPortalCode"),
            subjectName = getString("subjectName"),
            date = LocalDate.parse(getString("date")),
            time = optString("time"),
            location = optString("location"),
            teacher = optString("teacher"),
            surnameFrom = optString("surnameFrom"),
            surnameTo = optString("surnameTo"),
            testType = optString("testType"),
            appealType = optString("appealType"),
            registrationOpen = optString("registrationOpen").takeIf { it.isNotBlank() }
                ?.let(LocalDate::parse),
            registrationClose = optString("registrationClose").takeIf { it.isNotBlank() }
                ?.let(LocalDate::parse)
        )
    }.getOrNull()

    private companion object { val lock = Any() }
}
