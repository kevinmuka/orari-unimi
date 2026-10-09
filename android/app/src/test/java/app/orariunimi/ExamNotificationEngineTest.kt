package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ExamNotificationEngineTest {
    private val date = LocalDate.of(2026, 10, 20)

    @Test fun detectsEveryPublishedFieldChange() {
        val old = appeal()
        val fresh = old.copy(
            courseName = "Nuovo corso",
            subjectCode = "NEW",
            subjectPortalCode = "NEW-1",
            subjectName = "Nuovo insegnamento",
            date = date.plusDays(1),
            time = "11:00",
            location = "Aula 2",
            teacher = "Grace Hopper",
            surnameFrom = "M",
            surnameTo = "Z",
            testType = "Orale",
            appealType = "Straordinario",
            registrationOpen = date.minusDays(5),
            registrationClose = date.minusDays(1)
        )

        val result = ExamNotificationEngine.evaluate(listOf(old), listOf(fresh), emptyMap())

        assertEquals(1, result.drafts.size)
        val message = result.drafts.single().message
        listOf("nuova data", "orario", "luogo", "docente", "intervallo cognomi",
            "tipo di prova", "tipo di appello", "apertura iscrizioni",
            "chiusura iscrizioni", "insegnamento", "corso", "codice").forEach {
            assertTrue("Campo non descritto: $it", message.contains(it))
        }
        assertEquals(listOf(fresh), result.appeals)
    }

    @Test fun missingSavedAppealRequiresTwoConsecutiveSnapshots() {
        val old = appeal()
        val first = ExamNotificationEngine.evaluate(listOf(old), emptyList(), emptyMap())
        assertTrue(first.drafts.isEmpty())
        assertEquals(1, first.missingCounts[ExamNotificationEngine.identity(old)])

        val second = ExamNotificationEngine.evaluate(first.appeals, emptyList(), first.missingCounts)
        assertEquals("Appello non più disponibile", second.drafts.single().title)
        assertTrue(second.appeals.isEmpty())
    }

    @Test fun unchangedAppealDoesNotNotifyAndNewSelectionIsNotAnExternalChange() {
        val appeal = appeal()
        assertTrue(ExamNotificationEngine.evaluate(listOf(appeal), listOf(appeal), emptyMap()).drafts.isEmpty())
        assertTrue(ExamNotificationEngine.evaluate(emptyList(), listOf(appeal), emptyMap()).drafts.isEmpty())
    }

    @Test fun selectionAndRequestsAreStableAndDeduplicated() {
        val first = appeal(courseCode = "faa", id = "1")
        val second = appeal(courseCode = "FAA", id = "2")
        val third = appeal(courseCode = "FBA", id = "3")
        assertEquals(listOf("FAA", "FBA"), ExamNotificationEngine.courseCodes(
            listOf(first, second, third)))
        assertEquals(ExamNotificationEngine.selectionKey(listOf(first, third)),
            ExamNotificationEngine.selectionKey(listOf(third, first)))
        assertTrue(ExamNotificationEngine.selectionKey(listOf(first)) !=
            ExamNotificationEngine.selectionKey(listOf(first, third)))
    }

    @Test fun emptyEndpointResponseGetsOneExtraConfirmation() {
        assertTrue(ExamNotificationEngine.shouldDeferEmptySnapshot(listOf(appeal()), emptyList(), 0))
        assertEquals(false,
            ExamNotificationEngine.shouldDeferEmptySnapshot(listOf(appeal()), emptyList(), 1))
    }

    private fun appeal(courseCode: String = "FAA", id: String = "42") = ExamAppeal(
        id = id,
        courseCode = courseCode,
        courseName = "Informatica",
        subjectCode = "ALG",
        subjectPortalCode = "ALG-1",
        subjectName = "Algoritmi",
        date = date,
        time = "09:00",
        location = "Aula 1",
        teacher = "Ada Lovelace",
        surnameFrom = "A",
        surnameTo = "L",
        testType = "Scritto",
        appealType = "Ordinario",
        registrationOpen = date.minusDays(20),
        registrationClose = date.minusDays(2)
    )
}
