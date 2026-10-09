package de.meintraining.app.musclemap

import de.meintraining.app.catalog.Muscle
import kotlin.math.sqrt

enum class BodySide(val title: String) { FRONT("Vorne"), BACK("Hinten") }

/** One tappable muscle area: a closed, smoothed polygon in map units. */
class Region(val muscle: Muscle, val side: BodySide, val xs: FloatArray, val ys: FloatArray)

/**
 * Front and back view of a body with its main muscle groups, in a 200 × 440 box.
 * Shapes are described for the right half of the drawing and mirrored.
 * Pure Kotlin, shared with the desktop preview renderer.
 */
object BodyMap {
    const val W = 200f
    const val H = 440f

    // Right half of the silhouette, from the top of the head round to the crotch.
    private val halfOutline = pts(
        100, 6, 112, 9, 120, 18, 123, 32, 121, 47, 117, 57, 111, 65,
        110, 72, 112, 80,
        126, 85, 142, 89, 153, 93,
        161, 101, 165, 115, 166, 132,
        167, 152, 168, 172, 169, 186,
        173, 205, 176, 228, 176, 250, 174, 264,
        178, 274, 180, 288, 177, 300, 171, 306, 165, 302, 162, 290,
        161, 272, 160, 262,
        156, 240, 152, 214, 150, 194,
        148, 172, 147, 152, 145, 138,
        143, 150, 141, 172, 138, 196, 136, 210,
        140, 226, 145, 244, 148, 262,
        148, 284, 146, 306, 142, 326, 139, 342,
        138, 352, 141, 368, 141, 384, 137, 400, 133, 414,
        136, 424, 136, 432, 128, 436, 116, 434,
        113, 424, 114, 412, 112, 398, 110, 378, 110, 360, 111, 346,
        110, 330, 108, 306, 105, 284, 100, 268,
    )

    val outline: Pair<FloatArray, FloatArray> = run {
        val right = halfOutline
        // Mirror without repeating the points on the centre line.
        val left = right.subList(1, right.size - 1).reversed().map { (x, y) -> (W - x) to y }
        smooth(right + left)
    }

    private val front = listOf(
        Muscle.BRUST to pts(102, 94, 118, 91, 134, 93, 141, 99, 144, 112, 141, 126, 132, 137, 118, 142, 103, 140),
        Muscle.SCHULTERN to pts(143, 92, 153, 94, 160, 101, 164, 114, 164, 130, 158, 140, 151, 132, 147, 116, 145, 102),
        Muscle.BIZEPS to pts(150, 141, 158, 142, 164, 152, 166, 168, 163, 182, 156, 186, 151, 172, 149, 156),
        Muscle.UNTERARME to pts(153, 196, 164, 194, 171, 210, 174, 232, 172, 254, 165, 258, 159, 238, 155, 216),
        Muscle.NACKEN to pts(111, 76, 124, 84, 140, 89, 128, 91, 113, 86),
        Muscle.BAUCH to pts(102, 146, 118, 146, 122, 160, 122, 190, 120, 220, 114, 244, 102, 250),
        Muscle.SEITL_BAUCH to pts(124, 148, 134, 142, 139, 160, 137, 186, 133, 206, 137, 224, 128, 236, 122, 230, 125, 200, 126, 172),
        Muscle.ABDUKTOREN to pts(138, 232, 144, 242, 146, 258, 141, 256, 136, 244),
        Muscle.QUADRIZEPS to pts(112, 276, 128, 262, 140, 259, 146, 272, 145, 296, 140, 320, 133, 336, 122, 338, 115, 326, 112, 300),
        Muscle.ADDUKTOREN to pts(103, 271, 110, 272, 112, 296, 114, 318, 111, 324, 108, 306, 105, 284),
        Muscle.WADEN to pts(137, 356, 140, 368, 139, 384, 135, 392, 134, 372),
        Muscle.WADEN to pts(112, 358, 114, 368, 114, 386, 112, 392, 111, 374),
    )

    private val back = listOf(
        Muscle.NACKEN to pts(100, 64, 110, 70, 118, 80, 134, 87, 144, 92, 136, 100, 120, 112, 110, 132, 104, 152, 100, 164),
        Muscle.SCHULTERN to pts(144, 95, 154, 95, 161, 103, 165, 116, 164, 132, 158, 140, 152, 128, 146, 110),
        Muscle.TRIZEPS to pts(149, 141, 159, 142, 165, 152, 167, 168, 164, 184, 156, 188, 151, 172, 149, 156),
        Muscle.UNTERARME to pts(153, 196, 164, 194, 171, 210, 174, 232, 172, 254, 165, 258, 159, 238, 155, 216),
        Muscle.LAT to pts(110, 140, 120, 116, 136, 102, 146, 108, 146, 128, 141, 152, 138, 178, 130, 200, 118, 214, 115, 206, 115, 180),
        Muscle.UNTERER_RUECKEN to pts(102, 168, 108, 166, 111, 186, 112, 206, 114, 226, 110, 238, 102, 240),
        Muscle.ABDUKTOREN to pts(118, 226, 130, 219, 138, 222, 143, 238, 140, 248, 130, 240, 120, 236),
        Muscle.GESAESS to pts(102, 246, 116, 241, 130, 243, 142, 252, 146, 268, 142, 286, 128, 294, 112, 292, 102, 286),
        Muscle.BEINBEUGER to pts(108, 298, 124, 298, 140, 296, 143, 312, 140, 330, 134, 344, 122, 346, 113, 338, 110, 318),
        Muscle.WADEN to pts(113, 352, 124, 348, 136, 352, 140, 366, 139, 384, 133, 398, 122, 398, 114, 392, 111, 374),
    )

    val regions: List<Region> = buildList {
        for ((side, list) in listOf(BodySide.FRONT to front, BodySide.BACK to back)) {
            for ((muscle, half) in list) {
                val (xs, ys) = smooth(half)
                add(Region(muscle, side, xs, ys))
                add(Region(muscle, side, FloatArray(xs.size) { W - xs[it] }, ys))
            }
        }
    }

    /** Thin decorative lines (x1, y1, x2, y2, …) drawn over the muscles. */
    val details: Map<BodySide, List<FloatArray>> = mapOf(
        BodySide.FRONT to listOf(
            floatArrayOf(100f, 146f, 100f, 250f),
            floatArrayOf(80f, 170f, 120f, 170f),
            floatArrayOf(79f, 194f, 121f, 194f),
            floatArrayOf(81f, 218f, 119f, 218f),
        ),
        BodySide.BACK to listOf(
            floatArrayOf(100f, 150f, 100f, 242f),
        ),
    )

    fun regions(side: BodySide) = regions.filter { it.side == side }

    /** The muscle under ([x], [y]), or the nearest one within [tolerance] map units. */
    fun hit(side: BodySide, x: Float, y: Float, tolerance: Float = 8f): Muscle? {
        val candidates = regions(side)
        candidates.firstOrNull { contains(it.xs, it.ys, x, y) }?.let { return it.muscle }
        var best: Muscle? = null
        var bestD = tolerance
        for (r in candidates) {
            val d = distance(r.xs, r.ys, x, y)
            if (d < bestD) { bestD = d; best = r.muscle }
        }
        return best
    }

    // ---- geometry -----------------------------------------------------------------

    private fun pts(vararg c: Int): List<Pair<Float, Float>> =
        c.toList().chunked(2) { (x, y) -> x.toFloat() to y.toFloat() }

    /** Closed Catmull-Rom spline through the points, sampled as a polygon. */
    private fun smooth(p: List<Pair<Float, Float>>, steps: Int = 6): Pair<FloatArray, FloatArray> {
        val n = p.size
        val xs = FloatArray(n * steps)
        val ys = FloatArray(n * steps)
        for (i in 0 until n) {
            val p0 = p[(i - 1 + n) % n]
            val p1 = p[i]
            val p2 = p[(i + 1) % n]
            val p3 = p[(i + 2) % n]
            for (s in 0 until steps) {
                val t = s / steps.toFloat()
                val t2 = t * t
                val t3 = t2 * t
                fun cr(a: Float, b: Float, c: Float, d: Float) =
                    0.5f * (2 * b + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3)
                xs[i * steps + s] = cr(p0.first, p1.first, p2.first, p3.first)
                ys[i * steps + s] = cr(p0.second, p1.second, p2.second, p3.second)
            }
        }
        return xs to ys
    }

    fun contains(xs: FloatArray, ys: FloatArray, x: Float, y: Float): Boolean {
        var inside = false
        var j = xs.size - 1
        for (i in xs.indices) {
            if ((ys[i] > y) != (ys[j] > y) && x < (xs[j] - xs[i]) * (y - ys[i]) / (ys[j] - ys[i]) + xs[i]) {
                inside = !inside
            }
            j = i
        }
        return inside
    }

    private fun distance(xs: FloatArray, ys: FloatArray, x: Float, y: Float): Float {
        var best = Float.MAX_VALUE
        var j = xs.size - 1
        for (i in xs.indices) {
            val ax = xs[j]; val ay = ys[j]; val bx = xs[i]; val by = ys[i]
            val dx = bx - ax; val dy = by - ay
            val len2 = dx * dx + dy * dy
            val t = if (len2 == 0f) 0f else (((x - ax) * dx + (y - ay) * dy) / len2).coerceIn(0f, 1f)
            val px = ax + t * dx - x
            val py = ay + t * dy - y
            best = minOf(best, sqrt(px * px + py * py))
            j = i
        }
        return best
    }
}
