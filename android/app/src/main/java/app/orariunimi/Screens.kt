package app.orariunimi

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val italian = Locale.ITALIAN
private val dateLong = DateTimeFormatter.ofPattern("EEEE d MMMM", italian)
private val dateShort = DateTimeFormatter.ofPattern("d MMM", italian)
private val nextLessonDate = DateTimeFormatter.ofPattern("EEE d MMM", italian)
private val updateTime = DateTimeFormatter.ofPattern("HH:mm", italian)
private val updateDateTime = DateTimeFormatter.ofPattern("d MMM, HH:mm", italian)
private val lessonColors = listOf(
    Color(0xFF526FCC), Color(0xFF008A83), Color(0xFFB15D63), Color(0xFF927224),
    Color(0xFF8C68C2), Color(0xFF3C82B4), Color(0xFFB56D3C), Color(0xFF5F8A50)
)

@Composable
fun SearchScreen(
    years: List<AcademicYear>, year: AcademicYear?, loadingYears: Boolean,
    kind: SearchKind, query: String, results: List<SearchItem>?, loadingEntries: Boolean,
    favorites: List<FavoriteCourse>,
    onYear: (AcademicYear) -> Unit, onKind: (SearchKind) -> Unit,
    onQuery: (String) -> Unit, onSelect: (SearchItem) -> Unit,
    onToggleCourseFavorite: (SearchItem) -> Unit,
    onRetryYears: () -> Unit, onRetryEntries: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var yearMenu by remember { mutableStateOf(false) }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column(Modifier.fillMaxWidth()) {
            AppScreenHeader(
                title = "Esplora",
                subtitle = "Trova corsi, docenti e insegnamenti pubblicati da UNIMI."
            )
            Spacer(Modifier.height(18.dp))
            Text("ANNO ACCADEMICO", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Box(Modifier.padding(top = 6.dp)) {
                OutlinedButton(
                    onClick = { yearMenu = !yearMenu },
                    enabled = years.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(year?.name ?: "Anno accademico", Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Start)
                    Icon(if (yearMenu) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = if (yearMenu) "Chiudi elenco anni" else "Apri elenco anni",
                        modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = yearMenu, onDismissRequest = { yearMenu = false },
                    modifier = Modifier.fillMaxWidth(0.88f)) {
                    years.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.name) },
                            onClick = { yearMenu = false; onYear(option) },
                            trailingIcon = if (option == year) {{
                                Icon(Icons.Outlined.Check, contentDescription = "Anno selezionato")
                            }} else null
                        )
                    }
                }
            }
            AppSegmentedControl(
                options = SearchKind.entries.map { it.label },
                selectedIndex = SearchKind.entries.indexOf(kind),
                onSelected = { onKind(SearchKind.entries[it]) },
                modifier = Modifier.padding(top = 14.dp)
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value = query, onValueChange = onQuery, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(18.dp),
                label = { Text("Cerca ${kind.label.lowercase(italian)}") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                trailingIcon = if (query.isNotEmpty()) {{
                    IconButton(onClick = { onQuery("") }) { Icon(Icons.Outlined.Close, contentDescription = "Cancella ricerca") }
                }} else null
            )
            if (query.isNotBlank() && !loadingEntries && results != null) {
                Spacer(Modifier.height(10.dp))
                Text(if (results.size == 40) "Primi 40 risultati · affina la ricerca"
                    else "${results.size} ${if (results.size == 1) "risultato" else "risultati"}",
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            }
        }
        when {
            loadingYears || loadingEntries -> item { Box(
                Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            } }
            year == null -> item { AppEmptyState("Nessun anno disponibile",
                "Controlla la connessione e riprova.", action = "Riprova", onAction = onRetryYears) }
            query.isBlank() -> item { AppEmptyState("Inizia a cercare",
                "Scrivi il nome o il codice di un ${kind.subtitle.lowercase(italian)}.",
                icon = Icons.Outlined.Search) }
            results == null -> item { Box(
                Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.CircularProgressIndicator()
            } }
            results.isEmpty() -> item { AppEmptyState("Nessun risultato",
                "Prova con un nome diverso o con il codice.", action = "Ricarica elenco",
                onAction = onRetryEntries, icon = Icons.Outlined.Search) }
            else -> {
                items(results, key = { item -> "${item.kind}:${item.code}" }) { item ->
                    SearchResultCard(item,
                        favorite = favorites.any { it.year == year.code && it.code == item.code },
                        onClick = { focusManager.clearFocus(); onSelect(item) },
                        onToggleFavorite = { onToggleCourseFavorite(item) })
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(item: SearchItem, favorite: Boolean, onClick: () -> Unit,
                             onToggleFavorite: () -> Unit) {
    if (item.kind == SearchKind.COURSE) {
        DegreeCourseCard(
            course = item,
            favorite = favorite,
            onClick = onClick,
            onToggleFavorite = onToggleFavorite,
            favoriteDescription = if (favorite) "Rimuovi ${item.name} dai salvati"
                else "Salva ${item.name}",
            showChevron = false
        )
        return
    }
    val palette = item.degreeType?.let { degreePalette(it) }
    Card(
        onClick = onClick, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette?.container
            ?: MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(Modifier.fillMaxWidth().padding(start = 15.dp, top = 10.dp, bottom = 10.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(when (item.kind) {
                SearchKind.COURSE -> Icons.Outlined.School
                SearchKind.TEACHER -> Icons.Outlined.PersonOutline
                SearchKind.SUBJECT -> Icons.AutoMirrored.Outlined.MenuBook
            }, contentDescription = null, tint = palette?.accent ?: MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.name, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, color = palette?.content ?: MaterialTheme.colorScheme.onSurface,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text(item.code, style = MaterialTheme.typography.labelMedium,
                        color = (palette?.content ?: MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.75f))
                    if (item.degreeType != null) Text("· ${item.degreeType.label}",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette?.content ?: MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LessonsScreen(
    saved: List<SavedSubject>,
    personalLessons: List<Lesson>?, loadingPersonal: Boolean,
    onOpen: (SavedSubject) -> Unit, onCombined: () -> Unit,
    onOpenNext: (LocalDate) -> Unit, onAgenda: () -> Unit, onAdd: () -> Unit,
    onRemove: (SavedSubject) -> Unit, onClear: () -> Unit
) {
    var confirmClear by remember { mutableStateOf(false) }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Svuotare i miei orari?") },
        text = { Text("Gli insegnamenti salvati verranno rimossi da questo dispositivo.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClear() }) { Text("Elimina tutto") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Annulla") } }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            AppScreenHeader(
                title = "Lezioni",
                subtitle = "I tuoi insegnamenti, raccolti in un unico calendario.",
                actionIcon = Icons.Outlined.ViewAgenda.takeIf { saved.isNotEmpty() },
                actionDescription = "Apri vista agenda",
                onAction = onAgenda.takeIf { saved.isNotEmpty() }
            )
        }
        if (saved.isNotEmpty()) item {
            Spacer(Modifier.height(6.dp))
            NextLessonCard(personalLessons, loadingPersonal, onOpenNext)
        }
        item {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onCombined, enabled = saved.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.CalendarMonth, contentDescription = null, Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text("Apri calendario delle lezioni")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Add, contentDescription = null, Modifier.size(19.dp))
                Spacer(Modifier.width(9.dp))
                Text("Aggiungi insegnamento")
            }
        }
        if (saved.isEmpty()) item {
            Spacer(Modifier.height(12.dp))
            EmptyCard("Ancora nessun insegnamento",
                "Cerca un insegnamento e tocca il segnalibro nel calendario.")
        } else {
            item {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("SALVATI · ${saved.size}", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = { confirmClear = true }) { Text("Svuota elenco") }
                }
            }
            items(saved, key = { item -> "${item.year}:${item.code}" }) { item ->
                Card(onClick = { onOpen(item) }, shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Text("${item.code} · ${item.year}", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { onRemove(item) }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Rimuovi ${item.name}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextLessonCard(lessons: List<Lesson>?, loading: Boolean, onOpen: (LocalDate) -> Unit) {
    val next = lessons?.let(::nextLesson)
    Card(onClick = { next?.let { onOpen(it.date) } }, shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
                modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    if (loading && lessons == null) CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.CalendarMonth, contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text("PROSSIMA LEZIONE", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                when {
                    lessons == null -> Text("Aggiornamento del calendario…",
                        style = MaterialTheme.typography.bodyMedium)
                    next == null -> Text("Nessuna lezione in programma",
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    else -> {
                        Text(next.subject.ifBlank { "Insegnamento" }, style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${next.date.format(nextLessonDate).replaceFirstChar { it.titlecase(italian) }} · " +
                            "${next.start}–${next.end}" + if (next.room.isNotBlank()) " · ${next.room}" else "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2,
                            overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleAgendaScreen(
    lessons: List<Lesson>?, loading: Boolean, weekend: Boolean, onOpenDay: (LocalDate) -> Unit
) {
    var range by remember { mutableStateOf(AgendaRange.WEEK) }
    var anchorDay by remember { mutableStateOf(openingDay(LocalDate.now(), weekend)) }
    val viewport = AgendaViewport(range, anchorDay)

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically) {
            AnimatedContent(
                targetState = viewport,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "agenda-date"
            ) { page ->
                val pageWeek = startOfWeek(page.anchorDay)
                Column {
                    Text(if (page.range == AgendaRange.DAY) "GIORNO" else "SETTIMANA",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(if (page.range == AgendaRange.DAY)
                        page.anchorDay.format(dateLong).replaceFirstChar { it.titlecase(italian) }
                    else "${pageWeek.format(dateShort)} – ${pageWeek.plusDays(6).format(dateShort)}",
                        style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = { anchorDay = openingDay(LocalDate.now(), weekend) }) { Text("Oggi") }
            IconButton(onClick = {
                anchorDay = if (range == AgendaRange.DAY)
                    adjacentCalendarDay(anchorDay, -1, weekend) else anchorDay.minusWeeks(1)
            }) {
                Icon(Icons.Outlined.ChevronLeft,
                    contentDescription = if (range == AgendaRange.DAY) "Giorno precedente" else "Settimana precedente")
            }
            IconButton(onClick = {
                anchorDay = if (range == AgendaRange.DAY)
                    adjacentCalendarDay(anchorDay, 1, weekend) else anchorDay.plusWeeks(1)
            }) {
                Icon(Icons.Outlined.ChevronRight,
                    contentDescription = if (range == AgendaRange.DAY) "Giorno successivo" else "Settimana successiva")
            }
        }
        AgendaRangeSelector(range = range, onRange = { range = it })
        AnimatedContent(
            targetState = viewport,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            transitionSpec = {
                if (initialState.range != targetState.range) {
                    (fadeIn(tween(220)) + scaleIn(tween(280, easing = FastOutSlowInEasing), initialScale = 0.985f))
                        .togetherWith(fadeOut(tween(150)) + scaleOut(tween(180), targetScale = 0.995f))
                } else {
                    val forward = targetState.anchorDay.isAfter(initialState.anchorDay)
                    (slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) {
                        if (forward) it / 5 else -it / 5
                    } + fadeIn(tween(180))).togetherWith(
                        slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) {
                            if (forward) -it / 5 else it / 5
                        } + fadeOut(tween(140))
                    )
                }
            },
            label = "agenda-page"
        ) { page ->
            AgendaTimeline(
                lessons = lessons,
                loading = loading,
                weekend = weekend,
                range = page.range,
                anchorDay = page.anchorDay,
                onOpenDay = onOpenDay
            )
        }
    }
}

private data class AgendaViewport(val range: AgendaRange, val anchorDay: LocalDate)

@Composable
private fun AgendaTimeline(
    lessons: List<Lesson>?, loading: Boolean, weekend: Boolean, range: AgendaRange,
    anchorDay: LocalDate, onOpenDay: (LocalDate) -> Unit
) {
    val week = startOfWeek(anchorDay)
    val days = if (range == AgendaRange.DAY) listOf(anchorDay) else
        (0 until if (weekend) 7 else 5).map { week.plusDays(it.toLong()) }
    val weekEnd = week.plusDays(if (weekend) 7 else 5)
    val visibleLessons = remember(lessons, range, anchorDay, week, weekend) {
        if (range == AgendaRange.DAY) lessonsForDay(lessons.orEmpty(), anchorDay)
        else lessons.orEmpty().filter { !it.date.isBefore(week) && it.date.isBefore(weekEnd) }
    }
    val placements = remember(visibleLessons) { timelineLessons(visibleLessons) }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    val timeWidth = 58.dp
    val slotHeight = 52.dp
    val firstMinute = 8 * 60 + 30
    val lastMinute = 19 * 60 + 30
    val slots = (lastMinute - firstMinute) / 30
    val gridHeight = slotHeight * slots

    when {
        lessons == null && loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> BoxWithConstraints(
            Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, bottom = 8.dp)
        ) {
            val gridColor = MaterialTheme.colorScheme.outlineVariant
            val availableDayWidth = maxWidth - timeWidth
            val dayWidth = if (range == AgendaRange.DAY && availableDayWidth > 240.dp)
                availableDayWidth else if (range == AgendaRange.DAY) 240.dp else 138.dp
            val dayWidthPixels = with(LocalDensity.current) { dayWidth.roundToPx() }

            LaunchedEffect(horizontal.maxValue, range, anchorDay, weekend, dayWidthPixels) {
                if (range == AgendaRange.DAY) horizontal.scrollTo(0)
                else {
                    val todayIndex = days.indexOf(LocalDate.now())
                    if (horizontal.maxValue > 0 && todayIndex >= 0) {
                        horizontal.scrollTo((todayIndex * dayWidthPixels).coerceAtMost(horizontal.maxValue))
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    Modifier.fillMaxSize().clipToBounds().pointerInput(horizontal, vertical) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            horizontal.dispatchRawDelta(-dragAmount.x)
                            vertical.dispatchRawDelta(-dragAmount.y)
                        }
                    }
                ) {
                    Row(Modifier.fillMaxWidth().height(66.dp)) {
                        Box(
                            Modifier.width(timeWidth).fillMaxHeight(),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            Text("ORA", Modifier.padding(bottom = 9.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Canvas(Modifier.fillMaxSize()) {
                                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 1f)
                            }
                        }
                        Box(Modifier.weight(1f).clipToBounds()) {
                            Row(Modifier.horizontalScroll(horizontal, enabled = false)) {
                                days.forEach { day ->
                                    val today = day == LocalDate.now()
                                    Surface(color = if (today) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerLow,
                                        modifier = Modifier.width(dayWidth).fillMaxHeight()) {
                                        Box {
                                            Column(Modifier.fillMaxSize(),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.Center) {
                                                Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, italian).uppercase(italian),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (today) MaterialTheme.colorScheme.primary
                                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold)
                                                Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.Bold)
                                            }
                                            Canvas(Modifier.fillMaxSize()) {
                                                drawLine(gridColor,
                                                    start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height),
                                                    strokeWidth = 1f)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(Modifier.weight(1f).fillMaxWidth().clipToBounds()
                        .verticalScroll(vertical, enabled = false)) {
                        Box(Modifier.width(timeWidth).height(gridHeight)) {
                            val gridColor = MaterialTheme.colorScheme.outlineVariant
                            Canvas(Modifier.fillMaxSize()) {
                                for (index in 1 until slots) {
                                    val y = slotHeight.toPx() * index
                                    drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y),
                                        end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                                }
                                drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(size.width, 0f),
                                    end = androidx.compose.ui.geometry.Offset(size.width, size.height), strokeWidth = 1f)
                            }
                            repeat(slots + 1) { index ->
                                val minute = firstMinute + index * 30
                                val labelOffset = when (index) {
                                    0 -> 2.dp
                                    slots -> gridHeight - 18.dp
                                    else -> slotHeight * index - 8.dp
                                }
                                Text(LocalTime.of(minute / 60, minute % 60).format(updateTime),
                                    modifier = Modifier.width(timeWidth).offset(y = labelOffset),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1)
                            }
                        }
                        Box(Modifier.weight(1f).clipToBounds()) {
                            Box(Modifier.horizontalScroll(horizontal, enabled = false)) {
                                Box(Modifier.width(dayWidth * days.size).height(gridHeight)) {
                                    val gridColor = MaterialTheme.colorScheme.outlineVariant
                                    Canvas(Modifier.fillMaxSize()) {
                                        for (index in 1 until slots) {
                                            val y = slotHeight.toPx() * index
                                            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(0f, y),
                                                end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
                                        }
                                        for (index in 1 until days.size) {
                                            val x = dayWidth.toPx() * index
                                            drawLine(gridColor, start = androidx.compose.ui.geometry.Offset(x, 0f),
                                                end = androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1f)
                                        }
                                    }
                                    placements.forEach { placement ->
                                        val lesson = placement.lesson
                                        val dayIndex = days.indexOf(lesson.date)
                                        val start = timelineMinutes(lesson.start).coerceAtLeast(firstMinute)
                                        val end = timelineMinutes(lesson.end).coerceAtMost(lastMinute)
                                        if (dayIndex >= 0 && end > start) {
                                            val laneWidth = dayWidth / placement.laneCount
                                            val top = slotHeight * ((start - firstMinute) / 30f)
                                            val height = maxOf(44.dp, slotHeight * ((end - start) / 30f) - 4.dp)
                                            TimelineLessonCard(
                                                lesson = lesson,
                                                compact = placement.laneCount > 1,
                                                modifier = Modifier
                                                    .offset(
                                                        x = dayWidth * dayIndex + laneWidth * placement.lane + 2.dp,
                                                        y = top + 2.dp
                                                    )
                                                    .width(laneWidth - 4.dp)
                                                    .height(height),
                                                onClick = { onOpenDay(lesson.date) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private enum class AgendaRange { DAY, WEEK }

@Composable
private fun AgendaRangeSelector(range: AgendaRange, onRange: (AgendaRange) -> Unit) {
    AppSegmentedControl(
        options = listOf("Giorno", "Settimana"),
        selectedIndex = if (range == AgendaRange.DAY) 0 else 1,
        onSelected = { onRange(if (it == 0) AgendaRange.DAY else AgendaRange.WEEK) },
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp)
    )
}

@Composable
private fun TimelineLessonCard(lesson: Lesson, compact: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val accent = lessonColors[(lesson.subjectCode.hashCode() and Int.MAX_VALUE) % lessonColors.size]
    val background = if (lesson.cancelled) MaterialTheme.colorScheme.errorContainer else accent.copy(alpha = 0.24f)
    val content = if (lesson.cancelled) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurface
    Surface(onClick = onClick, modifier = modifier, color = background, shape = RoundedCornerShape(10.dp)) {
        Column(Modifier.padding(if (compact) 5.dp else 7.dp)) {
            Text(lesson.subject.ifBlank { "Insegnamento" },
                color = content, fontSize = if (compact) 9.sp else 12.sp,
                fontWeight = FontWeight.Bold, maxLines = if (compact) 5 else 3,
                overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(if (compact) 2.dp else 3.dp))
            Text("${lesson.start}–${lesson.end}", color = content.copy(alpha = 0.82f),
                fontSize = if (compact) 8.sp else 10.sp, maxLines = 1)
            if (lesson.room.isNotBlank()) Text(lesson.room, color = content.copy(alpha = 0.72f),
                fontSize = if (compact) 8.sp else 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun timelineMinutes(value: String): Int = runCatching {
    val time = LocalTime.parse(value, DateTimeFormatter.ofPattern("H:mm"))
    time.hour * 60 + time.minute
}.getOrDefault(0)

@Composable
fun SettingsScreen(
    weekend: Boolean,
    onWeekend: () -> Unit,
    currentVersion: String,
    updateState: AppUpdateState,
    onCheckUpdate: () -> Unit,
    onDownloadUpdate: (AppRelease) -> Unit,
    onInstallUpdate: (File) -> Unit,
    notificationPreferences: NotificationPreferences,
    onNotificationsEnabled: () -> Unit,
    onOpenNotificationSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Personalizza l'app", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("Le preferenze restano salvate su questo dispositivo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
        }
        item { SettingCard(Icons.Outlined.CalendarMonth, "Mostra il weekend",
            "Aggiunge sabato e domenica alla settimana.", weekend, onWeekend) }
        item {
            Spacer(Modifier.height(6.dp))
            Text("NOTIFICHE", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item { SettingCard(Icons.Outlined.Notifications, "Notifiche",
            "Attiva gli avvisi locali e scegli quali ricevere.",
            notificationPreferences.enabled, onNotificationsEnabled) }
        if (notificationPreferences.enabled) {
            item { NavigationSettingCard(Icons.Outlined.NotificationsActive, "Impostazioni notifiche",
                "Scegli lezioni, appelli, promemoria e aggiornamenti.", onOpenNotificationSettings) }
        }
        item {
            Spacer(Modifier.height(6.dp))
            Text("AGGIORNAMENTI", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            UpdateSettingCard(
                currentVersion = currentVersion,
                state = updateState,
                onCheck = onCheckUpdate,
                onDownload = onDownloadUpdate,
                onInstall = onInstallUpdate
            )
        }
        item {
            Spacer(Modifier.height(12.dp))
            Text("DATI E PRIVACY", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(9.dp))
            Text("Gli orari arrivano dal portale pubblico UNIMI. Insegnamenti, corsi salvati e preferenze rimangono solo sul telefono.",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun NotificationChangeDetailsScreen(
    entry: AppNotificationEntry,
    onOpenTarget: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(entry.targetTitle ?: entry.title, style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold)
            Text(entry.message, Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (entry.changes.isNotEmpty()) {
            item {
                Text("COSA È CAMBIATO", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            items(entry.changes, key = { "${it.label}:${it.before}:${it.after}" }) { change ->
                Surface(shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow) {
                    Column(Modifier.fillMaxWidth().padding(15.dp)) {
                        Text(change.label.uppercase(italian), style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold)
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(change.before, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("→", Modifier.padding(horizontal = 10.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold)
                            Text(change.after, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        if (entry.targetDate != null || entry.targetKey != null) item {
            FilledTonalButton(onClick = onOpenTarget, modifier = Modifier.fillMaxWidth()) {
                Icon(if (entry.type == AppNotificationType.EXAM_CHANGE ||
                    entry.type == AppNotificationType.EXAM_REGISTRATION_REMINDER)
                    Icons.Outlined.EventAvailable else Icons.Outlined.CalendarMonth,
                    contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (entry.type == AppNotificationType.EXAM_CHANGE ||
                    entry.type == AppNotificationType.EXAM_REGISTRATION_REMINDER)
                    "Apri appello" else "Apri nel calendario")
            }
        }
    }
}

@Composable
fun NotificationSettingsScreen(
    notificationPreferences: NotificationPreferences,
    lessonDiagnostics: SyncDiagnosticState,
    examDiagnostics: SyncDiagnosticState,
    checkingLessons: Boolean,
    checkingExams: Boolean,
    onLessonChanges: () -> Unit,
    onExamChanges: () -> Unit,
    onExamRegistrationOpenReminders: () -> Unit,
    onExamRegistrationCloseReminders: () -> Unit,
    onExamRegistrationCloseLeadDays: (Int) -> Unit,
    onLessonReminders: () -> Unit,
    onAppUpdates: () -> Unit,
    onReminderMinutes: (Int) -> Unit,
    onCheckLessons: () -> Unit,
    onCheckExams: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Scegli gli avvisi", style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("Ogni categoria è facoltativa e resta salvata su questo dispositivo.",
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(10.dp))
        }
        item { SettingCard(Icons.Outlined.NotificationsActive, "Variazioni delle lezioni",
            "Avvisa per lezioni rimosse, annullate o cambiate in data, ora, aula, docente o note.",
            notificationPreferences.lessonChanges, onLessonChanges) }
        item { SettingCard(Icons.Outlined.EventAvailable, "Variazioni degli appelli",
            "Avvisa quando un appello salvato cambia data, ora, luogo, docente, iscrizioni o altri dettagli.",
            notificationPreferences.examChanges, onExamChanges) }
        item {
            ExamRegistrationReminderSettingCard(
                openEnabled = notificationPreferences.examRegistrationOpenReminders,
                closeEnabled = notificationPreferences.examRegistrationCloseReminders,
                closeLeadDays = notificationPreferences.examRegistrationCloseLeadDays,
                onOpenChange = onExamRegistrationOpenReminders,
                onCloseChange = onExamRegistrationCloseReminders,
                onCloseLeadDays = onExamRegistrationCloseLeadDays
            )
        }
        item {
            LessonReminderSettingCard(
                enabled = notificationPreferences.lessonReminders,
                reminderMinutes = notificationPreferences.reminderMinutes,
                onChange = onLessonReminders,
                onReminderMinutes = onReminderMinutes
            )
        }
        item { SettingCard(Icons.Outlined.SystemUpdate, "Aggiornamenti dell'app",
            "Avviso silenzioso quando viene pubblicata una nuova versione.",
            notificationPreferences.appUpdates, onAppUpdates) }
        item {
            SyncDiagnosticsPanel(
                lessonState = lessonDiagnostics,
                examState = examDiagnostics,
                checkingLessons = checkingLessons,
                checkingExams = checkingExams,
                onCheckLessons = onCheckLessons,
                onCheckExams = onCheckExams
            )
        }
    }
}

@Composable
private fun ExamRegistrationReminderSettingCard(
    openEnabled: Boolean,
    closeEnabled: Boolean,
    closeLeadDays: Int,
    onOpenChange: () -> Unit,
    onCloseChange: () -> Unit,
    onCloseLeadDays: (Int) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.EventAvailable, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Iscrizioni agli appelli", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(3.dp))
                    Text("Promemoria per gli appelli salvati.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            HorizontalDivider(Modifier.padding(horizontal = 18.dp))
            ReminderToggleRow("Apertura iscrizioni", "Avvisa il giorno in cui diventano disponibili.",
                openEnabled, onOpenChange)
            HorizontalDivider(Modifier.padding(horizontal = 18.dp))
            ReminderToggleRow("Scadenza iscrizioni", "Avvisa prima della chiusura.",
                closeEnabled, onCloseChange)
            if (closeEnabled) {
                Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1, 3, 7).forEach { days ->
                        FilterChip(
                            selected = closeLeadDays == days,
                            onClick = { onCloseLeadDays(days) },
                            label = { Text(if (days == 1) "1 giorno" else "$days giorni") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReminderToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onChange: () -> Unit
) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onChange)
        .padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        Switch(checked = checked, onCheckedChange = { onChange() })
    }
}

@Composable
private fun SyncDiagnosticsPanel(
    lessonState: SyncDiagnosticState,
    examState: SyncDiagnosticState,
    checkingLessons: Boolean,
    checkingExams: Boolean,
    onCheckLessons: () -> Unit,
    onCheckExams: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val arrowRotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(180),
        label = "sync-diagnostics-arrow"
    )
    Column(Modifier.fillMaxWidth().padding(top = 3.dp)) {
        Row(Modifier.fillMaxWidth().clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = { expanded = !expanded }
        )
            .padding(horizontal = 5.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Stato sincronizzazione", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Diagnostica locale", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
            }
            Icon(Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (expanded) "Nascondi diagnostica" else "Mostra diagnostica",
                modifier = Modifier.graphicsLayer { rotationZ = arrowRotation },
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (expanded) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Column(Modifier.fillMaxWidth().padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DiagnosticRow("Lezioni", "gruppo di orari", "gruppi di orari", lessonState,
                        checkingLessons, onCheckLessons)
                    HorizontalDivider()
                    DiagnosticRow("Appelli", "corso di laurea", "corsi di laurea", examState,
                        checkingExams, onCheckExams)
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(
    title: String,
    sourceSingular: String,
    sourcePlural: String,
    state: SyncDiagnosticState,
    checking: Boolean,
    onCheck: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(diagnosticSummary(state), style = MaterialTheme.typography.bodySmall,
                color = if (state.lastError != null) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.lastSuccessMillis > 0) {
                val sourceLabel = if (state.sourceCount == 1) sourceSingular else sourcePlural
                Text("Controllati: ${state.sourceCount} $sourceLabel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                Text("Da UNIMI: ${state.networkRequests} · dalla cache: ${state.cacheHits}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
        }
        IconButton(onClick = onCheck, enabled = !checking) {
            if (checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Outlined.Refresh, contentDescription = "Controlla ora $title")
        }
    }
}

private fun diagnosticSummary(state: SyncDiagnosticState): String = when {
    state.lastError != null -> state.lastError
    state.lastSuccessMillis == 0L -> "Nessun controllo completato"
    else -> "Ultimo controllo ${Instant.ofEpochMilli(state.lastSuccessMillis)
        .atZone(ZoneId.systemDefault()).format(updateDateTime)} · ${state.changesFound} variazioni"
}

@Composable
private fun LessonReminderSettingCard(
    enabled: Boolean,
    reminderMinutes: Int,
    onChange: () -> Unit,
    onReminderMinutes: (Int) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().clickable(onClick = onChange).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Schedule, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Prossima lezione", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(3.dp))
                    Text("Promemoria silenzioso prima dell'inizio.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.width(8.dp))
                Switch(checked = enabled, onCheckedChange = { onChange() })
            }
            if (enabled) {
                HorizontalDivider(Modifier.padding(horizontal = 18.dp))
                Column(Modifier.fillMaxWidth().padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 16.dp)) {
                    Text("Avvisami prima", style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(9.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(15, 30, 60).forEach { minutes ->
                            FilterChip(
                                selected = reminderMinutes == minutes,
                                onClick = { onReminderMinutes(minutes) },
                                label = { Text("$minutes min") }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NavigationSettingCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                contentDescription = "Apri", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UpdateSettingCard(
    currentVersion: String,
    state: AppUpdateState,
    onCheck: () -> Unit,
    onDownload: (AppRelease) -> Unit,
    onInstall: (File) -> Unit
) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(onClick = onCheck,
                    enabled = state != AppUpdateState.Checking && state !is AppUpdateState.Downloading,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(42.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Controlla aggiornamenti",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text("Aggiornamenti", style = MaterialTheme.typography.titleMedium)
                    Text("Versione installata $currentVersion", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(14.dp))
            when (state) {
                AppUpdateState.Idle -> {
                    Text("Tocca l'icona per controllare le nuove versioni pubblicate su GitHub.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                AppUpdateState.Checking -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(10.dp))
                        Text("Controllo aggiornamenti…", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                AppUpdateState.UpToDate -> {
                    Text("Hai già la versione più recente.", style = MaterialTheme.typography.bodyMedium)
                }
                is AppUpdateState.Available -> {
                    Text("È disponibile la versione ${state.release.displayVersion(currentVersion)}.",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("L'APK sarà scaricato dalla release ufficiale e verificato prima dell'installazione.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    FilledTonalButton(onClick = { onDownload(state.release) },
                        modifier = Modifier.fillMaxWidth()) {
                        Text("Scarica aggiornamento")
                    }
                }
                is AppUpdateState.Downloading -> {
                    Text("Download della versione ${state.release.displayVersion(currentVersion)}…",
                        style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(10.dp))
                    if (state.progress == null) LinearProgressIndicator(Modifier.fillMaxWidth())
                    else LinearProgressIndicator(progress = { state.progress }, modifier = Modifier.fillMaxWidth())
                }
                is AppUpdateState.Ready -> {
                    Text("Versione ${state.release.displayVersion(currentVersion)} pronta per l'installazione.",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Android ti chiederà di confermare l'aggiornamento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { onInstall(state.file) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Installa aggiornamento")
                    }
                }
                is AppUpdateState.Error -> {
                    Text(state.message, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(5.dp))
                    Text("Tocca l'icona per riprovare.", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun AppRelease.displayVersion(currentVersion: String): String =
    if (version == currentVersion && versionCode != null) "$version (build $versionCode)" else version

@Composable
private fun SettingCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String,
                        value: Boolean, onChange: () -> Unit, enabled: Boolean = true) {
    ElevatedCard(shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.55f)
            .clickable(enabled = enabled, onClick = onChange).padding(18.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(3.dp))
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = value, enabled = enabled, onCheckedChange = { onChange() })
        }
    }
}

@Composable
fun CalendarScreen(
    calendar: CalendarData, week: LocalDate, selectedDay: LocalDate, weekend: Boolean,
    savedSubjects: List<SavedSubject>, onToggleSubject: (Lesson) -> Unit,
    refreshing: Boolean, onRefresh: () -> Unit,
    onMoveWeek: (Long) -> Unit, onSelectDay: (LocalDate) -> Unit,
    onOpenMonth: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 9.dp)) {
            WeekHeader(week, onMoveWeek, onOpenMonth = onOpenMonth)
            Spacer(Modifier.height(14.dp))
            AnimatedContent(
                targetState = week,
                transitionSpec = {
                    val forward = targetState.isAfter(initialState)
                    (slideInHorizontally(tween(260, easing = FastOutSlowInEasing)) {
                        if (forward) it / 5 else -it / 5
                    } + fadeIn(tween(170))).togetherWith(
                        slideOutHorizontally(tween(220, easing = FastOutSlowInEasing)) {
                            if (forward) -it / 5 else it / 5
                        } + fadeOut(tween(130))
                    )
                },
                label = "calendar-week"
            ) { displayedWeek ->
                CalendarWeekStrip(calendar, displayedWeek, selectedDay, weekend, onSelectDay)
            }
            Spacer(Modifier.height(6.dp))
            CalendarFreshness(calendar, weekend, refreshing, onRefresh)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainer)
        AnimatedContent(
            targetState = selectedDay,
            modifier = Modifier.weight(1f).fillMaxWidth().pointerInput(selectedDay, weekend) {
                var drag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { drag = 0f },
                    onHorizontalDrag = { change, amount -> drag += amount; change.consume() },
                    onDragCancel = { drag = 0f },
                    onDragEnd = {
                        if (drag > 70f) onSelectDay(adjacentCalendarDay(selectedDay, -1, weekend))
                        else if (drag < -70f) onSelectDay(adjacentCalendarDay(selectedDay, 1, weekend))
                        drag = 0f
                    }
                )
            },
            transitionSpec = {
                val forward = targetState.isAfter(initialState)
                (slideInHorizontally(tween(250, easing = FastOutSlowInEasing)) {
                    if (forward) it / 6 else -it / 6
                } + fadeIn(tween(170))).togetherWith(
                    slideOutHorizontally(tween(210, easing = FastOutSlowInEasing)) {
                        if (forward) -it / 6 else it / 6
                    } + fadeOut(tween(130))
                )
            },
            label = "calendar-day"
        ) { displayedDay ->
            CalendarDayLessons(calendar, displayedDay, savedSubjects, onToggleSubject)
        }
    }
}

@Composable
private fun CalendarWeekStrip(
    calendar: CalendarData,
    week: LocalDate,
    selectedDay: LocalDate,
    weekend: Boolean,
    onSelectDay: (LocalDate) -> Unit
) {
    val days = (0 until if (weekend) 7 else 5).map { week.plusDays(it.toLong()) }
    if (weekend) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val weekdayWidth = (maxWidth - 32.dp) / 5
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days) { day ->
                    DayTile(day, selected = day == selectedDay,
                        count = calendar.lessons.count { it.date == day },
                        modifier = Modifier.width(weekdayWidth), onClick = { onSelectDay(day) })
                }
            }
        }
    } else {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { day ->
                DayTile(day, selected = day == selectedDay,
                    count = calendar.lessons.count { it.date == day },
                    modifier = Modifier.weight(1f), onClick = { onSelectDay(day) })
            }
        }
    }
}

@Composable
private fun CalendarDayLessons(
    calendar: CalendarData,
    day: LocalDate,
    savedSubjects: List<SavedSubject>,
    onToggleSubject: (Lesson) -> Unit
) {
    val visibleLessons = calendar.lessons.filter { it.date == day }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(day.format(dateLong).replaceFirstChar { it.titlecase(italian) },
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text(if (visibleLessons.isEmpty()) "Nessuna lezione" else
                "${visibleLessons.size} ${if (visibleLessons.size == 1) "lezione" else "lezioni"}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (visibleLessons.isEmpty()) item {
            EmptyCard("Nessuna lezione in questo giorno", "Scegli un’altra data o cambia settimana.")
        }
        items(visibleLessons, key = { "${it.id}:${it.subjectCode}:${it.start}" }) { lesson ->
            val canSave = calendar.source?.kind == SearchKind.COURSE ||
                calendar.source?.kind == SearchKind.TEACHER
            LessonCard(lesson,
                saved = savedSubjects.any { it.year == calendar.year && it.code == lesson.subjectCode },
                onToggleSave = if (canSave) {{ onToggleSubject(lesson) }} else null)
        }
    }
}

@Composable
private fun CalendarFreshness(
    calendar: CalendarData, weekend: Boolean, refreshing: Boolean, onRefresh: () -> Unit
) {
    val updated = remember(calendar.updatedAtMillis) {
        Instant.ofEpochMilli(calendar.updatedAtMillis).atZone(ZoneId.systemDefault())
    }
    val timestamp = if (updated.toLocalDate() == LocalDate.now()) {
        "alle ${updated.format(updateTime)}"
    } else {
        "il ${updated.format(updateDateTime)}"
    }
    val status = when {
        calendar.offline -> "Dati offline · aggiornati $timestamp"
        refreshing -> "Aggiornato $timestamp · controllo in corso"
        else -> "Aggiornato $timestamp"
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        if (calendar.offline) {
            Icon(Icons.Outlined.CloudOff, contentDescription = null, Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.error)
            Spacer(Modifier.width(6.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("${calendar.lessons.size} lezioni nell’anno" +
                if (weekend) " · scorri i giorni per il weekend" else "",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(status, style = MaterialTheme.typography.labelSmall,
                color = if (calendar.offline) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onRefresh, enabled = !refreshing, modifier = Modifier.size(40.dp)) {
            if (refreshing) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            else Icon(Icons.Outlined.Refresh, contentDescription = "Aggiorna calendario")
        }
    }
}

@Composable
private fun WeekHeader(week: LocalDate, onMoveWeek: (Long) -> Unit, onOpenMonth: () -> Unit) {
    var drag by remember(week) { mutableFloatStateOf(0f) }
    Row(
        Modifier.fillMaxWidth().pointerInput(week) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { change, amount -> drag += amount; change.consume() },
                onDragEnd = {
                    if (drag > 70f) onMoveWeek(-1) else if (drag < -70f) onMoveWeek(1)
                    drag = 0f
                }
            )
        },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text("SETTIMANA", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            Text("${week.format(dateShort)} – ${week.plusDays(6).format(dateShort)}",
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        IconButton(onClick = onOpenMonth) {
            Icon(Icons.Outlined.CalendarMonth, "Apri calendario mensile")
        }
        IconButton(onClick = { onMoveWeek(-1) }) { Icon(Icons.Outlined.ChevronLeft, "Settimana precedente") }
        IconButton(onClick = { onMoveWeek(1) }) { Icon(Icons.Outlined.ChevronRight, "Settimana successiva") }
    }
}

@Composable
fun LessonMonthScreen(
    lessons: List<Lesson>,
    initialDay: LocalDate,
    weekend: Boolean,
    onOpenDay: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    var month by remember(initialDay) { mutableStateOf(YearMonth.from(initialDay)) }
    var selectedDay by remember(initialDay, weekend) { mutableStateOf(openingDay(initialDay, weekend)) }
    var drag by remember(month) { mutableFloatStateOf(0f) }
    val lessonsByDay = remember(lessons) {
        lessons.groupBy { it.date }.mapValues { (_, items) -> items.sortedBy { it.start } }
    }
    val selectedLessons = lessonsByDay[selectedDay].orEmpty()
    val monthTitle = remember(month) {
        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", italian))
            .replaceFirstChar { it.titlecase(italian) }
    }
    fun moveMonth(amount: Long) {
        val target = month.plusMonths(amount)
        selectedDay = openingDay(
            target.atDay(selectedDay.dayOfMonth.coerceAtMost(target.lengthOfMonth())), weekend)
        month = target
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(monthTitle, style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold)
                    Text("Calendario delle lezioni", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = {
                    selectedDay = openingDay(today, weekend)
                    month = YearMonth.from(selectedDay)
                }) { Text("Oggi") }
                IconButton(onClick = { moveMonth(-1) }) {
                    Icon(Icons.Outlined.ChevronLeft, "Mese precedente")
                }
                IconButton(onClick = { moveMonth(1) }) {
                    Icon(Icons.Outlined.ChevronRight, "Mese successivo")
                }
            }
        }
        item {
            Surface(
                modifier = Modifier.fillMaxWidth().pointerInput(month) {
                    detectHorizontalDragGestures(
                        onDragStart = { drag = 0f },
                        onHorizontalDrag = { change, amount -> drag += amount; change.consume() },
                        onDragCancel = { drag = 0f },
                        onDragEnd = {
                            if (drag > 70f) moveMonth(-1) else if (drag < -70f) moveMonth(1)
                            drag = 0f
                        }
                    )
                },
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow
            ) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 12.dp)) {
                    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                            val labels = if (weekend) listOf("LUN", "MAR", "MER", "GIO", "VEN", "SAB", "DOM")
                                else listOf("LUN", "MAR", "MER", "GIO", "VEN")
                            labels.forEach { label ->
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
                        label = "lesson-month"
                    ) { displayedMonth ->
                        Column(Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            val columns = if (weekend) 7 else 5
                            lessonMonthDates(displayedMonth, weekend).chunked(columns).forEach { row ->
                                Row(Modifier.fillMaxWidth()) {
                                    row.forEach { day ->
                                        LessonMonthDay(day, YearMonth.from(day) == displayedMonth,
                                            day == selectedDay, day == today,
                                            lessonsByDay[day].orEmpty().size) {
                                            selectedDay = it
                                            month = YearMonth.from(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(selectedDay.format(dateLong).replaceFirstChar { it.titlecase(italian) },
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(if (selectedLessons.isEmpty()) "Nessuna lezione" else
                        "${selectedLessons.size} ${if (selectedLessons.size == 1) "lezione" else "lezioni"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FilledTonalButton(onClick = { onOpenDay(selectedDay) }) { Text("Apri giornata") }
            }
        }
        if (selectedLessons.isEmpty()) item {
            EmptyCard("Nessuna lezione in questo giorno", "Scegli un’altra data nel calendario.")
        } else items(selectedLessons, key = { "month:${it.id}:${it.subjectCode}:${it.start}" }) { lesson ->
            LessonCard(lesson, saved = false, onToggleSave = null)
        }
    }
}

@Composable
private fun RowScope.LessonMonthDay(
    day: LocalDate,
    inCurrentMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    count: Int,
    onSelect: (LocalDate) -> Unit
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val indicatorOffset by animateDpAsState(
        targetValue = if (selected || today) 24.dp else 17.dp,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "lesson-day-indicator-offset"
    )
    Box(
        modifier = Modifier.weight(1f).height(56.dp).clickable(
            interactionSource = interactionSource, indication = null, onClick = { onSelect(day) }
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
            modifier = Modifier.size(34.dp)
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
        if (count > 0) Row(Modifier.align(Alignment.Center)
            .offset { IntOffset(0, indicatorOffset.roundToPx()) },
            horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(count.coerceAtMost(3)) {
                Box(Modifier.size(4.dp).background(
                    when {
                        selected -> MaterialTheme.colorScheme.onPrimary
                        !inCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        else -> MaterialTheme.colorScheme.primary
                    },
                    CircleShape))
            }
        }
    }
}

internal fun lessonMonthDates(month: YearMonth, weekend: Boolean = true): List<LocalDate> {
    val first = month.atDay(1)
    val leadingDays = first.dayOfWeek.value - 1
    val start = first.minusDays(leadingDays.toLong())
    val rows = if (leadingDays + month.lengthOfMonth() <= 35) 5 else 6
    val dates = List(rows * 7) { start.plusDays(it.toLong()) }
    return if (weekend) dates else dates.filter { it.dayOfWeek.value <= DayOfWeek.FRIDAY.value }
}

@Composable
private fun DayTile(
    day: LocalDate, selected: Boolean, count: Int, modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Surface(onClick = onClick, shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = modifier.height(86.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(day.dayOfWeek.getDisplayName(TextStyle.SHORT, italian).replaceFirstChar { it.titlecase(italian) },
                style = MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
            Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurface)
            Box(Modifier.size(6.dp).background(if (count > 0) {
                if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
            } else Color.Transparent, CircleShape))
        }
    }
}

@Composable
private fun LessonCard(lesson: Lesson, saved: Boolean, onToggleSave: (() -> Unit)?) {
    val accent = lessonColors[(lesson.subjectCode.hashCode() and Int.MAX_VALUE) % lessonColors.size]
    Card(shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(5.dp).fillMaxHeight()
                .background(if (lesson.cancelled) MaterialTheme.colorScheme.error else accent))
            Column(Modifier.weight(1f).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${lesson.start}–${lesson.end}", style = MaterialTheme.typography.labelLarge,
                        color = if (lesson.cancelled) MaterialTheme.colorScheme.error else accent,
                        fontWeight = FontWeight.Bold)
                    if (lesson.cancelled) {
                        Spacer(Modifier.width(10.dp))
                        Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(7.dp)) {
                            Text("ANNULLATA", Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
                Spacer(Modifier.height(7.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(lesson.subject.ifBlank { "Insegnamento" }, Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (onToggleSave != null) IconButton(onClick = onToggleSave) {
                        Icon(if (saved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = if (saved) "Rimuovi ${lesson.subject} dai miei orari"
                                else "Salva ${lesson.subject} nei miei orari")
                    }
                }
                if (lesson.room.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, contentDescription = null, Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(lesson.room, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (lesson.teacher.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(lesson.teacher, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (lesson.notes.isNotBlank()) {
                    Spacer(Modifier.height(7.dp))
                    Text(lesson.notes, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(message: String, onDismiss: () -> Unit, onRetry: (() -> Unit)? = null) {
    Surface(color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(message, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer)
                if (onRetry != null) TextButton(onClick = onRetry) { Text("Riprova") }
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Outlined.Close, contentDescription = "Chiudi") }
        }
    }
}

@Composable
private fun EmptyPanel(title: String, description: String, action: String? = null, onAction: (() -> Unit)? = null) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        AppEmptyState(
            title = title,
            message = description,
            action = action,
            onAction = onAction,
            icon = Icons.Outlined.Search
        )
    }
}

@Composable
private fun EmptyCard(title: String, description: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
