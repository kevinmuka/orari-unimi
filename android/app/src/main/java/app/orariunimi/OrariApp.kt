package app.orariunimi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CalendarContract
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import androidx.core.content.ContextCompat

data class CalendarData(
    val title: String,
    val lessons: List<Lesson>,
    val year: String,
    val source: SearchItem?,
    val updatedAtMillis: Long,
    val offline: Boolean,
    val combinedSubjects: List<SavedSubject>? = null
)
data class CourseDetailData(val year: String, val course: SearchItem)
data class SubjectSelectionTarget(val year: String, val item: SearchItem)
data class ExamViewData(
    val courseCode: String,
    val window: ExamWindow,
    val snapshot: ExamSnapshot,
    val offline: Boolean
)
private data class ExamSearchMatches(val year: String, val query: String, val items: List<SearchItem>)

private enum class RootTab(val index: Int, val title: String) {
    LESSONS(0, "Lezioni"),
    EXAMS(1, "Appelli"),
    EXPLORE(2, "Esplora"),
    FAVORITES(3, "Preferiti");

    companion object {
        fun fromIndex(index: Int): RootTab = entries.firstOrNull { it.index == index } ?: LESSONS
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrariApp(
    initialTab: Int = 0,
    openSavedRequest: Int = 0,
    openNotificationsRequest: Int = 0,
    openNotificationId: String? = null,
    openNotificationRequest: Int = 0
) {
    val context = LocalContext.current
    val store = remember(context) { LocalStore(context.applicationContext) }
    val diagnosticsStore = remember(context) { SyncDiagnosticsStore(context.applicationContext) }
    val responseCache = remember(context) { ResponseCache(File(context.cacheDir, "orari-responses")) }
    val api = remember(context, responseCache) { UnimiApi(cache = responseCache) }
    val examApi = remember(context, responseCache) { ExamApi(cache = responseCache) }
    val roomCache = remember(context) {
        ResponseCache(File(context.cacheDir, "orari-rooms"), maxBytes = 16L * 1024 * 1024)
    }
    val roomApi = remember(roomCache) { RoomApi(roomCache) }
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(RootTab.fromIndex(initialTab)) }
    var examSearchOpen by remember { mutableStateOf(false) }
    var subjectSelectionTarget by remember { mutableStateOf<SubjectSelectionTarget?>(null) }
    var favoriteSection by remember { mutableStateOf(FavoriteSection.SUBJECTS) }
    var settings by remember { mutableStateOf(false) }
    var notificationsOpen by remember { mutableStateOf(false) }
    var notificationSettingsOpen by remember { mutableStateOf(false) }
    var agendaOpen by remember { mutableStateOf(false) }
    var lessonMonthOpen by remember { mutableStateOf(false) }
    var calendar by remember { mutableStateOf<CalendarData?>(null) }
    var courseDetail by remember { mutableStateOf<CourseDetailData?>(null) }
    var examCourse by remember { mutableStateOf<SearchItem?>(null) }
    var examSubject by remember { mutableStateOf<FollowedExamSubject?>(null) }
    var examCourseYear by remember { mutableStateOf<String?>(null) }
    var examWindow by remember { mutableStateOf(ExamWindow.NEXT_30_DAYS) }
    var examData by remember { mutableStateOf<ExamViewData?>(null) }
    var examError by remember { mutableStateOf<String?>(null) }
    var examRefreshing by remember { mutableStateOf(false) }
    var examOpenedFromCourseDetail by remember { mutableStateOf(false) }
    var examPlannerAppeal by remember { mutableStateOf<ExamAppeal?>(null) }
    var examPlannerAppealAvailable by remember { mutableStateOf(true) }
    var examPlannerNote by remember { mutableStateOf("") }
    var examPlannerOpen by remember { mutableStateOf(false) }
    var examPlannerSavedMode by remember { mutableStateOf(false) }
    var examLoadId by remember { mutableIntStateOf(0) }
    var examQuery by remember { mutableStateOf("") }
    var years by remember { mutableStateOf<List<AcademicYear>>(emptyList()) }
    var year by remember { mutableStateOf<AcademicYear?>(null) }
    var yearRetry by remember { mutableIntStateOf(0) }
    var loadingYears by remember { mutableStateOf(true) }
    var kind by remember { mutableStateOf(SearchKind.COURSE) }
    var query by remember { mutableStateOf("") }
    var exploringRooms by remember { mutableStateOf(false) }
    var roomQuery by remember { mutableStateOf("") }
    var roomCatalog by remember { mutableStateOf<List<UnimiRoom>?>(null) }
    var roomCatalogLoading by remember { mutableStateOf(false) }
    var roomCatalogError by remember { mutableStateOf<String?>(null) }
    var roomCatalogRetry by remember { mutableIntStateOf(0) }
    var selectedBuilding by remember { mutableStateOf<UnimiBuilding?>(null) }
    var buildingDay by remember { mutableStateOf(LocalDate.now(ZoneId.of("Europe/Rome"))) }
    var buildingPlanning by remember { mutableStateOf<BuildingPlanning?>(null) }
    var buildingPlanningLoading by remember { mutableStateOf(false) }
    var buildingPlanningError by remember { mutableStateOf<String?>(null) }
    var buildingPlanningRetry by remember { mutableIntStateOf(0) }
    var selectedRoom by remember { mutableStateOf<UnimiRoom?>(null) }
    var roomDay by remember { mutableStateOf(LocalDate.now(ZoneId.of("Europe/Rome"))) }
    var roomPlanning by remember { mutableStateOf<RoomPlanning?>(null) }
    var roomPlanningLoading by remember { mutableStateOf(false) }
    var roomPlanningError by remember { mutableStateOf<String?>(null) }
    var roomPlanningRetry by remember { mutableIntStateOf(0) }
    var entries by remember { mutableStateOf<Map<SearchKind, SearchIndex>>(emptyMap()) }
    var entriesAttempted by remember { mutableStateOf<Set<SearchKind>>(emptySet()) }
    var entriesRetry by remember { mutableIntStateOf(0) }
    var busy by remember { mutableStateOf(false) }
    var calendarRefreshing by remember { mutableStateOf(false) }
    var calendarLoadId by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var saved by remember { mutableStateOf(store.saved()) }
    var savedSchedule by remember { mutableStateOf<ScheduleSnapshot?>(null) }
    var savedScheduleLoading by remember { mutableStateOf(false) }
    var savedScheduleAttempted by remember { mutableStateOf(false) }
    var savedScheduleRetry by remember { mutableIntStateOf(0) }
    var savedScheduleForceRefresh by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(store.favoriteCourses()) }
    var followedExamSubjects by remember { mutableStateOf(store.followedExamSubjects()) }
    var excludedExamSubjects by remember { mutableStateOf(store.excludedExamSubjects()) }
    var savedExamAppeals by remember { mutableStateOf(store.savedExamAppeals()) }
    var removedExamNotices by remember { mutableStateOf(store.removedExamNotices()) }
    var weekend by remember { mutableStateOf(store.showWeekend) }
    var notificationPreferences by remember { mutableStateOf(store.notificationPreferences) }
    var notificationEntries by remember { mutableStateOf(store.notifications()) }
    var notificationRefreshing by remember { mutableStateOf(false) }
    var notificationDetailEntry by remember { mutableStateOf<AppNotificationEntry?>(null) }
    var notificationDetailReturnsToCenter by remember { mutableStateOf(false) }
    var lessonDiagnostics by remember { mutableStateOf(diagnosticsStore.read(SyncDiagnosticKind.LESSONS)) }
    var examDiagnostics by remember { mutableStateOf(diagnosticsStore.read(SyncDiagnosticKind.EXAMS)) }
    var checkingLessonDiagnostics by remember { mutableStateOf(false) }
    var checkingExamDiagnostics by remember { mutableStateOf(false) }
    var week by remember { mutableStateOf(startOfWeek(LocalDate.now())) }
    var selectedDay by remember { mutableStateOf(LocalDate.now()) }
    var updateState by remember { mutableStateOf<AppUpdateState>(AppUpdateState.Idle) }
    var releaseNotes by remember { mutableStateOf(store.cachedReleaseNotes) }
    var pendingInstall by remember { mutableStateOf<File?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            notificationPreferences = notificationPreferences.copy(enabled = true)
            store.notificationPreferences = notificationPreferences
            NotificationScheduler.configure(context, runNow = true)
        } else {
            scope.launch { snackbar.showAppSnackbar("Per ricevere gli avvisi devi consentire le notifiche nelle impostazioni Android.") }
        }
    }
    val unknownSourcesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val apk = pendingInstall
        if (apk != null && AppUpdater.canInstall(context)) {
            runCatching { AppUpdater.install(context, apk) }
                .onFailure { failure ->
                    scope.launch { snackbar.showAppSnackbar(failure.message ?: "Impossibile aprire l'installazione.") }
                }
            pendingInstall = null
        }
    }

    DisposableEffect(store) {
        val preferences = context.getSharedPreferences("orari_unimi", android.content.Context.MODE_PRIVATE)
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "notification_inbox") scope.launch {
                notificationEntries = store.notifications()
            }
            if (key == "saved_exam_appeals" || key == "removed_exam_notices") scope.launch {
                savedExamAppeals = store.savedExamAppeals()
                removedExamNotices = store.removedExamNotices()
            }
            if (key == "sync_diagnostics") scope.launch {
                lessonDiagnostics = diagnosticsStore.read(SyncDiagnosticKind.LESSONS)
                examDiagnostics = diagnosticsStore.read(SyncDiagnosticKind.EXAMS)
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    LaunchedEffect(store) {
        while (true) {
            val now = ZonedDateTime.now()
            val nextDay = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            delay(Duration.between(now, nextDay).toMillis().coerceAtLeast(1_000L) + 1_000L)
            savedExamAppeals = store.savedExamAppeals()
            buildingDay = LocalDate.now(ZoneId.of("Europe/Rome"))
            if (examPlannerAppeal?.date?.isBefore(LocalDate.now()) == true) {
                examPlannerAppeal = null
                examPlannerNote = ""
            }
        }
    }

    LaunchedEffect(openSavedRequest) {
        if (openSavedRequest > 0) {
            settings = false
            notificationsOpen = false
            notificationSettingsOpen = false
            agendaOpen = false
            lessonMonthOpen = false
            calendar = null
            courseDetail = null
            selectedRoom = null
            selectedBuilding = null
            examCourse = null
            examSubject = null
            examCourseYear = null
            examData = null
            examError = null
            examPlannerAppeal = null
            examPlannerOpen = false
            examSearchOpen = false
            subjectSelectionTarget = null
            examLoadId++
            examRefreshing = false
            tab = RootTab.LESSONS
        }
    }

    LaunchedEffect(openNotificationsRequest) {
        if (openNotificationsRequest > 0) {
            settings = false
            notificationSettingsOpen = false
            agendaOpen = false
            lessonMonthOpen = false
            calendar = null
            courseDetail = null
            selectedRoom = null
            selectedBuilding = null
            examCourse = null
            examSubject = null
            examCourseYear = null
            examData = null
            examError = null
            examPlannerAppeal = null
            examPlannerOpen = false
            examSearchOpen = false
            subjectSelectionTarget = null
            examLoadId++
            examRefreshing = false
            notificationEntries = store.notifications()
            store.markNotificationsRead()
            notificationEntries = store.notifications()
            notificationsOpen = true
        }
    }

    LaunchedEffect(openNotificationRequest) {
        val entry = openNotificationId?.let { id -> store.notifications().firstOrNull { it.id == id } }
            ?: return@LaunchedEffect
        settings = entry.type == AppNotificationType.APP_UPDATE
        notificationsOpen = false
        notificationSettingsOpen = false
        agendaOpen = false
        lessonMonthOpen = false
        calendar = null
        courseDetail = null
        selectedRoom = null
        selectedBuilding = null
        examCourse = null
        examSubject = null
        examCourseYear = null
        examData = null
        examError = null
        examPlannerAppeal = null
        examPlannerOpen = false
        examSearchOpen = false
        subjectSelectionTarget = null
        notificationDetailReturnsToCenter = false
        notificationDetailEntry = entry.takeUnless { it.type == AppNotificationType.APP_UPDATE }
        store.markNotificationsRead()
        notificationEntries = store.notifications()
    }

    LaunchedEffect(yearRetry) {
        loadingYears = true
        try {
            val fetched = withContext(Dispatchers.IO) { api.years() }
            if (fetched.isEmpty()) throw IllegalStateException("Nessun anno accademico disponibile.")
            years = fetched
            year = fetched.first()
            error = null
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cause: Exception) {
            error = cause.message ?: "Impossibile recuperare gli anni accademici."
        } finally {
            loadingYears = false
        }
    }

    LaunchedEffect(year?.code, kind, tab, exploringRooms, favoriteSection, examSearchOpen,
        subjectSelectionTarget, entriesRetry) {
        val currentYear = year ?: return@LaunchedEffect
        val requestedKinds = when {
            subjectSelectionTarget?.year == currentYear.code -> listOf(SearchKind.SUBJECT)
            tab == RootTab.EXPLORE && !exploringRooms -> listOf(kind)
            tab == RootTab.EXAMS && examSearchOpen -> listOf(SearchKind.COURSE, SearchKind.SUBJECT)
            tab == RootTab.FAVORITES && favoriteSection == FavoriteSection.SUBJECTS -> listOf(SearchKind.SUBJECT)
            else -> return@LaunchedEffect
        }
        requestedKinds.filter { it !in entries && it !in entriesAttempted }.forEach { requestedKind ->
            launch {
                try {
                    val fetched = withContext(Dispatchers.IO) { api.entries(requestedKind, currentYear.code) }
                    val index = withContext(Dispatchers.Default) { SearchIndex(fetched) }
                    entries = entries + (requestedKind to index)
                    entriesAttempted = entriesAttempted + requestedKind
                    error = null
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (cause: Exception) {
                    entriesAttempted = entriesAttempted + requestedKind
                    if (!examSearchOpen) error = cause.message ?: "Impossibile caricare l'elenco."
                }
            }
        }
    }

    LaunchedEffect(tab, exploringRooms, roomCatalogRetry) {
        if (tab != RootTab.EXPLORE || !exploringRooms || roomCatalog != null) return@LaunchedEffect
        roomCatalogLoading = true
        roomCatalogError = null
        try {
            roomCatalog = withContext(Dispatchers.IO) { roomApi.catalog() }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cause: Exception) {
            roomCatalogError = cause.message ?: "Impossibile caricare le aule."
        } finally {
            roomCatalogLoading = false
        }
    }

    LaunchedEffect(selectedBuilding?.code, buildingDay, buildingPlanningRetry) {
        val building = selectedBuilding ?: return@LaunchedEffect
        val requestedDay = buildingDay
        val requestedRetry = buildingPlanningRetry
        buildingPlanningLoading = true
        buildingPlanningError = null
        try {
            buildingPlanning = withContext(Dispatchers.IO) {
                roomApi.buildingPlanning(building, requestedDay, force = requestedRetry > 0)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cause: Exception) {
            buildingPlanningError = cause.message ?: "Impossibile caricare le occupazioni."
        } finally {
            if (selectedBuilding?.code == building.code && buildingDay == requestedDay &&
                buildingPlanningRetry == requestedRetry) buildingPlanningLoading = false
        }
    }

    LaunchedEffect(selectedRoom?.code, roomDay, roomPlanningRetry) {
        val room = selectedRoom ?: return@LaunchedEffect
        val requestedDay = roomDay
        val requestedRetry = roomPlanningRetry
        roomPlanningLoading = true
        roomPlanningError = null
        try {
            roomPlanning = withContext(Dispatchers.IO) {
                roomApi.planning(room, requestedDay, force = requestedRetry > 0)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (cause: Exception) {
            roomPlanningError = cause.message ?: "Impossibile caricare il planning dell'aula."
        } finally {
            if (selectedRoom?.code == room.code && roomDay == requestedDay &&
                roomPlanningRetry == requestedRetry) {
                roomPlanningLoading = false
            }
        }
    }

    val roomIndex by produceState<RoomSearchIndex?>(null, roomCatalog) {
        val catalog = roomCatalog ?: return@produceState
        value = withContext(Dispatchers.Default) { RoomSearchIndex(catalog) }
    }
    val roomResults by produceState<List<UnimiBuilding>?>(null, roomQuery, roomIndex) {
        val index = roomIndex
        if (index != null) {
            if (roomQuery.isNotBlank()) delay(100)
            value = withContext(Dispatchers.Default) { index.search(roomQuery) }
        }
    }

    LaunchedEffect(saved, savedScheduleRetry) {
        val forceRefresh = savedScheduleForceRefresh
        savedScheduleForceRefresh = false
        if (saved.isEmpty()) {
            savedSchedule = null
            savedScheduleLoading = false
            savedScheduleAttempted = true
            ScheduleWidgetProvider.updateAll(context)
            return@LaunchedEffect
        }
        val requestedSubjects = saved
        savedScheduleLoading = true
        val cached = withContext(Dispatchers.IO) { runCatching { api.cachedSavedLessons(requestedSubjects) }.getOrNull() }
        if (cached != null && saved == requestedSubjects) {
            savedSchedule = cached
            ScheduleWidgetProvider.updateAll(context)
        }
        try {
            val refreshed = withContext(Dispatchers.IO) {
                if (forceRefresh) api.refreshSavedLessons(requestedSubjects)
                else api.refreshSavedLessonsIfStale(requestedSubjects)
            }
            if (saved == requestedSubjects) {
                savedSchedule = refreshed
                ScheduleWidgetProvider.updateAll(context)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            // The personal calendar remains available from its recent cache.
        } finally {
            if (saved == requestedSubjects) {
                savedScheduleAttempted = true
                savedScheduleLoading = false
            }
        }
    }

    val searchIndex = entries[kind]
    val results by produceState<List<SearchItem>?>(null, query, searchIndex) {
        if (query.isNotBlank() && searchIndex != null) {
            delay(120)
            value = withContext(Dispatchers.Default) { searchIndex.search(query) }
        }
    }
    val examCourseIndex = entries[SearchKind.COURSE]
    val examSubjectIndex = entries[SearchKind.SUBJECT]
    var examSearchSubjectIndex by remember(year?.code, examSubjectIndex) {
        mutableStateOf<SearchIndex?>(null)
    }
    LaunchedEffect(year?.code, examSubjectIndex) {
        examSearchSubjectIndex = examSubjectIndex?.let { index -> withContext(Dispatchers.Default) {
            examSubjectSearchIndex(index.items)
        } }
    }
    var examSearchMatches by remember { mutableStateOf<ExamSearchMatches?>(null) }
    LaunchedEffect(year?.code, examQuery, examCourseIndex, examSearchSubjectIndex) {
        val selectedYear = year?.code ?: return@LaunchedEffect
        if (examQuery.isBlank()) {
            examSearchMatches = null
            return@LaunchedEffect
        }
        if (examCourseIndex == null && examSearchSubjectIndex == null) return@LaunchedEffect
        delay(120)
        val found = withContext(Dispatchers.Default) {
            searchExamCatalogs(examCourseIndex, examSearchSubjectIndex, examQuery)
        }
        examSearchMatches = ExamSearchMatches(selectedYear, examQuery, found)
    }
    val examResults = examSearchMatches?.takeIf {
        it.year == year?.code && it.query == examQuery
    }?.items
    val currentExamYear = year?.code.orEmpty()
    val lessonExamSubjects = remember(saved, examSubjectIndex, currentExamYear, excludedExamSubjects) {
        val byCode = examSubjectIndex?.items.orEmpty().associateBy { it.code }
        saved.asSequence().filter {
            it.year == currentExamYear && (it.year to it.code) !in excludedExamSubjects
        }.mapNotNull { subject ->
            val indexed = byCode[subject.code] ?: return@mapNotNull null
            if (indexed.examSources.isEmpty()) null else FollowedExamSubject(
                subject.year, subject.code, subject.name, indexed.examSources)
        }.distinctBy { it.year to it.code }.sortedBy { it.name }.toList()
    }
    val manualExamSubjects = remember(followedExamSubjects, currentExamYear) {
        followedExamSubjects.filter { it.year == currentExamYear }.sortedBy { it.name }
    }
    val allFavoriteSubjects = remember(saved, followedExamSubjects, examSubjectIndex,
        currentExamYear, excludedExamSubjects) {
        favoriteSubjects(saved, followedExamSubjects, currentExamYear,
            examSubjectIndex?.items, excludedExamSubjects)
    }
    val examAppealsWithNotes = remember(savedExamAppeals, examPlannerNote) {
        savedExamAppeals.asSequence()
            .filter { store.examNote(it).isNotBlank() }
            .map { it.courseCode to it.id }
            .toSet()
    }

    fun toggleWeekend() {
        weekend = !weekend
        store.showWeekend = weekend
        if (!weekend && selectedDay.dayOfWeek.value > DayOfWeek.FRIDAY.value) selectedDay = week
        ScheduleWidgetProvider.updateAll(context)
    }

    fun checkForUpdate() {
        scope.launch {
            updateState = AppUpdateState.Checking
            updateState = try {
                val release = withContext(Dispatchers.IO) { AppUpdater.latestRelease() }
                val latestNotes = release.notes.takeIf { it.isNotBlank() }?.let {
                    ReleaseNotes(release.version, release.versionCode, it)
                }
                releaseNotes = latestNotes
                withContext(Dispatchers.IO) { store.cachedReleaseNotes = latestNotes }
                if (AppUpdater.isNewer(release, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)) {
                    AppUpdateState.Available(release)
                } else AppUpdateState.UpToDate
            } catch (_: Exception) {
                AppUpdateState.Error("Impossibile controllare gli aggiornamenti. Verifica la connessione e riprova.")
            }
        }
    }

    fun downloadUpdate(release: AppRelease) {
        scope.launch {
            updateState = AppUpdateState.Downloading(release, null)
            updateState = try {
                val apk = withContext(Dispatchers.IO) { AppUpdater.download(context, release) {} }
                AppUpdateState.Ready(release, apk)
            } catch (cause: Exception) {
                val message = cause.message.orEmpty()
                AppUpdateState.Error(if (message.contains("integrità")) message
                    else "Impossibile scaricare l'aggiornamento. Verifica la connessione e riprova.")
            }
        }
    }

    fun installUpdate(file: File) {
        if (AppUpdater.canInstall(context)) {
            runCatching { AppUpdater.install(context, file) }
                .onFailure { failure ->
                    scope.launch { snackbar.showAppSnackbar(failure.message ?: "Impossibile aprire l'installazione.") }
                }
        } else {
            pendingInstall = file
            runCatching { unknownSourcesLauncher.launch(AppUpdater.unknownSourcesIntent(context)) }
                .onFailure { failure ->
                    pendingInstall = null
                    scope.launch { snackbar.showAppSnackbar(failure.message ?: "Impossibile aprire il permesso di installazione.") }
                }
        }
    }

    fun openSettings() {
        examPlannerAppeal = null
        notificationsOpen = false
        notificationSettingsOpen = false
        settings = true
        error = null
        if (updateState is AppUpdateState.Idle) checkForUpdate()
    }

    fun openNotifications() {
        examPlannerAppeal = null
        settings = false
        notificationSettingsOpen = false
        notificationEntries = store.notifications()
        store.markNotificationsRead()
        notificationEntries = store.notifications()
        notificationsOpen = true
        error = null
    }

    fun openNotificationSettings() {
        examPlannerAppeal = null
        settings = false
        notificationsOpen = false
        notificationSettingsOpen = true
        error = null
    }

    fun refreshNotifications() {
        if (notificationRefreshing) return
        scope.launch {
            notificationRefreshing = true
            val ids = NotificationScheduler.refreshNow(context)
            val completed = if (ids.isEmpty()) {
                delay(450)
                null
            } else runCatching {
                withContext(Dispatchers.IO) { NotificationScheduler.waitForRefresh(context, ids) }
            }.getOrDefault(false)
            notificationEntries = store.notifications()
            store.markNotificationsRead()
            notificationEntries = store.notifications()
            notificationRefreshing = false
            snackbar.showAppSnackbar(when (completed) {
                true -> "Controllo notifiche completato"
                false -> "Il controllo continuerà in background"
                null -> "Attiva almeno un tipo di notifica nelle preferenze"
            })
        }
    }

    fun refreshDiagnostics(kind: SyncDiagnosticKind) {
        if (kind == SyncDiagnosticKind.LESSONS && checkingLessonDiagnostics ||
            kind == SyncDiagnosticKind.EXAMS && checkingExamDiagnostics) return
        scope.launch {
            if (kind == SyncDiagnosticKind.LESSONS) checkingLessonDiagnostics = true
            else checkingExamDiagnostics = true
            val id = if (kind == SyncDiagnosticKind.LESSONS)
                NotificationScheduler.refreshLessonsNow(context)
            else NotificationScheduler.refreshExamsNow(context)
            val completed = id?.let { requestId -> runCatching {
                withContext(Dispatchers.IO) {
                    NotificationScheduler.waitForRefresh(context, listOf(requestId))
                }
            }.getOrDefault(false) }
            lessonDiagnostics = diagnosticsStore.read(SyncDiagnosticKind.LESSONS)
            examDiagnostics = diagnosticsStore.read(SyncDiagnosticKind.EXAMS)
            if (kind == SyncDiagnosticKind.LESSONS) checkingLessonDiagnostics = false
            else checkingExamDiagnostics = false
            snackbar.showAppSnackbar(when (completed) {
                true -> if ((if (kind == SyncDiagnosticKind.LESSONS) lessonDiagnostics
                        else examDiagnostics).changesFound == 0) "Nessuna variazione rilevata"
                    else "Controllo completato: variazioni rilevate"
                false -> "Il controllo continuerà in background"
                null -> "Attiva il relativo controllo nelle preferenze"
            })
        }
    }

    fun setNotificationPreferences(updated: NotificationPreferences) {
        notificationPreferences = updated
        store.notificationPreferences = updated
        NotificationScheduler.configure(context, runNow = true)
    }

    fun toggleNotifications() {
        if (notificationPreferences.enabled) {
            setNotificationPreferences(notificationPreferences.copy(enabled = false))
            return
        }
        if (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            setNotificationPreferences(notificationPreferences.copy(enabled = true))
        } else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    fun savedSelectionChanged() {
        savedSchedule = null
        savedScheduleAttempted = false
        NotificationBaselineStore(context).clear()
        store.lastLessonReminderKey = null
        NotificationScheduler.cancelLessonReminder(context)
        NotificationScheduler.configure(context, runNow = true)
    }

    fun examSelectionChanged() {
        ExamNotificationBaselineStore(context).clear()
        NotificationScheduler.configure(context, runNow = true)
    }

    fun showBriefSnackbar(message: String) {
        scope.launch { snackbar.showAppSnackbar(message) }
    }

    fun toggleFavorite(course: FavoriteCourse) {
        val isFavorite = favorites.any { it.year == course.year && it.code == course.code }
        if (isFavorite) store.removeFavoriteCourse(course) else store.addFavoriteCourse(course)
        favorites = store.favoriteCourses()
        showBriefSnackbar(if (isFavorite) "Corso rimosso dai salvati" else "Corso salvato")
    }

    fun editSubjectSelection(selectedYear: String, item: SearchItem) {
        val existing = allFavoriteSubjects.firstOrNull {
            it.year == selectedYear && it.code == item.code
        }
        val indexed = if (selectedYear == year?.code)
            entries[SearchKind.SUBJECT]?.items?.firstOrNull { it.code == item.code }
        else null
        subjectSelectionTarget = SubjectSelectionTarget(selectedYear,
            item.copy(examSources = (item.examSources + indexed?.examSources.orEmpty() +
                existing?.examSources.orEmpty()).distinct()))
    }

    fun setSubjectDestinations(target: SubjectSelectionTarget, inLessons: Boolean, inExams: Boolean) {
        val item = target.item
        val lesson = SavedSubject(target.year, item.code, item.name)
        val wasLesson = saved.any { it.year == target.year && it.code == item.code }
        val followed = followedExamSubjects.firstOrNull {
            it.year == target.year && it.code == item.code
        }
        if (inLessons != wasLesson) {
            if (inLessons) store.add(lesson) else store.remove(lesson)
            saved = store.saved()
            savedSelectionChanged()
        }
        val exclude = inLessons && !inExams
        if (exclude != ((target.year to item.code) in excludedExamSubjects)) {
            store.setExamSubjectExcluded(target.year, item.code, exclude)
            excludedExamSubjects = store.excludedExamSubjects()
        }
        if (inExams) {
            val sources = (followed?.sources.orEmpty() + item.examSources).distinct()
            if (followed == null || followed.sources != sources) {
                if (followed != null) store.removeFollowedExamSubject(followed)
                store.addFollowedExamSubject(FollowedExamSubject(target.year, item.code, item.name, sources))
                followedExamSubjects = store.followedExamSubjects()
                examSelectionChanged()
            }
        } else if (followed != null) {
            store.removeFollowedExamSubject(followed)
            followedExamSubjects = store.followedExamSubjects()
            examSelectionChanged()
        }
        showBriefSnackbar(when {
            inLessons && inExams -> "Insegnamento seguito per lezioni e appelli"
            inLessons -> "Insegnamento seguito per le lezioni"
            inExams -> "Insegnamento seguito per gli appelli"
            else -> "Insegnamento rimosso dai preferiti"
        })
    }

    fun clearFavoriteSubjects() {
        store.clear()
        followedExamSubjects.forEach(store::removeFollowedExamSubject)
        saved = emptyList()
        followedExamSubjects = emptyList()
        store.clearExcludedExamSubjects()
        excludedExamSubjects = emptySet()
        savedSelectionChanged()
        examSelectionChanged()
        showBriefSnackbar("Insegnamenti rimossi dai preferiti")
    }

    fun clearFavoriteCourses() {
        favorites.forEach(store::removeFavoriteCourse)
        favorites = emptyList()
        showBriefSnackbar("Corsi di laurea rimossi dai preferiti")
    }

    fun openExamPlanner() {
        examPlannerOpen = true
        examPlannerSavedMode = false
        examPlannerAppeal = null
        savedExamAppeals = store.savedExamAppeals()
        error = null
    }

    fun toggleSavedExamAppeal(appeal: ExamAppeal) {
        val isSaved = savedExamAppeals.any { it.id == appeal.id && it.courseCode == appeal.courseCode }
        if (isSaved) store.removeSavedExamAppeal(appeal) else store.addSavedExamAppeal(appeal)
        savedExamAppeals = store.savedExamAppeals()
        examSelectionChanged()
        showBriefSnackbar(if (isSaved) "Appello rimosso dai promemoria" else "Appello salvato nei promemoria")
    }

    fun showExams(
        course: SearchItem,
        selectedWindow: ExamWindow = ExamWindow.NEXT_30_DAYS,
        returnToCourseDetail: Boolean = false,
        courseYear: String? = null
    ) {
        settings = false
        notificationsOpen = false
        notificationSettingsOpen = false
        agendaOpen = false
        lessonMonthOpen = false
        calendar = null
        examPlannerAppeal = null
        examPlannerOpen = false
        examSubject = null
        examCourse = course
        examCourseYear = courseYear ?: year?.code.orEmpty()
        examWindow = selectedWindow
        examError = null
        examOpenedFromCourseDetail = returnToCourseDetail
        examLoadId++
        val loadId = examLoadId
        val current = examData?.takeIf { it.courseCode == course.code && it.window == selectedWindow }
        if (current == null) examData = null
        examRefreshing = true
        scope.launch {
            error = null
            val cached = withContext(Dispatchers.IO) {
                runCatching { examApi.cachedAppeals(course.code, selectedWindow) }.getOrNull()
            }
            if (cached != null && loadId == examLoadId) {
                examData = ExamViewData(course.code, selectedWindow, cached, offline = false)
            }
            try {
                val (fresh, savedChanged) = withContext(Dispatchers.IO) {
                    val snapshot = examApi.refreshAppeals(course.code, selectedWindow)
                    snapshot to store.refreshSavedExamAppeals(snapshot.appeals)
                }
                if (loadId == examLoadId) {
                    examData = ExamViewData(course.code, selectedWindow, fresh, offline = false)
                    if (savedChanged) {
                        savedExamAppeals = store.savedExamAppeals()
                        NotificationScheduler.reconcileExamRegistrationReminders(context)
                    }
                    examError = null
                }
            } catch (cause: Exception) {
                if (loadId == examLoadId) {
                    examError = examFailureMessage(cause)
                    if (cached != null) {
                        examData = ExamViewData(course.code, selectedWindow, cached, offline = true)
                    }
                }
            } finally {
                if (loadId == examLoadId) examRefreshing = false
            }
        }
    }

    fun showSubjectExams(
        subject: FollowedExamSubject,
        selectedWindow: ExamWindow = ExamWindow.NEXT_30_DAYS
    ) {
        val item = SearchItem(subject.code, subject.name, SearchKind.SUBJECT,
            examSources = subject.sources)
        settings = false
        notificationsOpen = false
        notificationSettingsOpen = false
        agendaOpen = false
        lessonMonthOpen = false
        calendar = null
        examPlannerOpen = false
        examPlannerAppeal = null
        examCourse = item
        examSubject = subject
        examCourseYear = subject.year
        examWindow = selectedWindow
        examError = null
        examOpenedFromCourseDetail = false
        examLoadId++
        val loadId = examLoadId
        val current = examData?.takeIf { it.courseCode == subject.code && it.window == selectedWindow }
        if (current == null) examData = null
        examRefreshing = true
        scope.launch {
            error = null
            val sourcesByCourse = subject.sources
                .distinctBy { it.courseCode to it.examSubjectCode }
                .groupBy { it.courseCode }
            val cachedParts = withContext(Dispatchers.IO) {
                sourcesByCourse.flatMap { (courseCode, sources) ->
                    runCatching { examApi.cachedAppeals(courseCode, selectedWindow) }.getOrNull()
                        ?.let { snapshot -> sources.map { it to snapshot } }.orEmpty()
                }
            }
            val cached = mergeSubjectExamSnapshots(subject, cachedParts)
            if (cached != null && loadId == examLoadId) {
                examData = ExamViewData(subject.code, selectedWindow, cached, offline = false)
            }
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    val failures = mutableListOf<Throwable>()
                    val parts = sourcesByCourse.flatMap { (courseCode, sources) ->
                        runCatching { examApi.refreshAppeals(courseCode, selectedWindow) }
                            .onFailure(failures::add).getOrNull()
                            ?.let { snapshot -> sources.map { it to snapshot } }.orEmpty()
                    }
                    if (parts.isEmpty() && failures.isNotEmpty()) throw failures.first()
                    mergeSubjectExamSnapshots(subject, parts) ?: ExamSnapshot(emptyList(), System.currentTimeMillis())
                }
                val savedChanged = withContext(Dispatchers.IO) {
                    store.refreshSavedExamAppeals(refreshed.appeals)
                }
                if (loadId == examLoadId) {
                    examData = ExamViewData(subject.code, selectedWindow, refreshed, offline = false)
                    if (savedChanged) {
                        savedExamAppeals = store.savedExamAppeals()
                        NotificationScheduler.reconcileExamRegistrationReminders(context)
                    }
                    examError = null
                }
            } catch (cause: Exception) {
                if (loadId == examLoadId) {
                    examError = examFailureMessage(cause)
                    if (cached != null) examData = ExamViewData(
                        subject.code, selectedWindow, cached, offline = true)
                }
            } finally {
                if (loadId == examLoadId) examRefreshing = false
            }
        }
    }

    fun addExamToCalendar(appeal: ExamAppeal) {
        val parsedTime = runCatching {
            LocalTime.parse(appeal.time, DateTimeFormatter.ofPattern("HH:mm"))
        }.getOrNull()
        val zone = ZoneId.systemDefault()
        val start = (parsedTime?.let { appeal.date.atTime(it) } ?: appeal.date.atStartOfDay())
            .atZone(zone).toInstant().toEpochMilli()
        val registration = listOfNotNull(
            appeal.registrationOpen?.let { "Apertura iscrizioni: $it" },
            appeal.registrationClose?.let { "Chiusura iscrizioni: $it" }
        ).joinToString("\n")
        val description = listOf(appeal.testType, appeal.appealType,
            appeal.teacher.takeIf { it.isNotBlank() }?.let { "Docente: $it" },
            listOf(appeal.surnameFrom, appeal.surnameTo).filter { it.isNotBlank() }
                .joinToString("–").takeIf { it.isNotBlank() }?.let { "Cognomi: $it" },
            registration.takeIf { it.isNotBlank() }, "Appello pubblicato da UNIMI")
            .filterNotNull().filter { it.isNotBlank() }.joinToString("\n")
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.Events.TITLE, "Esame · ${appeal.subjectName}")
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            .putExtra(CalendarContract.Events.EVENT_LOCATION, appeal.location)
            .putExtra(CalendarContract.Events.DESCRIPTION, description)
            .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
        if (parsedTime == null) {
            val end = appeal.date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
                .putExtra(CalendarContract.Events.ALL_DAY, true)
        }
        runCatching { context.startActivity(intent) }
            .onFailure { scope.launch { snackbar.showAppSnackbar("Nessuna app calendario disponibile.") } }
    }

    fun moveWeek(amount: Long) {
        week = week.plusWeeks(amount)
        selectedDay = preferredDay(calendar?.lessons ?: savedSchedule?.lessons.orEmpty(), week, weekend)
    }

    fun showCalendar(
        title: String, yearCode: String, source: SearchItem?,
        combined: List<SavedSubject>? = null, openDay: LocalDate? = null,
        forceRefresh: Boolean = false
    ) {
        notificationsOpen = false
        notificationSettingsOpen = false
        settings = false
        lessonMonthOpen = false
        calendarLoadId++
        val loadId = calendarLoadId
        scope.launch {
            agendaOpen = false
            error = null
            val sameCalendar = calendar?.let {
                it.title == title && it.year == yearCode && it.source?.kind == source?.kind &&
                    it.source?.code == source?.code && it.combinedSubjects == combined
            } == true
            busy = !sameCalendar
            calendarRefreshing = true
            var selectionApplied = false

            fun show(snapshot: ScheduleSnapshot, offline: Boolean) {
                if (loadId != calendarLoadId) return
                val current = calendar
                val same = current?.let {
                    it.title == title && it.year == yearCode && it.source?.kind == source?.kind &&
                        it.source?.code == source?.code && it.combinedSubjects == combined
                } == true
                if (!same || openDay != null && !selectionApplied) {
                    val initialDay = openDay ?: openingDay(LocalDate.now(), weekend)
                    week = startOfWeek(initialDay)
                    selectedDay = initialDay
                    selectionApplied = true
                }
                calendar = CalendarData(title, snapshot.lessons, yearCode, source,
                    snapshot.updatedAtMillis, offline, combined)
            }

            val cached = withContext(Dispatchers.IO) {
                runCatching {
                    if (combined != null) api.cachedSavedLessons(combined)
                    else api.cachedLessons(yearCode, requireNotNull(source))
                }.getOrNull()
            }
            if (cached != null && loadId == calendarLoadId) {
                show(cached, offline = false)
                busy = false
            }

            try {
                val fresh = withContext(Dispatchers.IO) {
                    if (combined != null) {
                        if (forceRefresh) api.refreshSavedLessons(combined)
                        else api.refreshSavedLessonsIfStale(combined)
                    } else {
                        if (forceRefresh) api.refreshLessons(yearCode, requireNotNull(source))
                        else api.refreshLessonsIfStale(yearCode, requireNotNull(source))
                    }
                }
                show(fresh, offline = false)
            } catch (cause: Exception) {
                if (loadId == calendarLoadId) {
                    if (cached != null) show(cached, offline = true)
                    else error = cause.message ?: "Impossibile recuperare le lezioni."
                }
            } finally {
                if (loadId == calendarLoadId) {
                    busy = false
                    calendarRefreshing = false
                }
            }
        }
    }

    fun openItem(item: SearchItem) {
        val currentYear = year ?: return
        if (item.kind != SearchKind.COURSE) {
            showCalendar(item.name, currentYear.code, item)
            return
        }
        scope.launch {
            busy = true
            error = null
            try {
                val course = withContext(Dispatchers.IO) { api.courseWithTeachings(currentYear.code, item) }
                courseDetail = CourseDetailData(currentYear.code, course)
            } catch (cause: Exception) {
                error = cause.message ?: "Impossibile aprire il corso."
            } finally {
                busy = false
            }
        }
    }

    fun openFavorite(favorite: FavoriteCourse) {
        scope.launch {
            busy = true
            error = null
            try {
                val cached = if (favorite.year == year?.code) entries[SearchKind.COURSE]?.items
                    ?.firstOrNull { it.code == favorite.code } else null
                val course = cached ?: withContext(Dispatchers.IO) {
                    api.entries(SearchKind.COURSE, favorite.year).firstOrNull { it.code == favorite.code }
                } ?: throw IllegalStateException("Il corso non è più disponibile per l'anno ${favorite.year}.")
                val complete = withContext(Dispatchers.IO) { api.courseWithTeachings(favorite.year, course) }
                courseDetail = CourseDetailData(favorite.year, complete)
            } catch (cause: Exception) {
                error = cause.message ?: "Impossibile aprire il corso salvato."
            } finally {
                busy = false
            }
        }
    }

    fun openSaved(subject: SavedSubject) = showCalendar(
        subject.name, subject.year, SearchItem(subject.code, subject.name, SearchKind.SUBJECT)
    )

    fun goBack() {
        if (subjectSelectionTarget != null) {
            subjectSelectionTarget = null
            return
        }
        if (examSearchOpen && examCourse == null) {
            examSearchOpen = false
            return
        }
        calendarLoadId++
        calendarRefreshing = false
        busy = false
        when {
            notificationDetailEntry != null -> {
                notificationDetailEntry = null
                if (notificationDetailReturnsToCenter) notificationsOpen = true
                notificationDetailReturnsToCenter = false
            }
            examPlannerAppeal != null -> {
                examPlannerAppeal = null
                examPlannerNote = ""
                examPlannerAppealAvailable = true
            }
            examPlannerOpen -> examPlannerOpen = false
            lessonMonthOpen -> lessonMonthOpen = false
            agendaOpen -> agendaOpen = false
            examCourse != null -> {
                examLoadId++
                examRefreshing = false
                examCourse = null
                examSubject = null
                examCourseYear = null
                examData = null
                examError = null
                examOpenedFromCourseDetail = false
            }
            calendar != null -> calendar = null
            courseDetail != null -> courseDetail = null
            notificationSettingsOpen -> {
                notificationSettingsOpen = false
                settings = true
            }
            settings -> settings = false
            notificationsOpen -> notificationsOpen = false
            selectedRoom != null -> {
                selectedRoom = null
                roomPlanningLoading = false
            }
            selectedBuilding != null -> {
                selectedBuilding = null
                buildingPlanningLoading = false
            }
        }
        error = null
    }

    BackHandler(enabled = notificationDetailEntry != null || examPlannerAppeal != null || examPlannerOpen || lessonMonthOpen || calendar != null || courseDetail != null || selectedRoom != null || selectedBuilding != null || examCourse != null || examSearchOpen || subjectSelectionTarget != null || agendaOpen || settings ||
        notificationsOpen || notificationSettingsOpen) { goBack() }

    val showingRootTab = notificationDetailEntry == null && examPlannerAppeal == null && !examPlannerOpen && !lessonMonthOpen && calendar == null &&
        courseDetail == null && selectedRoom == null && selectedBuilding == null && examCourse == null && !agendaOpen && !settings &&
        !notificationsOpen && !notificationSettingsOpen
    val activeCatalogKind = when {
        subjectSelectionTarget?.year == year?.code -> SearchKind.SUBJECT
        examSearchOpen -> SearchKind.COURSE
        showingRootTab && tab == RootTab.EXPLORE && !exploringRooms -> kind
        showingRootTab && tab == RootTab.FAVORITES && favoriteSection == FavoriteSection.SUBJECTS -> SearchKind.SUBJECT
        else -> null
    }
    val catalogLoading = activeCatalogKind != null && (
        year == null && loadingYears || year != null &&
            entries[activeCatalogKind] == null && activeCatalogKind !in entriesAttempted)
    val examSearchLoading = year == null && loadingYears || year != null && (
        listOf(SearchKind.COURSE, SearchKind.SUBJECT).any {
            entries[it] == null && it !in entriesAttempted
        } || examSubjectIndex != null && examSearchSubjectIndex == null)

    val calendarTitle = calendar?.let { shown ->
        when {
            courseDetail?.course?.code == shown.source?.code &&
                shown.source?.kind == SearchKind.COURSE -> "Lezioni"
            shown.combinedSubjects != null && shown.title == tab.title -> "Calendario"
            else -> shown.title
        }
    }
    val examListTitle = examCourse?.let { selected ->
        if (examOpenedFromCourseDetail && courseDetail?.course?.code == selected.code) "Appelli"
        else if (selected.kind == SearchKind.COURSE) "Corso di laurea" else "Insegnamento"
    }
    val pageTitle = when {
        notificationDetailEntry != null -> "Dettagli variazione"
        notificationSettingsOpen -> "Impostazioni notifiche"
        notificationsOpen -> "Notifiche"
        settings -> "Preferenze"
        agendaOpen -> "Agenda"
        examPlannerAppeal != null -> "Dettagli appello"
        examPlannerOpen -> "I miei appelli"
        lessonMonthOpen -> "Calendario mensile"
        examCourse != null -> requireNotNull(examListTitle)
        calendar != null -> requireNotNull(calendarTitle)
        courseDetail != null -> "Corso di laurea"
        selectedRoom != null -> "Aula"
        selectedBuilding != null -> "Edificio"
        else -> "Orari UNIMI"
    }
    val backTitle = when {
        notificationDetailEntry != null -> when {
            notificationDetailReturnsToCenter -> "Notifiche"
            examPlannerOpen -> "I miei appelli"
            else -> tab.title
        }
        notificationSettingsOpen -> "Preferenze"
        notificationsOpen || settings -> tab.title
        agendaOpen || lessonMonthOpen -> calendarTitle ?: tab.title
        examPlannerAppeal != null -> when {
            examPlannerOpen -> "I miei appelli"
            examCourse != null -> requireNotNull(examListTitle)
            else -> tab.title
        }
        examPlannerOpen -> examListTitle ?: tab.title
        examCourse != null -> if (examOpenedFromCourseDetail && courseDetail != null)
            courseDetail!!.course.name else tab.title
        calendar != null -> courseDetail?.course?.name ?: tab.title
        courseDetail != null -> tab.title
        selectedRoom != null -> selectedBuilding?.name ?: RootTab.EXPLORE.title
        selectedBuilding != null -> RootTab.EXPLORE.title
        else -> null
    }

    fun retryCatalog(requestedKind: SearchKind) {
        entries = entries - requestedKind
        entriesAttempted = entriesAttempted - requestedKind
        entriesRetry++
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(pageTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        style = if (pageTitle.length > 22) MaterialTheme.typography.titleMedium
                            else MaterialTheme.typography.titleLarge)
                },
                navigationIcon = {
                    if (backTitle != null) TextButton(onClick = ::goBack,
                        modifier = Modifier.widthIn(max = 142.dp),
                        contentPadding = PaddingValues(start = 12.dp, end = 6.dp)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Torna a $backTitle", modifier = Modifier.size(21.dp))
                        androidx.compose.foundation.layout.Spacer(Modifier.width(4.dp))
                        Text(backTitle, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelLarge)
                    }
                },
                actions = {
                    val shown = calendar
                    if (shown?.source?.kind == SearchKind.SUBJECT && !agendaOpen && !lessonMonthOpen) {
                        val isSaved = allFavoriteSubjects.any {
                            it.year == shown.year && it.code == shown.source.code
                        }
                        IconButton(onClick = { editSubjectSelection(shown.year, shown.source) }) {
                            Icon(if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Scegli Lezioni e Appelli per ${shown.source.name}") }
                    } else if (examCourse == null && !agendaOpen && !lessonMonthOpen && (shown?.source?.kind == SearchKind.COURSE ||
                        calendar == null && courseDetail != null)) {
                        val detail = if (shown?.source?.kind == SearchKind.COURSE)
                            CourseDetailData(shown.year, shown.source) else courseDetail!!
                        val favorite = FavoriteCourse(detail.year, detail.course.code, detail.course.name,
                            detail.course.degreeType ?: DegreeType.OTHER)
                        val isFavorite = favorites.any { it.year == favorite.year && it.code == favorite.code }
                        IconButton(onClick = { toggleFavorite(favorite) }) {
                            Icon(if (isFavorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (isFavorite) "Rimuovi corso dai salvati"
                                    else "Salva corso")
                        }
                    } else if (showingRootTab) {
                        IconButton(onClick = ::openNotifications) {
                            BadgedBox(badge = {
                                val unread = notificationEntries.count { !it.read }
                                if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) }
                            }) {
                                Icon(Icons.Outlined.NotificationsNone, contentDescription = "Notifiche")
                            }
                        }
                        IconButton(onClick = ::openSettings) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Preferenze")
                        }
                    }
                    if (shown != null && examCourse == null && examPlannerAppeal == null &&
                        !examPlannerOpen && notificationDetailEntry == null && !notificationsOpen &&
                        !settings && !agendaOpen && !lessonMonthOpen) {
                        IconButton(onClick = { agendaOpen = true }) {
                            Icon(Icons.Outlined.ViewAgenda, contentDescription = "Apri agenda")
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (showingRootTab) {
                AppBottomNavigation(
                    items = listOf(
                        AppNavigationItem("Lezioni", Icons.Outlined.CalendarMonth),
                        AppNavigationItem("Appelli", Icons.Outlined.EventAvailable),
                        AppNavigationItem("Esplora", Icons.Outlined.Search),
                        AppNavigationItem("Preferiti", Icons.Outlined.BookmarkBorder)
                    ),
                    selectedIndex = tab.index,
                    onSelect = { index -> tab = RootTab.fromIndex(index); error = null }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.fillMaxSize()) {
                val lessonsLoading = showingRootTab && tab == RootTab.LESSONS && saved.isNotEmpty() &&
                    (savedScheduleLoading || savedSchedule == null && !savedScheduleAttempted)
                val loading = catalogLoading || roomCatalogLoading && exploringRooms ||
                    selectedBuilding != null && buildingPlanningLoading ||
                    selectedRoom != null && roomPlanningLoading || lessonsLoading ||
                    examCourse != null && examRefreshing ||
                    calendar != null && calendarRefreshing || busy
                Box(Modifier.fillMaxWidth().height(4.dp)) {
                    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(4.dp))
                }
                if (error != null) ErrorBanner(error!!, onDismiss = { error = null },
                    onRetry = if (year == null) {{ yearRetry++ }}
                        else activeCatalogKind?.let { requested -> { retryCatalog(requested) } })
                when {
                    notificationDetailEntry != null -> {
                        val entry = notificationDetailEntry!!
                        NotificationChangeDetailsScreen(entry = entry, onOpenTarget = {
                            notificationDetailEntry = null
                            notificationDetailReturnsToCenter = false
                            when (entry.type) {
                                AppNotificationType.EXAM_CHANGE,
                                AppNotificationType.EXAM_REGISTRATION_REMINDER -> {
                                    tab = RootTab.EXAMS
                                    savedExamAppeals = store.savedExamAppeals()
                                    removedExamNotices = store.removedExamNotices()
                                    val active = savedExamAppeals.firstOrNull {
                                        ExamNotificationEngine.identity(it) == entry.targetKey
                                    }
                                    val removed = removedExamNotices.firstOrNull {
                                        ExamNotificationEngine.identity(it.appeal) == entry.targetKey
                                    }?.appeal
                                    val target = active ?: removed
                                    if (target != null) {
                                        examPlannerAppeal = target
                                        examPlannerAppealAvailable = active != null
                                        examPlannerNote = if (active != null) store.examNote(target) else ""
                                    } else openExamPlanner()
                                }
                                else -> entry.targetDate?.let { day ->
                                    if (saved.isNotEmpty()) showCalendar(
                                        "Lezioni", year?.code.orEmpty(), null, saved, day)
                                }
                            }
                        })
                    }
                    notificationSettingsOpen -> NotificationSettingsScreen(
                        notificationPreferences = notificationPreferences,
                        lessonDiagnostics = lessonDiagnostics,
                        examDiagnostics = examDiagnostics,
                        checkingLessons = checkingLessonDiagnostics,
                        checkingExams = checkingExamDiagnostics,
                        onLessonChanges = {
                            NotificationBaselineStore(context).clear()
                            setNotificationPreferences(notificationPreferences.copy(
                                lessonChanges = !notificationPreferences.lessonChanges))
                        },
                        onExamChanges = {
                            ExamNotificationBaselineStore(context).clear()
                            setNotificationPreferences(notificationPreferences.copy(
                                examChanges = !notificationPreferences.examChanges))
                        },
                        onExamRegistrationOpenReminders = {
                            setNotificationPreferences(notificationPreferences.copy(
                                examRegistrationOpenReminders =
                                    !notificationPreferences.examRegistrationOpenReminders))
                        },
                        onExamRegistrationCloseReminders = {
                            setNotificationPreferences(notificationPreferences.copy(
                                examRegistrationCloseReminders =
                                    !notificationPreferences.examRegistrationCloseReminders))
                        },
                        onExamRegistrationCloseLeadDays = { days ->
                            setNotificationPreferences(notificationPreferences.copy(
                                examRegistrationCloseLeadDays = days))
                        },
                        onLessonReminders = {
                            store.lastLessonReminderKey = null
                            setNotificationPreferences(notificationPreferences.copy(
                                lessonReminders = !notificationPreferences.lessonReminders))
                        },
                        onAppUpdates = {
                            setNotificationPreferences(notificationPreferences.copy(
                                appUpdates = !notificationPreferences.appUpdates))
                        },
                        onReminderMinutes = { minutes ->
                            store.lastLessonReminderKey = null
                            setNotificationPreferences(notificationPreferences.copy(reminderMinutes = minutes))
                        },
                        onCheckLessons = { refreshDiagnostics(SyncDiagnosticKind.LESSONS) },
                        onCheckExams = { refreshDiagnostics(SyncDiagnosticKind.EXAMS) }
                    )
                    notificationsOpen -> NotificationCenterScreen(
                        entries = notificationEntries,
                        refreshing = notificationRefreshing,
                        onRefresh = ::refreshNotifications,
                        onDelete = { entry ->
                            store.deleteNotification(entry.id)
                            notificationEntries = store.notifications()
                            scope.launch {
                                val result = snackbar.showAppSnackbar(
                                    message = "Notifica eliminata",
                                    actionLabel = "Annulla",
                                    withDismissAction = true
                                )
                                if (result == SnackbarResult.ActionPerformed && store.addNotification(entry)) {
                                    notificationEntries = store.notifications()
                                }
                            }
                        },
                        onClear = {
                            store.clearNotifications()
                            NotificationPublisher.cancelAll(context)
                            notificationEntries = store.notifications()
                        },
                        onOpen = { entry ->
                            when (entry.type) {
                                AppNotificationType.APP_UPDATE -> openSettings()
                                AppNotificationType.LESSON_CHANGE,
                                AppNotificationType.EXAM_CHANGE,
                                AppNotificationType.EXAM_REGISTRATION_REMINDER -> {
                                    notificationsOpen = false
                                    notificationDetailReturnsToCenter = true
                                    notificationDetailEntry = entry
                                }
                                else -> if (saved.isNotEmpty()) showCalendar(
                                    "Lezioni", year?.code.orEmpty(), null, saved, entry.targetDate
                                )
                            }
                        }
                    )
                    settings -> SettingsScreen(
                        weekend = weekend,
                        onWeekend = ::toggleWeekend,
                        currentVersion = BuildConfig.VERSION_NAME,
                        updateState = updateState,
                        releaseNotes = releaseNotes,
                        onCheckUpdate = ::checkForUpdate,
                        onDownloadUpdate = ::downloadUpdate,
                        onInstallUpdate = ::installUpdate,
                        notificationPreferences = notificationPreferences,
                        onNotificationsEnabled = ::toggleNotifications,
                        onOpenNotificationSettings = ::openNotificationSettings
                    )
                    agendaOpen -> ScheduleAgendaScreen(
                        lessons = calendar?.lessons ?: savedSchedule?.lessons,
                        loading = if (calendar != null) calendarRefreshing
                            else savedScheduleLoading || !savedScheduleAttempted,
                        weekend = weekend,
                        onOpenDay = { day ->
                            agendaOpen = false
                            week = startOfWeek(day)
                            selectedDay = day
                        }
                    )
                    examPlannerAppeal != null -> {
                        val selected = examPlannerAppeal!!
                        val selectedSaved = savedExamAppeals.any {
                            it.id == selected.id && it.courseCode == selected.courseCode
                        }
                        ExamAppealDetailsScreen(
                            appeal = selected,
                            saved = selectedSaved,
                            available = examPlannerAppealAvailable,
                            savedNote = examPlannerNote,
                            onSaveNote = { appeal, note ->
                                store.setExamNote(appeal, note)
                                examPlannerNote = note.trim()
                                showBriefSnackbar(if (examPlannerNote.isBlank()) "Nota rimossa" else "Nota salvata")
                            },
                            onToggleSaved = { appeal ->
                                toggleSavedExamAppeal(appeal)
                                examPlannerNote = store.examNote(appeal)
                            },
                            onAddToCalendar = ::addExamToCalendar
                        )
                    }
                    examPlannerOpen -> ExamPlannerScreen(
                        savedAppeals = savedExamAppeals,
                        removedNotices = removedExamNotices,
                        appealsWithNotes = examAppealsWithNotes,
                        showSaved = examPlannerSavedMode,
                        onShowSaved = { examPlannerSavedMode = it },
                        onSelectAppeal = {
                            examPlannerAppeal = it
                            examPlannerAppealAvailable = true
                            examPlannerNote = store.examNote(it)
                        },
                        onSelectRemoved = { notice ->
                            notificationDetailReturnsToCenter = false
                            notificationDetailEntry = AppNotificationEntry(
                                id = notice.id,
                                type = AppNotificationType.EXAM_CHANGE,
                                title = "Appello non più disponibile",
                                message = "${notice.appeal.subjectName}: l’appello del ${notice.appeal.date} non risulta più pubblicato.",
                                timestampMillis = notice.detectedAtMillis,
                                targetDate = notice.appeal.date,
                                targetKey = ExamNotificationEngine.identity(notice.appeal),
                                targetTitle = notice.appeal.subjectName,
                                changes = listOf(NotificationFieldChange(
                                    "Stato", "Disponibile", "Non più pubblicato"))
                            )
                        },
                        onDismissRemoved = { notice ->
                            store.dismissRemovedExamNotice(notice.id)
                            removedExamNotices = store.removedExamNotices()
                        }
                    )
                    examCourse != null -> {
                        val selected = examCourse!!
                        val shown = examData?.takeIf {
                            it.courseCode == selected.code && it.window == examWindow
                        }
                        ExamsScreen(
                            courses = examCourseIndex?.items,
                            query = examQuery,
                            results = examResults,
                            selectedItem = selected,
                            showSelectedName = !examOpenedFromCourseDetail,
                            selectedSubjectFromLessons = examSubject?.let { subject ->
                                lessonExamSubjects.any { it.year == subject.year && it.code == subject.code }
                            } == true,
                            selectedSubjectManual = examSubject?.let { subject ->
                                manualExamSubjects.any { it.year == subject.year && it.code == subject.code }
                            } == true,
                            window = examWindow,
                            appeals = shown?.snapshot?.appeals,
                            updatedAtMillis = shown?.snapshot?.updatedAtMillis,
                            offline = shown?.offline == true,
                            errorMessage = examError,
                            loadingCourses = catalogLoading,
                            refreshing = examRefreshing,
                            savedAppeals = savedExamAppeals,
                            appealsWithNotes = examAppealsWithNotes,
                            onQuery = { examQuery = it },
                            onSelectCourse = { showExams(it) },
                            onToggleManualSubject = { item ->
                                editSubjectSelection(examCourseYear ?: year?.code.orEmpty(), item)
                            },
                            onWindow = { selectedWindow ->
                                val subject = examSubject
                                if (subject != null) showSubjectExams(subject, selectedWindow)
                                else showExams(selected, selectedWindow, examOpenedFromCourseDetail, examCourseYear)
                            },
                            onRefresh = {
                                val subject = examSubject
                                if (subject != null) showSubjectExams(subject, examWindow)
                                else showExams(selected, examWindow, examOpenedFromCourseDetail, examCourseYear)
                            },
                            onRetryCourses = { retryCatalog(SearchKind.COURSE) },
                            onOpenPlanner = ::openExamPlanner,
                            onOpenAppeal = {
                                examPlannerAppeal = it
                                examPlannerAppealAvailable = true
                                examPlannerNote = store.examNote(it)
                            },
                            onToggleSavedAppeal = ::toggleSavedExamAppeal,
                            onAddToCalendar = ::addExamToCalendar
                        )
                    }
                    lessonMonthOpen -> LessonMonthScreen(
                        lessons = calendar?.lessons ?: savedSchedule?.lessons.orEmpty(),
                        initialDay = selectedDay,
                        weekend = weekend,
                        onOpenDay = { day ->
                            selectedDay = day
                            week = startOfWeek(day)
                            lessonMonthOpen = false
                        }
                    )
                    calendar != null -> {
                        val shown = calendar!!
                        CalendarScreen(shown, week, selectedDay, weekend, allFavoriteSubjects,
                            refreshing = calendarRefreshing,
                            onToggleSubject = { lesson ->
                                editSubjectSelection(shown.year, SearchItem(lesson.subjectCode,
                                    lesson.subject, SearchKind.SUBJECT))
                            },
                            onRefresh = { showCalendar(shown.title, shown.year, shown.source,
                                shown.combinedSubjects, forceRefresh = true) },
                            onMoveWeek = ::moveWeek, onSelectDay = { day ->
                                week = startOfWeek(day)
                                selectedDay = day
                            }, onOpenMonth = { lessonMonthOpen = true })
                    }
                    courseDetail != null -> {
                        val detail = courseDetail!!
                        CourseDetailScreen(detail.course, detail.year,
                            preferredSubjects = allFavoriteSubjects,
                            onOpenCalendar = { showCalendar(detail.course.name, detail.year, detail.course) },
                            onOpenExams = { showExams(detail.course, returnToCourseDetail = true,
                                courseYear = detail.year) },
                            onOpenTeaching = { teaching ->
                                showCalendar(teaching.name, detail.year,
                                    SearchItem(teaching.code, teaching.name, SearchKind.SUBJECT))
                            },
                            onToggleTeaching = { teaching ->
                                editSubjectSelection(detail.year, SearchItem(teaching.code,
                                    teaching.name, SearchKind.SUBJECT))
                            })
                    }
                    tab == RootTab.EXPLORE && selectedRoom != null -> RoomDetailScreen(
                        room = selectedRoom!!,
                        date = roomDay,
                        planning = roomPlanning,
                        loading = roomPlanningLoading,
                        error = roomPlanningError,
                        onDate = { roomDay = it; roomPlanningRetry = 0 },
                        onRefresh = { roomPlanningRetry++ }
                    )
                    tab == RootTab.EXPLORE && selectedBuilding != null -> RoomBuildingScreen(
                        building = selectedBuilding!!,
                        planning = buildingPlanning,
                        loading = buildingPlanningLoading,
                        error = buildingPlanningError,
                        onOpenRoom = {
                            selectedRoom = it
                            roomDay = LocalDate.now(ZoneId.of("Europe/Rome"))
                            roomPlanningRetry = 0
                        },
                        onRefresh = { buildingPlanningRetry++ }
                    )
                    tab == RootTab.EXPLORE -> SearchScreen(
                        years = years, year = year, loadingYears = loadingYears,
                        kind = kind, query = query, results = results,
                        loadingEntries = year == null && loadingYears ||
                            year != null && entries[kind] == null && kind !in entriesAttempted,
                        roomsMode = exploringRooms, roomQuery = roomQuery,
                        roomResults = roomResults,
                        roomLoading = roomCatalogLoading || roomCatalog == null && roomCatalogError == null,
                        roomError = roomCatalogError,
                        favorites = favorites, selectedSubjects = allFavoriteSubjects,
                        onYear = { selected ->
                            year = selected
                            entries = emptyMap()
                            entriesAttempted = emptySet()
                            query = ""
                            examQuery = ""
                            error = null
                        },
                        onKind = {
                            exploringRooms = false
                            kind = it; query = ""; error = null
                        },
                        onRooms = {
                            exploringRooms = true; roomQuery = ""; error = null
                        },
                        onRoomQuery = { roomQuery = it },
                        onOpenBuilding = {
                            selectedBuilding = it
                            buildingDay = LocalDate.now(ZoneId.of("Europe/Rome"))
                            buildingPlanningRetry = 0
                        },
                        onRetryRooms = { roomCatalogError = null; roomCatalogRetry++ },
                        onQuery = { query = it }, onSelect = ::openItem,
                        onToggleCourseFavorite = { item ->
                            year?.let { selected -> toggleFavorite(FavoriteCourse(selected.code, item.code,
                                item.name, item.degreeType ?: DegreeType.OTHER)) }
                        },
                        onEditSubject = { item ->
                            year?.let { selected -> editSubjectSelection(selected.code, item) }
                        },
                        onRetryYears = { yearRetry++ }, onRetryEntries = {
                            retryCatalog(kind)
                        }
                    )
                    tab == RootTab.LESSONS && saved.isEmpty() -> LessonsEmptyScreen(
                        onManage = { tab = RootTab.FAVORITES; favoriteSection = FavoriteSection.SUBJECTS }
                    )
                    tab == RootTab.LESSONS -> {
                        val snapshot = savedSchedule
                        if (snapshot != null) CalendarScreen(
                            calendar = CalendarData("Lezioni", snapshot.lessons,
                                year?.code.orEmpty(), null, snapshot.updatedAtMillis,
                                offline = false, combinedSubjects = saved),
                            week = week, selectedDay = selectedDay, weekend = weekend,
                            preferredSubjects = allFavoriteSubjects,
                            onToggleSubject = { lesson ->
                                editSubjectSelection(year?.code.orEmpty(),
                                    SearchItem(lesson.subjectCode, lesson.subject, SearchKind.SUBJECT))
                            },
                            refreshing = savedScheduleLoading,
                            onRefresh = { savedScheduleForceRefresh = true; savedScheduleRetry++ },
                            onMoveWeek = ::moveWeek,
                            onSelectDay = { day -> week = startOfWeek(day); selectedDay = day },
                            onOpenMonth = { lessonMonthOpen = true },
                            rootLessons = true,
                            onOpenAgenda = { agendaOpen = true },
                            onManageSubjects = {
                                tab = RootTab.FAVORITES
                                favoriteSection = FavoriteSection.SUBJECTS
                            }
                        ) else Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
                            AppScreenHeader("Lezioni", "Il tuo orario settimanale")
                            if (savedScheduleLoading || !savedScheduleAttempted) AppLoadingState()
                            else AppEmptyState(
                                title = "Orario non disponibile",
                                message = "Non riesco a recuperare le lezioni. Riprova tra poco.",
                                action = "Riprova",
                                onAction = { savedScheduleAttempted = false; savedScheduleRetry++ }
                            )
                        }
                    }
                    tab == RootTab.EXAMS -> ExamPlannerScreen(
                        savedAppeals = savedExamAppeals,
                        removedNotices = removedExamNotices,
                        appealsWithNotes = examAppealsWithNotes,
                        showSaved = examPlannerSavedMode,
                        onShowSaved = { examPlannerSavedMode = it },
                        embedded = true,
                        rootMode = true,
                        onSearch = { examSearchOpen = true; examQuery = "" },
                        onFavorites = {
                            tab = RootTab.FAVORITES
                            favoriteSection = FavoriteSection.SUBJECTS
                        },
                        onSelectAppeal = {
                            examPlannerAppeal = it
                            examPlannerAppealAvailable = true
                            examPlannerNote = store.examNote(it)
                        },
                        onSelectRemoved = { notice ->
                            notificationDetailReturnsToCenter = false
                            notificationDetailEntry = AppNotificationEntry(
                                id = notice.id,
                                type = AppNotificationType.EXAM_CHANGE,
                                title = "Appello non più disponibile",
                                message = "${notice.appeal.subjectName}: l’appello del ${notice.appeal.date} non risulta più pubblicato.",
                                timestampMillis = notice.detectedAtMillis,
                                targetDate = notice.appeal.date,
                                targetKey = ExamNotificationEngine.identity(notice.appeal),
                                targetTitle = notice.appeal.subjectName,
                                changes = listOf(NotificationFieldChange(
                                    "Stato", "Disponibile", "Non più pubblicato"))
                            )
                        },
                        onDismissRemoved = { notice ->
                            store.dismissRemovedExamNotice(notice.id)
                            removedExamNotices = store.removedExamNotices()
                        }
                    )
                    else -> FavoritesScreen(
                        courses = favorites,
                        subjects = allFavoriteSubjects,
                        section = favoriteSection,
                        examIndexLoading = catalogLoading,
                        onSection = { favoriteSection = it },
                        onOpenCourse = ::openFavorite,
                        onOpenLesson = { openSaved(SavedSubject(it.year, it.code, it.name)) },
                        onOpenExams = { subject ->
                            if (subject.examSources.isNotEmpty()) showSubjectExams(FollowedExamSubject(
                                subject.year, subject.code, subject.name, subject.examSources))
                            else showBriefSnackbar("Appelli non disponibili per questo insegnamento")
                        },
                        onRemoveCourse = ::toggleFavorite,
                        onEditSubject = { subject ->
                            editSubjectSelection(subject.year, SearchItem(subject.code,
                                subject.name, SearchKind.SUBJECT, examSources = subject.examSources))
                        },
                        onAddCourse = {
                            exploringRooms = false
                            kind = SearchKind.COURSE; query = ""; error = null
                            tab = RootTab.EXPLORE
                        },
                        onAddSubject = {
                            exploringRooms = false
                            kind = SearchKind.SUBJECT; query = ""; error = null
                            tab = RootTab.EXPLORE
                        },
                        onClearCourses = ::clearFavoriteCourses,
                        onClearSubjects = ::clearFavoriteSubjects
                    )
                }
            }
            if (busy) CircularProgressIndicator(Modifier.align(androidx.compose.ui.Alignment.Center))
        }
    }
    if (examSearchOpen && examCourse == null) ExamCourseSearchSheet(
        courses = examCourseIndex?.items,
        subjects = examSearchSubjectIndex?.items,
        query = examQuery,
        results = examResults,
        loading = examSearchLoading,
        onQuery = { examQuery = it },
        onSelect = { item ->
            examSearchOpen = false
            if (item.kind == SearchKind.SUBJECT) showSubjectExams(FollowedExamSubject(
                year?.code.orEmpty(), item.code, item.name, item.examSources))
            else showExams(item)
        },
        onRetry = {
            listOf(SearchKind.COURSE, SearchKind.SUBJECT).filter { entries[it] == null }
                .forEach(::retryCatalog)
        },
        onDismiss = { examSearchOpen = false }
    )
    subjectSelectionTarget?.let { target ->
        val currentIndex = if (target.year == year?.code) entries[SearchKind.SUBJECT] else null
        val indexed = currentIndex?.items?.firstOrNull { it.code == target.item.code }
        val completeItem = target.item.copy(examSources =
            (target.item.examSources + indexed?.examSources.orEmpty()).distinct())
        val needsExamIndex = target.year == year?.code && currentIndex == null &&
            completeItem.examSources.isEmpty()
        val existing = allFavoriteSubjects.firstOrNull {
            it.year == target.year && it.code == target.item.code
        }
        SubjectSelectionSheet(
            year = target.year,
            subject = completeItem,
            inLessons = existing?.inLessons == true,
            inExams = existing?.inExams == true,
            alreadySaved = existing != null,
            loadingExams = needsExamIndex && SearchKind.SUBJECT !in entriesAttempted,
            examLookupFailed = needsExamIndex && SearchKind.SUBJECT in entriesAttempted,
            onRetryExams = { retryCatalog(SearchKind.SUBJECT) },
            onDismiss = { subjectSelectionTarget = null },
            onApply = { lessons, exams ->
                subjectSelectionTarget = null
                setSubjectDestinations(target.copy(item = completeItem), lessons, exams)
            }
        )
    }
}

private const val APP_SNACKBAR_DURATION_MILLIS = 1_800L

private suspend fun SnackbarHostState.showAppSnackbar(
    message: String,
    actionLabel: String? = null,
    withDismissAction: Boolean = false
): SnackbarResult = coroutineScope {
    currentSnackbarData?.dismiss()
    val dismissJob = launch {
        delay(APP_SNACKBAR_DURATION_MILLIS)
        if (currentSnackbarData?.visuals?.message == message) currentSnackbarData?.dismiss()
    }
    try {
        showSnackbar(
            message = message,
            actionLabel = actionLabel,
            withDismissAction = withDismissAction,
            duration = SnackbarDuration.Indefinite
        )
    } finally {
        dismissJob.cancel()
    }
}

fun startOfWeek(day: LocalDate): LocalDate = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

fun openingDay(today: LocalDate, weekend: Boolean): LocalDate =
    if (weekend || today.dayOfWeek.value <= DayOfWeek.FRIDAY.value) today
    else startOfWeek(today).plusDays(4)

fun adjacentCalendarDay(day: LocalDate, direction: Long, weekend: Boolean): LocalDate {
    require(direction == -1L || direction == 1L)
    var adjacent = day.plusDays(direction)
    while (!weekend && adjacent.dayOfWeek.value > DayOfWeek.FRIDAY.value) {
        adjacent = adjacent.plusDays(direction)
    }
    return adjacent
}

fun preferredDay(lessons: List<Lesson>, week: LocalDate, weekend: Boolean): LocalDate =
    lessons.firstOrNull { !it.date.isBefore(week) && it.date.isBefore(week.plusDays(if (weekend) 7 else 5)) }?.date
        ?: week

private fun examFailureMessage(cause: Throwable): String {
    val failure = generateSequence(cause) { it.cause }.firstOrNull {
        it is UnknownHostException || it is ConnectException || it is SocketTimeoutException ||
            it is org.json.JSONException
    } ?: cause
    return when (failure) {
        is UnknownHostException, is ConnectException ->
            "Nessuna connessione disponibile. Controlla la rete e riprova."
        is SocketTimeoutException ->
            "Il servizio UNIMI sta impiegando troppo tempo a rispondere. Riprova tra poco."
        is org.json.JSONException ->
            "Il servizio UNIMI ha restituito dati non leggibili. Riprova più tardi."
        else -> failure.message?.takeIf { it.isNotBlank() }?.let { "$it Riprova tra poco." }
            ?: "Il servizio UNIMI non è al momento raggiungibile. Riprova tra poco."
    }
}
