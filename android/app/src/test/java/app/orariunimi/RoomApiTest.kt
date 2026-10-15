package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.nio.file.Files
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.atomic.AtomicInteger

class RoomApiTest {
    private val room = UnimiRoom("24010#S037", "100", "24010", "Celoria", 158)
    private val date = LocalDate.of(2026, 10, 15)

    @Test fun catalogSearchesLocallyByAulaAndSede() = withCache { cache ->
        val calls = AtomicInteger()
        val api = RoomApi(cache, transport = { path, encoded, get ->
            assertEquals("combo.php", path)
            assertEquals("sw=rooms_", encoded)
            assertTrue(get)
            calls.incrementAndGet()
            catalogResponse
        })
        val catalog = api.catalog()
        val index = RoomSearchIndex(catalog)
        assertEquals(room, index.search("aula 100 celoria").single())
        assertEquals(room, index.search("24010#s037").single())
        assertTrue(index.search("AULA 101 CELORIA").isEmpty())
        api.catalog()
        assertEquals(1, calls.get())
    }

    @Test fun planningUsesOneRoomAndIgnoresCancelledEventsForAvailability() = withCache { cache ->
        var form = ""
        val api = RoomApi(cache, transport = { path, encoded, get ->
            assertEquals("rooms_call.php", path)
            assertFalse(get)
            form = URLDecoder.decode(encoded, "UTF-8")
            planningResponse
        })
        val planning = api.planning(room, date)
        assertTrue(form.contains("sede[]=24010"))
        assertTrue(form.contains("aula[]=24010#S037"))
        assertTrue(form.contains("date=15-10-2026"))
        assertEquals(2, planning.events.size)
        assertEquals("Lezione", planning.currentEvent(LocalDateTime.of(2026, 10, 15, 9, 0))?.type)
        assertEquals(null, planning.currentEvent(LocalDateTime.of(2026, 10, 15, 11, 0)))
        assertEquals(null, planning.nextEvent(LocalDateTime.of(2026, 10, 15, 11, 0)))
        assertEquals(1_792_050_003_000L, planning.sourceUpdatedAtMillis)
    }

    @Test fun cachedPlanningSurvivesNetworkFailureButIsMarkedOffline() = withCache { cache ->
        var fail = false
        val api = RoomApi(cache, transport = { _, _, _ ->
            if (fail) throw IllegalStateException("rete non disponibile")
            planningResponse
        })
        api.planning(room, date)
        fail = true
        val fallback = api.planning(room, date, force = true)
        assertTrue(fallback.offline)
        assertEquals(2, fallback.events.size)
    }

    private fun withCache(block: (ResponseCache) -> Unit) {
        val directory = Files.createTempDirectory("room-api-test").toFile()
        try { block(ResponseCache(directory)) } finally { directory.deleteRecursively() }
    }

    private val catalogResponse = """
        var elenco_sedi = [{"label":"Celoria","valore":"24010"}];
        var elenco_aule = {"24010":[{"label":"100","valore":"24010#S037",
            "sede_name":"Celoria","sede_code":"24010","capacity":158}]};
    """.trimIndent()

    private val planningResponse = """
        {"rooms":{"24010#S037":{"nome":"100"}},"file_timestamp":1792050003,
         "events":[
            {"id":"1","CodiceAula":"24010#S037","name":"Analisi",
             "type":"Lezione","from":"08:30:00","to":"10:30:00","Annullato":"0"},
            {"id":"2","CodiceAula":"24010#S037","name":"Impegno cancellato",
             "type":"Lezione","from":"10:30:00","to":"12:30:00","Annullato":"1"}]}
    """.trimIndent()
}
