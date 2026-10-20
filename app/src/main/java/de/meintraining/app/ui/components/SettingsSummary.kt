package de.meintraining.app.ui.components

import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.meintraining.app.data.MachineSettings

/** Her saved machine settings in large print, followed by her photos and videos. */
@Composable
fun SettingsSummary(
    settings: MachineSettings,
    onShowMedia: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (settings.isEmpty) {
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
                SettingValueRow(v.label, v.value, MaterialTheme.typography.headlineSmall)
            }
        }
        if (settings.note.isNotBlank()) {
            Text("Notiz: ${settings.note}", style = MaterialTheme.typography.bodyLarge)
        }
        if (settings.media.isNotEmpty()) {
            MediaStrip(settings.media, onOpen = onShowMedia, size = 120.dp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

/**
 * "Sitzhöhe ........ 4". The value may take at most a bit over half the width and wraps
 * beyond that. Without the limit a long value ("Schräger Metallgriff oder schwarzes
 * Seil") took the whole row, the label was squeezed to zero width, one letter per line,
 * and the card showed a tall blank box.
 */
@Composable
fun SettingValueRow(label: String, value: String, valueStyle: TextStyle, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val maxValue = maxWidth * 0.58f
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                value,
                style = valueStyle,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.End,
                modifier = Modifier
                    .padding(start = 8.dp)
                    .widthIn(max = maxValue),
            )
        }
    }
}
