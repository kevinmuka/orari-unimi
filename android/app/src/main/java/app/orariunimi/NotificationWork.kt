package app.orariunimi

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import java.io.File
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

object NotificationScheduler {
    private const val SCHEDULE_PERIODIC = "schedule-notifications-periodic"
    private const val SCHEDULE_NOW = "schedule-notifications-now"
    private const val EXAMS_PERIODIC = "exam-notifications-periodic"
    private const val EXAMS_NOW = "exam-notifications-now"
    private const val UPDATES_PERIODIC = "update-notifications-periodic"
    private const val UPDATES_NOW = "update-notifications-now"
    private const val LESSON_REMINDER = "next-lesson-reminder"
    internal const val INTERNAL_ONLY = "internal-only"
    internal const val FORCE_REFRESH = "force-refresh"
    private const val REMINDER_KEY = "reminder-key"
    private const val REMINDER_ID = "reminder-id"
    private const val REMINDER_TITLE = "reminder-title"
    private const val REMINDER_MESSAGE = "reminder-message"
    private const val REMINDER_DATE = "reminder-date"
    internal const val EXAM_REMINDER_KEY = "exam-reminder-key"
    internal const val EXAM_REMINDER_BOUNDARY = "exam-reminder-boundary"
    internal const val EXAM_REMINDER_EXPECTED_DATE = "exam-reminder-expected-date"
    internal const val EXAM_REMINDER_TITLE = "exam-reminder-title"
    internal const val EXAM_REMINDER_MESSAGE = "exam-reminder-message"
    internal const val EXAM_REMINDER_APPEAL_DATE = "exam-reminder-appeal-date"
    private val network = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

    fun configure(context: Context, runNow: Boolean = false) {
        val appContext = context.applicationContext
        val work = WorkManager.getInstance(appContext)
        val preferences = LocalStore(appContext).notificationPreferences
        if (!preferences.enabled) {
            work.cancelUniqueWork(SCHEDULE_PERIODIC)
            work.cancelUniqueWork(SCHEDULE_NOW)
            work.cancelUniqueWork(EXAMS_PERIODIC)
            work.cancelUniqueWork(EXAMS_NOW)
            work.cancelUniqueWork(UPDATES_PERIODIC)
            work.cancelUniqueWork(UPDATES_NOW)
            cancelLessonReminder(appContext)
            cancelExamRegistrationReminders(appContext)
            NotificationBaselineStore(appContext).clear()
            ExamNotificationBaselineStore(appContext).clear()
            NotificationPublisher.cancelAll(appContext)
            return
        }

        NotificationPublisher.createChannels(appContext)
        if (preferences.lessonChanges || preferences.lessonReminders) {
            val periodic = PeriodicWorkRequestBuilder<ScheduleNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(network).build()
            work.enqueueUniquePeriodicWork(SCHEDULE_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        } else {
            work.cancelUniqueWork(SCHEDULE_PERIODIC)
            work.cancelUniqueWork(SCHEDULE_NOW)
        }
        if (!preferences.lessonReminders) cancelLessonReminder(appContext)

        val examChecksEnabled = preferences.examChanges ||
            preferences.examRegistrationOpenReminders || preferences.examRegistrationCloseReminders
        if (examChecksEnabled) {
            val periodic = PeriodicWorkRequestBuilder<ExamNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(network).build()
            work.enqueueUniquePeriodicWork(EXAMS_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        } else {
            work.cancelUniqueWork(EXAMS_PERIODIC)
            work.cancelUniqueWork(EXAMS_NOW)
        }

        reconcileExamRegistrationReminders(appContext)

        if (preferences.appUpdates) {
            val periodic = PeriodicWorkRequestBuilder<AppUpdateNotificationWorker>(15, TimeUnit.MINUTES)
                .setConstraints(network).build()
            work.enqueueUniquePeriodicWork(UPDATES_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, periodic)
        } else {
            work.cancelUniqueWork(UPDATES_PERIODIC)
            work.cancelUniqueWork(UPDATES_NOW)
        }

        if (runNow) enqueueNow(work, preferences)
    }

    fun refreshNow(context: Context): List<UUID> {
        val appContext = context.applicationContext
        val preferences = LocalStore(appContext).notificationPreferences
        if (!preferences.enabled) return emptyList()
        return enqueueNow(WorkManager.getInstance(appContext), preferences, forceRefresh = true)
    }

    fun refreshLessonsNow(context: Context): UUID? {
        val appContext = context.applicationContext
        val preferences = LocalStore(appContext).notificationPreferences
        if (!preferences.enabled || !preferences.lessonChanges && !preferences.lessonReminders) return null
        val request = OneTimeWorkRequestBuilder<ScheduleNotificationWorker>()
            .setInputData(workDataOf(INTERNAL_ONLY to true, FORCE_REFRESH to true))
            .setConstraints(network).build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(SCHEDULE_NOW, ExistingWorkPolicy.REPLACE, request)
        return request.id
    }

    fun refreshExamsNow(context: Context): UUID? {
        val appContext = context.applicationContext
        val preferences = LocalStore(appContext).notificationPreferences
        if (!preferences.enabled || !preferences.examChanges &&
            !preferences.examRegistrationOpenReminders && !preferences.examRegistrationCloseReminders) return null
        val request = OneTimeWorkRequestBuilder<ExamNotificationWorker>()
            .setInputData(workDataOf(INTERNAL_ONLY to true, FORCE_REFRESH to true))
            .setConstraints(network).build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(EXAMS_NOW, ExistingWorkPolicy.REPLACE, request)
        return request.id
    }

    fun scheduleLessonReminder(
        context: Context,
        lesson: Lesson,
        start: LocalDateTime,
        leadMinutes: Int,
        now: LocalDateTime = LocalDateTime.now()
    ) {
        val appContext = context.applicationContext
        val store = LocalStore(appContext)
        val key = "${NotificationEngine.lessonIdentity(lesson)}|${lesson.date}|${lesson.start}"
        if (store.lastLessonReminderKey == key) return
        val target = start.minusMinutes(leadMinutes.toLong())
        val targetMillis = target.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        if (store.scheduledLessonReminder() == (key to targetMillis)) return
        val message = "${lesson.subject} alle ${lesson.start}${if (lesson.room.isBlank()) "" else " · ${lesson.room}"}"
        val request = OneTimeWorkRequestBuilder<LessonReminderWorker>()
            .setInitialDelay(reminderDelayMillis(start, leadMinutes, now), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(
                REMINDER_KEY to key,
                REMINDER_ID to "reminder-${NotificationEngine.lessonFingerprint(lesson)}",
                REMINDER_TITLE to "Prossima lezione",
                REMINDER_MESSAGE to message,
                REMINDER_DATE to lesson.date.toString()
            ))
            .build()
        WorkManager.getInstance(appContext)
            .enqueueUniqueWork(LESSON_REMINDER, ExistingWorkPolicy.REPLACE, request)
        store.setScheduledLessonReminder(key, targetMillis)
    }

    fun cancelLessonReminder(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(LESSON_REMINDER)
        LocalStore(context.applicationContext).clearScheduledLessonReminder()
    }

    fun reconcileExamRegistrationReminders(context: Context) {
        val appContext = context.applicationContext
        val store = LocalStore(appContext)
        val preferences = store.notificationPreferences
        val work = WorkManager.getInstance(appContext)
        val previous = store.scheduledExamRegistrationReminders()
        if (!preferences.enabled || !preferences.examRegistrationOpenReminders &&
            !preferences.examRegistrationCloseReminders) {
            previous.keys.forEach(work::cancelUniqueWork)
            store.setScheduledExamRegistrationReminders(emptyMap())
            return
        }
        val plans = ExamRegistrationReminderPlanner.plans(store.savedExamAppeals(), preferences)
        val desired = plans.associate { it.workName to it.triggerAtMillis }
        (previous.keys - desired.keys).forEach(work::cancelUniqueWork)
        plans.forEach { plan ->
            if (previous[plan.workName] == plan.triggerAtMillis) return@forEach
            val delay = (plan.triggerAtMillis - System.currentTimeMillis()).coerceAtLeast(0L)
            val request = OneTimeWorkRequestBuilder<ExamRegistrationReminderWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(
                    EXAM_REMINDER_KEY to plan.appealKey,
                    EXAM_REMINDER_BOUNDARY to plan.boundary.name,
                    EXAM_REMINDER_EXPECTED_DATE to plan.expectedDate.toString(),
                    EXAM_REMINDER_TITLE to plan.title,
                    EXAM_REMINDER_MESSAGE to plan.message,
                    EXAM_REMINDER_APPEAL_DATE to plan.appealDate.toString()
                )).build()
            work.enqueueUniqueWork(plan.workName, ExistingWorkPolicy.REPLACE, request)
        }
        store.setScheduledExamRegistrationReminders(desired)
    }

    fun cancelExamRegistrationReminders(context: Context) {
        val appContext = context.applicationContext
        val store = LocalStore(appContext)
        val work = WorkManager.getInstance(appContext)
        store.scheduledExamRegistrationReminders().keys.forEach(work::cancelUniqueWork)
        store.setScheduledExamRegistrationReminders(emptyMap())
    }

    fun waitForRefresh(context: Context, ids: List<UUID>, timeoutMillis: Long = 30_000L): Boolean {
        if (ids.isEmpty()) return false
        val work = WorkManager.getInstance(context.applicationContext)
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMillis)
        while (System.nanoTime() < deadline) {
            val states = runCatching {
                ids.mapNotNull { work.getWorkInfoById(it).get()?.state }
            }.getOrElse { return false }
            if (states.size == ids.size && states.all { it.isFinished }) {
                return states.all { it == androidx.work.WorkInfo.State.SUCCEEDED }
            }
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
                return false
            }
        }
        return false
    }

    private fun enqueueNow(
        work: WorkManager,
        preferences: NotificationPreferences,
        forceRefresh: Boolean = false
    ): List<UUID> {
        val ids = mutableListOf<UUID>()
        if (preferences.lessonChanges || preferences.lessonReminders) {
            val request = OneTimeWorkRequestBuilder<ScheduleNotificationWorker>()
                .setInputData(workDataOf(INTERNAL_ONLY to true, FORCE_REFRESH to forceRefresh))
                .setConstraints(network).build()
            work.enqueueUniqueWork(SCHEDULE_NOW, ExistingWorkPolicy.REPLACE, request)
            ids += request.id
        }
        if (preferences.examChanges || preferences.examRegistrationOpenReminders ||
            preferences.examRegistrationCloseReminders) {
            val request = OneTimeWorkRequestBuilder<ExamNotificationWorker>()
                .setInputData(workDataOf(INTERNAL_ONLY to true, FORCE_REFRESH to forceRefresh))
                .setConstraints(network).build()
            work.enqueueUniqueWork(EXAMS_NOW, ExistingWorkPolicy.REPLACE, request)
            ids += request.id
        }
        if (preferences.appUpdates) {
            val request = OneTimeWorkRequestBuilder<AppUpdateNotificationWorker>()
                .setInputData(workDataOf(INTERNAL_ONLY to true))
                .setConstraints(network).build()
            work.enqueueUniqueWork(UPDATES_NOW, ExistingWorkPolicy.REPLACE, request)
            ids += request.id
        }
        return ids
    }
}

class ScheduleNotificationWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val allowSystemNotification = !inputData.getBoolean(NotificationScheduler.INTERNAL_ONLY, false)
        val forceRefresh = inputData.getBoolean(NotificationScheduler.FORCE_REFRESH, false)
        val store = LocalStore(applicationContext)
        val preferences = store.notificationPreferences
        if (!preferences.enabled || !preferences.lessonChanges && !preferences.lessonReminders) return Result.success()
        val saved = store.saved()
        val selectionKey = NotificationEngine.lessonSelectionKey(saved)
        val baselineStore = NotificationBaselineStore(applicationContext)
        val diagnostics = SyncDiagnosticsStore(applicationContext)
        val sourceCount = NotificationEngine.lessonSourceCount(saved)
        if (saved.isEmpty()) {
            baselineStore.clear()
            diagnostics.recordSuccess(SyncDiagnosticKind.LESSONS, 0, 0, 0, 0)
            return Result.success()
        }

        val startedAt = System.currentTimeMillis()
        diagnostics.recordAttempt(SyncDiagnosticKind.LESSONS, sourceCount, startedAt)
        var cacheHits = 0
        var networkRequests = 0
        val snapshot = try {
            val api = UnimiApi(
                cache = ResponseCache(File(applicationContext.cacheDir, "orari-responses")),
                scheduleLoadObserver = { cacheHit ->
                    if (cacheHit) cacheHits++ else networkRequests++
                })
            if (forceRefresh) api.refreshSavedLessons(saved) else api.refreshSavedLessonsIfStale(saved)
        } catch (_: Exception) {
            diagnostics.recordFailure(SyncDiagnosticKind.LESSONS, sourceCount,
                "Impossibile recuperare gli orari")
            return Result.retry()
        }

        val now = LocalDateTime.now()
        if (NotificationEngine.lessonSelectionKey(store.saved()) != selectionKey) return Result.success()
        val changesFound = if (preferences.lessonChanges) processChanges(
            store, baselineStore, snapshot.lessons, snapshot.updatedAtMillis, selectionKey,
            now.toLocalDate(), allowSystemNotification)
        else 0
        if (preferences.lessonReminders) processReminder(
            snapshot.lessons, now, preferences.reminderMinutes)
        diagnostics.recordSuccess(SyncDiagnosticKind.LESSONS, sourceCount,
            networkRequests = networkRequests,
            cacheHits = cacheHits,
            changesFound = changesFound)
        return Result.success()
    }

    private fun processChanges(
        store: LocalStore,
        baselineStore: NotificationBaselineStore,
        current: List<Lesson>,
        sourceUpdatedAtMillis: Long,
        selectionKey: String,
        today: LocalDate,
        allowSystemNotification: Boolean
    ): Int {
        val future = current.filter { !it.date.isBefore(today) }
        val baseline = baselineStore.read()
        if (baseline == null || baseline.selectionKey != selectionKey) {
            baselineStore.write(NotificationBaseline(selectionKey, future,
                sourceUpdatedAtMillis = sourceUpdatedAtMillis))
            return 0
        }
        if (!isNewSourceSnapshot(baseline.sourceUpdatedAtMillis, sourceUpdatedAtMillis)) return 0

        val oldFuture = baseline.lessons.filter { !it.date.isBefore(today) }
        if (NotificationEngine.shouldDeferEmptySnapshot(
                oldFuture, future, baseline.consecutiveEmptyResponses, today)) {
            baselineStore.write(baseline.copy(
                lessons = oldFuture,
                consecutiveEmptyResponses = baseline.consecutiveEmptyResponses + 1,
                sourceUpdatedAtMillis = sourceUpdatedAtMillis
            ))
            return 0
        }
        val priorMissing = if (future.isEmpty() && oldFuture.isNotEmpty()) {
            oldFuture.associate { NotificationEngine.lessonIdentity(it) to
                maxOf(1, baseline.missingCounts.getOrDefault(NotificationEngine.lessonIdentity(it), 0)) }
        } else baseline.missingCounts
        val evaluation = NotificationEngine.evaluate(oldFuture, future, priorMissing, today)
        baselineStore.write(NotificationBaseline(selectionKey, evaluation.lessons, evaluation.missingCounts,
            consecutiveEmptyResponses = 0, sourceUpdatedAtMillis = sourceUpdatedAtMillis))

        val added = evaluation.drafts.map { draft ->
            AppNotificationEntry(draft.id, AppNotificationType.LESSON_CHANGE, draft.title,
                draft.message, System.currentTimeMillis(), draft.targetDate, draft.targetKey,
                draft.targetTitle, draft.changes)
        }.filter { store.addNotification(it) }
        when {
            added.size == 1 -> NotificationPublisher.post(
                applicationContext, added.single(), allowSystemNotification)
            added.size > 1 -> NotificationPublisher.postSummary(
                applicationContext,
                type = AppNotificationType.LESSON_CHANGE,
                count = added.size,
                title = "Lezioni aggiornate",
                message = "${added.size} variazioni rilevate nei tuoi orari",
                allowSystemNotification = allowSystemNotification)
        }
        return evaluation.drafts.size
    }

    private fun processReminder(
        lessons: List<Lesson>,
        now: LocalDateTime,
        leadMinutes: Int
    ) {
        val next = lessons.asSequence().filter { !it.cancelled }.mapNotNull { lesson ->
            val start = runCatching {
                LocalDateTime.of(lesson.date, LocalTime.parse(lesson.start, DateTimeFormatter.ofPattern("H:mm")))
            }.getOrNull() ?: return@mapNotNull null
            if (start.isBefore(now)) null else lesson to start
        }.minByOrNull { it.second } ?: run {
            NotificationScheduler.cancelLessonReminder(applicationContext)
            return
        }
        val lesson = next.first
        NotificationScheduler.scheduleLessonReminder(applicationContext, lesson, next.second, leadMinutes, now)
    }
}

class ExamNotificationWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val allowSystemNotification = !inputData.getBoolean(NotificationScheduler.INTERNAL_ONLY, false)
        val forceRefresh = inputData.getBoolean(NotificationScheduler.FORCE_REFRESH, false)
        val store = LocalStore(applicationContext)
        val preferences = store.notificationPreferences
        if (!preferences.enabled || !preferences.examChanges &&
            !preferences.examRegistrationOpenReminders && !preferences.examRegistrationCloseReminders) {
            return Result.success()
        }

        val saved = store.savedExamAppeals()
        val baselineStore = ExamNotificationBaselineStore(applicationContext)
        val diagnostics = SyncDiagnosticsStore(applicationContext)
        if (saved.isEmpty()) {
            baselineStore.clear()
            diagnostics.recordSuccess(SyncDiagnosticKind.EXAMS, 0, 0, 0, 0)
            NotificationScheduler.reconcileExamRegistrationReminders(applicationContext)
            return Result.success()
        }
        val selectionKey = ExamNotificationEngine.selectionKey(saved)
        val savedKeys = saved.map(ExamNotificationEngine::identity).toSet()
        val courseCodes = ExamNotificationEngine.courseCodes(saved)
        val startedAt = System.currentTimeMillis()
        diagnostics.recordAttempt(SyncDiagnosticKind.EXAMS, courseCodes.size, startedAt)
        var cacheHits = 0
        var networkRequests = 0
        val api = ExamApi(
            cache = ResponseCache(File(applicationContext.cacheDir, "orari-responses")),
            loadObserver = { cacheHit -> if (cacheHit) cacheHits++ else networkRequests++ })
        val snapshots = try {
            courseCodes.map { courseCode ->
                val snapshot = if (forceRefresh) api.refreshPublishedAppeals(courseCode, ExamWindow.ALL)
                    else api.refreshPublishedAppealsIfStale(courseCode, ExamWindow.ALL)
                snapshot
            }
        } catch (_: Exception) {
            diagnostics.recordFailure(SyncDiagnosticKind.EXAMS, courseCodes.size,
                "Impossibile recuperare gli appelli")
            return Result.retry()
        }
        if (ExamNotificationEngine.selectionKey(store.savedExamAppeals()) != selectionKey) {
            return Result.success()
        }

        val published = snapshots.flatMap { it.appeals }
        val current = published.filter { ExamNotificationEngine.identity(it) in savedKeys }
        val changesFound = processChanges(store, baselineStore, saved, current,
            snapshots.minOf { it.updatedAtMillis }, selectionKey,
            allowSystemNotification, preferences.examChanges)
        NotificationScheduler.reconcileExamRegistrationReminders(applicationContext)
        diagnostics.recordSuccess(SyncDiagnosticKind.EXAMS, courseCodes.size,
            networkRequests, cacheHits, changesFound)
        return Result.success()
    }

    private fun processChanges(
        store: LocalStore,
        baselineStore: ExamNotificationBaselineStore,
        saved: List<ExamAppeal>,
        current: List<ExamAppeal>,
        sourceUpdatedAtMillis: Long,
        selectionKey: String,
        allowSystemNotification: Boolean,
        notificationsEnabled: Boolean
    ): Int {
        val baseline = baselineStore.read()
        if (baseline == null || baseline.selectionKey != selectionKey) {
            baselineStore.write(ExamNotificationBaseline(selectionKey, current.ifEmpty { saved },
                sourceUpdatedAtMillis = sourceUpdatedAtMillis))
            store.refreshSavedExamAppeals(current)
            return 0
        }
        if (!isNewSourceSnapshot(baseline.sourceUpdatedAtMillis, sourceUpdatedAtMillis)) return 0
        if (ExamNotificationEngine.shouldDeferEmptySnapshot(
                baseline.appeals, current, baseline.consecutiveEmptyResponses)) {
            baselineStore.write(baseline.copy(
                consecutiveEmptyResponses = baseline.consecutiveEmptyResponses + 1,
                sourceUpdatedAtMillis = sourceUpdatedAtMillis))
            return 0
        }
        val priorMissing = if (current.isEmpty() && baseline.appeals.isNotEmpty()) {
            baseline.appeals.associate {
                ExamNotificationEngine.identity(it) to maxOf(
                    1, baseline.missingCounts.getOrDefault(ExamNotificationEngine.identity(it), 0))
            }
        } else baseline.missingCounts
        val evaluation = ExamNotificationEngine.evaluate(baseline.appeals, current, priorMissing)
        baselineStore.write(ExamNotificationBaseline(
            selectionKey, evaluation.appeals, evaluation.missingCounts,
            consecutiveEmptyResponses = 0, sourceUpdatedAtMillis = sourceUpdatedAtMillis))
        store.refreshSavedExamAppeals(current)
        if (evaluation.removedAppeals.isNotEmpty()) {
            store.addRemovedExamNotices(evaluation.removedAppeals, System.currentTimeMillis())
            store.removeSavedExamAppeals(evaluation.removedAppeals)
        }

        val added = if (notificationsEnabled) evaluation.drafts.map { draft ->
            AppNotificationEntry(draft.id, AppNotificationType.EXAM_CHANGE, draft.title,
                draft.message, System.currentTimeMillis(), draft.targetDate, draft.targetKey,
                draft.targetTitle, draft.changes)
        }.filter(store::addNotification) else emptyList()
        when {
            added.size == 1 -> NotificationPublisher.post(
                applicationContext, added.single(), allowSystemNotification)
            added.size > 1 -> NotificationPublisher.postSummary(
                applicationContext,
                type = AppNotificationType.EXAM_CHANGE,
                count = added.size,
                title = "Appelli aggiornati",
                message = "${added.size} variazioni rilevate negli appelli salvati",
                allowSystemNotification = allowSystemNotification
            )
        }
        return evaluation.drafts.size
    }
}

class LessonReminderWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val store = LocalStore(applicationContext)
        val key = inputData.getString("reminder-key") ?: return Result.failure()
        if (!store.notificationPreferences.let { it.enabled && it.lessonReminders }) {
            store.clearScheduledLessonReminder()
            return Result.success()
        }
        if (store.scheduledLessonReminder()?.first != key) return Result.success()
        if (store.lastLessonReminderKey == key) return Result.success()
        val entry = AppNotificationEntry(
            id = inputData.getString("reminder-id") ?: return Result.failure(),
            type = AppNotificationType.LESSON_REMINDER,
            title = inputData.getString("reminder-title") ?: "Prossima lezione",
            message = inputData.getString("reminder-message").orEmpty(),
            timestampMillis = System.currentTimeMillis(),
            targetDate = inputData.getString("reminder-date")?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            }
        )
        store.lastLessonReminderKey = key
        store.clearScheduledLessonReminder()
        if (store.addNotification(entry)) NotificationPublisher.post(applicationContext, entry)
        return Result.success()
    }
}

class ExamRegistrationReminderWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val store = LocalStore(applicationContext)
        val key = inputData.getString(NotificationScheduler.EXAM_REMINDER_KEY) ?: return Result.failure()
        val boundary = inputData.getString(NotificationScheduler.EXAM_REMINDER_BOUNDARY)?.let {
            runCatching { ExamRegistrationBoundary.valueOf(it) }.getOrNull()
        } ?: return Result.failure()
        val expectedDate = inputData.getString(NotificationScheduler.EXAM_REMINDER_EXPECTED_DATE)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        } ?: return Result.failure()
        val preferences = store.notificationPreferences
        val enabled = preferences.enabled && when (boundary) {
            ExamRegistrationBoundary.OPEN -> preferences.examRegistrationOpenReminders
            ExamRegistrationBoundary.CLOSE -> preferences.examRegistrationCloseReminders
        }
        val appeal = store.savedExamAppeals().firstOrNull {
            ExamNotificationEngine.identity(it) == key
        }
        val actualDate = when (boundary) {
            ExamRegistrationBoundary.OPEN -> appeal?.registrationOpen
            ExamRegistrationBoundary.CLOSE -> appeal?.registrationClose
        }
        val workName = "exam-registration-${boundary.name.lowercase()}-${NotificationEngine.digest(key)}"
        val scheduled = store.scheduledExamRegistrationReminders().toMutableMap().apply { remove(workName) }
        store.setScheduledExamRegistrationReminders(scheduled)
        if (!enabled || appeal == null || actualDate != expectedDate) {
            NotificationScheduler.reconcileExamRegistrationReminders(applicationContext)
            return Result.success()
        }
        val entry = AppNotificationEntry(
            id = "exam-registration-${boundary.name.lowercase()}-${NotificationEngine.digest("$key|$expectedDate")}",
            type = AppNotificationType.EXAM_REGISTRATION_REMINDER,
            title = inputData.getString(NotificationScheduler.EXAM_REMINDER_TITLE) ?: "Promemoria iscrizione",
            message = inputData.getString(NotificationScheduler.EXAM_REMINDER_MESSAGE).orEmpty(),
            timestampMillis = System.currentTimeMillis(),
            targetDate = inputData.getString(NotificationScheduler.EXAM_REMINDER_APPEAL_DATE)?.let {
                runCatching { LocalDate.parse(it) }.getOrNull()
            },
            targetKey = key,
            targetTitle = appeal.subjectName
        )
        if (store.addNotification(entry)) NotificationPublisher.post(applicationContext, entry)
        return Result.success()
    }
}

internal fun reminderDelayMillis(
    start: LocalDateTime,
    leadMinutes: Int,
    now: LocalDateTime,
    zone: ZoneId = ZoneId.systemDefault()
): Long = Duration.between(
    now.atZone(zone).toInstant(),
    start.minusMinutes(leadMinutes.toLong()).atZone(zone).toInstant()
).toMillis().coerceAtLeast(0L)

class AppUpdateNotificationWorker(context: Context, parameters: WorkerParameters) : Worker(context, parameters) {
    override fun doWork(): Result {
        val allowSystemNotification = !inputData.getBoolean(NotificationScheduler.INTERNAL_ONLY, false)
        val store = LocalStore(applicationContext)
        val preferences = store.notificationPreferences
        if (!preferences.enabled || !preferences.appUpdates) return Result.success()
        val release = try { AppUpdater.latestRelease(includeInAppOnly = true) }
            catch (_: Exception) { return Result.retry() }
        if (!AppUpdater.isNewer(release, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE) ||
            store.lastUpdateNotificationVersion == release.updateKey) return Result.success()
        val sameVisibleVersion = release.version == BuildConfig.VERSION_NAME
        val entry = AppNotificationEntry(
            id = "update-${release.updateKey}", type = AppNotificationType.APP_UPDATE,
            title = "Aggiornamento disponibile",
            message = if (sameVisibleVersion) "È disponibile una nuova build di Orari UNIMI ${release.version}."
                else "È disponibile Orari UNIMI ${release.version}.",
            timestampMillis = System.currentTimeMillis()
        )
        store.lastUpdateNotificationVersion = release.updateKey
        if (store.addNotification(entry)) {
            NotificationPublisher.post(applicationContext, entry,
                AppUpdater.shouldPostSystemUpdate(release, allowSystemNotification))
        }
        return Result.success()
    }
}

object NotificationPublisher {
    private const val TAG = "orari-unimi"
    private const val IMPORTANT_CHANNEL = "schedule_changes"
    private const val EXAM_CHANNEL = "exam_changes"
    private const val EXAM_REGISTRATION_CHANNEL = "exam_registration_reminders"
    private const val REMINDER_CHANNEL = "lesson_reminders"
    private const val UPDATE_CHANNEL = "app_updates"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(listOf(
            NotificationChannel(IMPORTANT_CHANNEL, "Variazioni degli orari", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Annullamenti e variazioni importanti delle lezioni" },
            NotificationChannel(EXAM_CHANNEL, "Variazioni degli appelli", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Cambi di data, ora, luogo e dettagli degli appelli salvati" },
            NotificationChannel(EXAM_REGISTRATION_CHANNEL, "Iscrizioni agli appelli",
                NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Apertura e scadenza delle iscrizioni agli appelli salvati" },
            silentChannel(REMINDER_CHANNEL, "Promemoria lezioni", "Promemoria silenziosi prima delle lezioni"),
            silentChannel(UPDATE_CHANNEL, "Aggiornamenti dell'app", "Avvisi silenziosi per nuove versioni")
        ))
    }

    fun post(context: Context, entry: AppNotificationEntry, allowSystemNotification: Boolean = true) {
        if (!NotificationDeliveryPolicy.shouldPostSystem(
                allowSystemNotification, AppVisibility.isForeground) || !canPost(context)) return
        createChannels(context)
        val notification = Notification.Builder(context, channel(entry.type))
            .setSmallIcon(R.drawable.ic_notification_calendar)
            .setContentTitle(entry.title)
            .setContentText(entry.message)
            .setStyle(Notification.BigTextStyle().bigText(entry.message))
            .setContentIntent(openEntryIntent(context, entry.id))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_EVENT)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(TAG, entry.id.hashCode(), notification)
    }

    fun postSummary(
        context: Context,
        type: AppNotificationType,
        count: Int,
        title: String,
        message: String,
        allowSystemNotification: Boolean = true
    ) {
        if (!NotificationDeliveryPolicy.shouldPostSystem(
                allowSystemNotification, AppVisibility.isForeground) || !canPost(context)) return
        createChannels(context)
        val notification = Notification.Builder(context, channel(type))
            .setSmallIcon(R.drawable.ic_notification_calendar)
            .setContentTitle(title)
            .setContentText(message)
            .setNumber(count)
            .setContentIntent(openCenterIntent(context))
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_EVENT)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(TAG, 15_000, notification)
    }

    fun cancelAll(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancelAll()
    }

    private fun channel(type: AppNotificationType) = when (type) {
        AppNotificationType.LESSON_CHANGE, AppNotificationType.IMPORTANT_CHANGE -> IMPORTANT_CHANNEL
        AppNotificationType.EXAM_CHANGE -> EXAM_CHANNEL
        AppNotificationType.EXAM_REGISTRATION_REMINDER -> EXAM_REGISTRATION_CHANNEL
        AppNotificationType.LESSON_REMINDER -> REMINDER_CHANNEL
        AppNotificationType.APP_UPDATE -> UPDATE_CHANNEL
    }

    private fun silentChannel(id: String, name: String, descriptionText: String) =
        NotificationChannel(id, name, NotificationManager.IMPORTANCE_LOW).apply {
            description = descriptionText
            setSound(null, null)
            enableVibration(false)
        }

    private fun canPost(context: Context) = Build.VERSION.SDK_INT < 33 ||
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openCenterIntent(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0,
        Intent(context, MainActivity::class.java).putExtra(MainActivity.OPEN_NOTIFICATIONS, true)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    private fun openEntryIntent(context: Context, entryId: String): PendingIntent = PendingIntent.getActivity(
        context, entryId.hashCode(),
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.OPEN_NOTIFICATION_ID, entryId)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
}

internal object NotificationDeliveryPolicy {
    fun shouldPostSystem(allowSystemNotification: Boolean, appInForeground: Boolean): Boolean =
        allowSystemNotification && !appInForeground
}

object AppVisibility {
    @Volatile
    var isForeground: Boolean = false
        private set

    fun enterForeground() {
        isForeground = true
    }

    fun enterBackground() {
        isForeground = false
    }
}
