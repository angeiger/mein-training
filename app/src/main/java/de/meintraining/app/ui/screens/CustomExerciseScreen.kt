package de.meintraining.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.meintraining.app.catalog.Kind
import de.meintraining.app.catalog.Muscle
import de.meintraining.app.ui.NoInsets
import de.meintraining.app.ui.components.BackTopBar
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.rememberRepository
import de.meintraining.app.ui.theme.Line
import de.meintraining.app.ui.theme.TealLight
import kotlinx.coroutines.launch

/** For a machine her gym has that is not in the catalogue. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CustomExerciseScreen(planId: Long?, onBack: () -> Unit, onCreated: (String) -> Unit) {
    val repo = rememberRepository()
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(Kind.REPS) }
    var muscles by rememberSaveable { mutableStateOf(listOf<String>()) }

    Scaffold(
        topBar = { BackTopBar("Eigene Übung", onBack) },
        contentWindowInsets = NoInsets,
        modifier = Modifier.imePadding(),
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                BigButton(
                    "Übung speichern",
                    enabled = name.isNotBlank(),
                    icon = Icons.Filled.Check,
                    modifier = Modifier.padding(16.dp),
                    onClick = {
                        scope.launch {
                            val id = repo.createCustomExercise(name, kind, muscles.map { Muscle.valueOf(it) }.toSet(), planId)
                            onCreated(id)
                        }
                    },
                )
            }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Für Geräte, die nicht in der Liste stehen. Ein Foto und deine Einstellungen kannst du danach eintragen.",
                style = MaterialTheme.typography.bodyLarge,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name der Übung") },
                placeholder = { Text("z. B. Klimmzug-Maschine") },
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier.fillMaxWidth(),
            )
            SectionCard(title = "Wie wird gezählt?") {
                KindOption("Wiederholungen", "z. B. 2 Sätze mit je 12 Wiederholungen", kind == Kind.REPS) { kind = Kind.REPS }
                KindOption("Halten", "z. B. 3 × 20 Sekunden", kind == Kind.HOLD) { kind = Kind.HOLD }
                KindOption("Minuten", "Ausdauer, z. B. 10 Minuten", kind == Kind.CARDIO) { kind = Kind.CARDIO }
            }
            SectionCard(title = "Welche Muskeln? (freiwillig)") {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (m in Muscle.entries) {
                        val on = m.name in muscles
                        FilterChip(
                            selected = on,
                            onClick = { muscles = if (on) muscles - m.name else muscles + m.name },
                            label = { Text(m.title, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KindOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = if (selected) TealLight else Color.White),
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else Line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(14.dp),
        ) {
            Icon(
                if (selected) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
