package de.meintraining.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.meintraining.app.catalog.Kind
import de.meintraining.app.catalog.Muscle
import de.meintraining.app.data.MachineSettings
import de.meintraining.app.data.PlanItem
import de.meintraining.app.stats.formatKilograms
import de.meintraining.app.stats.kilograms
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.MuscleMap
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.theme.Accent
import de.meintraining.app.ui.theme.AccentLight
import de.meintraining.app.ui.theme.Teal
import de.meintraining.app.ui.theme.TealLight

/** What a finished workout achieved. */
class WorkoutStats(
    val completed: Int,
    val total: Int,
    val minutes: Int,
    /** Sum of sets × repetitions × weight over the ticked sets, in kg. */
    val movedKg: Double,
    val primary: Set<Muscle>,
    val secondary: Set<Muscle>,
)

fun workoutStats(items: List<PlanItem>, done: List<String>, settings: Map<String, MachineSettings>, minutes: Int): WorkoutStats {
    fun setsDone(item: PlanItem) =
        if (item.exercise.kind == Kind.CARDIO) (if ("${item.id}:0" in done) 1 else 0)
        else (0 until item.sets).count { "${item.id}:$it" in done }

    val trained = items.filter { setsDone(it) > 0 }
    val moved = items.filter { it.exercise.kind == Kind.REPS }.sumOf { item ->
        val perRep = settings[item.exercise.id]?.filled.orEmpty()
            .map { kilograms(it.label, it.value) }
            .firstOrNull { it > 0 } ?: 0.0
        setsDone(item) * item.amount * perRep
    }
    val primary = trained.flatMap { it.exercise.primary }.toSet()
    return WorkoutStats(
        completed = items.count { item ->
            if (item.exercise.kind == Kind.CARDIO) setsDone(item) > 0 else setsDone(item) >= item.sets
        },
        total = items.size,
        minutes = minutes,
        movedKg = moved,
        primary = primary,
        secondary = trained.flatMap { it.exercise.secondary }.toSet() - primary,
    )
}

private val PRAISE = listOf("Tolle Leistung!", "Gut gemacht!", "Weiter so!", "Stark gemacht!", "Darauf kannst du stolz sein!")
private val ENCOURAGE = listOf("Gut gemacht!", "Weiter so!", "Jedes Training zählt!", "Schön, dass du da warst!")

@Composable
fun WorkoutSummary(stats: WorkoutStats, onClose: () -> Unit) {
    val all = stats.total > 0 && stats.completed == stats.total
    // Chosen once, so the saying does not change when the phone is turned.
    val saying = rememberSaveable { (if (all) PRAISE else ENCOURAGE).random() }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFE0A100), modifier = Modifier.size(96.dp))
        Text(saying, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
        Text(
            if (all) "Training geschafft." else "Training beendet.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StatTile("Dauer", formatMinutes(stats.minutes), Modifier.weight(1f))
            StatTile("Übungen", "${stats.completed} von ${stats.total}", Modifier.weight(1f))
        }
        if (stats.movedKg > 0) {
            StatTile("Insgesamt bewegt", formatKilograms(stats.movedKg), Modifier.fillMaxWidth(), big = true)
        } else {
            Text(
                "Tipp: Trag bei den Geräten das Gewicht ein – dann siehst du hier, wie viel du insgesamt bewegt hast.",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
            )
        }

        if (stats.primary.isNotEmpty()) {
            SectionCard(title = "Das hast du heute trainiert") {
                Legend(Accent, "Stark: " + stats.primary.sortedBy { it.ordinal }.joinToString { it.title })
                if (stats.secondary.isNotEmpty()) {
                    Legend(AccentLight, "Etwas mit: " + stats.secondary.sortedBy { it.ordinal }.joinToString { it.title })
                }
                MuscleMap(primary = stats.primary, secondary = stats.secondary, modifier = Modifier.padding(top = 4.dp))
            }
        }

        BigButton("Fertig", onClick = onClose, icon = Icons.Filled.Check)
        Spacer(Modifier.size(8.dp))
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier, big: Boolean = false) {
    Column(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (big) Teal else TealLight)
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, color = if (big) Color.White else Teal)
        Text(
            value,
            style = if (big) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (big) Color.White else Teal,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun Legend(color: Color, text: String) {
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

private fun formatMinutes(m: Int): String = when {
    m < 60 -> "$m Min."
    m % 60 == 0 -> "${m / 60} Std."
    else -> "${m / 60} Std. ${m % 60} Min."
}
