package app.orariunimi

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val roomZone = ZoneId.of("Europe/Rome")
private val roomItalian = Locale.ITALIAN
private val roomDate = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", roomItalian)
private val roomTime = DateTimeFormatter.ofPattern("HH:mm", roomItalian)
private val roomUpdated = DateTimeFormatter.ofPattern("d MMM, HH:mm", roomItalian)
private val exploreLabels = listOf("Corsi", "Docenti", "Insegnamenti", "Aule")
private val exploreWeights = listOf(0.8f, 1.0f, 1.5f, 0.7f)
private fun exploreOffset(index: Int) = exploreWeights.take(index).sum() / exploreWeights.sum()
private fun exploreWidth(index: Int) = exploreWeights[index] / exploreWeights.sum()

@Composable
fun ExploreCategoryTabs(selectedIndex: Int, initialIndex: Int, onSelect: (Int) -> Unit) {
    val safeIndex = selectedIndex.coerceIn(exploreLabels.indices)
    val safeInitial = initialIndex.coerceIn(exploreLabels.indices)
    val indicatorOffset = remember { Animatable(exploreOffset(safeInitial)) }
    val indicatorWidth = remember { Animatable(exploreWidth(safeInitial)) }
    LaunchedEffect(safeIndex) {
        coroutineScope {
            launch { indicatorOffset.animateTo(exploreOffset(safeIndex),
                spring(dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow)) }
            launch { indicatorWidth.animateTo(exploreWidth(safeIndex),
                spring(dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow)) }
        }
    }
    Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(4.dp)) {
            val widthPx = with(LocalDensity.current) { maxWidth.roundToPx() }
            Box(Modifier.offset { IntOffset((widthPx * indicatorOffset.value).roundToInt(), 0) }
                .width(maxWidth * indicatorWidth.value).height(48.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)))
            Row(Modifier.fillMaxWidth()) {
                exploreLabels.forEachIndexed { index, label ->
                    val selected = index == safeIndex
                    val content by animateColorAsState(
                        if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        label = "explore-category-text"
                    )
                    Box(
                        Modifier.weight(exploreWeights[index]).height(48.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onSelect(index) }
                            ).padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = content, textAlign = TextAlign.Center,
                            fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@Composable
fun RoomSearchScreen(
    query: String,
    results: List<UnimiBuilding>?,
    loading: Boolean,
    error: String?,
    onQuery: (String) -> Unit,
    onOpenBuilding: (UnimiBuilding) -> Unit,
    onRetry: () -> Unit,
    onCategory: (Int) -> Unit,
    initialCategory: Int
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
                ExploreCategoryTabs(3, initialCategory, onCategory)
                AppSearchField(query, onQuery, "Cerca un edificio o un'aula",
                    modifier = Modifier.padding(top = 10.dp))
                if (results != null && !loading) {
                    Text(
                        "${results.size} ${if (results.size == 1) "edificio" else "edifici"}",
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
            results == null -> item { AppLoadingState() }
            results.isEmpty() -> item {
                AppEmptyState("Nessun edificio trovato", "Prova con un'altra sede o un'altra aula.",
                    icon = Icons.Outlined.MeetingRoom)
            }
            else -> items(results, key = { it.code }) { building ->
                Card(onClick = { focus.clearFocus(); onOpenBuilding(building) },
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
                            Text(building.name, style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold, maxLines = 2,
                                overflow = TextOverflow.Ellipsis)
                            Text(buildString {
                                append(building.address.ifBlank { "Indirizzo non pubblicato" })
                                append(" · ${building.rooms.size} ")
                                append(if (building.rooms.size == 1) "aula" else "aule")
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
fun RoomBuildingScreen(
    building: UnimiBuilding,
    planning: BuildingPlanning?,
    loading: Boolean,
    error: String?,
    onOpenRoom: (UnimiRoom) -> Unit,
    onRefresh: () -> Unit
) {
    var now by remember { mutableStateOf(LocalDateTime.now(roomZone)) }
    LaunchedEffect(building.code) {
        while (true) {
            now = LocalDateTime.now(roomZone)
            delay(60_000)
        }
    }
    val shown = planning?.takeIf { it.buildingCode == building.code && it.date == now.toLocalDate() }
    LazyColumn(modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Column {
                Text(building.name, style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Place, contentDescription = null, Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(5.dp))
                    Text(building.address.ifBlank { "Indirizzo non pubblicato" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${building.rooms.size} ${if (building.rooms.size == 1) "aula" else "aule"}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 5.dp))
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (shown == null) "Occupazione non disponibile"
                    else if (shown.offline) "Planning salvato sul dispositivo"
                    else "Secondo il planning · aggiornato ${Instant.ofEpochMilli(shown.sourceUpdatedAtMillis).atZone(roomZone).format(roomUpdated)}",
                    Modifier.weight(1f), style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                IconButton(onClick = onRefresh) {
                    Icon(Icons.Outlined.Refresh, contentDescription = "Aggiorna occupazione")
                }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (error != null && shown == null) item {
            Text(error, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error)
        }
        items(building.rooms, key = { it.code }) { room ->
            val roomPlanning = shown?.forRoom(room)
            Card(onClick = { onOpenRoom(room) }, shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    RoomTrafficLight(roomPlanning, now)
                    Spacer(Modifier.width(13.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Aula ${room.name}", style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                        Text(buildString {
                            append(when {
                                roomPlanning == null || roomPlanning.offline -> "Stato non disponibile"
                                roomPlanning.currentEvent(now) != null -> "Occupata ora"
                                else -> "Libera ora"
                            })
                            room.capacity?.let { append(" · $it posti") }
                        }, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RoomTrafficLight(planning: RoomPlanning?, now: LocalDateTime) {
    val color = when {
        planning == null || planning.offline -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.42f)
        planning.currentEvent(now) != null -> MaterialTheme.colorScheme.error
        else -> Color(0xFF41BA83)
    }
    Box(Modifier.size(15.dp).background(color, RoundedCornerShape(50)))
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
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Impegni della giornata", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold)
                            Text(if (shown.offline) "Planning salvato sul dispositivo"
                                else "Aggiornato ${Instant.ofEpochMilli(shown.sourceUpdatedAtMillis).atZone(roomZone).format(roomUpdated)}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = onRefresh) {
                            Icon(Icons.Outlined.Refresh, contentDescription = "Aggiorna planning")
                        }
                    }
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
