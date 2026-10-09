package de.meintraining.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import de.meintraining.app.data.MachineSettings
import java.io.File

/** Her saved machine settings in large print, with the photo if there is one. */
@Composable
fun SettingsSummary(
    settings: MachineSettings,
    onShowPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    photoHeight: Dp = 200.dp,
    showPhoto: Boolean = true,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (showPhoto && settings.photoPath != null) {
            MachinePhoto(settings.photoPath, onShowPhoto, Modifier.height(photoHeight))
        }
        if (settings.filled.isEmpty() && settings.note.isBlank()) {
            Text(
                "Noch keine Einstellungen eingetragen.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        for (v in settings.filled) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(v.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                Text(v.value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
        }
        if (settings.note.isNotBlank()) {
            Text("Notiz: ${settings.note}", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
fun MachinePhoto(path: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Black)
            .clickable(onClickLabel = "Foto groß anzeigen", onClick = onClick),
    ) {
        AsyncImage(
            model = File(path),
            contentDescription = "Foto vom Gerät",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize(),
        )
        Row(
            Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.ZoomIn, contentDescription = null, tint = Color.White)
            Text(" Vergrößern", color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}
