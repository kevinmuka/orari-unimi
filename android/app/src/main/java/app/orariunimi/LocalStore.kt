package app.orariunimi

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** Device-local preferences; no account or cloud service is required. */
class LocalStore(context: Context) {
    private val preferences = context.getSharedPreferences("orari_unimi", Context.MODE_PRIVATE)

    companion object {
        private val notificationLock = Any()
        private const val MAX_NOTIFICATION_ENTRIES = 100
    }

    var showWeekend: Boolean
        get() = preferences.getBoolean("show_weekend", false)
        set(value) { preferences.edit().putBoolean("show_weekend", value).commit() }

    var notificationPreferences: NotificationPreferences
        get() {
            val previousLessonSetting = preferences.getBoolean("notifications_important_changes", false)
            return NotificationPreferences(
                enabled = preferences.getBoolean("notifications_enabled", false),
                lessonChanges = preferences.getBoolean("notifications_lesson_changes", previousLessonSetting),
                examChanges = preferences.getBoolean("notifications_exam_changes", false),
                examRegistrationOpenReminders = preferences.getBoolean(
                    "notifications_exam_registration_open", false),
                examRegistrationCloseReminders = preferences.getBoolean(
                    "notifications_exam_registration_close", false),
                examRegistrationCloseLeadDays = preferences.getInt(
                    "notifications_exam_registration_close_days", 3).takeIf { it in setOf(1, 3, 7) } ?: 3,
                lessonReminders = preferences.getBoolean("notifications_lesson_reminders", false),
                appUpdates = preferences.getBoolean("notifications_app_updates", false),
                reminderMinutes = preferences.getInt("notifications_reminder_minutes", 30)
                    .takeIf { it in setOf(15, 30, 60) } ?: 30
            )
        }
        set(value) {
            preferences.edit()
                .putBoolean("notifications_enabled", value.enabled)
                .putBoolean("notifications_lesson_changes", value.lessonChanges)
                .putBoolean("notifications_important_changes", value.lessonChanges)
                .putBoolean("notifications_exam_changes", value.examChanges)
                .putBoolean("notifications_exam_registration_open", value.examRegistrationOpenReminders)
                .putBoolean("notifications_exam_registration_close", value.examRegistrationCloseReminders)
                .putInt("notifications_exam_registration_close_days", value.examRegistrationCloseLeadDays)
                .putBoolean("notifications_lesson_reminders", value.lessonReminders)
                .putBoolean("notifications_app_updates", value.appUpdates)
                .putInt("notifications_reminder_minutes", value.reminderMinutes)
                .commit()
        }

    var lastLessonReminderKey: String?
        get() = preferences.getString("last_lesson_reminder_key", null)
        set(value) { preferences.edit().putString("last_lesson_reminder_key", value).commit() }

    fun scheduledLessonReminder(): Pair<String, Long>? {
        val key = preferences.getString("scheduled_lesson_reminder_key", null) ?: return null
        val atMillis = preferences.getLong("scheduled_lesson_reminder_at", -1L)
        return if (atMillis >= 0L) key to atMillis else null
    }

    fun setScheduledLessonReminder(key: String, atMillis: Long) {
        preferences.edit()
            .putString("scheduled_lesson_reminder_key", key)
            .putLong("scheduled_lesson_reminder_at", atMillis)
            .commit()
    }

    fun clearScheduledLessonReminder() {
        preferences.edit()
            .remove("scheduled_lesson_reminder_key")
            .remove("scheduled_lesson_reminder_at")
            .commit()
    }

    fun scheduledExamRegistrationReminders(): Map<String, Long> = try {
        val values = JSONObject(preferences.getString("scheduled_exam_registration_reminders", "{}") ?: "{}")
        values.keys().asSequence().associateWith { values.optLong(it) }
    } catch (_: Exception) {
        emptyMap()
    }

    fun setScheduledExamRegistrationReminders(values: Map<String, Long>) {
        val json = JSONObject().apply { values.forEach { (key, atMillis) -> put(key, atMillis) } }
        preferences.edit().putString("scheduled_exam_registration_reminders", json.toString()).commit()
    }

    var lastUpdateNotificationVersion: String?
        get() = preferences.getString("last_update_notification_version", null)
        set(value) { preferences.edit().putString("last_update_notification_version", value).commit() }

    var cachedReleaseNotes: ReleaseNotes?
        get() = try {
            val value = JSONObject(preferences.getString("cached_release_notes", null) ?: return null)
            val version = value.optString("version")
            val body = value.optString("body")
            if (version.isBlank() || body.isBlank()) null else ReleaseNotes(
                version, value.optInt("versionCode").takeIf { it > 0 }, body)
        } catch (_: Exception) {
            null
        }
        set(value) {
            val editor = preferences.edit()
            if (value == null) editor.remove("cached_release_notes")
            else editor.putString("cached_release_notes", JSONObject().apply {
                put("version", value.version)
                value.versionCode?.let { put("versionCode", it) }
                put("body", value.body)
            }.toString())
            editor.commit()
        }

    fun notifications(): List<AppNotificationEntry> = synchronized(notificationLock) {
        readNotifications()
    }

    fun addNotification(entry: AppNotificationEntry): Boolean = synchronized(notificationLock) {
        val current = readNotifications()
        if (current.any { it.id == entry.id }) return@synchronized false
        writeNotifications((listOf(entry) + current).take(MAX_NOTIFICATION_ENTRIES))
        true
    }

    fun deleteNotification(id: String) = synchronized(notificationLock) {
        writeNotifications(readNotifications().filterNot { it.id == id })
    }

    fun markNotificationsRead() = synchronized(notificationLock) {
        val current = readNotifications()
        if (current.any { !it.read }) writeNotifications(current.map { it.copy(read = true) })
    }

    fun clearNotifications() = synchronized(notificationLock) { writeNotifications(emptyList()) }

    fun saved(): List<SavedSubject> = try {
        val values = JSONArray(preferences.getString("saved_subjects", "[]"))
        (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            val year = value.optString("year")
            val code = value.optString("code")
            val name = value.optString("name")
            if (year.isBlank() || code.isBlank() || name.isBlank()) null else SavedSubject(year, code, name)
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun add(subject: SavedSubject): Boolean {
        val items = saved()
        if (items.any { it.year == subject.year && it.code == subject.code }) return false
        save(items + subject)
        return true
    }

    fun remove(subject: SavedSubject) = save(saved().filterNot { it.year == subject.year && it.code == subject.code })

    fun clear() = save(emptyList())

    fun favoriteCourses(): List<FavoriteCourse> = try {
        val values = JSONArray(preferences.getString("favorite_courses", "[]"))
        (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            val year = value.optString("year")
            val code = value.optString("code")
            val name = value.optString("name")
            if (year.isBlank() || code.isBlank() || name.isBlank()) null else FavoriteCourse(
                year, code, name,
                runCatching { DegreeType.valueOf(value.optString("degreeType")) }.getOrDefault(DegreeType.OTHER)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun addFavoriteCourse(course: FavoriteCourse) {
        val items = favoriteCourses()
        if (items.none { it.year == course.year && it.code == course.code }) saveFavoriteCourses(items + course)
    }

    fun removeFavoriteCourse(course: FavoriteCourse) = saveFavoriteCourses(
        favoriteCourses().filterNot { it.year == course.year && it.code == course.code }
    )

    fun followedExamCourses(): List<FavoriteCourse> = readCourses("followed_exam_courses")

    fun addFollowedExamCourse(course: FavoriteCourse) {
        val items = followedExamCourses()
        if (items.none { it.year == course.year && it.code == course.code }) {
            saveCourses("followed_exam_courses", items + course)
        }
    }

    fun removeFollowedExamCourse(course: FavoriteCourse) = saveCourses(
        "followed_exam_courses",
        followedExamCourses().filterNot { it.year == course.year && it.code == course.code }
    )

    fun followedExamSubjects(): List<FollowedExamSubject> = try {
        val values = JSONArray(preferences.getString("followed_exam_subjects", "[]"))
        (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            val year = value.optString("year")
            val code = value.optString("code")
            val name = value.optString("name")
            if (year.isBlank() || code.isBlank() || name.isBlank()) return@mapNotNull null
            val sourcesJson = value.optJSONArray("sources") ?: JSONArray()
            val sources = (0 until sourcesJson.length()).mapNotNull sourceLoop@ { sourceIndex ->
                val source = sourcesJson.optJSONObject(sourceIndex) ?: return@sourceLoop null
                val courseCode = source.optString("courseCode")
                val examSubjectCode = source.optString("examSubjectCode")
                if (courseCode.isBlank() || examSubjectCode.isBlank()) null
                else ExamSubjectSource(courseCode, examSubjectCode)
            }.distinct()
            FollowedExamSubject(year, code, name, sources)
        }
    } catch (_: Exception) {
        emptyList()
    }

    fun addFollowedExamSubject(subject: FollowedExamSubject) {
        val items = followedExamSubjects()
        if (items.none { it.year == subject.year && it.code == subject.code }) {
            saveFollowedExamSubjects(items + subject)
        }
    }

    fun removeFollowedExamSubject(subject: FollowedExamSubject) = saveFollowedExamSubjects(
        followedExamSubjects().filterNot { it.year == subject.year && it.code == subject.code }
    )

    fun excludedExamSubjects(): Set<Pair<String, String>> = try {
        val values = JSONArray(preferences.getString("excluded_exam_subjects", "[]"))
        (0 until values.length()).mapNotNull { index ->
            val item = values.optJSONObject(index) ?: return@mapNotNull null
            val year = item.optString("year")
            val code = item.optString("code")
            if (year.isBlank() || code.isBlank()) null else year to code
        }.toSet()
    } catch (_: Exception) {
        emptySet()
    }

    fun setExamSubjectExcluded(year: String, code: String, excluded: Boolean) {
        val updated = excludedExamSubjects().toMutableSet()
        if (excluded) updated.add(year to code) else updated.remove(year to code)
        val values = JSONArray()
        updated.forEach { (itemYear, itemCode) ->
            values.put(JSONObject().put("year", itemYear).put("code", itemCode))
        }
        preferences.edit().putString("excluded_exam_subjects", values.toString()).commit()
    }

    fun clearExcludedExamSubjects() {
        preferences.edit().remove("excluded_exam_subjects").commit()
    }

    fun savedExamAppeals(): List<ExamAppeal> = try {
        val values = JSONArray(preferences.getString("saved_exam_appeals", "[]"))
        val parsed = (0 until values.length()).mapNotNull { index ->
            values.optJSONObject(index)?.toExamAppeal()
        }
        val (expired, active) = parsed.partition { it.date.isBefore(java.time.LocalDate.now()) }
        if (expired.isNotEmpty()) {
            saveExamAppeals(active)
            removeExamNotes(expired)
        }
        active.sortedWith(compareBy<ExamAppeal> { it.date }.thenBy { it.time }.thenBy { it.subjectName })
    } catch (_: Exception) {
        emptyList()
    }

    fun addSavedExamAppeal(appeal: ExamAppeal): Boolean {
        val items = savedExamAppeals()
        if (items.any { it.id == appeal.id && it.courseCode == appeal.courseCode }) return false
        saveExamAppeals(items + appeal)
        return true
    }

    fun removeSavedExamAppeal(appeal: ExamAppeal) {
        saveExamAppeals(savedExamAppeals().filterNot {
            it.id == appeal.id && it.courseCode == appeal.courseCode
        })
        removeExamNotes(listOf(appeal))
    }

    fun removeSavedExamAppeals(appeals: List<ExamAppeal>) {
        if (appeals.isEmpty()) return
        val identities = appeals.map { ExamNotificationEngine.identity(it) }.toSet()
        saveExamAppeals(savedExamAppeals().filterNot {
            ExamNotificationEngine.identity(it) in identities
        })
        removeExamNotes(appeals)
    }

    fun removedExamNotices(): List<RemovedExamNotice> = try {
        val values = JSONArray(preferences.getString("removed_exam_notices", "[]"))
        (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            val appeal = value.optJSONObject("appeal")?.toExamAppeal() ?: return@mapNotNull null
            RemovedExamNotice(value.getString("id"), appeal, value.optLong("detectedAtMillis"))
        }.sortedByDescending { it.detectedAtMillis }
    } catch (_: Exception) {
        emptyList()
    }

    fun addRemovedExamNotices(appeals: List<ExamAppeal>, detectedAtMillis: Long) {
        if (appeals.isEmpty()) return
        val current = removedExamNotices().associateBy { it.id }.toMutableMap()
        appeals.forEach { appeal ->
            val id = "removed-${ExamNotificationEngine.identity(appeal)}"
            current[id] = RemovedExamNotice(id, appeal, detectedAtMillis)
        }
        saveRemovedExamNotices(current.values.sortedByDescending { it.detectedAtMillis }.take(20))
    }

    fun dismissRemovedExamNotice(id: String) = saveRemovedExamNotices(
        removedExamNotices().filterNot { it.id == id }
    )

    fun examNote(appeal: ExamAppeal): String = try {
        JSONObject(preferences.getString("saved_exam_notes", "{}") ?: "{}")
            .optString(examAppealKey(appeal))
    } catch (_: Exception) {
        ""
    }

    fun setExamNote(appeal: ExamAppeal, note: String) {
        val notes = try {
            JSONObject(preferences.getString("saved_exam_notes", "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        val normalized = note.trim()
        if (normalized.isBlank()) notes.remove(examAppealKey(appeal))
        else notes.put(examAppealKey(appeal), normalized)
        preferences.edit().putString("saved_exam_notes", notes.toString()).commit()
    }

    fun refreshSavedExamAppeals(freshAppeals: List<ExamAppeal>): Boolean {
        val current = savedExamAppeals()
        if (current.isEmpty() || freshAppeals.isEmpty()) return false
        val freshById = freshAppeals.associateBy { it.courseCode to it.id }
        val updated = current.map { saved -> freshById[saved.courseCode to saved.id] ?: saved }
        if (updated == current) return false
        saveExamAppeals(updated)
        return true
    }

    private fun save(items: List<SavedSubject>) {
        val values = JSONArray()
        items.forEach { item ->
            values.put(JSONObject().put("year", item.year).put("code", item.code).put("name", item.name))
        }
        preferences.edit().putString("saved_subjects", values.toString()).commit()
    }

    private fun saveFavoriteCourses(items: List<FavoriteCourse>) {
        saveCourses("favorite_courses", items)
    }

    private fun readCourses(key: String): List<FavoriteCourse> = try {
        val values = JSONArray(preferences.getString(key, "[]"))
        (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            val year = value.optString("year")
            val code = value.optString("code")
            val name = value.optString("name")
            if (year.isBlank() || code.isBlank() || name.isBlank()) null else FavoriteCourse(
                year, code, name,
                runCatching { DegreeType.valueOf(value.optString("degreeType")) }
                    .getOrDefault(DegreeType.OTHER)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun saveCourses(key: String, items: List<FavoriteCourse>) {
        val values = JSONArray()
        items.forEach { item ->
            values.put(JSONObject().put("year", item.year).put("code", item.code)
                .put("name", item.name).put("degreeType", item.degreeType.name))
        }
        preferences.edit().putString(key, values.toString()).commit()
    }

    private fun saveFollowedExamSubjects(items: List<FollowedExamSubject>) {
        val values = JSONArray()
        items.forEach { item ->
            val sources = JSONArray()
            item.sources.distinct().forEach { source ->
                sources.put(JSONObject().put("courseCode", source.courseCode)
                    .put("examSubjectCode", source.examSubjectCode))
            }
            values.put(JSONObject().put("year", item.year).put("code", item.code)
                .put("name", item.name).put("sources", sources))
        }
        preferences.edit().putString("followed_exam_subjects", values.toString()).commit()
    }

    private fun saveExamAppeals(items: List<ExamAppeal>) {
        val values = JSONArray()
        items.forEach { values.put(it.toJson()) }
        preferences.edit().putString("saved_exam_appeals", values.toString()).commit()
    }

    private fun saveRemovedExamNotices(items: List<RemovedExamNotice>) {
        val values = JSONArray().apply { items.forEach { notice ->
            put(JSONObject().put("id", notice.id).put("appeal", notice.appeal.toJson())
                .put("detectedAtMillis", notice.detectedAtMillis))
        } }
        preferences.edit().putString("removed_exam_notices", values.toString()).commit()
    }

    private fun ExamAppeal.toJson() = JSONObject()
        .put("id", id).put("courseCode", courseCode).put("courseName", courseName)
        .put("subjectCode", subjectCode).put("subjectPortalCode", subjectPortalCode)
        .put("subjectName", subjectName).put("date", date.toString()).put("time", time)
        .put("location", location).put("teacher", teacher).put("surnameFrom", surnameFrom)
        .put("surnameTo", surnameTo).put("testType", testType).put("appealType", appealType)
        .put("registrationOpen", registrationOpen?.toString().orEmpty())
        .put("registrationClose", registrationClose?.toString().orEmpty())

    private fun JSONObject.toExamAppeal(): ExamAppeal? = runCatching {
        ExamAppeal(
            id = getString("id"), courseCode = getString("courseCode"),
            courseName = optString("courseName"), subjectCode = optString("subjectCode"),
            subjectPortalCode = optString("subjectPortalCode"), subjectName = getString("subjectName"),
            date = java.time.LocalDate.parse(getString("date")), time = optString("time"),
            location = optString("location"), teacher = optString("teacher"),
            surnameFrom = optString("surnameFrom"), surnameTo = optString("surnameTo"),
            testType = optString("testType"), appealType = optString("appealType"),
            registrationOpen = optString("registrationOpen").takeIf { it.isNotBlank() }
                ?.let(java.time.LocalDate::parse),
            registrationClose = optString("registrationClose").takeIf { it.isNotBlank() }
                ?.let(java.time.LocalDate::parse)
        )
    }.getOrNull()

    private fun removeExamNotes(appeals: List<ExamAppeal>) {
        if (appeals.isEmpty()) return
        val notes = try {
            JSONObject(preferences.getString("saved_exam_notes", "{}") ?: "{}")
        } catch (_: Exception) {
            JSONObject()
        }
        var changed = false
        appeals.forEach { appeal ->
            if (notes.has(examAppealKey(appeal))) {
                notes.remove(examAppealKey(appeal))
                changed = true
            }
        }
        if (changed) preferences.edit().putString("saved_exam_notes", notes.toString()).commit()
    }

    private fun examAppealKey(appeal: ExamAppeal): String = "${appeal.courseCode}:${appeal.id}"

    private fun readNotifications(): List<AppNotificationEntry> = try {
        val values = JSONArray(preferences.getString("notification_inbox", "[]"))
        (0 until values.length()).mapNotNull { index ->
            val value = values.optJSONObject(index) ?: return@mapNotNull null
            val id = value.optString("id")
            val title = value.optString("title")
            if (id.isBlank() || title.isBlank()) return@mapNotNull null
            AppNotificationEntry(
                id = id,
                type = runCatching { AppNotificationType.valueOf(value.optString("type")) }
                    .getOrDefault(AppNotificationType.IMPORTANT_CHANGE),
                title = title,
                message = value.optString("message"),
                timestampMillis = value.optLong("timestampMillis"),
                targetDate = value.optString("targetDate").takeIf { it.isNotBlank() }
                    ?.let { runCatching { java.time.LocalDate.parse(it) }.getOrNull() },
                targetKey = value.optString("targetKey").takeIf { it.isNotBlank() },
                targetTitle = value.optString("targetTitle").takeIf { it.isNotBlank() },
                changes = value.optJSONArray("changes")?.let { changes ->
                    (0 until changes.length()).mapNotNull { changeIndex ->
                        changes.optJSONObject(changeIndex)?.let { change ->
                            val label = change.optString("label")
                            if (label.isBlank()) null else NotificationFieldChange(
                                label, change.optString("before"), change.optString("after"))
                        }
                    }
                }.orEmpty(),
                read = value.optBoolean("read", false)
            )
        }
    } catch (_: Exception) {
        emptyList()
    }

    private fun writeNotifications(items: List<AppNotificationEntry>) {
        val values = JSONArray()
        items.forEach { item ->
            values.put(JSONObject()
                .put("id", item.id)
                .put("type", item.type.name)
                .put("title", item.title)
                .put("message", item.message)
                .put("timestampMillis", item.timestampMillis)
                .put("targetDate", item.targetDate?.toString().orEmpty())
                .put("targetKey", item.targetKey.orEmpty())
                .put("targetTitle", item.targetTitle.orEmpty())
                .put("changes", JSONArray().apply { item.changes.forEach { change ->
                    put(JSONObject().put("label", change.label).put("before", change.before)
                        .put("after", change.after))
                } })
                .put("read", item.read))
        }
        preferences.edit().putString("notification_inbox", values.toString()).commit()
    }
}
