package app.orariunimi

import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class UnimiRoom(
    val code: String,
    val name: String,
    val buildingCode: String,
    val building: String,
    val capacity: Int?
)

data class RoomEvent(
    val id: String,
    val title: String,
    val type: String,
    val start: LocalTime,
    val end: LocalTime,
    val cancelled: Boolean
)

data class RoomPlanning(
    val roomCode: String,
    val date: LocalDate,
    val events: List<RoomEvent>,
    val sourceUpdatedAtMillis: Long,
    val cachedAtMillis: Long,
    val offline: Boolean = false
) {
    fun currentEvent(now: LocalDateTime): RoomEvent? =
        if (now.toLocalDate() != date) null else events.firstOrNull {
            !it.cancelled && !now.toLocalTime().isBefore(it.start) && now.toLocalTime().isBefore(it.end)
        }

    fun nextEvent(now: LocalDateTime): RoomEvent? =
        if (now.toLocalDate() != date) null else events.firstOrNull {
            !it.cancelled && it.start.isAfter(now.toLocalTime())
        }
}

/** Search is entirely local; opening an aula is the first planning request. */
class RoomSearchIndex(rooms: List<UnimiRoom>) {
    private val indexed = rooms.map { room ->
        room to normalized("aula ${room.name} ${room.building} ${room.code}")
    }

    fun search(query: String, limit: Int = 50): List<UnimiRoom> {
        val words = normalized(query).split(' ').filter(String::isNotBlank)
        if (words.isEmpty()) return emptyList()
        return indexed.asSequence().filter { (_, text) -> words.all(text::contains) }
            .take(limit).map { it.first }.toList()
    }

    private fun normalized(value: String): String = Normalizer.normalize(
        value.lowercase(Locale.ITALIAN), Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "").replace(Regex("\\s+"), " ").trim()
}

/** Public AgendaWeb room catalog and daily planning; no request is made per search result. */
class RoomApi(
    private val cache: ResponseCache,
    private val baseUrl: String = "https://orari-be.divsi.unimi.it/AgendaWeb/Orario/",
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val transport: ((path: String, encoded: String, get: Boolean) -> String)? = null
) {
    private val dateFormat = DateTimeFormatter.ofPattern("dd-MM-yyyy")

    private companion object {
        const val CATALOG_FRESH = 24L * 60 * 60 * 1000
        const val CATALOG_OFFLINE = 14L * 24 * 60 * 60 * 1000
        const val TODAY_FRESH = 5L * 60 * 1000
        const val OTHER_DAY_FRESH = 60L * 60 * 1000
        const val PLANNING_OFFLINE = 24L * 60 * 60 * 1000
    }

    fun catalog(force: Boolean = false): List<UnimiRoom> {
        val key = "rooms|catalog|$baseUrl"
        if (!force) cache.read(key, CATALOG_FRESH)?.let { body ->
            runCatching { parseCatalog(body) }.getOrNull()?.let { return it }
        }
        return try {
            val response = fetch("combo.php", "sw=rooms_", get = true)
            parseCatalog(response).also { cache.write(key, response) }
        } catch (cause: Exception) {
            cache.read(key, CATALOG_OFFLINE)?.let { parseCatalog(it) } ?: throw cause
        }
    }

    fun cachedPlanning(room: UnimiRoom, date: LocalDate): RoomPlanning? =
        cache.readEntry(planningKey(room, date), PLANNING_OFFLINE)?.let {
            parsePlanning(it.value, room, date, it.storedAtMillis, offline = true)
        }

    fun planning(room: UnimiRoom, date: LocalDate, force: Boolean = false): RoomPlanning {
        val key = planningKey(room, date)
        val freshAge = if (date == LocalDate.now(java.time.ZoneId.of("Europe/Rome"))) TODAY_FRESH
            else OTHER_DAY_FRESH
        if (!force) cache.readEntry(key, freshAge)?.let { entry ->
            runCatching { parsePlanning(entry.value, room, date, entry.storedAtMillis) }
                .getOrNull()?.let { return it }
        }
        val fields = listOf(
            "form-type" to "rooms", "view" to "rooms", "include" to "rooms",
            "sede[]" to room.buildingCode, "aula[]" to room.code,
            "date" to date.format(dateFormat), "_lang" to "it"
        )
        return try {
            val response = fetch("rooms_call.php", encode(fields), get = false)
            val parsed = parsePlanning(response, room, date, clockMillis())
            val storedAt = cache.write(key, response) ?: clockMillis()
            parsed.copy(cachedAtMillis = storedAt)
        } catch (cause: Exception) {
            cache.readEntry(key, PLANNING_OFFLINE)?.let {
                parsePlanning(it.value, room, date, it.storedAtMillis, offline = true)
            } ?: throw cause
        }
    }

    private fun planningKey(room: UnimiRoom, date: LocalDate) =
        "rooms|planning|$baseUrl|${room.code}|$date"

    private fun parseCatalog(body: String): List<UnimiRoom> {
        val buildings = JSONArray(extractAgendaVariable(body, "elenco_sedi"))
        val buildingNames = (0 until buildings.length()).mapNotNull { index ->
            buildings.optJSONObject(index)?.let { it.optString("valore") to it.optString("label") }
        }.toMap()
        val groups = JSONObject(extractAgendaVariable(body, "elenco_aule"))
        return groups.keys().asSequence().flatMap { buildingCode ->
            val rooms = groups.optJSONArray(buildingCode) ?: JSONArray()
            (0 until rooms.length()).asSequence().mapNotNull { index ->
                val value = rooms.optJSONObject(index) ?: return@mapNotNull null
                val code = value.optString("valore")
                val name = value.optString("label")
                if (code.isBlank() || name.isBlank()) return@mapNotNull null
                val codeOfBuilding = value.optString("sede_code").ifBlank { buildingCode }
                UnimiRoom(
                    code, name, codeOfBuilding,
                    value.optString("sede_name").ifBlank { buildingNames[codeOfBuilding].orEmpty() },
                    value.optInt("capacity").takeIf { it > 0 }
                )
            }
        }.distinctBy { it.code }.sortedWith(compareBy<UnimiRoom> { it.building.lowercase(Locale.ITALIAN) }
            .thenBy { it.name.lowercase(Locale.ITALIAN) }).toList()
    }

    private fun parsePlanning(
        body: String, room: UnimiRoom, date: LocalDate, cachedAt: Long, offline: Boolean = false
    ): RoomPlanning {
        val response = JSONObject(body)
        if (!response.optJSONObject("rooms").let { it != null && it.has(room.code) }) {
            throw IllegalStateException("Il planning dell'aula non è disponibile.")
        }
        val values = response.optJSONArray("events") ?: JSONArray()
        val events = (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            if (value.optString("CodiceAula") != room.code) return@mapNotNull null
            val start = runCatching { LocalTime.parse(value.optString("from")) }.getOrNull()
                ?: return@mapNotNull null
            val end = runCatching { LocalTime.parse(value.optString("to")) }.getOrNull()
                ?: return@mapNotNull null
            if (!end.isAfter(start)) return@mapNotNull null
            RoomEvent(
                id = value.optString("id"),
                title = value.optString("name").ifBlank { "Impegno in aula" },
                type = value.optString("type"),
                start = start,
                end = end,
                cancelled = value.optString("Annullato") == "1"
            )
        }.sortedWith(compareBy<RoomEvent> { it.start }.thenBy { it.end })
        return RoomPlanning(room.code, date, events,
            sourceUpdatedAtMillis = response.optLong("file_timestamp").takeIf { it > 0 }
                ?.times(1000) ?: cachedAt,
            cachedAtMillis = cachedAt,
            offline = offline)
    }

    private fun encode(fields: List<Pair<String, String>>): String = fields.joinToString("&") { (key, value) ->
        "${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value, "UTF-8")}"
    }

    private fun fetch(path: String, encoded: String, get: Boolean): String {
        transport?.let { return it(path, encoded, get) }
        val connection = URL(baseUrl + path + if (get) "?$encoded" else "").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "orari-unimi-android/1.9")
            connection.setRequestProperty("Accept", if (get) "application/javascript, application/json" else "application/json")
            if (!get) {
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                connection.outputStream.use { it.write(encoded.toByteArray(StandardCharsets.UTF_8)) }
            }
            if (connection.responseCode !in 200..299) {
                throw IllegalStateException("Il portale UNIMI ha risposto ${connection.responseCode}.")
            }
            val output = ByteArrayOutputStream()
            connection.inputStream.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    if (output.size() + count > 4 * 1024 * 1024) {
                        throw IllegalStateException("Risposta aule UNIMI troppo grande.")
                    }
                    output.write(buffer, 0, count)
                }
            }
            return output.toString("UTF-8")
        } finally {
            connection.disconnect()
        }
    }
}
