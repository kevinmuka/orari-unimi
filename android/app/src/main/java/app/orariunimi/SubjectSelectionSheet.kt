package app.orariunimi

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectSelectionSheet(
    year: String,
    subject: SearchItem,
    inLessons: Boolean,
    inExams: Boolean,
    alreadySaved: Boolean,
    loadingExams: Boolean,
    examLookupFailed: Boolean,
    onRetryExams: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (Boolean, Boolean) -> Unit
) {
    val supportsExams = subject.examSources.isNotEmpty() || inExams
    var lessons by remember(year, subject.code, alreadySaved, inLessons) {
        mutableStateOf(if (alreadySaved) inLessons else true)
    }
    var exams by remember(year, subject.code, alreadySaved, inExams, supportsExams) {
        mutableStateOf(if (alreadySaved) inExams else supportsExams)
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(subject.name, style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold)
            Text("Scegli dove seguire questo insegnamento", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (loadingExams) Text("Controllo la disponibilità degli appelli…",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (examLookupFailed) {
                Text("Impossibile verificare gli appelli per questo insegnamento.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onRetryExams) { Text("Riprova") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(selected = lessons, onClick = { lessons = !lessons },
                    label = { Text("Lezioni") },
                    leadingIcon = if (lessons) {{ Icon(Icons.Outlined.Check,
                        contentDescription = null, modifier = Modifier.size(17.dp)) }} else null)
                FilterChip(selected = exams, onClick = { exams = !exams },
                    enabled = supportsExams && !loadingExams && !examLookupFailed,
                    label = { Text("Appelli") },
                    leadingIcon = if (exams) {{ Icon(Icons.Outlined.Check,
                        contentDescription = null, modifier = Modifier.size(17.dp)) }} else null)
            }
            if (!supportsExams && !loadingExams && !examLookupFailed)
                Text("Appelli non disponibili per questo insegnamento",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(when {
                lessons && exams -> "Comparirà nel calendario delle lezioni e tra gli insegnamenti seguiti per gli appelli."
                lessons -> "Comparirà solo nel calendario delle lezioni."
                exams -> "Seguirai solo i suoi appelli."
                else -> "Verrà rimosso dai preferiti."
            }, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = { onApply(lessons, exams) }, modifier = Modifier.fillMaxWidth(),
                enabled = !loadingExams && !examLookupFailed) {
                Text(if (!lessons && !exams) "Rimuovi dai preferiti" else "Salva scelta")
            }
        }
    }
}
