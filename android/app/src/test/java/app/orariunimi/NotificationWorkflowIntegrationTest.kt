package app.orariunimi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

class NotificationWorkflowIntegrationTest {
    private val zone = ZoneId.of("Europe/Rome")
    private val now = ZonedDateTime.of(2026, 10, 9, 8, 0, 0, 0, zone)

    @Test fun confirmedRemovalLeavesNoReminderPlanAndProducesPersistentPayload() {
        val appeal = appeal()
        val first = ExamNotificationEngine.evaluate(listOf(appeal), emptyList(), emptyMap())
        assertTrue(first.removedAppeals.isEmpty())
        assertEquals(1, first.appeals.size)

        val second = ExamNotificationEngine.evaluate(first.appeals, emptyList(), first.missingCounts)
        assertEquals(listOf(appeal), second.removedAppeals)
        assertEquals("Non più pubblicato", second.drafts.single().changes.single().after)
        assertTrue(ExamRegistrationReminderPlanner.plans(
            second.appeals, enabledPreferences(), now, zone).isEmpty())
    }

    @Test fun changedRegistrationDeadlineUpdatesDiffAndReminderTrigger() {
        val old = appeal()
        val fresh = old.copy(registrationClose = old.registrationClose!!.plusDays(2))

        val evaluation = ExamNotificationEngine.evaluate(listOf(old), listOf(fresh), emptyMap())
        val deadline = evaluation.drafts.single().changes.single { it.label == "Chiusura iscrizioni" }
        assertEquals("18/10/2026", deadline.before)
        assertEquals("20/10/2026", deadline.after)

        val oldPlan = ExamRegistrationReminderPlanner.plans(listOf(old), enabledPreferences(), now, zone)
            .single { it.boundary == ExamRegistrationBoundary.CLOSE }
        val newPlan = ExamRegistrationReminderPlanner.plans(listOf(fresh), enabledPreferences(), now, zone)
            .single { it.boundary == ExamRegistrationBoundary.CLOSE }
        assertEquals(2L * 24 * 60 * 60 * 1000, newPlan.triggerAtMillis - oldPlan.triggerAtMillis)
        assertEquals(oldPlan.workName, newPlan.workName)
    }

    @Test fun openingAndDeadlineRemindersAreIndependentlyOptional() {
        val appeal = appeal()
        val openingOnly = enabledPreferences().copy(examRegistrationCloseReminders = false)
        val closingOnly = enabledPreferences().copy(examRegistrationOpenReminders = false)

        assertEquals(listOf(ExamRegistrationBoundary.OPEN),
            ExamRegistrationReminderPlanner.plans(listOf(appeal), openingOnly, now, zone)
                .map { it.boundary })
        assertEquals(listOf(ExamRegistrationBoundary.CLOSE),
            ExamRegistrationReminderPlanner.plans(listOf(appeal), closingOnly, now, zone)
                .map { it.boundary })
    }

    private fun enabledPreferences() = NotificationPreferences(
        enabled = true,
        examRegistrationOpenReminders = true,
        examRegistrationCloseReminders = true,
        examRegistrationCloseLeadDays = 3
    )

    private fun appeal() = ExamAppeal(
        id = "42", courseCode = "FAA", courseName = "Informatica",
        subjectCode = "ALG", subjectPortalCode = "ALG-1", subjectName = "Algoritmi",
        date = LocalDate.of(2026, 10, 25), time = "09:00", location = "Aula 1",
        teacher = "Ada Lovelace", surnameFrom = "A", surnameTo = "L",
        testType = "Scritto", appealType = "Ordinario",
        registrationOpen = LocalDate.of(2026, 10, 10),
        registrationClose = LocalDate.of(2026, 10, 18)
    )
}
