package de.meintraining.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import de.meintraining.app.data.MachineMedia
import de.meintraining.app.ui.components.MissingFile
import de.meintraining.app.ui.components.VideoPlayer
import de.meintraining.app.ui.rememberRepository
import java.io.File
import kotlinx.coroutines.launch

/**
 * The photos and videos of one machine, full screen on black. Photos zoom with two
 * fingers; videos play on a loop. Big buttons step through them, swiping works too.
 */
@Composable
fun MediaViewerScreen(exerciseId: String, start: Int, onBack: () -> Unit) {
    val repo = rememberRepository()
    val settings by repo.settings(exerciseId).collectAsStateWithLifecycle(initialValue = null)
    val media = settings?.media ?: return
    if (media.isEmpty()) {
        onBack()
        return
    }
    val pager = rememberPagerState(initialPage = start.coerceIn(media.indices)) { media.size }
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                modifier = Modifier.heightIn(min = 56.dp),
            ) {
                Icon(Icons.Filled.Close, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Schließen", style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.weight(1f))
            if (media.size > 1) {
                Text("${pager.currentPage + 1} von ${media.size}", color = Color.White, style = MaterialTheme.typography.titleMedium)
            }
        }

        HorizontalPager(
            state = pager,
            key = { media[it].id },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) { page ->
            val m = media[page]
            val file = File(m.path)
            when {
                !file.exists() -> MissingFile(m.isVideo)
                m.isVideo -> VideoPlayer(m.path, active = pager.currentPage == page, modifier = Modifier.fillMaxSize())
                else -> ZoomablePhoto(m)
            }
        }

        val current = media[pager.currentPage.coerceIn(media.indices)]
        if (current.caption.isNotBlank()) {
            Text(
                current.caption,
                color = Color.White,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
        if (media.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            ) {
                val white = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } },
                    enabled = pager.currentPage > 0,
                    colors = white,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 60.dp),
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Vorheriges", style = MaterialTheme.typography.labelLarge)
                }
                OutlinedButton(
                    onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
                    enabled = pager.currentPage < media.lastIndex,
                    colors = white,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 60.dp),
                ) {
                    Text("Nächstes", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        } else {
            Spacer(Modifier.padding(bottom = 16.dp))
        }
    }
}

/** Pinch to zoom, double-tap to reset. */
@Composable
private fun ZoomablePhoto(media: MachineMedia) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { scale = 1f; offset = Offset.Zero })
            }
            .pointerInput(Unit) {
                // Claims the gesture only while pinching or zoomed in; a plain one-finger
                // swipe is left to the pager so it turns to the next photo.
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pinching = event.changes.count { it.pressed } > 1
                        if (pinching || scale > 1f) {
                            scale = (scale * event.calculateZoom()).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + event.calculatePan()
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
    ) {
        AsyncImage(
            model = File(media.path),
            contentDescription = media.caption.ifBlank { "Foto vom Gerät" },
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                },
        )
    }
}
