package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.nio.file.Files

class ExamApiTest {
    @Test fun parsesAndSortsPublicExamResponseWithOptionalFields() {
        val body = """
            [{
              "codIns":"FAA0AN",
              "codW4":"FAA-17",
              "descrIns":"ARCHITETTURA DEGLI ELABORATORI I ",
              "appelli":[
                {
                  "dataStr":"22/09/2026",
                  "ora":"08:30",
                  "luogo":"Aula G09",
                  "prova":"Scritto",
                  "docente":{"nome":"NICOLA","cognome":"BASILICO"},
                  "rangeDa":"H","rangeA":"Z",
                  "idAppello":"18463261",
                  "tipoAppello":"Appello",
                  "aperturaStr":"01/08/2026",
                  "chiusuraStr":"15/09/2026"
                },
                {
                  "dataStr":"18/09/2026",
                  "prova":"Laboratorio",
                  "docente":{"nome":"GABRIELLA","cognome":"TRUCCO"},
                  "idAppello":"18462947"
                }
              ]
            }]
        """.trimIndent()

        val appeals = parseExamAppeals(body, "faa")

        assertEquals(listOf("18462947", "18463261"), appeals.map { it.id })
        assertEquals("FAA", appeals.first().courseCode)
        assertEquals("", appeals.first().time)
        assertEquals("", appeals.first().location)
        assertEquals("BASILICO NICOLA", appeals.last().teacher)
        assertEquals(LocalDate.of(2026, 8, 1), appeals.last().registrationOpen)
        assertEquals(LocalDate.of(2026, 9, 15), appeals.last().registrationClose)
    }

    @Test fun malformedDatesRejectTheWholeSnapshotAndDuplicateIdsAreDeduplicated() {
        val body = """
            [{"codIns":"X","descrIns":"Test","appelli":[
              {"dataStr":"20/09/2026","idAppello":"same"},
              {"dataStr":"20/09/2026","idAppello":"same"}
            ]}]
        """.trimIndent()

        val appeals = parseExamAppeals(body, "FAA")

        assertEquals(1, appeals.size)
        assertThrows(Exception::class.java) {
            parseExamAppeals("""[{"codIns":"X","descrIns":"Test","appelli":[
                {"dataStr":"20/09/2026","idAppello":"same"},
                {"dataStr":"not-a-date","idAppello":"invalid"}]}]""", "FAA")
        }
    }

    @Test fun incompleteAppealsDoNotReplaceLastValidCache() {
        val directory = Files.createTempDirectory("exam-api-invalid-cache").toFile()
        var now = 1_000L
        var body: String? = """[{"codIns":"X","descrIns":"Test","appelli":[
            {"dataStr":"20/09/2026","idAppello":"42"}]}]"""
        try {
            val cache = ResponseCache(directory, clockMillis = { now })
            val api = ExamApi(cache = cache, clockMillis = { now }, transport = { _, _ -> body })
            val first = api.refreshAppeals("FAA", ExamWindow.ALL)

            now += 3 * 60 * 1000
            body = """[{"codIns":"X","descrIns":"Test"}]"""
            assertThrows(Exception::class.java) { api.refreshAppeals("FAA", ExamWindow.ALL) }
            assertEquals(first, api.cachedAppeals("FAA", ExamWindow.ALL))

            body = """[{"codIns":"X","descrIns":"Test","appelli":[
                {"dataStr":"not-a-date","idAppello":"42"}]}]"""
            assertThrows(Exception::class.java) { api.refreshAppeals("FAA", ExamWindow.ALL) }
            assertEquals(first, api.cachedAppeals("FAA", ExamWindow.ALL))

            body = "[]"
            assertTrue(api.refreshAppeals("FAA", ExamWindow.ALL).appeals.isEmpty())
            assertTrue(api.cachedAppeals("FAA", ExamWindow.ALL)!!.appeals.isEmpty())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun previouslyCachedMalformedAppealsAreIgnoredAndRefetched() {
        val directory = Files.createTempDirectory("exam-api-malformed-cache").toFile()
        var now = 1_000L
        var calls = 0
        try {
            val cache = ResponseCache(directory, clockMillis = { now })
            val api = ExamApi(cache = cache, clockMillis = { now }, transport = { _, _ ->
                calls++
                "[]"
            })
            api.refreshAppeals("FAA", ExamWindow.ALL)
            cache.write("GET|https://work.unimi.it/foProssimiEsami/json/FAA/", "not json")
            assertEquals(null, api.cachedAppeals("FAA", ExamWindow.ALL))

            now += 1_000L
            assertTrue(api.refreshAppealsIfStale("FAA", ExamWindow.ALL).appeals.isEmpty())
            assertEquals(2, calls)
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun notFoundMayBeShownAsEmptyButCannotConfirmSavedAppealRemoval() {
        val directory = Files.createTempDirectory("exam-api-not-found").toFile()
        var now = 1_000L
        var body: String? = """[{"codIns":"X","descrIns":"Test","appelli":[
            {"dataStr":"20/09/2026","idAppello":"42"}]}]"""
        try {
            val cache = ResponseCache(directory, clockMillis = { now })
            val api = ExamApi(cache = cache, clockMillis = { now }, transport = { _, _ -> body })
            val first = api.refreshPublishedAppeals("FAA", ExamWindow.ALL)
            now += 3 * 60 * 1000
            body = null // The transport maps HTTP 404 to null, without caching it.

            repeat(2) {
                assertThrows(Exception::class.java) {
                    api.refreshPublishedAppeals("FAA", ExamWindow.ALL)
                }
                assertEquals(first, api.cachedAppeals("FAA", ExamWindow.ALL))
            }
            assertThrows(Exception::class.java) { api.refreshAppeals("FAA", ExamWindow.ALL) }
            assertEquals(first, api.cachedAppeals("FAA", ExamWindow.ALL))
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun notFoundWithoutPreviousSnapshotStillShowsEmptySearchResults() {
        val directory = Files.createTempDirectory("exam-api-empty-not-found").toFile()
        try {
            val api = ExamApi(cache = ResponseCache(directory), transport = { _, _ -> null })
            assertTrue(api.refreshAppeals("FAA", ExamWindow.ALL).appeals.isEmpty())
            assertEquals(null, api.cachedAppeals("FAA", ExamWindow.ALL))
            assertThrows(Exception::class.java) {
                api.refreshPublishedAppeals("FAA", ExamWindow.ALL)
            }
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun cachedAbsenceDoesNotCountAsSecondIndependentCheck() {
        val directory = Files.createTempDirectory("exam-api-confirmation").toFile()
        var now = 1_000L
        var body = """[{"codIns":"X","descrIns":"Test","appelli":[
            {"dataStr":"20/09/2026","idAppello":"42"},
            {"dataStr":"21/09/2026","idAppello":"43"}]}]"""
        try {
            val api = ExamApi(cache = ResponseCache(directory, clockMillis = { now }),
                clockMillis = { now }, transport = { _, _ -> body })
            val baseline = api.refreshPublishedAppeals("FAA", ExamWindow.ALL)
            body = """[{"codIns":"X","descrIns":"Test","appelli":[
                {"dataStr":"21/09/2026","idAppello":"43"}]}]"""

            now += 121_000L
            val firstMissing = api.refreshPublishedAppealsIfStale("FAA", ExamWindow.ALL)
            val firstCheck = ExamNotificationEngine.evaluate(
                baseline.appeals, firstMissing.appeals, emptyMap())
            assertTrue(firstCheck.removedAppeals.isEmpty())

            now += 1_000L
            val reused = api.refreshPublishedAppealsIfStale("FAA", ExamWindow.ALL)
            assertEquals(firstMissing.updatedAtMillis, reused.updatedAtMillis)
            assertFalse(isNewSourceSnapshot(firstMissing.updatedAtMillis, reused.updatedAtMillis))

            now += 121_000L
            val secondMissing = api.refreshPublishedAppealsIfStale("FAA", ExamWindow.ALL)
            assertTrue(isNewSourceSnapshot(firstMissing.updatedAtMillis, secondMissing.updatedAtMillis))
            val secondCheck = ExamNotificationEngine.evaluate(
                firstCheck.appeals, secondMissing.appeals, firstCheck.missingCounts)
            assertEquals(listOf("42"), secondCheck.removedAppeals.map { it.id })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test fun automaticExamRefreshReusesTwoMinuteCache() {
        val directory = Files.createTempDirectory("exam-api-cache").toFile()
        var now = 1_000L
        var calls = 0
        val cacheEvents = mutableListOf<Boolean>()
        try {
            val cache = ResponseCache(directory, clockMillis = { now })
            val api = ExamApi(cache = cache, clockMillis = { now }, transport = { _, _ ->
                calls++
                "[]"
            }, loadObserver = cacheEvents::add)
            api.refreshAppealsIfStale("FAA", ExamWindow.ALL)
            api.refreshAppealsIfStale("FAA", ExamWindow.ALL)
            assertEquals(1, calls)
            now += 121_000L
            api.refreshAppealsIfStale("FAA", ExamWindow.ALL)
            assertEquals(2, calls)
            assertEquals(listOf(false, true, false), cacheEvents)
        } finally {
            directory.deleteRecursively()
        }
    }
}
