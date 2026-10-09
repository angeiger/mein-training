package de.meintraining.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import de.meintraining.app.catalog.Muscle
import de.meintraining.app.musclemap.BodyMap
import de.meintraining.app.musclemap.BodySide
import de.meintraining.app.ui.theme.Accent
import de.meintraining.app.ui.theme.AccentLight
import kotlin.math.min

private val Skin = Color(0xFFEFE9E1)
private val SkinEdge = Color(0xFFB3A89C)
private val MuscleIdle = Color(0xFFD9CCBD)

private class MapPaths(val outline: Path, val regions: Map<BodySide, List<Pair<Muscle, Path>>>)

private fun path(xs: FloatArray, ys: FloatArray) = Path().apply {
    for (i in xs.indices) if (i == 0) moveTo(xs[i], ys[i]) else lineTo(xs[i], ys[i])
    close()
}

/**
 * Front and back of a body side by side. [primary] muscles are drawn in the accent
 * colour, [secondary] ones lighter. With [onTap], tapping a muscle reports it.
 */
@Composable
fun MuscleMap(
    primary: Set<Muscle>,
    modifier: Modifier = Modifier,
    secondary: Set<Muscle> = emptySet(),
    onTap: ((Muscle) -> Unit)? = null,
) {
    val paths = remember {
        MapPaths(
            outline = path(BodyMap.outline.first, BodyMap.outline.second),
            regions = BodySide.entries.associateWith { side ->
                BodyMap.regions(side).map { it.muscle to path(it.xs, it.ys) }
            },
        )
    }
    val tap = rememberUpdatedState(onTap)
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        for (side in BodySide.entries) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    side.title,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(BodyMap.W / BodyMap.H)
                        .semantics {
                            contentDescription = "Muskelkarte ${side.title}" +
                                if (primary.isNotEmpty()) ": " + primary.joinToString { it.title } else ""
                        }
                        .then(
                            if (onTap == null) Modifier else Modifier.pointerInput(side) {
                                detectTapGestures { pos ->
                                    val s = min(size.width / BodyMap.W, size.height / BodyMap.H)
                                    val ox = (size.width - BodyMap.W * s) / 2
                                    val oy = (size.height - BodyMap.H * s) / 2
                                    // Generous tolerance: small muscles are hard to hit.
                                    val m = BodyMap.hit(side, (pos.x - ox) / s, (pos.y - oy) / s, tolerance = 12f)
                                    if (m != null) tap.value?.invoke(m)
                                }
                            },
                        ),
                ) {
                    val s = min(size.width / BodyMap.W, size.height / BodyMap.H)
                    translate((size.width - BodyMap.W * s) / 2, (size.height - BodyMap.H * s) / 2) {
                        withTransform({ scale(s, s, pivot = Offset.Zero) }) {
                            drawPath(paths.outline, Skin)
                            drawPath(paths.outline, SkinEdge, style = Stroke(width = 1.4f))
                            for ((muscle, p) in paths.regions.getValue(side)) {
                                val c = when (muscle) {
                                    in primary -> Accent
                                    in secondary -> AccentLight
                                    else -> MuscleIdle
                                }
                                drawPath(p, c)
                                drawPath(p, Color.White, style = Stroke(width = 1.6f, join = StrokeJoin.Round))
                            }
                            for (line in BodyMap.details.getValue(side)) {
                                for (i in 0 until line.size / 4) {
                                    drawLine(
                                        Color.White,
                                        Offset(line[i * 4], line[i * 4 + 1]),
                                        Offset(line[i * 4 + 2], line[i * 4 + 3]),
                                        strokeWidth = 1.4f,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
