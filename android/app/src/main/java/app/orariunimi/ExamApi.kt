package app.orariunimi

import org.json.JSONArray
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class ExamWindow(val label: String, internal val suffix: String?) {
    TODAY("Oggi", "1"),
    NEXT_30_DAYS("30 giorni", "30"),
    ALL("Tutti", null)
}

data class ExamAppeal(
    val id: String,
    val courseCode: String,
    val courseName: String,
    val subjectCode: String,
    val subjectPortalCode: String,
    val subjectName: String,
    val date: LocalDate,
    val time: String,
    val location: String,
    val teacher: String,
    val surnameFrom: String,
    val surnameTo: String,
    val testType: String,
    val appealType: String,
    val registrationOpen: LocalDate?,
    val registrationClose: LocalDate?
)

data class ExamSnapshot(val appeals: List<ExamAppeal>, val updatedAtMillis: Long)

internal fun mergeSubjectExamSnapshots(
    subject: FollowedExamSubject,
    snapshots: List<Pair<ExamSubjectSource, ExamSnapshot>>
): ExamSnapshot? {
    if (snapshots.isEmpty()) return null
    val appeals = snapshots.flatMap { (source, snapshot) ->
        snapshot.appeals.filter { appeal -> examAppealMatches(subject, source, appeal) }
    }.distinctBy { it.courseCode to it.id }.sortedWith(
        compareBy<ExamAppeal> { it.date }.thenBy { it.time.ifBlank { "99:99" } }
            .thenBy { it.subjectName }
    )
    return ExamSnapshot(appeals, snapshots.minOf { it.second.updatedAtMillis })
}

internal fun examAppealMatches(
    subject: FollowedExamSubject,
    source: ExamSubjectSource,
    appeal: ExamAppeal
): Boolean {
    if (!appeal.courseCode.equals(source.courseCode, ignoreCase = true)) return false
    val sourceCode = source.examSubjectCode.trim()
    if (appeal.subjectCode.equals(sourceCode, ignoreCase = true) ||
        appeal.subjectPortalCode.equals(sourceCode, ignoreCase = true)) return true
    return appeal.subjectName.trim().equals(subject.name.trim(), ignoreCase = true)
}

class ExamApi(
    private val baseUrl: String = "https://work.unimi.it/foProssimiEsami/json/",
    private val cache: ResponseCache? = null,
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val transport: ((courseCode: String, window: ExamWindow) -> String)? = null,
    private val loadObserver: ((cacheHit: Boolean) -> Unit)? = null
) {
    private companion object {
        const val OFFLINE_MAX_AGE = 24L * 60 * 60 * 1000
        const val FRESH_MAX_AGE = 2L * 60 * 1000
        val requestLocks = java.util.concurrent.ConcurrentHashMap<String, Any>()
    }

    fun cachedAppeals(courseCode: String, window: ExamWindow): ExamSnapshot? {
        val entry = cache?.readEntry(cacheKey(courseCode, window), OFFLINE_MAX_AGE) ?: return null
        return ExamSnapshot(parseExamAppeals(entry.value, courseCode), entry.storedAtMillis)
    }

    fun refreshAppeals(courseCode: String, window: ExamWindow): ExamSnapshot {
        return loadAppeals(courseCode, window, force = true)
    }

    fun refreshAppealsIfStale(courseCode: String, window: ExamWindow): ExamSnapshot =
        loadAppeals(courseCode, window, force = false)

    private fun loadAppeals(courseCode: String, window: ExamWindow, force: Boolean): ExamSnapshot {
        val key = cacheKey(courseCode, window)
        val startedAt = clockMillis()
        if (!force) cache?.readEntry(key, FRESH_MAX_AGE)?.let { entry ->
            loadObserver?.invoke(true)
            return ExamSnapshot(parseExamAppeals(entry.value, courseCode), entry.storedAtMillis)
        }
        val lock = requestLocks.computeIfAbsent(key) { Any() }
        return synchronized(lock) {
            val existing = cache?.readEntry(key, OFFLINE_MAX_AGE)
            if (existing != null) {
                val age = (clockMillis() - existing.storedAtMillis).coerceAtLeast(0)
                if (existing.storedAtMillis >= startedAt || !force && age <= FRESH_MAX_AGE) {
                    loadObserver?.invoke(true)
                    return@synchronized ExamSnapshot(
                        parseExamAppeals(existing.value, courseCode), existing.storedAtMillis)
                }
            }
            val response = fetch(courseCode, window)
            val storedAt = cache?.write(key, response) ?: clockMillis()
            loadObserver?.invoke(false)
            ExamSnapshot(parseExamAppeals(response, courseCode), storedAt)
        }
    }

    private fun cacheKey(courseCode: String, window: ExamWindow): String =
        "GET|$baseUrl${courseCode.trim().uppercase()}/${window.suffix.orEmpty()}"

    private fun fetch(courseCode: String, window: ExamWindow): String {
        require(courseCode.matches(Regex("[A-Za-z0-9]+"))) { "Codice del corso non valido." }
        transport?.let { return it(courseCode, window) }
        val path = buildString {
            append(courseCode.uppercase())
            window.suffix?.let { append('/').append(it) }
        }
        val connection = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000
            connection.useCaches = false
            connection.setRequestProperty("User-Agent", "orari-unimi-android/1")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Cache-Control", "no-cache")
            if (connection.responseCode == HttpURLConnection.HTTP_NOT_FOUND) return "[]"
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Il servizio appelli UNIMI ha risposto ${connection.responseCode}.")
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 16 * 1024 * 1024) {
                        throw IllegalStateException("La risposta degli appelli è troppo grande.")
                    }
                    output.write(buffer, 0, count)
                }
            }
            return output.toString(StandardCharsets.UTF_8.name())
        } finally {
            connection.disconnect()
        }
    }
}

internal fun parseExamAppeals(body: String, courseCode: String): List<ExamAppeal> {
    val dateFormat = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    val groups = JSONArray(body)
    return (0 until groups.length()).flatMap { groupIndex ->
        val group = groups.optJSONObject(groupIndex) ?: return@flatMap emptyList()
        val subjectCode = group.optString("codIns").trim()
        val portalCode = group.optString("codW4").trim()
        val subjectName = group.optString("descrIns").trim()
        val sessions = group.optJSONArray("appelli") ?: JSONArray()
        (0 until sessions.length()).mapNotNull { sessionIndex ->
            val session = sessions.optJSONObject(sessionIndex) ?: return@mapNotNull null
            val date = runCatching { LocalDate.parse(session.optString("dataStr"), dateFormat) }.getOrNull()
                ?: return@mapNotNull null
            val teacher = session.optJSONObject("docente")?.let { docente ->
                listOf(docente.optString("cognome").trim(), docente.optString("nome").trim())
                    .filter { it.isNotBlank() }.joinToString(" ")
            }.orEmpty()
            val id = session.optString("idAppello").trim().ifBlank {
                "$subjectCode|$date|${session.optString("ora")}|$teacher|$sessionIndex"
            }
            ExamAppeal(
                id = id,
                courseCode = courseCode.uppercase(),
                courseName = session.optString("descrCor").trim(),
                subjectCode = subjectCode,
                subjectPortalCode = portalCode,
                subjectName = subjectName.ifBlank { "Insegnamento" },
                date = date,
                time = session.optString("ora").trim(),
                location = session.optString("luogo").trim(),
                teacher = teacher,
                surnameFrom = session.optString("rangeDa").trim(),
                surnameTo = session.optString("rangeA").trim(),
                testType = session.optString("prova").trim(),
                appealType = session.optString("tipoAppello").trim(),
                registrationOpen = session.optString("aperturaStr").takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it, dateFormat) }.getOrNull() },
                registrationClose = session.optString("chiusuraStr").takeIf { it.isNotBlank() }
                    ?.let { runCatching { LocalDate.parse(it, dateFormat) }.getOrNull() }
            )
        }
    }.distinctBy { it.id }.sortedWith(
        compareBy<ExamAppeal> { it.date }.thenBy { it.time.ifBlank { "99:99" } }.thenBy { it.subjectName }
    )
}
