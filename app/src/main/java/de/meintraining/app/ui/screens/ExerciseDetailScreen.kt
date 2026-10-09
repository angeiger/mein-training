package de.meintraining.app.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.meintraining.app.catalog.Exercise
import de.meintraining.app.data.MachineSettings
import de.meintraining.app.ui.NoInsets
import de.meintraining.app.ui.components.BackTopBar
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.BigOutlinedButton
import de.meintraining.app.ui.components.ConfirmDialog
import de.meintraining.app.ui.components.ExerciseAnimation
import de.meintraining.app.ui.components.MuscleMap
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.components.SettingsSummary
import de.meintraining.app.ui.rememberRepository
import de.meintraining.app.ui.theme.Accent
import de.meintraining.app.ui.theme.AccentLight
import de.meintraining.app.ui.theme.TipYellow

@Composable
fun ExerciseDetailScreen(
    exerciseId: String,
    onBack: () -> Unit,
    onEditSettings: () -> Unit,
    onShowPhoto: () -> Unit,
) {
    val repo = rememberRepository()
    val all by repo.exercises.collectAsStateWithLifecycle()
    val exercise = all.firstOrNull { it.id == exerciseId }
    val settings by repo.settings(exerciseId).collectAsStateWithLifecycle(initialValue = MachineSettings())
    var addToPlan by rememberSaveable { mutableStateOf(false) }
    var deleting by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            BackTopBar(exercise?.name ?: "", onBack) {
                if (exercise?.custom == true) {
                    IconButton(onClick = { deleting = true }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Übung löschen", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        contentWindowInsets = NoInsets,
    ) { padding ->
        if (exercise == null) return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            exercise.aka?.let {
                Text("Auch: $it", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ExerciseAnimation(exercise, height = 260.dp)

            if (exercise.steps.isNotEmpty()) {
                SectionCard(title = "So geht's") { Steps(exercise.steps) }
            }
            exercise.tip?.let { TipCard(it) }

            SectionCard(title = "Meine Einstellungen") {
                SettingsSummary(settings, onShowPhoto = onShowPhoto)
                BigOutlinedButton(
                    if (settings.isEmpty) "Einstellungen eintragen" else "Einstellungen ändern",
                    onClick = onEditSettings,
                    icon = if (settings.photoPath == null) Icons.Filled.PhotoCamera else Icons.Filled.Edit,
                )
            }

            if (exercise.primary.isNotEmpty()) {
                SectionCard(title = "Diese Muskeln arbeiten") {
                    MuscleLegend(exercise)
                    MuscleMap(
                        primary = exercise.primary.toSet(),
                        secondary = exercise.secondary.toSet(),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            BigButton("Zu einem Plan hinzufügen", onClick = { addToPlan = true }, icon = Icons.Filled.Add)
        }
    }

    if (addToPlan && exercise != null) {
        AddToPlanDialog(exercise, onDismiss = { addToPlan = false })
    }
    if (deleting && exercise != null) {
        ConfirmDialog(
            title = "Übung löschen?",
            text = "„${exercise.name}“ wird gelöscht und aus allen Plänen entfernt – mit Foto und Einstellungen.",
            confirm = "Löschen",
            onConfirm = {
                deleting = false
                repo.deleteCustomExercise(exercise.id)
                onBack()
            },
            onDismiss = { deleting = false },
        )
    }
}

@Composable
fun Steps(steps: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        steps.forEachIndexed { i, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("${i + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Text(step, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TipCard(tip: String) {
    SectionCard(containerColor = TipYellow) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Filled.WarningAmber, contentDescription = null, tint = Color(0xFF8A6100), modifier = Modifier.size(30.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text("Wichtig", style = MaterialTheme.typography.titleMedium)
                Text(tip, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Composable
private fun MuscleLegend(exercise: Exercise) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LegendRow(Accent, "Hauptsächlich: " + exercise.primary.joinToString { it.title })
        if (exercise.secondary.isNotEmpty()) {
            LegendRow(AccentLight, "Etwas mit: " + exercise.secondary.joinToString { it.title })
        }
    }
}

@Composable
private fun LegendRow(color: Color, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .padding(top = 4.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(color),
        )
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AddToPlanDialog(exercise: Exercise, onDismiss: () -> Unit) {
    val repo = rememberRepository()
    val context = LocalContext.current
    val plans by repo.plans.collectAsStateWithLifecycle(initialValue = emptyList())
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zu welchem Plan?", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (plans.isEmpty()) {
                    Text("Du hast noch keinen Plan. Leg zuerst im Bereich „Training“ einen an.", style = MaterialTheme.typography.bodyLarge)
                }
                for (plan in plans) {
                    OutlinedButton(
                        onClick = {
                            repo.addExercises(plan.id, listOf(exercise))
                            Toast.makeText(context, "Zu „${plan.name}“ hinzugefügt", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                    ) { Text(plan.name, style = MaterialTheme.typography.titleSmall) }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Abbrechen", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
