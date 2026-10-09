package de.meintraining.app.ui.screens

import android.content.ActivityNotFoundException
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.meintraining.app.TrainingApp
import de.meintraining.app.data.SettingValue
import de.meintraining.app.ui.NoInsets
import de.meintraining.app.ui.components.BackTopBar
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.BigOutlinedButton
import de.meintraining.app.ui.components.ConfirmDialog
import de.meintraining.app.ui.components.MachinePhoto
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.rememberRepository
import java.io.File
import kotlinx.coroutines.launch

/** What she is typing, kept separately until she taps "Speichern". */
private data class Draft(val values: List<SettingValue>, val note: String)

private val DraftSaver = listSaver<Draft?, String>(
    save = { d -> if (d == null) emptyList() else listOf(d.note) + d.values.flatMap { listOf(it.label, it.value) } },
    restore = { l ->
        if (l.isEmpty()) null
        else Draft(l.drop(1).chunked(2).map { (a, b) -> SettingValue(a, b) }, l[0])
    },
)

@Composable
fun SettingsEditScreen(exerciseId: String, onBack: () -> Unit, onShowPhoto: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TrainingApp
    val repo = rememberRepository()
    val all by repo.exercises.collectAsStateWithLifecycle()
    val exercise = all.firstOrNull { it.id == exerciseId }
    val saved by repo.settings(exerciseId).collectAsStateWithLifecycle(initialValue = null)
    var draft by rememberSaveable(stateSaver = DraftSaver) { mutableStateOf(null) }
    var askLeave by rememberSaveable { mutableStateOf(false) }
    var addingLabel by rememberSaveable { mutableStateOf(false) }
    var deletingPhoto by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var pendingCapture by rememberSaveable { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Start from what is saved, plus empty fields for the settings this machine usually has.
    LaunchedEffect(saved, exercise) {
        val s = saved ?: return@LaunchedEffect
        if (draft != null || exercise == null) return@LaunchedEffect
        val missing = exercise.settings.filter { label -> s.values.none { it.label == label } }
        draft = Draft(s.values + missing.map { SettingValue(it, "") }, s.note)
    }

    val dirty = draft != null && saved != null && run {
        val d = draft!!
        d.note.trim() != saved!!.note ||
            d.values.filter { it.value.isNotBlank() }.map { it.label to it.value.trim() } !=
            saved!!.filled.map { it.label to it.value }
    }

    fun save() {
        draft?.let { repo.saveSettings(exerciseId, it.note, it.values) }
        onBack()
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingCapture ?: return@rememberLauncherForActivityResult
        pendingCapture = null
        if (!ok) {
            File(path).delete()
            return@rememberLauncherForActivityResult
        }
        busy = true
        scope.launch {
            val stored = app.photos.importCapture(File(path), exerciseId)
            if (stored != null) repo.setPhoto(exerciseId, stored)
            else Toast.makeText(context, "Das Foto konnte nicht gespeichert werden.", Toast.LENGTH_LONG).show()
            busy = false
        }
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val stored = app.photos.importFromGallery(uri, exerciseId)
            if (stored != null) repo.setPhoto(exerciseId, stored)
            else Toast.makeText(context, "Das Bild konnte nicht übernommen werden.", Toast.LENGTH_LONG).show()
            busy = false
        }
    }
    fun capture() {
        val (file, uri) = app.photos.newCapture()
        pendingCapture = file.absolutePath
        try {
            takePicture.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingCapture = null
            app.photos.discard(file)
            Toast.makeText(context, "Auf diesem Handy wurde keine Kamera-App gefunden.", Toast.LENGTH_LONG).show()
        }
    }

    BackHandler(enabled = dirty) { askLeave = true }

    Scaffold(
        topBar = { BackTopBar("Einstellungen: ${exercise?.name.orEmpty()}", onBack = { if (dirty) askLeave = true else onBack() }) },
        contentWindowInsets = NoInsets,
        modifier = Modifier.imePadding(),
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 8.dp) {
                BigButton("Speichern", onClick = { save() }, icon = Icons.Filled.Check, modifier = Modifier.padding(16.dp))
            }
        },
    ) { padding ->
        val d = draft ?: return@Scaffold
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionCard(title = "Foto vom Gerät") {
                val photo = saved?.photoPath
                if (busy) {
                    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (photo != null) {
                    MachinePhoto(photo, onShowPhoto, Modifier.height(240.dp))
                    BigOutlinedButton("Neues Foto aufnehmen", onClick = { capture() }, icon = Icons.Filled.PhotoCamera)
                    TextButton(
                        onClick = { deletingPhoto = true },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text("Foto löschen", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.error)
                    }
                } else {
                    Text(
                        "Mach ein Foto vom Gerät – am besten so, dass man die Einstellungen sieht.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    BigButton("Foto aufnehmen", onClick = { capture() }, icon = Icons.Filled.PhotoCamera)
                    TextButton(
                        onClick = { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Bild aus der Galerie wählen", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }

            SectionCard(title = "Einstellungen") {
                Text(
                    "Trag ein, wie du das Gerät eingestellt hast. Leere Felder werden nicht angezeigt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                d.values.forEachIndexed { i, v ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = v.value,
                            onValueChange = { new ->
                                draft = d.copy(values = d.values.toMutableList().also { it[i] = v.copy(value = new) })
                            },
                            label = { Text(v.label) },
                            placeholder = { Text(placeholderFor(v.label)) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { draft = d.copy(values = d.values.toMutableList().also { it.removeAt(i) }) },
                            modifier = Modifier.size(56.dp),
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = "${v.label} entfernen", tint = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
                BigOutlinedButton("Weitere Einstellung", onClick = { addingLabel = true }, icon = Icons.Filled.Add)
            }

            SectionCard(title = "Notiz") {
                OutlinedTextField(
                    value = d.note,
                    onValueChange = { draft = d.copy(note = it) },
                    placeholder = { Text("z. B. Griff ganz unten nehmen") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    if (askLeave) {
        AlertDialog(
            onDismissRequest = { askLeave = false },
            title = { Text("Änderungen speichern?", style = MaterialTheme.typography.headlineSmall) },
            text = { Text("Du hast etwas geändert, aber noch nicht gespeichert.", style = MaterialTheme.typography.bodyLarge) },
            confirmButton = {
                Button(onClick = { askLeave = false; save() }, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text("Speichern", style = MaterialTheme.typography.labelLarge)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { askLeave = false; onBack() }, modifier = Modifier.heightIn(min = 52.dp)) {
                    Text("Nicht speichern", style = MaterialTheme.typography.labelLarge)
                }
            },
        )
    }
    if (addingLabel) {
        NewLabelDialog(
            onDismiss = { addingLabel = false },
            onAdd = { label ->
                addingLabel = false
                draft = draft?.let { it.copy(values = it.values + SettingValue(label, "")) }
            },
        )
    }
    if (deletingPhoto) {
        ConfirmDialog(
            title = "Foto löschen?",
            text = "Das Foto von diesem Gerät wird gelöscht.",
            confirm = "Löschen",
            onConfirm = {
                deletingPhoto = false
                repo.setPhoto(exerciseId, null)
            },
            onDismiss = { deletingPhoto = false },
        )
    }
}

private fun placeholderFor(label: String) = when {
    label.contains("Gewicht", ignoreCase = true) -> "z. B. 20 kg"
    label.contains("Stufe", ignoreCase = true) -> "z. B. 5"
    label.contains("Geschwindigkeit", ignoreCase = true) -> "z. B. 4,5 km/h"
    label.contains("Steigung", ignoreCase = true) -> "z. B. 2 %"
    else -> "z. B. 4"
}

@Composable
private fun NewLabelDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var label by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Weitere Einstellung", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Wie heißt die Einstellung?", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    singleLine = true,
                    placeholder = { Text("z. B. Griffposition") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onAdd(label.trim()) }, enabled = label.isNotBlank(), modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Hinzufügen", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Abbrechen", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
