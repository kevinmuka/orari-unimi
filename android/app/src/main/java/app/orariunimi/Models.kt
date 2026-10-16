package app.orariunimi

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

enum class SearchKind(val label: String, val subtitle: String) {
    COURSE("Corsi", "Corso di studio"),
    TEACHER("Docenti", "Docente"),
    SUBJECT("Insegnamenti", "Insegnamento")
}

data class AcademicYear(val code: String, val name: String)

enum class DegreeType(val label: String) {
    BACHELOR("Triennale"), MASTER("Magistrale"), SINGLE_CYCLE("Ciclo unico"), OTHER("Altro");

    companion object {
        fun fromPortal(value: String): DegreeType = when (value.trim().uppercase(Locale.ROOT)) {
            "CDS TRIENNALE" -> BACHELOR
            "CDS MAGISTRALE" -> MASTER
            "CDS MAGISTRALE A CICLO UNICO" -> SINGLE_CYCLE
            else -> OTHER
        }
    }
}

data class CourseTeaching(val code: String, val name: String, val teacher: String)
data class CoursePath(val code: String, val name: String, val teachings: List<CourseTeaching>)
data class CourseTeachingCandidate(val combinedCode: String, val teaching: CourseTeaching)
data class ExamSubjectSource(val courseCode: String, val examSubjectCode: String)

data class SearchItem(
    val code: String,
    val name: String,
    val kind: SearchKind,
    val paths: List<String> = emptyList(),
    val degreeType: DegreeType? = null,
    val coursePaths: List<CoursePath> = emptyList(),
    val examSources: List<ExamSubjectSource> = emptyList()
)

data class SavedSubject(val year: String, val code: String, val name: String)
data class FavoriteCourse(val year: String, val code: String, val name: String, val degreeType: DegreeType)
data class FollowedExamSubject(
    val year: String,
    val code: String,
    val name: String,
    val sources: List<ExamSubjectSource>
)

/** One visible entry even when an insegnamento is used by both Lezioni and Appelli. */
data class FavoriteSubject(
    val year: String,
    val code: String,
    val name: String,
    val inLessons: Boolean,
    val inExams: Boolean,
    val manuallyFollowedForExams: Boolean,
    val examSources: List<ExamSubjectSource>
)

fun favoriteSubjects(
    lessons: List<SavedSubject>,
    examOnly: List<FollowedExamSubject>,
    currentYear: String,
    indexedSubjects: List<SearchItem>?,
    excludedFromExams: Set<Pair<String, String>> = emptySet()
): List<FavoriteSubject> {
    val lessonByKey = lessons.associateBy { it.year to it.code }
    val examByKey = examOnly.associateBy { it.year to it.code }
    val indexedByCode = indexedSubjects?.associateBy { it.code }.orEmpty()
    return (lessonByKey.keys + examByKey.keys).distinct().map { key ->
        val lesson = lessonByKey[key]
        val followed = examByKey[key]
        val indexedSources = if (key.first == currentYear) indexedByCode[key.second]?.examSources.orEmpty()
            else emptyList()
        FavoriteSubject(
            year = key.first,
            code = key.second,
            name = lesson?.name ?: requireNotNull(followed).name,
            inLessons = lesson != null,
            inExams = followed != null || lesson != null &&
                key !in excludedFromExams && (followed?.sources.orEmpty() + indexedSources).isNotEmpty(),
            manuallyFollowedForExams = followed != null,
            examSources = (followed?.sources.orEmpty() + indexedSources).distinct()
        )
    }.sortedWith(compareBy<FavoriteSubject> { it.name.lowercase(Locale.ITALIAN) }
        .thenByDescending { it.year })
}

data class NotificationPreferences(
    val enabled: Boolean = false,
    val lessonChanges: Boolean = false,
    val examChanges: Boolean = false,
    val examRegistrationOpenReminders: Boolean = false,
    val examRegistrationCloseReminders: Boolean = false,
    val examRegistrationCloseLeadDays: Int = 3,
    val lessonReminders: Boolean = false,
    val appUpdates: Boolean = false,
    val reminderMinutes: Int = 30
)

enum class AppNotificationType {
    LESSON_CHANGE,
    EXAM_CHANGE,
    EXAM_REGISTRATION_REMINDER,
    LESSON_REMINDER,
    APP_UPDATE,
    /** Kept only to render inbox entries written by versions up to 1.7.0. */
    IMPORTANT_CHANGE
}

data class NotificationFieldChange(
    val label: String,
    val before: String,
    val after: String
)

data class AppNotificationEntry(
    val id: String,
    val type: AppNotificationType,
    val title: String,
    val message: String,
    val timestampMillis: Long,
    val targetDate: LocalDate? = null,
    val targetKey: String? = null,
    val targetTitle: String? = null,
    val changes: List<NotificationFieldChange> = emptyList(),
    val read: Boolean = false
)

data class RemovedExamNotice(
    val id: String,
    val appeal: ExamAppeal,
    val detectedAtMillis: Long
)

data class Lesson(
    val id: String,
    val subjectCode: String,
    val subject: String,
    val date: LocalDate,
    val start: String,
    val end: String,
    val room: String,
    val teacher: String,
    val type: String,
    val notes: String,
    val cancelled: Boolean
)

class SearchIndex(val items: List<SearchItem>) {
    private data class Entry(val item: SearchItem, val text: String, val name: String)
    private val entries = items.map { Entry(it, normalize("${it.code} ${it.name}"), normalize(it.name)) }

    fun search(query: String, limit: Int = 40): List<SearchItem> {
        val terms = normalize(query).split(' ').filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()
        return entries.asSequence().mapNotNull { entry ->
            val positions = terms.map { entry.text.indexOf(it) }
            if (positions.any { it < 0 }) null else Triple(entry.item, positions.min(), entry.name)
        }.sortedWith(compareBy<Triple<SearchItem, Int, String>> { it.second }.thenBy { it.third })
            .take(limit).map { it.first }.toList()
    }
}

fun filterItems(items: List<SearchItem>, query: String, limit: Int = 40): List<SearchItem> =
    SearchIndex(items).search(query, limit)

/** Courses first, then only teachings that can actually open an exam calendar. */
fun examSubjectSearchIndex(items: List<SearchItem>): SearchIndex =
    SearchIndex(items.filter { it.kind == SearchKind.SUBJECT && it.examSources.isNotEmpty() })

fun searchExamCatalogs(courses: SearchIndex?, subjects: SearchIndex?, query: String): List<SearchItem> =
    courses?.search(query, limit = 20).orEmpty() + subjects?.search(query, limit = 20).orEmpty()

fun SearchItem.withFallbackTeachings(candidates: List<CourseTeachingCandidate>): SearchItem {
    if (kind != SearchKind.COURSE || coursePaths.any { it.teachings.isNotEmpty() }) return this
    val matching = candidates.filter {
        val parts = it.combinedCode.split('^')
        parts.size >= 3 && parts[0] == code
    }
    if (matching.isEmpty()) return this
    val byPath = matching.groupBy { it.combinedCode.split('^')[1] }
    val originalByBase = coursePaths.groupBy { it.code.substringBefore('|') }
    val rebuilt = byPath.map { (pathCode, values) ->
        val original = originalByBase[pathCode].orEmpty()
        val label = if (original.size == 1) original.single().name else "Insegnamenti disponibili"
        CoursePath("catalog:$pathCode", label, values.map { it.teaching }
            .distinctBy { it.code }.sortedBy { normalize(it.name) })
    }.sortedBy { normalize(it.name) }
    return copy(coursePaths = rebuilt)
}

private fun normalize(value: String): String = Normalizer.normalize(
    value.lowercase(Locale.ROOT).trim(), Normalizer.Form.NFD
).replace(Regex("\\p{M}+"), "")
