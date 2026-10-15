package app.orariunimi

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val roomZone = ZoneId.of("Europe/Rome")
private val roomItalian = Locale.ITALIAN
private val roomDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", roomItalian)
private val roomTime = DateTimeFormatter.ofPattern("HH:mm", roomItalian)
private val roomUpdated = DateTimeFormatter.ofPattern("d MMM, HH:mm", roomItalian)
private val exploreLabels = listOf("Corsi", "Docenti", "Insegnamenti", "Aule")

@Composable
fun ExploreCategoryTabs(selectedIndex: Int, onSelect: (Int) -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(selectedIndex) { listState.animateScrollToItem((selectedIndex - 1).coerceAtLeast(0)) }
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(4.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            items(exploreLabels.size) { index ->
                val selected = index == selectedIndex
                val container by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    label = "explore-category-background"
                )
                val content by animateColorAsState(
                    if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    label = "explore-category-text"
                )
                Box(
                    Modifier.height(42.dp)
                        .background(container, RoundedCornerShape(14.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) }
                        ).padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(exploreLabels[index], color = content,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun RoomSearchScreen(
    query: String,
    results: List<UnimiRoom>?,
    loading: Boolean,
    error: String?,
    onQuery: (String) -> Unit,
    onOpenRoom: (UnimiRoom) -> Unit,
    onRetry: () -> Unit,
    onCategory: (Int) -> Unit
) {
    val focus = LocalFocusManager.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                AppScreenHeader("Esplora", "Corsi, docenti, insegnamenti e aule UNIMI")
                Spacer(Modifier.height(16.dp))
                ExploreCategoryTabs(3, onCategory)
                AppSearchField(query, onQuery, "Cerca un'aula o una sede",
                    modifier = Modifier.padding(top = 10.dp))
                if (query.isNotBlank() && results != null && !loading) {
                    Text(
                        if (results.size == 50) "Primi 50 risultati · affina la ricerca"
                        else "${results.size} ${if (results.size == 1) "aula" else "aule"}",
                        Modifier.padding(top = 9.dp), style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        when {
            loading -> item { AppLoadingState() }
            error != null -> item {
                AppEmptyState("Aule non disponibili", error, action = "Riprova", onAction = onRetry,
                    icon = Icons.Outlined.MeetingRoom)
            }
            query.isBlank() -> item {
                AppEmptyState("Trova un'aula", "Cerca per nome, sede o codice. Lo stato di occupazione si carica quando apri l'aula.",
                    icon = Icons.Outlined.MeetingRoom)
            }
            results == null -> item { AppLoadingState() }
            results.isEmpty() -> item {
                AppEmptyState("Nessuna aula trovata", "Prova con un'altra sede o un altro nome.",
                    icon = Icons.Outlined.MeetingRoom)
            }
            else -> items(results, key = { it.code }) { room ->
                Card(onClick = { focus.clearFocus(); onOpenRoom(room) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer) {
                            Icon(Icons.Outlined.MeetingRoom, contentDescription = null,
                                Modifier.padding(11.dp).size(22.dp),
                                tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Aula ${room.name}", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Text(buildString {
                                append(room.building)
                                room.capacity?.let { append(" · $it posti") }
                            }, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun RoomDetailScreen(
    room: UnimiRoom,
    date: LocalDate,
    planning: RoomPlanning?,
    loading: Boolean,
    error: String?,
    onDate: (LocalDate) -> Unit,
    onRefresh: () -> Unit
) {
    var now by remember { mutableStateOf(LocalDateTime.now(roomZone)) }
    LaunchedEffect(room.code, date) {
        while (true) {
            now = LocalDateTime.now(roomZone)
            delay(60_000)
        }
    }
    val today = now.toLocalDate()
    val shown = planning?.takeIf { it.roomCode == room.code && it.date == date }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text(room.building, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text(buildString {
                        append("Aula ${room.name}")
                        room.capacity?.let { append(" · $it posti") }
                    }, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { onDate(date.minusDays(1)) }) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Giorno precedente")
                    }
                    Text(date.format(roomDate).replaceFirstChar { it.titlecase(roomItalian) },
                        Modifier.weight(1f), style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                        overflow = TextOverflow.Ellipsis)
                    if (date != today) OutlinedButton(onClick = { onDate(today) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)) {
                        Text("Oggi")
                    }
                    IconButton(onClick = { onDate(date.plusDays(1)) }) {
                        Icon(Icons.Outlined.ChevronRight,
                            contentDescription = "Giorno successivo")
                    }
                }
            }
        }
        if (loading && shown != null) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
        when {
            shown == null && loading -> item { AppLoadingState() }
            shown == null -> item {
                AppEmptyState("Planning non disponibile", error ?: "Impossibile caricare questa giornata.",
                    action = "Riprova", onAction = onRefresh, icon = Icons.Outlined.MeetingRoom)
            }
            else -> {
                item {
                    RoomStatusCard(shown, now, onRefresh)
                }
                item {
                    Text("Impegni della giornata", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                }
                if (shown.events.isEmpty()) item {
                    AppEmptyState("Nessun impegno pubblicato",
                        "Controlla sempre il planning prima di usare l'aula.",
                        icon = Icons.Outlined.MeetingRoom)
                } else items(shown.events, key = { "${it.id}:${it.start}:${it.end}" }) { event ->
                    RoomEventCard(event)
                }
            }
        }
    }
}

@Composable
private fun RoomStatusCard(planning: RoomPlanning, now: LocalDateTime, onRefresh: () -> Unit) {
    val isToday = planning.date == now.toLocalDate()
    val outdated = planning.offline || now.atZone(roomZone).toInstant().toEpochMilli() -
        planning.sourceUpdatedAtMillis > 30L * 60 * 1000
    val current = planning.currentEvent(now)
    val next = planning.nextEvent(now)
    val title = when {
        !isToday -> "${planning.events.count { !it.cancelled }} impegni programmati"
        outdated -> "Disponibilità da verificare"
        current != null -> "Occupata fino alle ${current.end.format(roomTime)}"
        else -> "Libera secondo il planning"
    }
    val detail = when {
        planning.offline -> "Dati salvati sul dispositivo: il portale non è raggiungibile."
        isToday && outdated -> "Il planning pubblicato potrebbe non essere aggiornato."
        isToday && current == null && next != null -> "Prossimo impegno alle ${next.start.format(roomTime)}."
        isToday && current == null -> "Nessun altro impegno pubblicato oggi."
        else -> "Occupazione prevista dagli eventi pubblicati da UNIMI."
    }
    Card(shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Aggiorna occupazione",
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer)
            Spacer(Modifier.height(10.dp))
            val updated = Instant.ofEpochMilli(planning.sourceUpdatedAtMillis).atZone(roomZone)
            Text("Planning aggiornato: ${updated.format(roomUpdated)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.76f))
        }
    }
}

@Composable
private fun RoomEventCard(event: RoomEvent) {
    Card(shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.Top) {
            Text("${event.start.format(roomTime)}\n${event.end.format(roomTime)}",
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(14.dp))
            Column {
                Text(event.title, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (event.cancelled) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface)
                if (event.type.isNotBlank() || event.cancelled) Text(
                    if (event.cancelled) "Annullato" else event.type,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
