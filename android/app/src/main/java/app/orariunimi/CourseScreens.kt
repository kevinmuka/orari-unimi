package app.orariunimi

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

data class DegreePalette(val accent: Color, val container: Color, val content: Color)

@Composable
fun degreePalette(type: DegreeType): DegreePalette {
    val dark = isSystemInDarkTheme()
    return when (type) {
        DegreeType.BACHELOR -> if (dark) DegreePalette(Color(0xFFA9C5FF), Color(0xFF1C355C), Color.White)
            else DegreePalette(Color(0xFF315BB7), Color(0xFFE4EDFF), Color(0xFF17356E))
        DegreeType.MASTER -> if (dark) DegreePalette(Color(0xFF8DDCCE), Color(0xFF17443F), Color.White)
            else DegreePalette(Color(0xFF087B71), Color(0xFFDCF5EF), Color(0xFF15584F))
        DegreeType.SINGLE_CYCLE -> if (dark) DegreePalette(Color(0xFFD9B4F5), Color(0xFF493259), Color.White)
            else DegreePalette(Color(0xFF8651AE), Color(0xFFF0E5FA), Color(0xFF5B3576))
        DegreeType.OTHER -> DegreePalette(MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun DegreeBadge(type: DegreeType) {
    val palette = degreePalette(type)
    Surface(color = palette.container, shape = RoundedCornerShape(8.dp)) {
        Text(type.label, Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall, color = palette.content, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun DegreeCourseCard(
    course: SearchItem,
    favorite: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String = listOf(course.code, course.degreeType?.label)
        .filterNotNull().filter { it.isNotBlank() }.joinToString(" · "),
    onToggleFavorite: (() -> Unit)? = null,
    favoriteDescription: String? = null,
    showChevron: Boolean = false
) {
    val palette = degreePalette(course.degreeType ?: DegreeType.OTHER)
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = palette.container)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(start = 15.dp, top = 12.dp, bottom = 12.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.School, contentDescription = null, tint = palette.accent,
                        modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleMedium,
                    color = palette.content, fontWeight = FontWeight.SemiBold,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (supportingText.isNotBlank()) Text(supportingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.content.copy(alpha = 0.78f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            when {
                onToggleFavorite != null -> IconButton(onClick = onToggleFavorite) {
                    Icon(if (favorite) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = favoriteDescription, tint = palette.accent)
                }
                favorite -> Icon(Icons.Outlined.Bookmark, contentDescription = "Corso salvato",
                    tint = palette.accent, modifier = Modifier.padding(horizontal = 12.dp).size(21.dp))
                showChevron -> Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null, tint = palette.content.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 10.dp))
            }
        }
    }
}

@Composable
fun CourseDetailScreen(
    course: SearchItem, year: String, preferredSubjects: List<FavoriteSubject>,
    onOpenCalendar: () -> Unit,
    onOpenExams: () -> Unit,
    onOpenTeaching: (CourseTeaching) -> Unit, onToggleTeaching: (CourseTeaching) -> Unit
) {
    val type = course.degreeType ?: DegreeType.OTHER
    val palette = degreePalette(type)
    val visiblePaths = course.coursePaths.filter { it.teachings.isNotEmpty() }
    val count = visiblePaths.flatMap { it.teachings }.distinctBy { it.code }.size
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                DegreeBadge(type)
                Text(course.name, Modifier.padding(top = 12.dp),
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("${yearLabel(year)} · $count insegnamenti disponibili",
                    Modifier.padding(top = 5.dp), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().padding(top = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(onClick = onOpenCalendar, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Lezioni")
                    }
                    FilledTonalButton(onClick = onOpenExams, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.EventAvailable, contentDescription = null, Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Appelli")
                    }
                }
                Text(if (course.coursePaths.any { it.code.startsWith("catalog:") }) "INSEGNAMENTI"
                    else "INSEGNAMENTI PER PERCORSO", Modifier.padding(top = 25.dp),
                    style = MaterialTheme.typography.labelMedium, color = palette.accent,
                    fontWeight = FontWeight.Bold)
            }
        }
        if (count == 0) item {
            Text("Il portale non elenca insegnamenti per questo corso.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        visiblePaths.forEachIndexed { pathIndex, path ->
            item(key = "path:$pathIndex:${path.code}") {
                Text(path.name.ifBlank { "Percorso" }, Modifier.padding(top = 9.dp, bottom = 3.dp),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            itemsIndexed(path.teachings, key = { teachingIndex, teaching ->
                "$pathIndex:$teachingIndex:${teaching.code}"
            }) { _, teaching ->
                val isSaved = preferredSubjects.any { it.year == year && it.code == teaching.code }
                SubjectCard(name = teaching.name,
                    supportingText = listOf(teaching.code, teaching.teacher)
                        .filter { it.isNotBlank() }.joinToString(" · "),
                    actionIcon = if (isSaved) Icons.Outlined.Bookmark else Icons.Outlined.BookmarkBorder,
                    actionDescription = "Scegli Lezioni e Appelli per ${teaching.name}",
                    onAction = { onToggleTeaching(teaching) },
                    onOpen = { onOpenTeaching(teaching) })
            }
        }
    }
}

internal fun yearLabel(year: String): String = year.toIntOrNull()?.let { "$it/${it + 1}" } ?: year
