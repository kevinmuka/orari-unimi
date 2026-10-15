package app.orariunimi

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
    val capacity: Int?,
    val address: String = ""
)

data class UnimiBuilding(
    val code: String,
    val name: String,
    val address: String,
    val rooms: List<UnimiRoom>
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

data class BuildingPlanning(
    val buildingCode: String,
    val date: LocalDate,
    val coveredRooms: Set<String>,
    val eventsByRoom: Map<String, List<RoomEvent>>,
    val sourceUpdatedAtMillis: Long,
    val cachedAtMillis: Long,
    val offline: Boolean = false
) {
    fun forRoom(room: UnimiRoom): RoomPlanning? =
        if (room.code !in coveredRooms) null else RoomPlanning(
            room.code, date, eventsByRoom[room.code].orEmpty(), sourceUpdatedAtMillis,
            cachedAtMillis, offline
        )
}

/** Search is entirely local and always returns buildings, even for a room-name match. */
class RoomSearchIndex(rooms: List<UnimiRoom>) {
    val buildings = rooms.groupBy { it.buildingCode }.map { (code, inBuilding) ->
        UnimiBuilding(code, inBuilding.first().building,
            inBuilding.firstOrNull { it.address.isNotBlank() }?.address.orEmpty(),
            inBuilding.sortedBy { it.name.lowercase(Locale.ITALIAN) })
    }.sortedBy { it.name.lowercase(Locale.ITALIAN) }
    private val indexed = buildings.map { building ->
        val location = "${building.name} ${building.address} ${building.code}"
        IndexedBuilding(building, normalized(location), building.rooms.map { room ->
            normalized("aula ${room.name} ${room.code} ${building.name}")
        })
    }

    fun search(query: String): List<UnimiBuilding> {
        val words = normalized(query).split(' ').filter(String::isNotBlank)
        if (words.isEmpty()) return buildings
        val byLocation = indexed.filter { matches(it.location, words) }
        if (byLocation.isNotEmpty()) return byLocation.map { it.building }
        return indexed.filter { entry -> entry.rooms.any { matches(it, words) } }
            .map { it.building }
    }

    private fun matches(text: String, words: List<String>): Boolean {
        val tokens = text.split(' ')
        return words.all { word ->
            if (word.all(Char::isDigit)) word in tokens else text.contains(word)
        }
    }

    private fun normalized(value: String): String = Normalizer.normalize(
        value.lowercase(Locale.ITALIAN), Normalizer.Form.NFD
    ).replace(Regex("\\p{M}+"), "")
        .replace(Regex("[^\\p{L}\\p{N}#]+"), " ").trim()

    private data class IndexedBuilding(
        val building: UnimiBuilding,
        val location: String,
        val rooms: List<String>
    )
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
        val key = "rooms|catalog-v2|$baseUrl"
        if (!force) cache.read(key, CATALOG_FRESH)?.let { body ->
            runCatching { parseCatalog(body) }.getOrNull()?.let { return it }
        }
        return try {
            // Without sede[] AgendaWeb sends metadata for every building, but no events.
            val response = fetch("rooms_call.php", encode(roomFields(LocalDate.now(
                java.time.ZoneId.of("Europe/Rome")))), get = false)
            parseCatalog(response).also { cache.write(key, response) }
        } catch (cause: Exception) {
            cache.read(key, CATALOG_OFFLINE)?.let { parseCatalog(it) } ?: throw cause
        }
    }

    fun cachedPlanning(room: UnimiRoom, date: LocalDate): RoomPlanning? =
        cachedBuildingPlanning(room.buildingCode, date)?.forRoom(room)
            ?: cache.readEntry(planningKey(room, date), PLANNING_OFFLINE)?.let {
                parsePlanning(it.value, room, date, it.storedAtMillis, offline = true)
            }

    fun cachedBuildingPlanning(buildingCode: String, date: LocalDate): BuildingPlanning? =
        cache.readEntry(buildingKey(buildingCode, date), PLANNING_OFFLINE)?.let { entry ->
            runCatching { parseBuildingPlanning(entry.value, buildingCode, date,
                entry.storedAtMillis, offline = true) }.getOrNull()
        }

    fun buildingPlanning(building: UnimiBuilding, date: LocalDate, force: Boolean = false): BuildingPlanning {
        val key = buildingKey(building.code, date)
        if (!force) cache.readEntry(key, freshAge(date))?.let { entry ->
            runCatching { parseBuildingPlanning(entry.value, building.code, date, entry.storedAtMillis) }
                .getOrNull()?.let { return it }
        }
        val fields = roomFields(date) + ("sede[]" to building.code)
        return try {
            val response = fetch("rooms_call.php", encode(fields), get = false)
            val parsed = parseBuildingPlanning(response, building.code, date, clockMillis())
            parsed.copy(cachedAtMillis = cache.write(key, response) ?: clockMillis())
        } catch (cause: Exception) {
            cachedBuildingPlanning(building.code, date) ?: throw cause
        }
    }

    fun planning(room: UnimiRoom, date: LocalDate, force: Boolean = false): RoomPlanning {
        val key = planningKey(room, date)
        if (!force) {
            cache.readEntry(buildingKey(room.buildingCode, date), freshAge(date))?.let { entry ->
                runCatching { parseBuildingPlanning(entry.value, room.buildingCode, date,
                    entry.storedAtMillis).forRoom(room) }.getOrNull()?.let { return it }
            }
            cache.readEntry(key, freshAge(date))?.let { entry ->
                runCatching { parsePlanning(entry.value, room, date, entry.storedAtMillis) }
                    .getOrNull()?.let { return it }
            }
        }
        val fields = roomFields(date) + listOf("sede[]" to room.buildingCode, "aula[]" to room.code)
        return try {
            val response = fetch("rooms_call.php", encode(fields), get = false)
            val parsed = parsePlanning(response, room, date, clockMillis())
            val storedAt = cache.write(key, response) ?: clockMillis()
            parsed.copy(cachedAtMillis = storedAt)
        } catch (cause: Exception) {
            cache.readEntry(key, PLANNING_OFFLINE)?.let {
                parsePlanning(it.value, room, date, it.storedAtMillis, offline = true)
            } ?: cachedBuildingPlanning(room.buildingCode, date)?.forRoom(room) ?: throw cause
        }
    }

    private fun roomFields(date: LocalDate) = listOf(
        "form-type" to "rooms", "view" to "rooms", "include" to "rooms",
        "date" to date.format(dateFormat), "_lang" to "it"
    )

    private fun freshAge(date: LocalDate) =
        if (date == LocalDate.now(java.time.ZoneId.of("Europe/Rome"))) TODAY_FRESH else OTHER_DAY_FRESH

    private fun buildingKey(buildingCode: String, date: LocalDate) =
        "rooms|building|$baseUrl|$buildingCode|$date"

    private fun planningKey(room: UnimiRoom, date: LocalDate) =
        "rooms|planning|$baseUrl|${room.code}|$date"

    private fun parseCatalog(body: String): List<UnimiRoom> {
        val groups = JSONObject(body).optJSONObject("area_rooms")
            ?: throw IllegalStateException("Il catalogo delle aule non è disponibile.")
        return groups.keys().asSequence().flatMap { buildingCode ->
            val rooms = groups.optJSONObject(buildingCode) ?: JSONObject()
            rooms.keys().asSequence().mapNotNull { roomCode ->
                val value = rooms.optJSONObject(roomCode) ?: return@mapNotNull null
                val name = value.optString("room_name")
                if (name.isBlank()) return@mapNotNull null
                UnimiRoom(roomCode, name, buildingCode, value.optString("area"),
                    value.optInt("capacity").takeIf { it > 0 },
                    if (value.isNull("address")) "" else value.optString("address"))
            }
        }.distinctBy { it.code }.sortedWith(compareBy<UnimiRoom> { it.building.lowercase(Locale.ITALIAN) }
            .thenBy { it.name.lowercase(Locale.ITALIAN) }).toList().also {
                if (it.isEmpty()) throw IllegalStateException("Il catalogo delle aule è vuoto.")
            }
    }

    private fun parseBuildingPlanning(
        body: String, buildingCode: String, date: LocalDate, cachedAt: Long, offline: Boolean = false
    ): BuildingPlanning {
        val response = JSONObject(body)
        val selected = response.optJSONObject("search_data")?.optJSONArray("sede")
        if (selected == null || (0 until selected.length()).none { selected.optString(it) == buildingCode }) {
            throw IllegalStateException("Il planning dell'edificio non è disponibile.")
        }
        val rooms = response.optJSONObject("all_rooms")
            ?: response.optJSONObject("area_rooms")?.optJSONObject(buildingCode)
            ?: throw IllegalStateException("Le aule dell'edificio non sono disponibili.")
        val covered = rooms.keys().asSequence().toSet()
        val events = parseEvents(response).filterKeys { it in covered }
        return BuildingPlanning(buildingCode, date, covered, events,
            sourceUpdatedAtMillis = sourceTimestamp(response, cachedAt),
            cachedAtMillis = cachedAt, offline = offline)
    }

    private fun parsePlanning(
        body: String, room: UnimiRoom, date: LocalDate, cachedAt: Long, offline: Boolean = false
    ): RoomPlanning {
        val response = JSONObject(body)
        if (!response.optJSONObject("all_rooms").let { it != null && it.has(room.code) }) {
            throw IllegalStateException("Il planning dell'aula non è disponibile.")
        }
        return RoomPlanning(room.code, date, parseEvents(response)[room.code].orEmpty(),
            sourceUpdatedAtMillis = sourceTimestamp(response, cachedAt),
            cachedAtMillis = cachedAt, offline = offline)
    }

    private fun parseEvents(response: JSONObject): Map<String, List<RoomEvent>> {
        val values = response.optJSONArray("events") ?: return emptyMap()
        return (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            val roomCode = value.optString("CodiceAula")
            if (roomCode.isBlank()) return@mapNotNull null
            val start = runCatching { LocalTime.parse(value.optString("from")) }.getOrNull()
                ?: return@mapNotNull null
            val end = runCatching { LocalTime.parse(value.optString("to")) }.getOrNull()
                ?: return@mapNotNull null
            if (!end.isAfter(start)) return@mapNotNull null
            roomCode to RoomEvent(
                id = value.optString("id"),
                title = value.optString("name").ifBlank { "Impegno in aula" },
                type = value.optString("type"),
                start = start,
                end = end,
                cancelled = value.optString("Annullato") == "1"
            )
        }.groupBy({ it.first }, { it.second }).mapValues { (_, events) ->
            events.sortedWith(compareBy<RoomEvent> { it.start }.thenBy { it.end })
        }
    }

    private fun sourceTimestamp(response: JSONObject, cachedAt: Long) =
        response.optLong("file_timestamp").takeIf { it > 0 }?.times(1000) ?: cachedAt

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
