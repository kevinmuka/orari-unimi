package app.orariunimi

import java.time.LocalDate

data class ExamChangeEvaluation(
    val drafts: List<NotificationDraft>,
    val appeals: List<ExamAppeal>,
    val missingCounts: Map<String, Int>,
    val removedAppeals: List<ExamAppeal>
)

object ExamNotificationEngine {
    fun selectionKey(saved: List<ExamAppeal>): String = NotificationEngine.digest(saved
        .distinctBy(::identity)
        .sortedBy(::identity)
        .joinToString("|") { identity(it) })

    fun courseCodes(saved: List<ExamAppeal>): List<String> = saved
        .map { it.courseCode.uppercase() }.distinct().sorted()

    fun shouldDeferEmptySnapshot(
        previous: List<ExamAppeal>,
        current: List<ExamAppeal>,
        consecutiveEmptyResponses: Int
    ): Boolean = current.isEmpty() && previous.isNotEmpty() && consecutiveEmptyResponses < 1

    fun evaluate(
        previous: List<ExamAppeal>,
        current: List<ExamAppeal>,
        previousMissingCounts: Map<String, Int>
    ): ExamChangeEvaluation {
        val old = previous.associateBy(::identity)
        val fresh = current.associateBy(::identity)
        val next = fresh.toMutableMap()
        val missing = mutableMapOf<String, Int>()
        val drafts = mutableListOf<NotificationDraft>()
        val removed = mutableListOf<ExamAppeal>()

        old.forEach { (key, oldAppeal) ->
            val freshAppeal = fresh[key]
            if (freshAppeal == null) {
                val count = previousMissingCounts.getOrDefault(key, 0) + 1
                if (count >= 2) {
                    removed += oldAppeal
                    drafts += draft(
                        kind = "removed",
                        key = key,
                        appeal = oldAppeal,
                        title = "Appello non più disponibile",
                        message = "${oldAppeal.subjectName}: l’appello del ${formatDate(oldAppeal.date)} non risulta più pubblicato.",
                        changes = listOf(NotificationFieldChange(
                            "Stato", "Disponibile", "Non più pubblicato"))
                    )
                } else {
                    missing[key] = count
                    next[key] = oldAppeal
                }
                return@forEach
            }

            describeChange(oldAppeal, freshAppeal)?.let { (description, changes) ->
                drafts += draft("changed", key, freshAppeal, "Appello aggiornato", description, changes)
            }
        }

        return ExamChangeEvaluation(
            drafts = drafts,
            appeals = next.values.sortedWith(
                compareBy<ExamAppeal> { it.date }.thenBy { it.time }.thenBy { it.subjectName }),
            missingCounts = missing,
            removedAppeals = removed
        )
    }

    fun identity(appeal: ExamAppeal): String = "${appeal.courseCode.uppercase()}:${appeal.id}"

    private fun describeChange(old: ExamAppeal, fresh: ExamAppeal): Pair<String, List<NotificationFieldChange>>? {
        val changes = mutableListOf<String>()
        val fields = mutableListOf<NotificationFieldChange>()
        fun changed(label: String, before: String, after: String, summary: String) {
            changes += summary
            fields += NotificationFieldChange(label, shown(before), shown(after))
        }
        if (old.date != fresh.date) changed("Data", formatDate(old.date), formatDate(fresh.date),
            "nuova data ${formatDate(fresh.date)}")
        if (old.time != fresh.time) changed("Ora", old.time, fresh.time, valueChange("orario", fresh.time))
        if (old.location != fresh.location) changed("Luogo", old.location, fresh.location,
            valueChange("luogo", fresh.location))
        if (old.teacher != fresh.teacher) changed("Docente", old.teacher, fresh.teacher,
            valueChange("docente", fresh.teacher))
        if (old.surnameFrom != fresh.surnameFrom || old.surnameTo != fresh.surnameTo) {
            val oldRange = listOf(old.surnameFrom, old.surnameTo).filter { it.isNotBlank() }.joinToString("–")
            val newRange = listOf(fresh.surnameFrom, fresh.surnameTo).filter { it.isNotBlank() }.joinToString("–")
            changed("Fascia alfabetica", oldRange, newRange, valueChange("intervallo cognomi", newRange))
        }
        if (old.testType != fresh.testType) changed("Tipo di prova", old.testType, fresh.testType,
            valueChange("tipo di prova", fresh.testType))
        if (old.appealType != fresh.appealType) changed("Tipo di appello", old.appealType, fresh.appealType,
            valueChange("tipo di appello", fresh.appealType))
        if (old.registrationOpen != fresh.registrationOpen) {
            changes += dateChange("apertura iscrizioni", fresh.registrationOpen)
            fields += NotificationFieldChange("Apertura iscrizioni",
                old.registrationOpen?.let(::formatDate) ?: "Da definire",
                fresh.registrationOpen?.let(::formatDate) ?: "Da definire")
        }
        if (old.registrationClose != fresh.registrationClose) {
            changes += dateChange("chiusura iscrizioni", fresh.registrationClose)
            fields += NotificationFieldChange("Chiusura iscrizioni",
                old.registrationClose?.let(::formatDate) ?: "Da definire",
                fresh.registrationClose?.let(::formatDate) ?: "Da definire")
        }
        if (old.subjectName != fresh.subjectName) changed("Insegnamento", old.subjectName, fresh.subjectName,
            valueChange("insegnamento", fresh.subjectName))
        if (old.courseName != fresh.courseName) changed("Corso di laurea", old.courseName, fresh.courseName,
            valueChange("corso", fresh.courseName))
        if (old.subjectCode != fresh.subjectCode || old.subjectPortalCode != fresh.subjectPortalCode) {
            changes += "codice dell’insegnamento aggiornato"
            fields += NotificationFieldChange("Codice insegnamento",
                listOf(old.subjectCode, old.subjectPortalCode).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Da definire" },
                listOf(fresh.subjectCode, fresh.subjectPortalCode).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Da definire" })
        }
        if (fields.isEmpty()) return null
        return "${fresh.subjectName}: ${changes.joinToString(", ")}." to fields
    }

    private fun valueChange(label: String, value: String): String =
        if (value.isBlank()) "$label da definire" else "$label $value"

    private fun dateChange(label: String, value: LocalDate?): String =
        if (value == null) "$label da definire" else "$label ${formatDate(value)}"

    private fun draft(
        kind: String,
        key: String,
        appeal: ExamAppeal,
        title: String,
        message: String,
        changes: List<NotificationFieldChange>
    ) = NotificationDraft(
        id = "exam-$kind-${NotificationEngine.digest("$key|${fingerprint(appeal)}|$message")}",
        title = title,
        message = message,
        targetDate = appeal.date,
        targetKey = key,
        targetTitle = appeal.subjectName,
        changes = changes
    )

    private fun fingerprint(appeal: ExamAppeal): String = NotificationEngine.digest(listOf(
        identity(appeal), appeal.courseName, appeal.subjectCode, appeal.subjectPortalCode,
        appeal.subjectName, appeal.date, appeal.time, appeal.location, appeal.teacher,
        appeal.surnameFrom, appeal.surnameTo, appeal.testType, appeal.appealType,
        appeal.registrationOpen, appeal.registrationClose
    ).joinToString("|"))

    private fun formatDate(date: LocalDate) =
        "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)

    private fun shown(value: String) = value.ifBlank { "Da definire" }
}
