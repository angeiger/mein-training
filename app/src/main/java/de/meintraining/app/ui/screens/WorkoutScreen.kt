package de.meintraining.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import de.meintraining.app.catalog.Kind
import de.meintraining.app.data.MachineSettings
import de.meintraining.app.data.PlanItem
import de.meintraining.app.ui.NoInsets
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.ConfirmDialog
import de.meintraining.app.ui.components.ExerciseAnimation
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.theme.Accent
import de.meintraining.app.ui.theme.Done
import de.meintraining.app.ui.theme.DoneLight
import de.meintraining.app.ui.theme.Line
import de.meintraining.app.ui.theme.Teal
import de.meintraining.app.ui.theme.TealLight
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun WorkoutScreen(
    planId: Long,
    onExit: () -> Unit,
    onEditSettings: (String) -> Unit,
    onShowPhoto: (String) -> Unit,
) {
    val vm: WorkoutViewModel = viewModel(factory = WorkoutViewModel.factory(planId))
    val plan by vm.plan.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val index by vm.index.collectAsStateWithLifecycle()
    val done by vm.done.collectAsStateWithLifecycle()
    val finished by vm.finished.collectAsStateWithLifecycle()
    val timer by vm.timer.collectAsStateWithLifecycle()
    var askExit by rememberSaveable { mutableStateOf(false) }

    // The phone must not lock itself between two sets.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }

    val items = plan?.items ?: return
    if (finished || items.isEmpty()) {
        Summary(
            completed = items.count { isComplete(it, done) },
            total = items.size,
            minutes = vm.minutesSoFar(),
            onClose = onExit,
        )
        return
    }
    BackHandler { askExit = true }

    val i = index.coerceIn(items.indices)
    val item = items[i]
    val last = i == items.lastIndex

    Scaffold(
        contentWindowInsets = NoInsets,
        topBar = { ProgressBar(i, items.size, onClose = { askExit = true }) },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 10.dp) {
                Column {
                    timer?.let { TimerBanner(it, onStop = vm::stopTimer) }
                    Row(
                        Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        OutlinedButton(
                            onClick = { vm.goTo(i - 1) },
                            enabled = i > 0,
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 64.dp),
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Zurück", style = MaterialTheme.typography.titleSmall)
                        }
                        Button(
                            onClick = { if (last) vm.finish() else vm.goTo(i + 1) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isComplete(item, done)) Done else MaterialTheme.colorScheme.primary,
                            ),
                            modifier = Modifier
                                .weight(1.4f)
                                .heightIn(min = 64.dp),
                        ) {
                            Text(if (last) "Training beenden" else "Weiter", style = MaterialTheme.typography.titleMedium)
                            if (!last) {
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        // A fresh scroll position for every exercise.
        key(item.id) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(item.exercise.name, style = MaterialTheme.typography.headlineMedium)
                MachineCard(
                    settings = settings[item.exercise.id] ?: MachineSettings(),
                    onEdit = { onEditSettings(item.exercise.id) },
                    onShowPhoto = { onShowPhoto(item.exercise.id) },
                )
                // Sets before the animation: the rest timer at the bottom must not cover them.
                if (item.exercise.kind == Kind.CARDIO) {
                    CardioCard(item, done, timer, onStartPause = { vm.cardioStartPause(item) }, onToggleDone = { vm.toggleCardioDone(item) })
                } else {
                    SetsCard(item, done, timer, onTap = { set -> vm.tapSet(item, set) })
                }
                ExerciseAnimation(item.exercise, height = 200.dp)
                if (item.exercise.steps.isNotEmpty()) HowTo(item)
            }
        }
    }

    if (askExit) {
        ConfirmDialog(
            title = "Training beenden?",
            text = "Du kannst den Plan jederzeit wieder von vorne starten.",
            confirm = "Beenden",
            dismiss = "Weiter trainieren",
            onConfirm = {
                askExit = false
                onExit()
            },
            onDismiss = { askExit = false },
        )
    }
}

private fun isComplete(item: PlanItem, done: List<String>): Boolean =
    if (item.exercise.kind == Kind.CARDIO) "${item.id}:0" in done
    else (0 until item.sets).all { "${item.id}:$it" in done }

@Composable
private fun ProgressBar(index: Int, total: Int, onClose: () -> Unit) {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Übung ${index + 1} von $total",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClose, modifier = Modifier.heightIn(min = 52.dp)) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Beenden", style = MaterialTheme.typography.labelLarge)
            }
        }
        LinearProgressIndicator(
            progress = { (index + 1) / total.toFloat() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp)),
            trackColor = Line,
            drawStopIndicator = {},
        )
    }
}

/** Photo and values of the machine, so she can set it up before starting. */
@Composable
private fun MachineCard(settings: MachineSettings, onEdit: () -> Unit, onShowPhoto: () -> Unit) {
    if (settings.isEmpty) {
        OutlinedButton(
            onClick = onEdit,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Geräte-Einstellungen eintragen", style = MaterialTheme.typography.labelLarge)
        }
        return
    }
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Deine Einstellungen",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEdit, modifier = Modifier.heightIn(min = 48.dp)) {
                Icon(Icons.Filled.Edit, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Ändern", style = MaterialTheme.typography.labelLarge)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            settings.photoPath?.let { path ->
                AsyncImage(
                    model = File(path),
                    contentDescription = "Foto vom Gerät, antippen zum Vergrößern",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(110.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.Black)
                        .clickable(onClick = onShowPhoto),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (v in settings.filled) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(v.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        Text(v.value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
                if (settings.note.isNotBlank()) {
                    Text(settings.note, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SetsCard(item: PlanItem, done: List<String>, timer: WorkoutTimer?, onTap: (Int) -> Unit) {
    val kind = item.exercise.kind
    val count = (0 until item.sets).count { "${item.id}:$it" in done }
    SectionCard {
        Text(
            "${item.sets} ${if (item.sets == 1) "Satz" else "Sätze"} × ${kind.amount(item.amount)}",
            style = MaterialTheme.typography.titleLarge,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (s in 0 until item.sets) {
                val isDone = "${item.id}:$s" in done
                val holding = timer is WorkoutTimer.Hold && timer.itemId == item.id && timer.set == s
                SetCircle(number = s + 1, done = isDone, active = holding, onClick = { onTap(s) })
            }
        }
        Text(
            when {
                count >= item.sets -> "Geschafft! Weiter zur nächsten Übung."
                kind == Kind.HOLD -> "Tippe auf einen Kreis: dann läuft die Zeit zum Halten."
                else -> "Tippe nach jedem Satz auf den Kreis."
            },
            style = MaterialTheme.typography.bodyLarge,
            color = if (count >= item.sets) Done else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (count >= item.sets) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun SetCircle(number: Int, done: Boolean, active: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(78.dp)
                .clip(CircleShape)
                .background(
                    when {
                        done -> Done
                        active -> Accent
                        else -> Color.White
                    },
                )
                .border(BorderStroke(3.dp, if (done) Done else if (active) Accent else Teal), CircleShape)
                .clickable(role = Role.Checkbox, onClick = onClick)
                .semantics { contentDescription = "Satz $number" + if (done) ", erledigt" else "" },
            contentAlignment = Alignment.Center,
        ) {
            when {
                done -> Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
                active -> Icon(Icons.Filled.Timer, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                else -> Text("$number", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Teal)
            }
        }
        Text("Satz $number", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun CardioCard(
    item: PlanItem,
    done: List<String>,
    timer: WorkoutTimer?,
    onStartPause: () -> Unit,
    onToggleDone: () -> Unit,
) {
    val t = timer as? WorkoutTimer.Cardio
    val mine = t?.itemId == item.id
    val total = item.amount * 60_000L
    val remaining = rememberRemaining(if (mine && t!!.running) t.endsAt else null) ?: if (mine) t!!.remainingMs else total
    val isDone = "${item.id}:0" in done

    SectionCard {
        Text(item.exercise.kind.amount(item.amount), style = MaterialTheme.typography.titleLarge)
        Text(
            formatTime(remaining),
            fontSize = 56.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDone) Done else Teal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (!isDone) {
            BigButton(
                text = when {
                    mine && t!!.running -> "Pause"
                    mine -> "Weiter"
                    else -> "Zeit starten"
                },
                onClick = onStartPause,
                icon = if (mine && t!!.running) Icons.Filled.Pause else Icons.Filled.PlayArrow,
            )
        }
        OutlinedButton(
            onClick = onToggleDone,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = if (isDone) DoneLight else Color.Transparent),
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = Done)
            Spacer(Modifier.width(8.dp))
            Text(if (isDone) "Erledigt" else "Als erledigt abhaken", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TimerBanner(timer: WorkoutTimer, onStop: () -> Unit) {
    val endsAt = when (timer) {
        is WorkoutTimer.Rest -> timer.endsAt
        is WorkoutTimer.Hold -> timer.endsAt
        is WorkoutTimer.Cardio -> if (timer.running) timer.endsAt else null
    }
    // Cardio has its own big clock in the card above.
    if (timer is WorkoutTimer.Cardio) return
    val remaining = rememberRemaining(endsAt) ?: 0L
    val isRest = timer is WorkoutTimer.Rest
    val bg = if (isRest) TealLight else Color(0xFFFFE3D9)
    val fg = if (isRest) Teal else Accent
    Column(
            Modifier
                .fillMaxWidth()
                .background(bg)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (isRest) "Pause" else "Halten!", style = MaterialTheme.typography.titleMedium, color = fg)
                    Text(formatTime(remaining), fontSize = 40.sp, fontWeight = FontWeight.Bold, color = fg)
                }
                Button(
                    onClick = onStop,
                    colors = ButtonDefaults.buttonColors(containerColor = fg),
                    modifier = Modifier.heightIn(min = 56.dp),
                ) {
                    Text(if (isRest) "Pause beenden" else "Abbrechen", style = MaterialTheme.typography.labelLarge)
                }
            }
            LinearProgressIndicator(
                progress = { 1f - remaining / timer.totalMs.toFloat() },
                color = fg,
                trackColor = Color.White,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
    }
}

@Composable
private fun HowTo(item: PlanItem) {
    var open by rememberSaveable(item.id) { mutableStateOf(false) }
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable { open = !open }
                .heightIn(min = 48.dp),
        ) {
            Text("So geht's", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            Text(if (open) "Zuklappen" else "Anzeigen", style = MaterialTheme.typography.labelLarge, color = Teal)
            Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = Teal)
        }
        if (open) {
            Steps(item.exercise.steps)
            item.exercise.tip?.let {
                Text("Wichtig: $it", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun Summary(completed: Int, total: Int, minutes: Int, onClose: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = Color(0xFFE0A100), modifier = Modifier.size(120.dp))
        val all = total > 0 && completed == total
        Text(
            if (all) "Geschafft!" else "Training beendet",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp),
        )
        Text(
            if (all) "Super gemacht." else "Schön, dass du da warst.",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            if (total == 0) "Dieser Plan hat keine Übungen." else "$completed von $total Übungen erledigt",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(if (minutes == 1) "Dauer: 1 Minute" else "Dauer: $minutes Minuten", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(32.dp))
        BigButton("Fertig", onClick = onClose, icon = Icons.Filled.Check)
    }
}

/** Milliseconds left until [endsAt], updated a few times per second; null when not counting. */
@Composable
private fun rememberRemaining(endsAt: Long?): Long? {
    if (endsAt == null) return null
    var now by remember { mutableLongStateOf(WorkoutViewModel.now()) }
    LaunchedEffect(endsAt) {
        while (true) {
            now = WorkoutViewModel.now()
            if (now >= endsAt) break
            delay(200)
        }
    }
    return (endsAt - now).coerceAtLeast(0)
}

private fun formatTime(ms: Long): String {
    val total = ((ms + 999) / 1000).toInt()
    return "%d:%02d".format(total / 60, total % 60)
}
