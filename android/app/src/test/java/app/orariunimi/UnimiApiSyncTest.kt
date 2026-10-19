package app.orariunimi

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.nio.file.Files
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class UnimiApiSyncTest {
    @Test fun savedSubjectsAreGroupedByAcademicYear() = withCache { directory ->
        val requests = Collections.synchronizedList(mutableListOf<Map<String, List<String>>>())
        val api = UnimiApi(cache = ResponseCache(directory), transport = { path, encoded, get ->
            assertEquals("grid_call.php", path)
            assertEquals(false, get)
            val form = formValues(encoded)
            requests += form
            response(form.getValue("attivita[]"))
        })

        val snapshot = api.refreshSavedLessons(listOf(
            SavedSubject("2025", "B", "B"),
            SavedSubject("2025", "A", "A"),
            SavedSubject("2024", "C", "C")
        ))

        assertEquals(2, requests.size)
        assertEquals(setOf("2024", "2025"), requests.map { it.getValue("anno").single() }.toSet())
        assertEquals(listOf("A", "B"), requests.single {
            it.getValue("anno").single() == "2025"
        }.getValue("attivita[]"))
        assertEquals(setOf("A", "B", "C"), snapshot.lessons.map { it.subjectCode }.toSet())
    }

    @Test fun automaticRefreshReusesOnlyVeryRecentData() = withCache { directory ->
        var now = 1_000_000L
        val calls = AtomicInteger()
        val cacheEvents = mutableListOf<Boolean>()
        val cache = ResponseCache(directory, clockMillis = { now })
        val api = UnimiApi(cache = cache, clockMillis = { now }, transport = { _, encoded, _ ->
            calls.incrementAndGet()
            response(formValues(encoded).getValue("attivita[]"))
        }, scheduleLoadObserver = cacheEvents::add)
        val saved = listOf(SavedSubject("2025", "A", "A"))

        api.refreshSavedLessons(saved)
        now += 119_000L
        api.refreshSavedLessonsIfStale(saved)
        assertEquals(1, calls.get())

        now += 2_000L
        api.refreshSavedLessonsIfStale(saved)
        assertEquals(2, calls.get())

        now += 1L
        api.refreshSavedLessons(saved)
        assertEquals(3, calls.get())
        assertEquals(listOf(false, true, false, false), cacheEvents)
    }

    @Test fun concurrentForcedRefreshesShareOneNetworkResponse() = withCache { directory ->
        val now = 2_000_000L
        val calls = AtomicInteger()
        val enteredTransport = CountDownLatch(1)
        val releaseTransport = CountDownLatch(1)
        val cache = ResponseCache(directory, clockMillis = { now })
        val api = UnimiApi(cache = cache, clockMillis = { now }, transport = { _, encoded, _ ->
            calls.incrementAndGet()
            enteredTransport.countDown()
            assertTrue(releaseTransport.await(5, TimeUnit.SECONDS))
            response(formValues(encoded).getValue("attivita[]"))
        })
        val saved = listOf(SavedSubject("2025", "A", "A"), SavedSubject("2025", "B", "B"))
        val executor = Executors.newFixedThreadPool(2)
        try {
            val first = executor.submit<ScheduleSnapshot> { api.refreshSavedLessons(saved) }
            assertTrue(enteredTransport.await(5, TimeUnit.SECONDS))
            val second = executor.submit<ScheduleSnapshot> { api.refreshSavedLessons(saved) }
            releaseTransport.countDown()

            assertEquals(2, first.get(5, TimeUnit.SECONDS).lessons.size)
            assertEquals(2, second.get(5, TimeUnit.SECONDS).lessons.size)
            assertEquals(1, calls.get())
        } finally {
            releaseTransport.countDown()
            executor.shutdownNow()
        }
    }

    @Test fun incompleteScheduleDoesNotReplaceLastValidSnapshot() = withCache { directory ->
        var now = 1_000_000L
        var body = response(listOf("A"))
        val cache = ResponseCache(directory, clockMillis = { now })
        val api = UnimiApi(cache = cache, clockMillis = { now }, transport = { _, _, _ -> body })
        val saved = listOf(SavedSubject("2025", "A", "A"))
        val first = api.refreshSavedLessons(saved)

        body = """{"celle":[${JSONObject(response(listOf("A"))).getJSONArray("celle").getJSONObject(0)},
            {"codice_insegnamento":"B","nome_insegnamento":"B","data":"21-09-2026"}]}"""
        now += 3 * 60 * 1000
        assertThrows(Exception::class.java) { api.refreshSavedLessons(saved) }
        assertEquals(first, api.cachedSavedLessons(saved))

        body = "{}"
        assertThrows(Exception::class.java) { api.refreshSavedLessons(saved) }
        assertEquals(first, api.cachedSavedLessons(saved))

        body = """{"celle":[]}"""
        assertTrue(api.refreshSavedLessons(saved).lessons.isEmpty())
        assertTrue(api.cachedSavedLessons(saved)!!.lessons.isEmpty())
    }

    @Test fun previouslyCachedMalformedScheduleIsIgnoredAndRefetched() = withCache { directory ->
        var now = 1_000_000L
        var encodedForm = ""
        var calls = 0
        val cache = ResponseCache(directory, clockMillis = { now })
        val api = UnimiApi(cache = cache, clockMillis = { now }, transport = { _, encoded, _ ->
            encodedForm = encoded
            calls++
            response(listOf("A"))
        })
        val saved = listOf(SavedSubject("2025", "A", "A"))
        api.refreshSavedLessons(saved)

        cache.write("POST|https://orari-be.divsi.unimi.it/AgendaWeb/Orario/grid_call.php|$encodedForm", "{}")
        assertEquals(null, api.cachedSavedLessons(saved))
        now += 1_000L
        assertEquals(1, api.refreshSavedLessonsIfStale(saved).lessons.size)
        assertEquals(2, calls)
    }

    @Test fun nonTeachingClosureCellRemainsValid() = withCache { directory ->
        val body = """{"celle":[{"nome":"Sospensione","data":"07-12-2026",
            "ora_inizio":"00:00","ora_fine":"24:00"}]}"""
        val api = UnimiApi(cache = ResponseCache(directory), transport = { _, _, _ -> body })
        assertTrue(api.refreshSavedLessons(listOf(SavedSubject("2025", "A", "A"))).lessons.isEmpty())
    }

    @Test fun invalidCatalogResponseDoesNotReplaceLastValidCatalog() = withCache { directory ->
        var now = 1_000_000L
        var body = """var anni_accademici_ec = {"2025":{"valore":"2025","label":"2025/2026"}};"""
        val cache = ResponseCache(directory, clockMillis = { now })
        val api = UnimiApi(cache = cache, clockMillis = { now }, transport = { _, _, _ -> body })
        assertEquals("2025", api.years().single().code)

        now += 13L * 60 * 60 * 1000
        body = "var anni_accademici_ec = {"
        assertEquals("2025", api.years().single().code)

        body = "var other_variable = {};"
        assertEquals("2025", api.years().single().code)
    }

    private fun response(codes: List<String>): String {
        val cells = JSONArray()
        codes.forEachIndexed { index, code ->
            cells.put(JSONObject()
                .put("id", "lesson-$code")
                .put("codice_insegnamento", code)
                .put("nome_insegnamento", "Insegnamento $code")
                .put("data", "21-09-2026")
                .put("ora_inizio", "%02d:00".format(9 + index))
                .put("ora_fine", "%02d:00".format(10 + index))
                .put("aula", "Aula $code")
                .put("docente", "Docente $code")
                .put("tipo", "Lezione")
                .put("Annullato", "0"))
        }
        return JSONObject().put("celle", cells).toString()
    }

    private fun formValues(encoded: String): Map<String, List<String>> = encoded.split('&')
        .map { value ->
            val parts = value.split('=', limit = 2)
            URLDecoder.decode(parts[0], "UTF-8") to URLDecoder.decode(parts.getOrElse(1) { "" }, "UTF-8")
        }
        .groupBy({ it.first }, { it.second })

    private inline fun withCache(block: (java.io.File) -> Unit) {
        val directory = Files.createTempDirectory("unimi-sync-test").toFile()
        try {
            block(directory)
        } finally {
            directory.deleteRecursively()
        }
    }
}
