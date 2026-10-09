package app.orariunimi

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

enum class ExamRegistrationBoundary { OPEN, CLOSE }

data class ExamRegistrationReminderPlan(
    val workName: String,
    val appealKey: String,
    val boundary: ExamRegistrationBoundary,
    val expectedDate: LocalDate,
    val triggerAtMillis: Long,
    val title: String,
    val message: String,
    val appealDate: LocalDate
)

object ExamRegistrationReminderPlanner {
    fun plans(
        appeals: List<ExamAppeal>,
        preferences: NotificationPreferences,
        now: ZonedDateTime = ZonedDateTime.now(),
        zone: ZoneId = now.zone
    ): List<ExamRegistrationReminderPlan> = appeals.flatMap { appeal ->
        buildList {
            if (preferences.examRegistrationOpenReminders) appeal.registrationOpen?.let { date ->
                create(appeal, ExamRegistrationBoundary.OPEN, date, date, now, zone)?.let(::add)
            }
            if (preferences.examRegistrationCloseReminders) appeal.registrationClose?.let { date ->
                create(appeal, ExamRegistrationBoundary.CLOSE, date,
                    date.minusDays(preferences.examRegistrationCloseLeadDays.toLong()), now, zone)?.let(::add)
            }
        }
    }.sortedBy { it.triggerAtMillis }

    private fun create(
        appeal: ExamAppeal,
        boundary: ExamRegistrationBoundary,
        expectedDate: LocalDate,
        triggerDate: LocalDate,
        now: ZonedDateTime,
        zone: ZoneId
    ): ExamRegistrationReminderPlan? {
        val trigger = triggerDate.atTime(LocalTime.of(9, 0)).atZone(zone)
        if (!trigger.isAfter(now)) return null
        val key = ExamNotificationEngine.identity(appeal)
        val kind = boundary.name.lowercase()
        val dateText = "%02d/%02d/%04d".format(
            expectedDate.dayOfMonth, expectedDate.monthValue, expectedDate.year)
        return ExamRegistrationReminderPlan(
            workName = "exam-registration-$kind-${NotificationEngine.digest(key)}",
            appealKey = key,
            boundary = boundary,
            expectedDate = expectedDate,
            triggerAtMillis = trigger.toInstant().toEpochMilli(),
            title = if (boundary == ExamRegistrationBoundary.OPEN) "Iscrizioni aperte"
                else "Iscrizioni in scadenza",
            message = if (boundary == ExamRegistrationBoundary.OPEN)
                "Puoi iscriverti all’appello di ${appeal.subjectName} del ${format(appeal.date)}."
            else "Le iscrizioni a ${appeal.subjectName} chiudono il $dateText.",
            appealDate = appeal.date
        )
    }

    private fun format(date: LocalDate) =
        "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
}
