package de.meintraining.app.ui.components

import android.media.MediaMetadataRetriever
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import de.meintraining.app.data.MachineMedia
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Photos and videos of a machine side by side, each with its caption. */
@Composable
fun MediaStrip(media: List<MachineMedia>, onOpen: (Int) -> Unit, modifier: Modifier = Modifier, size: Dp = 104.dp) {
    LazyRow(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        itemsIndexed(media, key = { _, m -> m.id }) { i, m ->
            Column(Modifier.width(size)) {
                MediaTile(m, size = size, onClick = { onOpen(i) })
                if (m.caption.isNotBlank()) {
                    Text(
                        m.caption,
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

/** Square picture of a photo or video; videos get a big play symbol. */
@Composable
fun MediaTile(media: MachineMedia, size: Dp, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val preview = media.preview?.let(::File)?.takeIf { it.exists() }
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF2B2F33))
            .clickable(onClickLabel = if (media.isVideo) "Video abspielen" else "Foto groß anzeigen", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (preview != null) {
            AsyncImage(
                model = preview,
                contentDescription = media.caption.ifBlank { if (media.isVideo) "Video" else "Foto vom Gerät" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize(),
            )
        } else if (!media.isVideo) {
            Icon(Icons.Filled.BrokenImage, contentDescription = "Foto fehlt", tint = Color.White)
        }
        if (media.isVideo) {
            Box(
                Modifier
                    .size(size * 0.42f)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(size * 0.3f))
            }
        }
    }
}

/**
 * Plays a video on a loop, with sound, as large as fits. [active] is false while the
 * page is not on screen, which pauses it. A big button pauses and resumes.
 */
@Composable
fun VideoPlayer(path: String, active: Boolean, modifier: Modifier = Modifier) {
    var playing by remember(path) { mutableStateOf(true) }
    var ratio by remember(path) { mutableFloatStateOf(0f) }
    LaunchedEffect(path) { ratio = withContext(Dispatchers.IO) { videoAspect(path) } }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            if (ratio > 0f) {
                AndroidView(
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoPath(path)
                            setOnPreparedListener { it.isLooping = true }
                        }
                    },
                    update = { v ->
                        if (active && playing) {
                            if (!v.isPlaying) v.start()
                        } else if (v.isPlaying) {
                            v.pause()
                        }
                    },
                    onRelease = { it.stopPlayback() },
                    modifier = Modifier.aspectRatio(ratio),
                )
            }
        }
        Button(
            onClick = { playing = !playing },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
            modifier = Modifier
                .padding(top = 12.dp)
                .heightIn(min = 56.dp),
        ) {
            Icon(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(28.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (playing) "Anhalten" else "Abspielen", style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Width / height as shown on screen, taking the recording's rotation into account. */
private fun videoAspect(path: String): Float = runCatching {
    val r = MediaMetadataRetriever()
    try {
        r.setDataSource(path)
        val w = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
        val h = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 0f
        val rot = r.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        if (w <= 0f || h <= 0f) 16f / 9f else if (rot % 180 != 0) h / w else w / h
    } finally {
        r.release()
    }
}.getOrDefault(16f / 9f)

/** Placeholder when a file has gone missing (e.g. videos are not part of phone backups). */
@Composable
fun MissingFile(isVideo: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            if (isVideo) "Dieses Video ist nicht mehr auf dem Handy." else "Dieses Foto ist nicht mehr auf dem Handy.",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
