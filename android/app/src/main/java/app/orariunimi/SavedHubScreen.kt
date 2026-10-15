package app.orariunimi

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

enum class FavoriteSection { SUBJECTS, DEGREES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    courses: List<FavoriteCourse>,
    subjects: List<FavoriteSubject>,
    section: FavoriteSection,
    examIndexLoading: Boolean,
    onSection: (FavoriteSection) -> Unit,
    onOpenCourse: (FavoriteCourse) -> Unit,
    onOpenLesson: (FavoriteSubject) -> Unit,
    onOpenExams: (FavoriteSubject) -> Unit,
    onRemoveCourse: (FavoriteCourse) -> Unit,
    onEditSubject: (FavoriteSubject) -> Unit,
    onAddCourse: () -> Unit,
    onAddSubject: () -> Unit,
    onClearCourses: () -> Unit,
    onClearSubjects: () -> Unit
) {
    var actionsOpen by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }
    val showingSubjects = section == FavoriteSection.SUBJECTS
    val count = if (showingSubjects) subjects.size else courses.size
    val collection = if (showingSubjects) "insegnamenti" else "corsi di laurea"
    if (actionsOpen) ModalBottomSheet(onDismissRequest = { actionsOpen = false }) {
        Text("Gestisci $collection", Modifier.padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
            style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Row(
            modifier = Modifier.fillMaxWidth().clickable {
                actionsOpen = false
                confirmClear = true
            }.padding(horizontal = 24.dp, vertical = 15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.padding(start = 16.dp)) {
                Text("Svuota elenco", style = MaterialTheme.typography.bodyLarge)
                Text("Rimuovi $collection dai preferiti su questo dispositivo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.size(24.dp))
    }
    if (confirmClear) AlertDialog(
        onDismissRequest = { confirmClear = false },
        title = { Text("Svuotare $collection?") },
        text = { Text("Questi elementi verranno rimossi dai preferiti su questo dispositivo.") },
        confirmButton = { TextButton(onClick = {
            confirmClear = false
            if (showingSubjects) onClearSubjects() else onClearCourses()
        }) { Text("Elimina tutto") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Annulla") } }
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { AppScreenHeader("Preferiti", "Insegnamenti e corsi di laurea che vuoi ritrovare") }
        item {
            AppSegmentedControl(
                options = listOf("Insegnamenti", "Lauree"),
                selectedIndex = if (showingSubjects) 0 else 1,
                onSelected = { onSection(if (it == 0) FavoriteSection.SUBJECTS else FavoriteSection.DEGREES) },
                modifier = Modifier.padding(top = 4.dp)
            )
        }
        item {
            AppSectionHeader(
                title = if (showingSubjects) "Insegnamenti" else "Corsi di laurea",
                count = count,
                actionIcon = Icons.Outlined.Add,
                actionDescription = if (showingSubjects) "Aggiungi insegnamento" else "Aggiungi corso di laurea",
                onAction = if (showingSubjects) onAddSubject else onAddCourse,
                modifier = Modifier.padding(top = 10.dp),
                trailing = {
                    if (count > 0) IconButton(onClick = { actionsOpen = true }) {
                        Icon(Icons.Outlined.MoreVert, contentDescription = "Altre azioni sui preferiti")
                    }
                }
            )
        }
        if (showingSubjects) {
            if (subjects.isEmpty()) item {
                AppEmptyState("Nessun insegnamento preferito",
                    "Aggiungi un insegnamento alle lezioni o seguilo per gli appelli.",
                    action = "Aggiungi insegnamento", onAction = onAddSubject,
                    icon = Icons.AutoMirrored.Outlined.MenuBook)
            }
            items(subjects, key = { "subject:${it.year}:${it.code}" }) { subject ->
                FavoriteSubjectCard(
                    subject = subject,
                    examIndexLoading = examIndexLoading && subject.inLessons,
                    onOpen = {
                        if (subject.inLessons) onOpenLesson(subject) else onOpenExams(subject)
                    },
                    onOpenExams = { onOpenExams(subject) },
                    onEdit = { onEditSubject(subject) },
                    modifier = Modifier.animateItem()
                )
            }
        } else {
            if (courses.isEmpty()) item {
                AppEmptyState("Nessun corso di laurea preferito",
                    "Salva un corso di laurea per ritrovarlo qui.",
                    action = "Esplora i corsi", onAction = onAddCourse,
                    icon = Icons.Outlined.School)
            }
            items(courses, key = { "degree:${it.year}:${it.code}" }) { course ->
                DegreeCourseCard(
                    course = SearchItem(course.code, course.name, SearchKind.COURSE,
                        degreeType = course.degreeType),
                    favorite = true,
                    modifier = Modifier.animateItem(),
                    onClick = { onOpenCourse(course) },
                    supportingText = "${course.degreeType.label} · ${yearLabel(course.year)}",
                    onToggleFavorite = { onRemoveCourse(course) },
                    favoriteDescription = "Rimuovi ${course.name} dai preferiti"
                )
            }
        }
    }
}
@Composable
private fun FavoriteSubjectCard(
    subject: FavoriteSubject,
    examIndexLoading: Boolean,
    onOpen: () -> Unit,
    onOpenExams: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(onClick = onOpen, modifier = modifier, shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 11.dp, bottom = 11.dp, end = 5.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Column(Modifier.weight(1f).padding(start = 11.dp)) {
                Text(subject.name, style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${subject.code} · ${yearLabel(subject.year)}", style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(when {
                    subject.inLessons && subject.inExams -> "Lezioni · Appelli"
                    subject.inLessons -> "Solo lezioni"
                    else -> "Solo appelli"
                }, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary)
            }
            if (subject.inExams && subject.inLessons && subject.examSources.isNotEmpty()) IconButton(onClick = onOpenExams) {
                Icon(Icons.Outlined.EventAvailable, contentDescription = "Apri gli appelli di ${subject.name}")
            } else if (examIndexLoading && subject.examSources.isEmpty()) {
                CircularProgressIndicator(Modifier.padding(horizontal = 13.dp).size(18.dp), strokeWidth = 2.dp)
            }
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit,
                    contentDescription = "Scegli Lezioni e Appelli per ${subject.name}")
            }
        }
    }
}
