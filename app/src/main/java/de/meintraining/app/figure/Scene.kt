package de.meintraining.app.figure

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Body parts that can light up to show which muscles are working. */
enum class Part { TORSO, PELVIS, UPPER_ARM, FOREARM, THIGH, SHIN }

object Palette {
    const val BODY = 0xFF46607EL
    const val HAIR = 0xFF2C3E55L
    const val ACCENT = 0xFFE4572EL
    const val FRAME = 0xFF4B535CL
    const val PAD = 0xFF7D8590L
    const val METAL = 0xFFB9C1C9L
    const val PLATE = 0xFF33383EL
    const val CABLE = 0xFF2B2F33L
    const val FLOOR = 0xFFE4E0D8L
    const val MAT = 0xFF8FB7A6L
    const val WALL = 0xFFD9D4CBL
    /** Controls she has to operate herself, such as a foot lever. */
    const val CONTROL = 0xFF0E8C7EL
}

// ---- 3D primitives -------------------------------------------------------------

sealed class Prim {
    /** Pushes a primitive towards the back (negative) or front when depths tie. */
    abstract val bias: Float
}

/** A rod with round ends: limbs, tubes, cables. */
data class Cap(
    val a: V3, val b: V3, val w: Float, val color: Long,
    override val bias: Float = 0f, val shade: Boolean = false,
) : Prim()

data class Ball(
    val c: V3, val r: Float, val color: Long,
    override val bias: Float = 0f, val shade: Boolean = false,
) : Prim()

/** A cuboid, optionally tilted by [pitch] around z (like a backrest) and turned by [yaw]. */
data class Box(
    val c: V3, val size: V3, val color: Long,
    val pitch: Float = 0f, val yaw: Float = 0f,
    override val bias: Float = 0f,
) : Prim()

/** A flat round plate, e.g. a weight plate or the floor shadow. */
data class Disc(
    val c: V3, val normal: V3, val r: Float, val color: Long,
    override val bias: Float = 0f,
) : Prim()

/** The convex outline of a set of points, with rounded corners. Used for the trunk. */
data class Hull(
    val pts: List<V3>, val color: Long, val round: Float,
    override val bias: Float = 0f, val shade: Boolean = false,
) : Prim()

// ---- 2D output ------------------------------------------------------------------

sealed class Shape2 {
    abstract val depth: Float
    abstract val color: Long
}

data class Seg2(
    val x1: Float, val y1: Float, val x2: Float, val y2: Float, val w: Float,
    override val color: Long, override val depth: Float,
) : Shape2()

data class Dot2(
    val x: Float, val y: Float, val r: Float,
    override val color: Long, override val depth: Float,
) : Shape2()

/** A filled polygon; [stroke] > 0 also strokes it with round joins in the same colour. */
class Poly2(
    val xs: FloatArray, val ys: FloatArray, val stroke: Float,
    override val color: Long, override val depth: Float,
) : Shape2()

class Bounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float) {
    val width get() = maxX - minX
    val height get() = maxY - minY
}

// ---- Motion -----------------------------------------------------------------------

enum class Phase { FORWARD, BACK, LOOP }

sealed class Timing {
    abstract val cycleMs: Int

    /** Goes from pose 0 to pose 1 and back, with short holds at both ends. */
    data class PingPong(
        val forwardMs: Int = 1500, val backMs: Int = 2000,
        val holdStartMs: Int = 500, val holdEndMs: Int = 400,
    ) : Timing() {
        override val cycleMs get() = forwardMs + backMs + holdStartMs + holdEndMs
    }

    /** A continuous loop such as pedalling. */
    data class Loop(val periodMs: Int) : Timing() {
        override val cycleMs get() = periodMs
    }

    /** No movement at all, for holding exercises such as the plank. */
    data object Still : Timing() {
        override val cycleMs get() = 3000
    }

    /** Progress 0..1 and the current phase at [ms] into the cycle. */
    fun at(ms: Float): Pair<Float, Phase> {
        val t = ((ms % cycleMs) + cycleMs) % cycleMs
        return when (this) {
            is PingPong -> {
                var r = t
                if (r < holdStartMs) return 0f to Phase.FORWARD
                r -= holdStartMs
                if (r < forwardMs) return ease(r / forwardMs) to Phase.FORWARD
                r -= forwardMs
                if (r < holdEndMs) return 1f to Phase.BACK
                r -= holdEndMs
                (1f - ease(r / backMs)) to Phase.BACK
            }
            is Loop -> t / periodMs to Phase.LOOP
            Still -> 0f to Phase.LOOP
        }
    }
}

/**
 * Everything needed to draw one exercise: the camera, how the body moves, and the
 * equipment around it. Both [pose] and [props] take the progress p (0..1).
 */
class Motion(
    val camera: Camera = Camera(),
    val timing: Timing = Timing.PingPong(),
    /** What to say while moving towards pose 1, and while returning. */
    val captions: Pair<String, String> = "Anspannen – ausatmen" to "Langsam zurück – einatmen",
    /** Progress used for the still picture in lists. */
    val thumbP: Float = 1f,
    /** Floor shadow centre; null hides the floor (e.g. for exercises on a mat). */
    val floorX: Float? = 0f,
    /** For a [Script]ed loop: the caption at progress t, overriding [captions]. */
    val captionAt: ((Float) -> String)? = null,
    val pose: (Float) -> Pose,
    val props: (Float, Skeleton) -> List<Prim> = { _, _ -> emptyList() },
) {
    /** The cue to show [ms] milliseconds into the cycle. */
    fun caption(ms: Float): String {
        val (p, phase) = timing.at(ms)
        captionAt?.let { return it(p) }
        return when (phase) {
            Phase.FORWARD -> captions.first
            Phase.BACK -> captions.second
            Phase.LOOP -> captions.first
        }
    }
}

/**
 * A choreographed loop for machines that need more than "there and back", such as
 * setting the machine up before the first repetition. Each step moves a few values
 * (angles, how far a pedal is pressed, …) to new targets and has its own caption.
 */
class Script(start: FloatArray, vararg steps: Step) {
    class Step(val ms: Int, val caption: String, val to: FloatArray)

    private val starts = ArrayList<FloatArray>()
    private val steps = steps.toList()
    val totalMs: Int = steps.sumOf { it.ms }

    init {
        var current = start
        for (s in steps) {
            starts += current
            current = s.to
        }
    }

    private fun index(t: Float): Pair<Int, Float> {
        var ms = (((t % 1f) + 1f) % 1f) * totalMs
        for ((i, s) in steps.withIndex()) {
            if (ms < s.ms || i == steps.lastIndex) return i to (ms / s.ms).coerceIn(0f, 1f)
            ms -= s.ms
        }
        return steps.lastIndex to 1f
    }

    /** Values at progress [t] (0..1 over the whole loop), eased within each step. */
    fun at(t: Float): FloatArray {
        val (i, f) = index(t)
        val a = starts[i]
        val b = steps[i].to
        val e = ease(f)
        return FloatArray(a.size) { lerp(a[it], b[it], e) }
    }

    fun caption(t: Float): String = steps[index(t).first].caption

    companion object {
        fun step(ms: Int, caption: String, vararg to: Float) = Step(ms, caption, to)
    }
}

// ---- Scene building -----------------------------------------------------------------

object FigureRenderer {

    fun frame(motion: Motion, p: Float, highlight: Set<Part>, pulse: Float = 1f): List<Shape2> {
        val sk = solve(motion.pose(p))
        val prims = ArrayList<Prim>(48)
        motion.floorX?.let { prims += Disc(v(it, 0, 0), V3.UP, 105f, Palette.FLOOR, bias = -10_000f) }
        prims += motion.props(p, sk)
        body(sk, highlight, pulse, prims)
        return project(prims, motion.camera)
    }

    /** Bounds over the whole movement, so the drawing never jumps or gets clipped. */
    fun bounds(motion: Motion): Bounds {
        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE
        val steps = 12
        for (i in 0..steps) {
            val p = i / steps.toFloat()
            val sk = solve(motion.pose(p))
            val prims = ArrayList<Prim>()
            prims += motion.props(p, sk)
            body(sk, emptySet(), 1f, prims)
            for (s in project(prims, motion.camera)) {
                when (s) {
                    is Seg2 -> {
                        val h = s.w / 2
                        minX = min(minX, min(s.x1, s.x2) - h); maxX = max(maxX, max(s.x1, s.x2) + h)
                        minY = min(minY, min(s.y1, s.y2) - h); maxY = max(maxY, max(s.y1, s.y2) + h)
                    }
                    is Dot2 -> {
                        minX = min(minX, s.x - s.r); maxX = max(maxX, s.x + s.r)
                        minY = min(minY, s.y - s.r); maxY = max(maxY, s.y + s.r)
                    }
                    is Poly2 -> {
                        val h = s.stroke / 2
                        for (k in s.xs.indices) {
                            minX = min(minX, s.xs[k] - h); maxX = max(maxX, s.xs[k] + h)
                            minY = min(minY, s.ys[k] - h); maxY = max(maxY, s.ys[k] + h)
                        }
                    }
                }
            }
            if (motion.timing is Timing.Still) break
        }
        // Keep the floor line in view for standing exercises.
        motion.floorX?.let { maxY = max(maxY, motion.camera.project(v(it, 0, 0))[1] + 6f) }
        return Bounds(minX, minY, maxX, maxY)
    }

    private fun body(sk: Skeleton, hl: Set<Part>, pulse: Float, out: MutableList<Prim>) {
        fun c(part: Part) = if (part in hl) mix(Palette.BODY, Palette.ACCENT, pulse) else Palette.BODY
        val f = sk.frame
        val (sR, sL) = sk.shoulder
        val (pR, pL) = sk.hip

        // Trunk: a rounded hull from shoulders to waist.
        val trunk = listOf(
            sR + f.up * 3f, sL + f.up * 3f,
            sk.neck - f.up * 16f + f.fwd * 10f + f.right * 8f,
            sk.neck - f.up * 16f + f.fwd * 10f - f.right * 8f,
            sk.neck - f.up * 14f - f.fwd * 8f + f.right * 10f,
            sk.neck - f.up * 14f - f.fwd * 8f - f.right * 10f,
            sk.pelvis + f.up * 18f + f.fwd * 6f + f.right * 12f,
            sk.pelvis + f.up * 18f + f.fwd * 6f - f.right * 12f,
            sk.pelvis + f.up * 18f - f.fwd * 6f,
            sk.pelvis + f.up * 8f + f.right * 11f,
            sk.pelvis + f.up * 8f - f.right * 11f,
        )
        out += Hull(trunk, c(Part.TORSO), 9f, shade = true)
        out += Cap(pL, pR, 25f, c(Part.PELVIS), bias = 0.5f, shade = true)

        out += Cap(sk.neck - f.up * 2f, sk.head, 10f, Palette.BODY, bias = -1f, shade = true)
        out += Ball(sk.head, Body.HEAD_R, Palette.BODY, bias = 1f, shade = true)
        // Hair on the back of the head and a nose, so you can tell which way the person faces.
        out += Ball(sk.head - f.fwd * 3f + f.up * 2.5f, Body.HEAD_R - 2f, Palette.HAIR, bias = 0.8f)
        out += Ball(sk.head + f.fwd * (Body.HEAD_R - 0.5f), 2.4f, Palette.BODY, bias = 0.9f)

        for (side in 0..1) {
            val s = if (side == 0) sR else sL
            val e = if (side == 0) sk.elbow.first else sk.elbow.second
            val h = if (side == 0) sk.hand.first else sk.hand.second
            val hip = if (side == 0) pR else pL
            val k = if (side == 0) sk.knee.first else sk.knee.second
            val a = if (side == 0) sk.ankle.first else sk.ankle.second
            val heel = if (side == 0) sk.heel.first else sk.heel.second
            val toe = if (side == 0) sk.toe.first else sk.toe.second

            out += Cap(s, e, 12f, c(Part.UPPER_ARM), shade = true)
            out += Cap(e, h, 10f, c(Part.FOREARM), shade = true)
            out += Ball(h, 5.5f, Palette.BODY, bias = 0.3f, shade = true)
            out += Cap(hip, k, 17f, c(Part.THIGH), shade = true)
            out += Cap(k, a, 13f, c(Part.SHIN), shade = true)
            out += Cap(heel, toe, 8f, Palette.BODY, bias = -0.2f, shade = true)
        }
    }

    fun project(prims: List<Prim>, cam: Camera): List<Shape2> {
        // Depth range of shaded parts, to darken whatever is further away.
        var dMin = Float.MAX_VALUE
        var dMax = -Float.MAX_VALUE
        for (p in prims) {
            val d = when (p) {
                is Cap -> if (p.shade) cam.project((p.a + p.b) * 0.5f)[2] else null
                is Ball -> if (p.shade) cam.project(p.c)[2] else null
                else -> null
            } ?: continue
            dMin = min(dMin, d); dMax = max(dMax, d)
        }
        fun shaded(color: Long, depth: Float, on: Boolean): Long {
            if (!on || dMax - dMin < 1f) return color
            val t = ((depth - dMin) / (dMax - dMin)).coerceIn(0f, 1f)
            return darken(color, 0.22f * (1f - t))
        }

        val out = ArrayList<Shape2>(prims.size + 16)
        for (p in prims) when (p) {
            is Cap -> {
                // Long rods are split so each piece sorts correctly against the body.
                val len = (p.b - p.a).length
                val n = if (p.shade) 1 else max(1, (len / 22f).toInt())
                for (i in 0 until n) {
                    val a = lerp(p.a, p.b, i / n.toFloat())
                    val b = lerp(p.a, p.b, (i + 1) / n.toFloat())
                    val pa = cam.project(a)
                    val pb = cam.project(b)
                    val d = (pa[2] + pb[2]) / 2 + p.bias
                    out += Seg2(pa[0], pa[1], pb[0], pb[1], p.w, shaded(p.color, d, p.shade), d)
                }
            }
            is Ball -> {
                val pc = cam.project(p.c)
                val d = pc[2] + p.bias
                out += Dot2(pc[0], pc[1], p.r, shaded(p.color, d, p.shade), d)
            }
            is Hull -> {
                val pts = p.pts.map { cam.project(it) }
                val d = pts.map { it[2] }.average().toFloat() + p.bias
                val hull = convexHull(pts.map { it[0] }, pts.map { it[1] })
                out += Poly2(hull.first, hull.second, p.round, shaded(p.color, d, p.shade), d)
            }
            is Disc -> {
                val n = p.normal.normalized()
                val u = anyPerpendicular(n)
                val w = n cross u
                val k = 28
                val xs = FloatArray(k)
                val ys = FloatArray(k)
                for (i in 0 until k) {
                    val a = i * (2 * Math.PI / k)
                    val pt = p.c + u * (p.r * cos(a).toFloat()) + w * (p.r * sin(a).toFloat())
                    val pp = cam.project(pt)
                    xs[i] = pp[0]; ys[i] = pp[1]
                }
                val d = cam.project(p.c)[2] + p.bias
                out += Poly2(xs, ys, 0f, p.color, d)
            }
            is Box -> boxFaces(p, cam, out)
        }
        out.sortBy { it.depth }
        return out
    }

    private fun boxFaces(b: Box, cam: Camera, out: MutableList<Shape2>) {
        // Local axes of the box after its tilt and turn.
        var ax = dirXY(b.pitch)
        var ay = dirXY(b.pitch + 90f)
        var az = V3.RIGHT
        if (b.yaw != 0f) {
            ax = rotate(ax, V3.UP, b.yaw); ay = rotate(ay, V3.UP, b.yaw); az = rotate(az, V3.UP, b.yaw)
        }
        val hx = ax * (b.size.x / 2)
        val hy = ay * (b.size.y / 2)
        val hz = az * (b.size.z / 2)
        val centreDepth = cam.project(b.c)[2] + b.bias
        class Face(val offset: V3, val u: V3, val w: V3, val light: Float)
        // Top is lit, the underside is darkest.
        val faces = listOf(
            Face(hy, hx, hz, 0.10f), Face(-hy, hx, hz, -0.30f),
            Face(hx, hy, hz, -0.08f), Face(-hx, hy, hz, -0.16f),
            Face(hz, hx, hy, -0.04f), Face(-hz, hx, hy, -0.20f),
        )
        for (face in faces) {
            if (cam.facing(face.offset) <= 0.01f) continue
            val centre = b.c + face.offset
            val u = face.u
            val w = face.w
            val light = face.light
            val corners = listOf(centre + u + w, centre + u - w, centre - u - w, centre - u + w).map { cam.project(it) }
            val color = if (light >= 0) lighten(b.color, light) else darken(b.color, -light)
            out += Poly2(
                FloatArray(4) { corners[it][0] }, FloatArray(4) { corners[it][1] },
                0.6f, color, centreDepth,
            )
        }
    }
}

// ---- Helpers ------------------------------------------------------------------------

fun mix(a: Long, b: Long, t: Float): Long {
    fun ch(c: Long, s: Int) = ((c shr s) and 0xFF).toFloat()
    val r = lerp(ch(a, 16), ch(b, 16), t).toInt()
    val g = lerp(ch(a, 8), ch(b, 8), t).toInt()
    val bl = lerp(ch(a, 0), ch(b, 0), t).toInt()
    return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or bl.toLong()
}

fun darken(c: Long, t: Float) = mix(c, 0xFF000000L, t)
fun lighten(c: Long, t: Float) = mix(c, 0xFFFFFFFFL, t)

/** Andrew's monotone chain. Returns the hull's x and y coordinates. */
fun convexHull(xs: List<Float>, ys: List<Float>): Pair<FloatArray, FloatArray> {
    val idx = xs.indices.sortedWith(compareBy({ xs[it] }, { ys[it] }))
    fun cross(o: Int, a: Int, b: Int) =
        (xs[a] - xs[o]) * (ys[b] - ys[o]) - (ys[a] - ys[o]) * (xs[b] - xs[o])
    val lower = ArrayList<Int>()
    for (i in idx) {
        while (lower.size >= 2 && cross(lower[lower.size - 2], lower.last(), i) <= 0) lower.removeAt(lower.size - 1)
        lower += i
    }
    val upper = ArrayList<Int>()
    for (i in idx.reversed()) {
        while (upper.size >= 2 && cross(upper[upper.size - 2], upper.last(), i) <= 0) upper.removeAt(upper.size - 1)
        upper += i
    }
    val hull = lower.dropLast(1) + upper.dropLast(1)
    return FloatArray(hull.size) { xs[hull[it]] } to FloatArray(hull.size) { ys[hull[it]] }
}
