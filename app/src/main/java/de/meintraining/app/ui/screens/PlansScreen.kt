package de.meintraining.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.meintraining.app.data.Plan
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.BigOutlinedButton
import de.meintraining.app.ui.components.ExerciseThumbnail
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.rememberRepository
import java.util.Calendar
import kotlinx.coroutines.launch

@Composable
fun PlansScreen(onStart: (Long) -> Unit, onEdit: (Long) -> Unit) {
    val repo = rememberRepository()
    val plans by repo.plans.collectAsStateWithLifecycle(initialValue = null)
    var showNew by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(Modifier.padding(top = 8.dp, bottom = 4.dp)) {
                Text(greeting(), style = MaterialTheme.typography.headlineLarge)
                Text(
                    "Welchen Plan trainierst du heute?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val list = plans
        if (list != null && list.isEmpty()) {
            item {
                SectionCard {
                    Text("Du hast noch keinen Trainingsplan.", style = MaterialTheme.typography.titleMedium)
                    Text("Leg einen Plan an und füge die Übungen hinzu, die du im Studio machst.", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
        items(list.orEmpty(), key = { it.id }) { plan ->
            PlanCard(plan, onStart = { onStart(plan.id) }, onEdit = { onEdit(plan.id) })
        }
        item {
            BigOutlinedButton("Neuen Plan anlegen", onClick = { showNew = true }, icon = Icons.Filled.Add)
        }
    }

    if (showNew) {
        NewPlanDialog(
            onDismiss = { showNew = false },
            onCreate = { name ->
                showNew = false
                scope.launch { onEdit(repo.createPlan(name)) }
            },
        )
    }
}

@Composable
private fun PlanCard(plan: Plan, onStart: () -> Unit, onEdit: () -> Unit) {
    SectionCard {
        Text(plan.name, style = MaterialTheme.typography.headlineSmall)
        val count = plan.items.size
        Text(
            when (count) {
                0 -> "Noch keine Übungen"
                1 -> "1 Übung · ca. ${plan.minutes} Minuten"
                else -> "$count Übungen · ca. ${plan.minutes} Minuten"
            },
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (count > 0) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 4.dp)) {
                plan.items.take(5).forEach { ExerciseThumbnail(it.exercise, size = 52.dp) }
                if (count > 5) {
                    Text(
                        "+${count - 5}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, top = 14.dp),
                    )
                }
            }
        }
        if (count > 0) {
            BigButton("Training starten", onClick = onStart, icon = Icons.Filled.PlayArrow)
        }
        BigOutlinedButton(if (count == 0) "Übungen hinzufügen" else "Plan bearbeiten", onClick = onEdit, icon = Icons.Filled.Edit)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NewPlanDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Plan", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Wie soll der Plan heißen?", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    placeholder = { Text("z. B. Tag A") },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Oder einen Vorschlag wählen:", style = MaterialTheme.typography.bodyMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in remember { listOf("Tag A", "Tag B", "Ganzkörper", "Beine & Po", "Oberkörper", "Rücken") }) {
                        SuggestionChip(
                            onClick = { name = s },
                            label = { Text(s, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name) }, enabled = name.isNotBlank(), modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Anlegen", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Abbrechen", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}

private fun greeting(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..10 -> "Guten Morgen!"
    in 11..17 -> "Guten Tag!"
    in 18..22 -> "Guten Abend!"
    else -> "Hallo!"
}
