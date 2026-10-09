package de.meintraining.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.meintraining.app.catalog.Category
import de.meintraining.app.catalog.Exercise
import de.meintraining.app.ui.NoInsets
import de.meintraining.app.ui.components.BackTopBar
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.BigOutlinedButton
import de.meintraining.app.ui.components.ExerciseThumbnail
import de.meintraining.app.ui.rememberRepository
import de.meintraining.app.ui.theme.Done
import de.meintraining.app.ui.theme.DoneLight
import de.meintraining.app.ui.theme.Line

/** The "Übungen" tab: every exercise, grouped, with a search field. */
@Composable
fun CatalogScreen(onOpen: (String) -> Unit, onCreateOwn: () -> Unit) {
    val repo = rememberRepository()
    val all by repo.exercises.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Übungen", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 8.dp))
            Text(
                "Tippe auf eine Übung, um zu sehen, wie sie geht.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item { SearchField(query) { query = it } }
        exerciseGroups(filter(all, query)) { e ->
            ExerciseRow(e, onClick = { onOpen(e.id) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, modifier = Modifier.size(32.dp))
            }
        }
        item {
            BigOutlinedButton("Eigene Übung anlegen", onClick = onCreateOwn, icon = Icons.Filled.Add, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** Choosing exercises to add to a plan. Several can be ticked at once. */
@Composable
fun ExercisePickerScreen(planId: Long, onDone: () -> Unit, onCreateOwn: () -> Unit) {
    val repo = rememberRepository()
    val all by repo.exercises.collectAsStateWithLifecycle()
    val plan by repo.plan(planId).collectAsStateWithLifecycle(initialValue = null)
    var query by rememberSaveable { mutableStateOf("") }
    var selected by rememberSaveable { mutableStateOf(listOf<String>()) }
    val inPlan = plan?.items.orEmpty().map { it.exercise.id }.toSet()

    Scaffold(
        topBar = { BackTopBar("Übungen hinzufügen", onDone) },
        contentWindowInsets = NoInsets,
        bottomBar = {
            if (selected.isNotEmpty()) {
                Surface(color = Color.White, shadowElevation = 8.dp) {
                    BigButton(
                        if (selected.size == 1) "1 Übung hinzufügen" else "${selected.size} Übungen hinzufügen",
                        onClick = {
                            val byId = all.associateBy { it.id }
                            repo.addExercises(planId, selected.mapNotNull { byId[it] })
                            onDone()
                        },
                        icon = Icons.Filled.Add,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text(
                    "Tippe die Übungen an, die du hinzufügen möchtest.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
            item { SearchField(query) { query = it } }
            exerciseGroups(filter(all, query)) { e ->
                val isSelected = e.id in selected
                ExerciseRow(
                    exercise = e,
                    selected = isSelected,
                    note = if (e.id in inPlan) "Schon im Plan" else null,
                    onClick = { selected = if (isSelected) selected - e.id else selected + e.id },
                ) {
                    Icon(
                        if (isSelected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = if (isSelected) "Ausgewählt" else "Nicht ausgewählt",
                        tint = if (isSelected) Done else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp),
                    )
                }
            }
            item {
                BigOutlinedButton("Eigene Übung anlegen", onClick = onCreateOwn, icon = Icons.Filled.Add, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun filter(all: List<Exercise>, query: String): List<Exercise> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return all
    return all.filter { e ->
        e.name.lowercase().contains(q) ||
            e.aka?.lowercase()?.contains(q) == true ||
            (e.primary + e.secondary).any { it.title.lowercase().contains(q) }
    }
}

private fun LazyListScope.exerciseGroups(list: List<Exercise>, row: @Composable (Exercise) -> Unit) {
    if (list.isEmpty()) {
        item {
            Text(
                "Keine Übung gefunden.",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 16.dp),
            )
        }
        return
    }
    for (category in Category.entries) {
        val inGroup = list.filter { it.category == category }
        if (inGroup.isEmpty()) continue
        item(key = "header-${category.name}") {
            Text(
                category.title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 14.dp, bottom = 2.dp),
            )
        }
        items(inGroup, key = { it.id }) { row(it) }
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge,
        placeholder = { Text("Suchen, z. B. Bein oder Rücken") },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) { Icon(Icons.Filled.Clear, contentDescription = "Suche löschen") }
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ExerciseRow(
    exercise: Exercise,
    onClick: () -> Unit,
    selected: Boolean = false,
    note: String? = null,
    trailing: @Composable () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected) DoneLight else Color.White),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) Done else Line),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 84.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(10.dp),
        ) {
            ExerciseThumbnail(exercise, size = 72.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                if (exercise.primary.isNotEmpty()) {
                    Text(
                        exercise.muscleSummary,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (note != null) {
                    Text(note, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            trailing()
        }
    }
}
