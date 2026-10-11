package de.meintraining.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import de.meintraining.app.TrainingApp
import de.meintraining.app.data.MachineMedia
import de.meintraining.app.ui.components.BigButton
import de.meintraining.app.ui.components.BigOutlinedButton
import de.meintraining.app.ui.components.ConfirmDialog
import de.meintraining.app.ui.components.MediaTile
import de.meintraining.app.ui.components.SectionCard
import de.meintraining.app.ui.rememberRepository
import de.meintraining.app.ui.theme.Line
import java.io.File
import kotlinx.coroutines.launch

/** Camera video capture, stopped automatically after two minutes to keep files small. */
private class CaptureShortVideo : ActivityResultContracts.CaptureVideo() {
    override fun createIntent(context: Context, input: Uri): Intent =
        super.createIntent(context, input).putExtra(MediaStore.EXTRA_DURATION_LIMIT, 120)
}

/**
 * Photos and videos of a machine in the settings editor. Unlike the typed values they
 * are saved straight away, because the camera may take a while and she should not
 * lose a picture by forgetting to tap "Speichern".
 */
@Composable
fun MediaSection(
    exerciseId: String,
    media: List<MachineMedia>,
    /** Names of this machine's settings, offered as captions. */
    labels: List<String>,
    onOpen: (Int) -> Unit,
) {
    val context = LocalContext.current
    val app = context.applicationContext as TrainingApp
    val repo = rememberRepository()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var pendingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingVideo by rememberSaveable { mutableStateOf<String?>(null) }
    var captioning by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by rememberSaveable { mutableStateOf<Long?>(null) }

    fun toast(text: String) = Toast.makeText(context, text, Toast.LENGTH_LONG).show()

    val takePhoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val path = pendingPhoto ?: return@rememberLauncherForActivityResult
        pendingPhoto = null
        if (!ok) {
            File(path).delete()
            return@rememberLauncherForActivityResult
        }
        busy = true
        scope.launch {
            val stored = app.media.importPhotoCapture(File(path), exerciseId)
            if (stored != null) captioning = repo.addMedia(exerciseId, stored)
            else toast("Das Foto konnte nicht gespeichert werden.")
            busy = false
        }
    }
    val takeVideo = rememberLauncherForActivityResult(CaptureShortVideo()) { _ ->
        // Some camera apps report "cancelled" even after recording, so look at the file instead.
        val path = pendingVideo ?: return@rememberLauncherForActivityResult
        pendingVideo = null
        busy = true
        scope.launch {
            val stored = app.media.importVideoCapture(File(path), exerciseId)
            if (stored != null) captioning = repo.addMedia(exerciseId, stored)
            busy = false
        }
    }
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(10)) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            var added = 0
            for (uri in uris) {
                val stored = app.media.importFromGallery(uri, exerciseId) ?: continue
                repo.addMedia(exerciseId, stored)
                added++
            }
            busy = false
            when {
                added == 0 -> toast("Das konnte nicht übernommen werden.")
                added < uris.size -> toast("$added von ${uris.size} übernommen.")
            }
        }
    }

    fun launchCamera(video: Boolean) {
        val (file, uri) = if (video) app.media.newVideoCapture() else app.media.newPhotoCapture()
        if (video) pendingVideo = file.absolutePath else pendingPhoto = file.absolutePath
        try {
            if (video) takeVideo.launch(uri) else takePhoto.launch(uri)
        } catch (e: ActivityNotFoundException) {
            pendingPhoto = null
            pendingVideo = null
            app.media.discard(file)
            toast("Auf diesem Handy wurde keine Kamera-App gefunden.")
        }
    }

    SectionCard(title = "Fotos & Videos") {
        Text(
            if (media.isEmpty()) {
                "Mach Fotos von den Einstellungen – zum Beispiel vom Sitz und vom Beinpolster. " +
                    "In einem kurzen Video kann man zeigen, wie die Übung geht."
            } else {
                "Antippen zum Ansehen. Mit den Pfeilen änderst du die Reihenfolge."
            },
            style = MaterialTheme.typography.bodyLarge,
        )
        media.forEachIndexed { i, m ->
            MediaRow(
                media = m,
                canMoveUp = i > 0,
                canMoveDown = i < media.lastIndex,
                onOpen = { onOpen(i) },
                onMove = { repo.moveMedia(exerciseId, m.id, it) },
                onCaption = { captioning = m.id },
                onDelete = { deleting = m.id },
            )
        }
        if (busy) {
            Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        BigButton("Foto aufnehmen", onClick = { launchCamera(video = false) }, icon = Icons.Filled.PhotoCamera, enabled = !busy)
        BigOutlinedButton("Video aufnehmen", onClick = { if (!busy) launchCamera(video = true) }, icon = Icons.Filled.Videocam)
        TextButton(
            onClick = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)) },
            enabled = !busy,
            modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Aus der Galerie wählen", style = MaterialTheme.typography.labelLarge)
        }
    }

    media.firstOrNull { it.id == captioning }?.let { m ->
        CaptionDialog(
            media = m,
            suggestions = (
                if (m.isVideo) listOf("So geht die Übung") + labels + "Ganzes Gerät"
                else labels + listOf("Ganzes Gerät", "So geht die Übung")
            ).distinct(),
            onDismiss = { captioning = null },
            onSave = {
                repo.setCaption(m.id, it)
                captioning = null
            },
        )
    }
    media.firstOrNull { it.id == deleting }?.let { m ->
        ConfirmDialog(
            title = if (m.isVideo) "Video löschen?" else "Foto löschen?",
            text = if (m.isVideo) "Das Video wird vom Handy gelöscht." else "Das Foto wird vom Handy gelöscht.",
            confirm = "Löschen",
            onConfirm = {
                repo.deleteMedia(m.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun MediaRow(
    media: MachineMedia,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onOpen: () -> Unit,
    onMove: (Int) -> Unit,
    onCaption: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Line),
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MediaTile(media, size = 96.dp, onClick = onOpen)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (media.isVideo) "Video" else "Foto",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (media.caption.isBlank()) {
                        Text("Ohne Beschriftung", style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic)
                    } else {
                        Text(media.caption, style = MaterialTheme.typography.titleMedium)
                    }
                    TextButton(onClick = onCaption, modifier = Modifier.heightIn(min = 48.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Beschriften", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp),
            ) {
                FilledTonalIconButton(onClick = { onMove(-1) }, enabled = canMoveUp, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "Nach vorne", modifier = Modifier.size(32.dp))
                }
                FilledTonalIconButton(onClick = { onMove(1) }, enabled = canMoveDown, modifier = Modifier.size(52.dp)) {
                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Nach hinten", modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete, modifier = Modifier.size(52.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = if (media.isVideo) "Video löschen" else "Foto löschen",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }
    }
}

/** "Was zeigt das Foto?" — one tap on a suggestion, or free text. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CaptionDialog(media: MachineMedia, suggestions: List<String>, onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var text by rememberSaveable(media.id) { mutableStateOf(media.caption) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (media.isVideo) "Was zeigt das Video?" else "Was zeigt das Foto?", style = MaterialTheme.typography.headlineSmall) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (s in suggestions) {
                        SuggestionChip(
                            onClick = { text = s },
                            label = { Text(s, style = MaterialTheme.typography.labelLarge) },
                            modifier = Modifier.heightIn(min = 48.dp),
                        )
                    }
                }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = { Text("z. B. Sitzhöhe") },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(text) }, modifier = Modifier.heightIn(min = 52.dp)) {
                Text("Speichern", style = MaterialTheme.typography.labelLarge)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.heightIn(min = 52.dp)) {
                Text(if (media.caption.isBlank()) "Ohne Beschriftung" else "Abbrechen", style = MaterialTheme.typography.labelLarge)
            }
        },
    )
}
