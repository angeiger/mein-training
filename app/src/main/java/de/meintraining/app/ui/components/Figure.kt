package de.meintraining.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.meintraining.app.catalog.Exercise
import de.meintraining.app.figure.Bounds
import de.meintraining.app.figure.Dot2
import de.meintraining.app.figure.FigureRenderer
import de.meintraining.app.figure.Poly2
import de.meintraining.app.figure.Seg2
import de.meintraining.app.figure.Shape2
import de.meintraining.app.figure.Timing
import de.meintraining.app.ui.theme.Teal
import de.meintraining.app.ui.theme.TealLight
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin

private val FigureBackground = Color(0xFFFFFFFF)

/**
 * The looping movement of an exercise, with a short breathing cue underneath.
 * Working muscles are drawn in the accent colour.
 */
@Composable
fun ExerciseAnimation(exercise: Exercise, modifier: Modifier = Modifier, height: Dp = 240.dp) {
    val motion = exercise.motion
    if (motion == null) {
        NoAnimation(modifier.height(height))
        return
    }
    val bounds = remember(motion) { FigureRenderer.bounds(motion) }
    val highlight = remember(exercise.id) { exercise.highlight }
    val cycle = motion.timing.cycleMs
    val transition = rememberInfiniteTransition(label = "figure")
    val ms = transition.animateFloat(
        initialValue = 0f,
        targetValue = cycle.toFloat(),
        animationSpec = infiniteRepeatable(tween(cycle, easing = LinearEasing)),
        label = "time",
    )
    // Only the caption needs recomposing, and only when it changes.
    val caption by remember(motion) { derivedStateOf { motion.caption(ms.value) } }

    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(FigureBackground),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(height)
                .clipToBounds(),
        ) {
            val t = ms.value
            val (p, _) = motion.timing.at(t)
            val pulse = if (motion.timing is Timing.Still) 0.6f + 0.4f * sin(t / cycle * 2 * PI.toFloat()) else 1f
            drawFigure(FigureRenderer.frame(motion, p, highlight, pulse), bounds)
        }
        Text(
            caption,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = Teal,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .background(TealLight)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

/** A still picture of the exercise for lists. */
@Composable
fun ExerciseThumbnail(exercise: Exercise, modifier: Modifier = Modifier, size: Dp = 72.dp) {
    val motion = exercise.motion
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(14.dp))
            .background(FigureBackground),
        contentAlignment = Alignment.Center,
    ) {
        if (motion == null) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Teal, modifier = Modifier.size(size * 0.5f))
        } else {
            val shapes = remember(exercise.id) {
                FigureRenderer.frame(motion, motion.thumbP, exercise.highlight).filterNot { it.depth < -8_000f } // no floor shadow in small pictures
            }
            val bounds = remember(exercise.id) { FigureRenderer.bounds(motion) }
            Canvas(Modifier.size(size)) { drawFigure(shapes, bounds, padding = 0.04f) }
        }
    }
}

@Composable
private fun NoAnimation(modifier: Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FigureBackground),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Filled.FitnessCenter, contentDescription = null, tint = Teal, modifier = Modifier.size(56.dp))
            Text(
                "Für eigene Übungen gibt es keine Animation.\nEin Foto vom Gerät hilft beim Wiedererkennen.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

fun DrawScope.drawFigure(shapes: List<Shape2>, b: Bounds, padding: Float = 0.06f) {
    val s = min(size.width * (1 - 2 * padding) / b.width, size.height * (1 - 2 * padding) / b.height)
    val ox = (size.width - b.width * s) / 2 - b.minX * s
    val oy = (size.height - b.height * s) / 2 - b.minY * s
    for (sh in shapes) {
        val color = Color(sh.color)
        when (sh) {
            is Seg2 -> drawLine(
                color,
                Offset(ox + sh.x1 * s, oy + sh.y1 * s),
                Offset(ox + sh.x2 * s, oy + sh.y2 * s),
                strokeWidth = sh.w * s,
                cap = StrokeCap.Round,
            )
            is Dot2 -> drawCircle(color, sh.r * s, Offset(ox + sh.x * s, oy + sh.y * s))
            is Poly2 -> {
                val path = Path()
                for (i in sh.xs.indices) {
                    val x = ox + sh.xs[i] * s
                    val y = oy + sh.ys[i] * s
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close()
                drawPath(path, color)
                if (sh.stroke > 0) drawPath(path, color, style = Stroke(width = sh.stroke * s, join = StrokeJoin.Round))
            }
        }
    }
}
