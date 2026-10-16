package app.orariunimi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.text.Normalizer
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamCourseSearchSheet(
    courses: List<SearchItem>?,
    query: String,
    results: List<SearchItem>?,
    loading: Boolean,
    onQuery: (String) -> Unit,
    onSelect: (SearchItem) -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.82f)) {
            Text("Cerca appelli", Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
            ExamCoursePicker(courses, query, results, loading, onQuery, onSelect, onRetry)
        }
    }
}

@Composable
fun ExamsScreen(
    courses: List<SearchItem>?,
    query: String,
    results: List<SearchItem>?,
    selectedItem: SearchItem?,
    selectedSubjectFromLessons: Boolean,
    selectedSubjectManual: Boolean,
    window: ExamWindow,
    appeals: List<ExamAppeal>?,
    updatedAtMillis: Long?,
    offline: Boolean,
    errorMessage: String?,
    loadingCourses: Boolean,
    refreshing: Boolean,
    savedAppeals: List<ExamAppeal>,
    appealsWithNotes: Set<Pair<String, String>>,
    onQuery: (String) -> Unit,
    onSelectCourse: (SearchItem) -> Unit,
    onToggleManualSubject: (SearchItem) -> Unit,
    onWindow: (ExamWindow) -> Unit,
    onRefresh: () -> Unit,
    onRetryCourses: () -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenAppeal: (ExamAppeal) -> Unit,
    onToggleSavedAppeal: (ExamAppeal) -> Unit,
    onAddToCalendar: (ExamAppeal) -> Unit
) {
    if (selectedItem == null) {
        Column(Modifier.fillMaxSize()) {
            AppScreenHeader(
                title = "Cerca appelli",
                subtitle = "Trova gli esami di un corso di laurea",
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp)
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
            Box(Modifier.weight(1f)) {
                ExamCoursePicker(courses, query, results, loadingCourses,
                    onQuery, onSelectCourse, onRetryCourses)
            }
        }
    } else {
        ExamAppealsList(selectedItem, window, appeals, updatedAtMillis, offline, errorMessage, refreshing,
            savedAppeals, appealsWithNotes,
            followedFromLessons = selectedSubjectFromLessons,
            followedManually = selectedSubjectManual,
            onToggleFollowed = if (selectedItem.kind == SearchKind.SUBJECT) {
                { onToggleManualSubject(selectedItem) }
            } else null,
            onWindow, onRefresh, onOpenPlanner,
            onOpenAppeal, onToggleSavedAppeal, onAddToCalendar)
    }
}

@Composable
private fun ExamCoursePicker(
    courses: List<SearchItem>?, query: String,
    results: List<SearchItem>?, loading: Boolean, onQuery: (String) -> Unit,
    onSelect: (SearchItem) -> Unit, onRetry: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AppSearchField(query, onQuery, "Cerca corso di laurea")
        }

        when {
            loading && courses == null -> item { AppLoadingState() }
            courses == null -> item {
                ExamEmptyPanel("Elenco non disponibile", "Controlla la connessione e riprova.", "Riprova", onRetry)
            }
            query.isNotBlank() && results == null -> item { AppLoadingState() }
            query.isNotBlank() && results?.isEmpty() == true -> item {
                ExamEmptyPanel("Nessun corso trovato", "Prova con il nome o il codice del corso.")
            }
            query.isNotBlank() -> {
                item {
                    Text("${results.orEmpty().size} ${if (results.orEmpty().size == 1) "risultato" else "risultati"}",
                        Modifier.padding(top = 3.dp), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                items(results.orEmpty(), key = { "result:${it.code}" }) { course ->
                    DegreeCourseCard(course,
                        favorite = false,
                        onClick = { onSelect(course) },
                        showChevron = true)
                }
            }
            query.isBlank() && !loading -> item {
                AppEmptyState(
                    title = "Cerca un corso",
                    message = "Scrivi il nome o il codice del corso di laurea per vedere gli appelli programmati.",
                    icon = Icons.Outlined.Search
                )
            }
        }
    }
}

@Composable
private fun ExamAppealsList(
    course: SearchItem, window: ExamWindow, appeals: List<ExamAppeal>?, updatedAtMillis: Long?,
    offline: Boolean, errorMessage: String?, refreshing: Boolean, savedAppeals: List<ExamAppeal>,
    appealsWithNotes: Set<Pair<String, String>>,
    followedFromLessons: Boolean, followedManually: Boolean, onToggleFollowed: (() -> Unit)?,
    onWindow: (ExamWindow) -> Unit, onRefresh: () -> Unit, onOpenPlanner: () -> Unit,
    onOpenAppeal: (ExamAppeal) -> Unit,
    onToggleSavedAppeal: (ExamAppeal) -> Unit,
    onAddToCalendar: (ExamAppeal) -> Unit
) {
    var filter by remember(course.code, window) { mutableStateOf("") }
    val visibleAppeals = remember(appeals, filter) {
        if (filter.isBlank()) appeals.orEmpty() else appeals.orEmpty().filter { appeal ->
            val searchable = normalizeExamText(listOf(appeal.subjectName, appeal.subjectCode,
                appeal.subjectPortalCode, appeal.teacher).joinToString(" "))
            normalizeExamText(filter).split(' ').filter { it.isNotBlank() }.all { it in searchable }
        }
    }
    val groups = visibleAppeals.groupBy { it.date }.toSortedMap()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    if (course.kind == SearchKind.COURSE) {
                        DegreeBadge(course.degreeType ?: DegreeType.OTHER)
                    } else {
                        Surface(color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp)) {
                            Text(if (followedFromLessons) "Dai tuoi orari" else "Insegnamento seguito",
                                Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(course.name, Modifier.padding(top = 9.dp),
                        style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                        maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(course.code, style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (onToggleFollowed != null) AppSurfaceIconButton(
                        icon = if (followedFromLessons || followedManually)
                            Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = "Scegli Lezioni e Appelli per questo insegnamento",
                        onClick = onToggleFollowed)
                    AppSurfaceIconButton(
                        icon = Icons.Outlined.CalendarMonth,
                        contentDescription = "Apri i promemoria degli appelli",
                        onClick = onOpenPlanner
                    )
                }
            }
            ExamWindowSelector(window, onWindow, Modifier.padding(top = 14.dp))
            if (!appeals.isNullOrEmpty()) OutlinedTextField(
                value = filter,
                onValueChange = { filter = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                label = { Text("Filtra insegnamento o docente") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (filter.isNotEmpty()) {{
                    IconButton(onClick = { filter = "" }) {
                        Icon(Icons.Outlined.Close, contentDescription = "Cancella filtro")
                    }
                }} else null
            )
            if (offline) {
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(Modifier.padding(13.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Outlined.CloudOff, contentDescription = null,
                            modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(9.dp))
                        Column {
                            Text("Dati offline", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                            Text(errorMessage ?: "Non è stato possibile aggiornare gli appelli. Stai vedendo l'ultima copia salvata.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }
            if (updatedAtMillis != null) {
                ExamFreshness(appeals.orEmpty().size, updatedAtMillis, offline, refreshing, onRefresh)
            }
        }

        when {
            appeals == null && refreshing -> item { AppLoadingState() }
            appeals == null -> item {
                ExamEmptyPanel(
                    "Impossibile caricare gli appelli",
                    errorMessage ?: "Il servizio UNIMI non è al momento raggiungibile. Riprova tra poco.",
                    "Riprova",
                    onRefresh
                )
            }
            appeals.isEmpty() -> item {
                ExamEmptyPanel("Nessun appello pubblicato", when (window) {
                    ExamWindow.TODAY -> "Il servizio UNIMI non riporta appelli per oggi per questo corso."
                    ExamWindow.NEXT_30_DAYS -> "Il servizio UNIMI non riporta appelli nei prossimi 30 giorni."
                    ExamWindow.ALL -> "Il servizio UNIMI non riporta al momento appelli programmati."
                })
            }
            visibleAppeals.isEmpty() -> item {
                ExamEmptyPanel("Nessun risultato", "Nessun appello corrisponde al filtro inserito.")
            }
            else -> groups.forEach { (date, dayAppeals) ->
                item(key = "date:$date") {
                    Text(formatExamDate(date), Modifier.padding(top = 9.dp, bottom = 1.dp),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                }
                items(dayAppeals, key = { "appeal:${it.id}" }) { appeal ->
                    ExamAppealCard(
                        appeal,
                        saved = savedAppeals.any { it.id == appeal.id && it.courseCode == appeal.courseCode },
                        hasNote = (appeal.courseCode to appeal.id) in appealsWithNotes,
                        onOpen = { onOpenAppeal(appeal) },
                        onToggleSaved = { onToggleSavedAppeal(appeal) },
                        onAddToCalendar = onAddToCalendar
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamWindowSelector(
    window: ExamWindow,
    onWindow: (ExamWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = ExamWindow.entries
    AppSegmentedControl(
        options = options.map { it.label },
        selectedIndex = options.indexOf(window),
        onSelected = { onWindow(options[it]) },
        modifier = modifier
    )
}

@Composable
private fun ExamFreshness(
    count: Int,
    updatedAtMillis: Long,
    offline: Boolean,
    refreshing: Boolean,
    onRefresh: () -> Unit
) {
    val updated = remember(updatedAtMillis) {
        Instant.ofEpochMilli(updatedAtMillis).atZone(ZoneId.systemDefault())
    }
    val timestamp = if (updated.toLocalDate() == LocalDate.now()) {
        "alle ${updated.format(DateTimeFormatter.ofPattern("HH:mm"))}"
    } else {
        "il ${updated.format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))}"
    }
    val status = when {
        offline -> "Dati offline · aggiornati $timestamp"
        refreshing -> "Aggiornato $timestamp · controllo in corso"
        else -> "Aggiornato $timestamp"
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (offline) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("$count ${if (count == 1) "appello" else "appelli"}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status, style = MaterialTheme.typography.labelSmall,
                color = if (offline) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRefresh, enabled = !refreshing, modifier = Modifier.size(40.dp)) {
            if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Outlined.Refresh, contentDescription = "Aggiorna appelli")
        }
    }
}

@Composable
fun ExamPlannerScreen(
    savedAppeals: List<ExamAppeal>,
    removedNotices: List<RemovedExamNotice>,
    appealsWithNotes: Set<Pair<String, String>>,
    showSaved: Boolean,
    onShowSaved: (Boolean) -> Unit,
    showModeSelector: Boolean = true,
    embedded: Boolean = false,
    rootMode: Boolean = false,
    onSearch: (() -> Unit)? = null,
    onFavorites: (() -> Unit)? = null,
    onSelectAppeal: (ExamAppeal) -> Unit,
    onSelectRemoved: (RemovedExamNotice) -> Unit,
    onDismissRemoved: (RemovedExamNotice) -> Unit
) {
    val today = LocalDate.now()
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedDay by remember { mutableStateOf(today) }
    var drag by remember(month) { mutableFloatStateOf(0f) }
    val appealsByDay = remember(savedAppeals) {
        savedAppeals.groupBy { it.date }.mapValues { (_, items) ->
            items.sortedWith(compareBy<ExamAppeal> { it.time }.thenBy { it.subjectName })
        }
    }
    val selectedAppeals = appealsByDay[selectedDay].orEmpty()
    val savedGroups = remember(savedAppeals) { savedAppeals.groupBy { it.date }.toSortedMap() }
    val monthTitle = remember(month) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", examLocale))
            .replaceFirstChar { it.titlecase(examLocale) }
    }

    fun moveMonth(amount: Long) {
        val target = month.plusMonths(amount)
        val day = selectedDay.dayOfMonth.coerceAtMost(target.lengthOfMonth())
        month = target
        selectedDay = target.atDay(day)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (rootMode) item {
            AppScreenHeader(
                title = "Appelli",
                subtitle = "${savedAppeals.size} ${if (savedAppeals.size == 1) "appello salvato" else "appelli salvati"}",
                secondaryActionIcon = Icons.Outlined.BookmarkBorder,
                secondaryActionDescription = "Apri insegnamenti preferiti",
                onSecondaryAction = onFavorites,
                actionIcon = Icons.Outlined.Search,
                actionDescription = "Cerca appelli",
                onAction = onSearch
            )
        }
        if (showModeSelector) item {
            AppSegmentedControl(
                options = listOf("Calendario", "Salvati"),
                selectedIndex = if (showSaved) 1 else 0,
                onSelected = { onShowSaved(it == 1) }
            )
        }
        if (!showSaved || !embedded) item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (!showSaved) monthTitle else "Appelli salvati",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    if (!rootMode) Text("${savedAppeals.size} ${if (savedAppeals.size == 1) "appello salvato" else "appelli salvati"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!showSaved) {
                    TextButton(onClick = {
                        month = YearMonth.from(today)
                        selectedDay = today
                    }) { Text("Oggi") }
                    IconButton(onClick = { moveMonth(-1) }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Mese precedente")
                    }
                    IconButton(onClick = { moveMonth(1) }) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Mese successivo")
                    }
                }
            }
        }
        if (removedNotices.isNotEmpty()) {
            item {
                Text("DA VERIFICARE", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            }
            items(removedNotices, key = { it.id }) { notice ->
                RemovedExamNoticeCard(notice,
                    onOpen = { onSelectRemoved(notice) },
                    onDismiss = { onDismissRemoved(notice) })
            }
        }
        if (!showSaved) item {
            Surface(
                modifier = Modifier.fillMaxWidth().pointerInput(month) {
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { change, amount -> drag += amount; change.consume() },
                    onDragCancel = { drag = 0f },
                    onDragEnd = {
                        when {
                            drag > 70f -> moveMonth(-1)
                            drag < -70f -> moveMonth(1)
                        }
                        drag = 0f
                    }
                )
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(13.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            listOf("LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM")
                                .forEach { label ->
                                Box(Modifier.weight(1f).height(30.dp), contentAlignment = Alignment.Center) {
                                    Text(label, style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    AnimatedContent(
                        targetState = month,
                        transitionSpec = {
                            val forward = targetState.isAfter(initialState)
                            (slideInHorizontally(tween(240, easing = FastOutSlowInEasing)) {
                                if (forward) it / 4 else -it / 4
                            } + fadeIn(tween(180))).togetherWith(
                                slideOutHorizontally(tween(210, easing = FastOutSlowInEasing)) {
                                    if (forward) -it / 4 else it / 4
                                } + fadeOut(tween(140))
                            )
                        },
                        label = "exam-month"
                    ) { displayedMonth ->
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            completeExamMonthDates(displayedMonth).chunked(7).forEach { row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { day ->
                                        ExamPlannerDay(
                                            day = day,
                                            inCurrentMonth = YearMonth.from(day) == displayedMonth,
                                            selected = day == selectedDay,
                                            today = day == today,
                                            count = appealsByDay[day].orEmpty().size,
                                            onSelect = {
                                                selectedDay = it
                                                month = YearMonth.from(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!showSaved) item {
            Column {
                Text(formatExamDate(selectedDay), style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold)
                Text(if (selectedAppeals.isEmpty()) "Nessun appello" else
                    "${selectedAppeals.size} ${if (selectedAppeals.size == 1) "appello" else "appelli"}",
                    Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (!showSaved && selectedAppeals.isEmpty()) item {
            ExamEmptyPanel(
                if (savedAppeals.isEmpty()) "Nessun appello salvato" else "Giornata libera",
                if (savedAppeals.isEmpty()) "Salva un appello con il segnalibro per ritrovarlo nel calendario."
                else "Non hai appelli salvati per questa data."
            )
        } else if (!showSaved) items(
            selectedAppeals,
            key = { "planner:${it.courseCode}:${it.id}" }
        ) { appeal ->
            ExamPlannerAppeal(
                appeal,
                hasNote = (appeal.courseCode to appeal.id) in appealsWithNotes,
                onClick = { onSelectAppeal(appeal) }
            )
        }
        if (showSaved && savedAppeals.isEmpty()) item {
            ExamEmptyPanel(
                "Nessun appello salvato",
                "Salva un appello con il segnalibro per ritrovarlo qui e nel calendario."
            )
        } else if (showSaved) savedGroups.forEach { (date, appeals) ->
            item(key = "saved-date:$date") {
                Text(formatExamDate(date), Modifier.padding(top = 5.dp),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold)
            }
            items(appeals, key = { "saved:${it.courseCode}:${it.id}" }) { appeal ->
                ExamPlannerAppeal(
                    appeal,
                    hasNote = (appeal.courseCode to appeal.id) in appealsWithNotes,
                    onClick = { onSelectAppeal(appeal) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.ExamPlannerDay(
    day: LocalDate,
    inCurrentMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    count: Int,
    onSelect: (LocalDate) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val indicatorOffset by animateDpAsState(
        targetValue = when {
            count >= 4 && (selected || today) -> 27.dp
            selected || today -> 24.dp
            else -> 17.dp
        },
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "exam-day-indicator-offset"
    )
    Box(
        modifier = Modifier.weight(1f).height(56.dp).clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { onSelect(day) }
        ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                selected -> MaterialTheme.colorScheme.primary
                today -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.Transparent
            },
            modifier = Modifier.size(34.dp).align(Alignment.Center)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected || today) FontWeight.Bold else FontWeight.Normal,
                    color = when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        !inCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        today -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurface
                    })
            }
        }
        CalendarItemIndicator(
            count = count,
            color = when {
                selected -> MaterialTheme.colorScheme.onPrimary
                !inCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.primary
            },
            modifier = Modifier.align(Alignment.Center)
                .offset { IntOffset(0, indicatorOffset.roundToPx()) }
        )
    }
}

@Composable
private fun RemovedExamNoticeCard(
    notice: RemovedExamNotice,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(onClick = onOpen, shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(notice.appeal.subjectName, style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold)
                Text("L’appello del ${formatShortDate(notice.appeal.date)} non è più pubblicato.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
            }
            TextButton(onClick = onDismiss) { Text("Ho capito") }
        }
    }
}

@Composable
private fun ExamPlannerAppeal(appeal: ExamAppeal, hasNote: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Text(appeal.time.ifBlank { "--:--" }, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(appeal.subjectName, style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(listOf(appeal.courseName.ifBlank { appeal.courseCode }, appeal.location)
                    .filter { it.isNotBlank() }.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                if (hasNote) Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(28.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Description, contentDescription = "Nota personale",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Apri dettagli",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ExamAppealDetailsScreen(
    appeal: ExamAppeal,
    saved: Boolean,
    available: Boolean = true,
    savedNote: String,
    onSaveNote: (ExamAppeal, String) -> Unit,
    onToggleSaved: (ExamAppeal) -> Unit,
    onAddToCalendar: (ExamAppeal) -> Unit
) {
    var note by remember(appeal.courseCode, appeal.id, savedNote) { mutableStateOf(savedNote) }
    var noteFocused by remember(appeal.courseCode, appeal.id) { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val noteEditorRequester = remember { BringIntoViewRequester() }
    val imeBottom = WindowInsets.ime.getBottom(LocalDensity.current)
    LaunchedEffect(noteFocused, imeBottom) {
        if (noteFocused && imeBottom > 0) {
            noteEditorRequester.bringIntoView()
        }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (!available) item {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.errorContainer) {
                Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(10.dp))
                    Text("Questo appello non risulta più pubblicato da UNIMI.",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Text(appeal.subjectName, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(listOf(appeal.subjectCode, appeal.subjectPortalCode).filter { it.isNotBlank() }.joinToString(" · "),
                Modifier.padding(top = 4.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ExamDetailField("Data", formatExamDate(appeal.date))
                    ExamDetailField("Ora", appeal.time.ifBlank { "Da definire" })
                    ExamDetailField("Corso di laurea",
                        listOf(appeal.courseName, appeal.courseCode).filter { it.isNotBlank() }.joinToString(" · "))
                    if (appeal.location.isNotBlank()) ExamDetailField("Luogo", appeal.location)
                    if (appeal.teacher.isNotBlank()) ExamDetailField("Docente", appeal.teacher)
                    if (appeal.testType.isNotBlank()) ExamDetailField("Prova", appeal.testType)
                    if (appeal.appealType.isNotBlank()) ExamDetailField("Tipo di appello", appeal.appealType)
                    val range = listOf(appeal.surnameFrom, appeal.surnameTo)
                        .filter { it.isNotBlank() }.joinToString("–")
                    if (range.isNotBlank()) ExamDetailField("Fascia alfabetica", range)
                    val registration = listOfNotNull(
                        appeal.registrationOpen?.let { "dal ${formatShortDate(it)}" },
                        appeal.registrationClose?.let { "al ${formatShortDate(it)}" }
                    ).joinToString(" ")
                    if (registration.isNotBlank()) ExamDetailField("Iscrizioni", registration)
                    RegistrationBadge(appeal)
                }
            }
        }
        if (available) item {
            FilledTonalButton(
                onClick = { onAddToCalendar(appeal) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Aggiungi al calendario")
            }
            if (saved) OutlinedButton(
                    onClick = { onToggleSaved(appeal) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.Bookmark, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rimuovi dai miei appelli")
                }
            else FilledTonalButton(
                    onClick = { onToggleSaved(appeal) },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Icon(Icons.Outlined.BookmarkBorder, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Salva nei miei appelli")
                }
        }
        if (saved) item {
            Column(Modifier.fillMaxWidth().bringIntoViewRequester(noteEditorRequester)) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    modifier = Modifier.fillMaxWidth().onFocusChanged { noteFocused = it.isFocused },
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(18.dp),
                    label = { Text("Note personali") },
                    supportingText = { Text("${note.length}/500 · salvate solo su questo dispositivo") }
                )
                Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (savedNote.isNotBlank()) OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            note = ""
                            onSaveNote(appeal, "")
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Outlined.DeleteOutline, contentDescription = null,
                            modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Elimina")
                    }
                    FilledTonalButton(
                        onClick = {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            onSaveNote(appeal, note)
                        },
                        enabled = note.isNotBlank() && note.trim() != savedNote,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Salva nota")
                    }
                }
            }
        } else item {
            Text("Salva l’appello per aggiungere una nota personale.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun completeExamMonthDates(month: YearMonth): List<LocalDate> {
    val leadingDays = month.atDay(1).dayOfWeek.value - 1
    val visibleDays = leadingDays + month.lengthOfMonth()
    val cellCount = ((visibleDays + 6) / 7) * 7
    val firstVisibleDay = month.atDay(1).minusDays(leadingDays.toLong())
    return List(cellCount) { firstVisibleDay.plusDays(it.toLong()) }
}

@Composable
private fun ExamDetailField(label: String, value: String) {
    if (value.isBlank()) return
    Column {
        Text(label.uppercase(examLocale), style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(value, Modifier.padding(top = 2.dp), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun ExamAppealCard(
    appeal: ExamAppeal, saved: Boolean, hasNote: Boolean, onOpen: () -> Unit, onToggleSaved: () -> Unit,
    onAddToCalendar: (ExamAppeal) -> Unit
) {
    Card(onClick = onOpen, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(appeal.subjectName, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Text(listOf(appeal.subjectCode, appeal.subjectPortalCode).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        IconButton(onClick = onToggleSaved, modifier = Modifier.size(42.dp)) {
                            Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = if (saved) "Rimuovi dai miei appelli" else "Salva nei miei appelli")
                        }
                    }
                    Surface(shape = RoundedCornerShape(13.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        IconButton(onClick = { onAddToCalendar(appeal) }, modifier = Modifier.size(42.dp)) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Aggiungi al calendario")
                        }
                    }
                }
            }
            Spacer(Modifier.height(11.dp))
            ExamInfoRow(Icons.Outlined.EventAvailable,
                listOf(appeal.time.ifBlank { "Orario da definire" }, appeal.testType, appeal.appealType)
                    .filter { it.isNotBlank() }.joinToString(" · "))
            if (appeal.location.isNotBlank()) ExamInfoRow(Icons.Outlined.LocationOn, appeal.location)
            if (appeal.teacher.isNotBlank()) ExamInfoRow(Icons.Outlined.PersonOutline, appeal.teacher)
            val range = listOf(appeal.surnameFrom, appeal.surnameTo).filter { it.isNotBlank() }.joinToString("–")
            if (range.isNotBlank()) Text("Cognomi $range", Modifier.padding(top = 7.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (hasNote) {
                Row(Modifier.padding(top = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text("Nota personale", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            RegistrationBadge(appeal)
        }
    }
}

@Composable
private fun ExamInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(Modifier.padding(top = 5.dp), verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(17.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RegistrationBadge(appeal: ExamAppeal) {
    val today = LocalDate.now()
    val open = appeal.registrationOpen
    val close = appeal.registrationClose
    val label = when {
        open == null && close == null -> null
        open != null && today.isBefore(open) -> "Iscrizioni dal ${formatShortDate(open)}"
        close != null && today.isAfter(close) -> "Iscrizioni chiuse il ${formatShortDate(close)}"
        close != null -> "Iscrizioni aperte fino al ${formatShortDate(close)}"
        else -> "Iscrizioni aperte"
    } ?: return
    val active = (open == null || !today.isBefore(open)) && (close == null || !today.isAfter(close))
    Surface(
        modifier = Modifier.padding(top = 11.dp),
        color = if (active) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(9.dp)
    ) {
        Text(label, Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (active) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ExamEmptyPanel(title: String, message: String, action: String? = null, onAction: () -> Unit = {}) {
    AppEmptyState(
        title = title,
        message = message,
        action = action,
        onAction = onAction.takeIf { action != null },
        icon = Icons.Outlined.EventAvailable
    )
}

private val examLocale = Locale.ITALIAN
private val examDate = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(examLocale)
private val examShortDate = DateTimeFormatter.ofPattern("dd/MM", examLocale)

private fun formatExamDate(date: LocalDate): String =
    date.format(examDate).replaceFirstChar { it.titlecase(examLocale) }

private fun formatShortDate(date: LocalDate): String = date.format(examShortDate)

private fun normalizeExamText(value: String): String = Normalizer.normalize(
    value.lowercase(Locale.ROOT).trim(), Normalizer.Form.NFD
).replace(Regex("\\p{M}+"), "")
