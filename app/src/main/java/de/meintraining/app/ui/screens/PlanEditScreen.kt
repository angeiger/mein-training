package de.meintraining.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.meintraining.app.catalog.Kind
import de.meintraining.app.data.PlanItem
import de.meintraining.app.ui.NoInsets
import de.meintraining.app.ui.components.BackTopBar
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.ConfirmDialog
import de.meintraining.app.ui.components.ExerciseThumbnail
import de.meintraining.app.ui.components.Stepper
import de.meintraining.app.ui.rememberRepository
import de.meintraining.app.ui.theme.Line

@Composable
fun PlanEditScreen(
    planId: Long,
    onBack: () -> Unit,
    onAddExercises: () -> Unit,
    onOpenExercise: (String) -> Unit,
) {
    val repo = rememberRepository()
    val plan by repo.plan(planId).collectAsStateWithLifecycle(initialValue = null)
    var name by rememberSaveable { mutableStateOf<String?>(null) }
    var editing by rememberSaveable { mutableStateOf<Long?>(null) }
    var removing by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingPlan by rememberSaveable { mutableStateOf(false) }

    // Take the name from the database once; afterwards the text field is the source.
    LaunchedEffect(plan?.name) { if (name == null && plan != null) name = plan!!.name }

    Scaffold(
        topBar = { BackTopBar("Plan bearbeiten", onBack) },
        contentWindowInsets = NoInsets,
    ) { padding ->
        val p = plan ?: return@Scaffold
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = name.orEmpty(),
                    onValueChange = {
                        name = it
                        if (it.isNotBlank()) repo.renamePlan(planId, it.trim())
                    },
                    label = { Text("Name des Plans") },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.titleLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Column(Modifier.padding(top = 8.dp)) {
                    Text("Übungen in diesem Plan", style = MaterialTheme.typography.titleLarge)
                    if (p.items.size > 1) {
                        Text(
                            "Mit den Pfeilen änderst du die Reihenfolge.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (p.items.isEmpty()) {
                item {
                    Text(
                        "Noch keine Übungen. Tippe unten auf „Übung hinzufügen“.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            itemsIndexed(p.items, key = { _, it -> it.id }) { i, item ->
                PlanItemRow(
                    number = i + 1,
                    item = item,
                    canMoveUp = i > 0,
                    canMoveDown = i < p.items.lastIndex,
                    onMove = { dir -> repo.moveItem(planId, item.id, dir) },
                    onEdit = { editing = item.id },
                    onRemove = { removing = item.id },
                    onOpen = { onOpenExercise(item.exercise.id) },
                )
            }
            item {
                BigButton("Übung hinzufügen", onClick = onAddExercises, icon = Icons.Filled.Add, modifier = Modifier.padding(top = 8.dp))
            }
            item {
                TextButton(
                    onClick = { deletingPlan = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .padding(top = 16.dp),
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.width(8.dp))
                    Text("Diesen Plan löschen", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelLarge)
                }
            }
        }

        p.items.firstOrNull { it.id == editing }?.let { item ->
            ItemEditDialog(
                item = item,
                onDismiss = { editing = null },
                onSave = { sets, amount, rest ->
                    repo.updateItem(item.id, sets, amount, rest)
                    editing = null
                },
            )
        }
        p.items.firstOrNull { it.id == removing }?.let { item ->
            ConfirmDialog(
                title = "Übung entfernen?",
                text = "„${item.exercise.name}“ wird aus diesem Plan entfernt. Deine Einstellungen am Gerät bleiben gespeichert.",
                confirm = "Entfernen",
                onConfirm = {
                    repo.removeItem(item.id)
                    removing = null
                },
                onDismiss = { removing = null },
            )
        }
        if (deletingPlan) {
            ConfirmDialog(
                title = "Plan löschen?",
                text = "Der Plan „${p.name}“ wird gelöscht. Deine Einstellungen und Fotos an den Geräten bleiben erhalten.",
                confirm = "Löschen",
                onConfirm = {
                    deletingPlan = false
                    repo.deletePlan(planId)
                    onBack()
                },
                onDismiss = { deletingPlan = false },
            )
        }
    }
}

@Composable
private fun PlanItemRow(
    number: Int,
    item: PlanItem,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMove: (Int) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    onOpen: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(onClick = onOpen)
                    .padding(4.dp),
            ) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("$number", color = Color.White, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(10.dp))
                ExerciseThumbnail(item.exercise, size = 60.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.exercise.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (item.exercise.kind == Kind.CARDIO) item.summary
                        else "${item.summary} · ${item.restSeconds} Sek. Pause",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilledTonalIconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Nach oben", modifier = Modifier.size(32.dp))
                }
                FilledTonalIconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Nach unten", modifier = Modifier.size(32.dp))
                }
                OutlinedButton(onClick = onEdit, modifier = Modifier.heightIn(min = 52.dp).weight(1f)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Ändern", style = MaterialTheme.typography.labelLarge)
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Aus dem Plan entfernen",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ItemEditDialog(item: PlanItem, onDismiss: () -> Unit, onSave: (Int, Int, Int) -> Unit) {
    var sets by rememberSaveable { mutableIntStateOf(item.sets) }
    var amount by rememberSaveable { mutableIntStateOf(item.amount) }
    var rest by rememberSaveable { mutableIntStateOf(item.restSeconds) }
    val kind = item.exercise.kind
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.exercise.name, style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (kind) {
                    Kind.CARDIO -> Stepper("Dauer", amount, { amount = it }, unit = "Min.", min = 1, max = 120)
                    Kind.HOLD -> {
                        Stepper("Sätze", sets, { sets = it }, min = 1, max = 10)
                        Stepper("Halten", amount, { amount = it }, unit = "Sek.", step = 5, min = 5, max = 300)
                        Stepper("Pause danach", rest, { rest = it }, unit = "Sek.", step = 15, min = 0, max = 300)
                    }
                    Kind.REPS -> {
                        Stepper("Sätze", sets, { sets = it }, min = 1, max = 10)
                        Stepper("Wiederholungen", amount, { amount = it }, min = 1, max = 100)
                        Stepper("Pause danach", rest, { rest = it }, unit = "Sek.", step = 15, min = 0, max = 300)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(sets, amount, rest) }, modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Speichern", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Abbrechen", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
