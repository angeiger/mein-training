package de.meintraining.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.meintraining.app.catalog.Catalog
import de.meintraining.app.catalog.Muscle
import de.meintraining.app.ui.components.ExerciseThumbnail
import de.meintraining.app.ui.components.MuscleMap
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.rememberRepository
import de.meintraining.app.ui.theme.Accent
import de.meintraining.app.ui.theme.AccentLight
import de.meintraining.app.ui.theme.Line

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun MusclesScreen(onOpenExercise: (String) -> Unit) {
    val repo = rememberRepository()
    val all by repo.exercises.collectAsStateWithLifecycle()
    var selectedName by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = selectedName?.let { Muscle.valueOf(it) }
    val infoRequester = remember { BringIntoViewRequester() }
    val headerPx = with(LocalDensity.current) { 140.dp.toPx() }

    // Scroll just far enough that the top of the info card shows under the map.
    LaunchedEffect(selected) {
        if (selected != null) infoRequester.bringIntoView(Rect(0f, 0f, 1f, headerPx))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.padding(top = 8.dp)) {
            Text("Muskeln", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Tippe auf einen Muskel. Dann siehst du, was er macht und welche Übungen ihn trainieren.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        MuscleMap(
            primary = setOfNotNull(selected),
            onTap = { selectedName = if (it == selected) null else it.name },
        )

        if (selected != null) {
            Column(Modifier.bringIntoViewRequester(infoRequester)) {
                MuscleInfo(
                    muscle = selected,
                    exercises = Catalog.forMuscle(selected, all),
                    onClose = { selectedName = null },
                    onOpenExercise = onOpenExercise,
                )
            }
        }

        Text("Alle Muskelgruppen", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (m in Muscle.entries) {
                FilterChip(
                    selected = m == selected,
                    onClick = { selectedName = if (m == selected) null else m.name },
                    label = { Text(m.title, style = MaterialTheme.typography.labelLarge) },
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = Accent,
                        selectedLabelColor = Color.White,
                    ),
                )
            }
        }
    }
}

@Composable
private fun MuscleInfo(
    muscle: Muscle,
    exercises: List<de.meintraining.app.catalog.Exercise>,
    onClose: () -> Unit,
    onOpenExercise: (String) -> Unit,
) {
    SectionCard(containerColor = Color(0xFFFFF8F5)) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(muscle.title, style = MaterialTheme.typography.headlineMedium, color = Accent)
                Text(muscle.latin, style = MaterialTheme.typography.bodyMedium, fontStyle = FontStyle.Italic)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Schließen")
            }
        }
        Text("Was macht er?", style = MaterialTheme.typography.titleMedium)
        Text(muscle.what, style = MaterialTheme.typography.bodyLarge)
        Text("Im Alltag", style = MaterialTheme.typography.titleMedium)
        Text(muscle.everyday, style = MaterialTheme.typography.bodyLarge)
        Text("Übungen dafür", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
        if (exercises.isEmpty()) {
            Text("Keine Übung gefunden.", style = MaterialTheme.typography.bodyLarge)
        }
        for (e in exercises) {
            val main = muscle in e.primary
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Line),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onOpenExercise(e.id) }
                        .padding(8.dp),
                ) {
                    ExerciseThumbnail(e, size = 60.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (main) "trainiert ihn stark" else "trainiert ihn etwas mit",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (main) Accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = if (main) Accent else AccentLight,
                        modifier = Modifier.size(30.dp),
                    )
                }
            }
        }
    }
}
