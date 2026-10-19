package app.orariunimi

import java.security.MessageDigest
import java.time.LocalDate

internal fun isNewSourceSnapshot(previousAtMillis: Long, currentAtMillis: Long): Boolean =
    previousAtMillis != currentAtMillis

data class NotificationDraft(
    val id: String,
    val title: String,
    val message: String,
    val targetDate: LocalDate?,
    val targetKey: String? = null,
    val targetTitle: String? = null,
    val changes: List<NotificationFieldChange> = emptyList()
)

data class ScheduleChangeEvaluation(
    val drafts: List<NotificationDraft>,
    val lessons: List<Lesson>,
    val missingCounts: Map<String, Int>
)

object NotificationEngine {
    fun lessonSourceCount(saved: List<SavedSubject>): Int = saved
        .distinctBy { it.year to it.code }.groupBy { it.year }.values.sumOf { (it.size + 19) / 20 }

    fun lessonSelectionKey(saved: List<SavedSubject>): String = digest(saved
        .distinctBy { it.year to it.code }
        .sortedWith(compareBy<SavedSubject> { it.year }.thenBy { it.code })
        .joinToString("|") { "${it.year}:${it.code}" })

    fun shouldDeferEmptySnapshot(
        previous: List<Lesson>,
        current: List<Lesson>,
        consecutiveEmptyResponses: Int,
        today: LocalDate
    ): Boolean = current.none { !it.date.isBefore(today) } &&
        previous.any { !it.date.isBefore(today) } && consecutiveEmptyResponses < 1

    fun evaluate(
        previous: List<Lesson>,
        current: List<Lesson>,
        previousMissingCounts: Map<String, Int>,
        today: LocalDate
    ): ScheduleChangeEvaluation {
        val old = previous.filter { !it.date.isBefore(today) }.associateBy(::lessonIdentity)
        val fresh = current.filter { !it.date.isBefore(today) }.associateBy(::lessonIdentity)
        val next = fresh.toMutableMap()
        val missing = mutableMapOf<String, Int>()
        val drafts = mutableListOf<NotificationDraft>()

        old.forEach { (key, oldLesson) ->
            val newLesson = fresh[key]
            if (newLesson == null) {
                val count = previousMissingCounts.getOrDefault(key, 0) + 1
                if (count >= 2) {
                    drafts += draft(
                        "removed", key, oldLesson,
                        "Lezione rimossa",
                        "${oldLesson.subject}: non risulta più in programma il ${formatDate(oldLesson.date)} alle ${oldLesson.start}.",
                        listOf(NotificationFieldChange("Stato", "In programma", "Non più pubblicata"))
                    )
                } else {
                    missing[key] = count
                    next[key] = oldLesson
                }
                return@forEach
            }

            describeChange(oldLesson, newLesson)?.let { (description, changes) ->
                drafts += draft("changed", key, newLesson, "Orario aggiornato", description, changes)
            }
        }

        fresh.forEach { (key, lesson) ->
            if (key !in old) {
                drafts += draft(
                    "added", key, lesson,
                    "Nuova lezione",
                    "${lesson.subject}: ${formatDate(lesson.date)}, ${lesson.start}–${lesson.end}${roomSuffix(lesson.room)}.",
                    listOf(NotificationFieldChange("Stato", "Non presente", "Nuova lezione"))
                )
            }
        }

        return ScheduleChangeEvaluation(
            drafts = drafts,
            lessons = next.values.sortedWith(compareBy<Lesson> { it.date }.thenBy { it.start }.thenBy { it.subject }),
            missingCounts = missing
        )
    }

    fun lessonIdentity(lesson: Lesson): String = lesson.id.takeIf { it.isNotBlank() }
        ?.let { "id:$it" }
        ?: "fallback:${lesson.subjectCode}|${lesson.date}|${lesson.start}|${lesson.end}"

    fun lessonFingerprint(lesson: Lesson): String = digest(
        listOf(lessonIdentity(lesson), lesson.date, lesson.start, lesson.end, lesson.room,
            lesson.teacher, lesson.type, lesson.notes, lesson.cancelled).joinToString("|")
    )

    private fun describeChange(old: Lesson, fresh: Lesson): Pair<String, List<NotificationFieldChange>>? {
        val changes = mutableListOf<String>()
        val fields = mutableListOf<NotificationFieldChange>()
        if (old.cancelled != fresh.cancelled) {
            changes += if (fresh.cancelled) "lezione annullata" else "lezione ripristinata"
            fields += NotificationFieldChange("Stato", if (old.cancelled) "Annullata" else "In programma",
                if (fresh.cancelled) "Annullata" else "In programma")
        }
        if (old.date != fresh.date) {
            changes += "nuova data ${formatDate(fresh.date)}"
            fields += NotificationFieldChange("Data", formatDate(old.date), formatDate(fresh.date))
        }
        if (old.start != fresh.start || old.end != fresh.end) {
            changes += "nuovo orario ${fresh.start}–${fresh.end}"
            fields += NotificationFieldChange("Orario", "${old.start}–${old.end}", "${fresh.start}–${fresh.end}")
        }
        if (old.room != fresh.room) {
            changes += if (fresh.room.isBlank()) "aula da definire" else "nuova aula ${fresh.room}"
            fields += NotificationFieldChange("Aula", shown(old.room), shown(fresh.room))
        }
        if (old.teacher != fresh.teacher) {
            changes += if (fresh.teacher.isBlank()) "docente da definire" else "docente ${fresh.teacher}"
            fields += NotificationFieldChange("Docente", shown(old.teacher), shown(fresh.teacher))
        }
        if (old.type != fresh.type) {
            changes += "tipo ${shown(fresh.type)}"
            fields += NotificationFieldChange("Tipo", shown(old.type), shown(fresh.type))
        }
        if (old.notes != fresh.notes) {
            changes += if (fresh.notes.isBlank()) "nota rimossa" else "nuova nota: ${fresh.notes}"
            fields += NotificationFieldChange("Note", shown(old.notes), shown(fresh.notes))
        }
        if (fields.isEmpty()) return null
        return "${fresh.subject}: ${changes.joinToString(", ")}." to fields
    }

    private fun draft(
        kind: String, key: String, lesson: Lesson, title: String, message: String,
        changes: List<NotificationFieldChange>
    ) =
        NotificationDraft("schedule-$kind-${digest("$key|${lessonFingerprint(lesson)}|$message")}",
            title, message, lesson.date, key, lesson.subject, changes)

    private fun roomSuffix(room: String) = if (room.isBlank()) "" else " · $room"
    private fun shown(value: String) = value.ifBlank { "Da definire" }
    private fun formatDate(date: LocalDate) = "%02d/%02d/%04d".format(date.dayOfMonth, date.monthValue, date.year)
    internal fun digest(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).take(10).joinToString("") { "%02x".format(it) }
}
