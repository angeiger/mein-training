package de.meintraining.app.figure

import de.meintraining.app.figure.Palette.CABLE
import de.meintraining.app.figure.Palette.FRAME
import de.meintraining.app.figure.Palette.MAT
import de.meintraining.app.figure.Palette.METAL
import de.meintraining.app.figure.Palette.PAD
import de.meintraining.app.figure.Palette.PLATE

/** Gym equipment drawn around the figure. */
object Props {

    fun tube(a: V3, b: V3, w: Float = 5f, color: Long = FRAME, bias: Float = 0f) = Cap(a, b, w, color, bias)

    fun cable(a: V3, b: V3) = Cap(a, b, 1.6f, CABLE)

    fun pulley(at: V3) = Disc(at, V3.RIGHT, 5f, FRAME, bias = 0.2f)

    /** Seat cushion on a post. [top] is the height of the sitting surface. */
    fun seat(x: Float, top: Float, depth: Float = 42f, width: Float = 40f): List<Prim> = listOf(
        Box(v(x, top - 4f, 0), v(depth, 8f, width), PAD, bias = -30f),
        tube(v(x, 2, 0), v(x, top - 8f, 0), 7f, bias = -40f),
        tube(v(x - depth / 2, 2, 0), v(x + depth / 2, 2, 0), 6f, bias = -40f),
    )

    /** Backrest that follows a torso leaning by [pitch]. */
    fun backrest(pelvis: V3, pitch: Float, length: Float = 58f, from: Float = 6f, width: Float = 38f): List<Prim> {
        val f = torsoFrame(pitch)
        val c = pelvis + f.up * (from + length / 2) - f.fwd * 13.5f
        val bottom = pelvis + f.up * from - f.fwd * 17f
        return listOf(
            Box(c, v(7, length, width), PAD, pitch = -pitch, bias = -30f),
            tube(bottom, v(bottom.x, 2, 0), 6f, bias = -40f),
        )
    }

    /** A weight stack whose top plates rise while the exercise is performed. */
    fun stack(x: Float, z: Float = 0f, lift: Float, top: Float = 150f): List<Prim> {
        val out = ArrayList<Prim>()
        out += tube(v(x, 0, z - 9), v(x, top, z - 9), 2.4f, METAL, bias = -20f)
        out += tube(v(x, 0, z + 9), v(x, top, z + 9), 2.4f, METAL, bias = -20f)
        out += tube(v(x, top, z - 12), v(x, top, z + 12), 5f, bias = -20f)
        out += Box(v(x, 2, z), v(16, 4, 26), FRAME, bias = -20f)
        var topY = 0f
        for (i in 0 until 9) {
            val lifted = i >= 6
            val y = 6.5f + i * 5f + if (lifted) lift * 18f else 0f
            out += Box(v(x, y, z), v(12, 4.4f, 18), PLATE, bias = -19f)
            topY = y
        }
        out += cable(v(x, topY + 2f, z), v(x, top, z))
        return out
    }

    /** Dumbbell held at [at], with the handle along [axis]. */
    fun dumbbell(at: V3, axis: V3): List<Prim> {
        val a = axis.normalized()
        return listOf(
            Cap(at - a * 7f, at + a * 7f, 3.4f, METAL, bias = 0.6f),
            Cap(at + a * 6.5f, at + a * 10f, 11f, PLATE, bias = 0.7f),
            Cap(at - a * 6.5f, at - a * 10f, 11f, PLATE, bias = 0.7f),
        )
    }

    /** Barbell lying along z with its centre at [c]. */
    fun barbell(c: V3): List<Prim> {
        val out = ArrayList<Prim>()
        out += Cap(c + v(0, 0, -64), c + v(0, 0, 64), 3f, METAL, bias = 0.6f)
        for (s in listOf(-1f, 1f)) {
            out += Disc(c + v(0, 0, 44f * s), V3.RIGHT, 15f, PLATE, bias = 0.7f)
            out += Disc(c + v(0, 0, 48f * s), V3.RIGHT, 15f, PLATE, bias = 0.7f)
            out += Cap(c + v(0, 0, 51f * s), c + v(0, 0, 54f * s), 6f, METAL, bias = 0.7f)
        }
        return out
    }

    /** Straight bar between two hands, sticking out by [extra] on each side. */
    fun bar(a: V3, b: V3, extra: Float = 10f, w: Float = 3.5f): Cap {
        val d = (b - a).normalized()
        return Cap(a - d * extra, b + d * extra, w, FRAME, bias = 0.5f)
    }

    /** Short vertical grip held in the hand. */
    fun grip(at: V3, axis: V3 = V3.UP) = Cap(at - axis * 6f, at + axis * 6f, 4.5f, FRAME, bias = 0.5f)

    /** Round foam roller across both legs, centred on [c]. */
    fun roller(c: V3, half: Float = 17f, w: Float = 10f) =
        Cap(v(c.x, c.y, -half), v(c.x, c.y, half), w, PAD, bias = 0.4f)

    fun mat(x: Float, length: Float = 190f) = Box(v(x, 1, 0), v(length, 2, 66), MAT, bias = -5000f)

    /** Perpendicular to a limb in the x/y plane, pointing to its front (shin) side. */
    fun front(from: V3, to: V3): V3 {
        val d = (to - from).normalized()
        return V3(-d.y, d.x, 0f)
    }
}
