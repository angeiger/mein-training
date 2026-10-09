package de.meintraining.app.figure

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// Pure Kotlin on purpose: the figure engine is also rendered by a JVM unit test that
// writes preview images, so nothing in this package may touch Android classes.

/**
 * A point or direction in the figure's world. Units are roughly centimetres.
 * x points the way the person faces, y points up (floor is y = 0), and z points to
 * the person's right-hand side.
 */
data class V3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: V3) = V3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: V3) = V3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = V3(x * s, y * s, z * s)
    operator fun unaryMinus() = V3(-x, -y, -z)
    infix fun dot(o: V3) = x * o.x + y * o.y + z * o.z
    infix fun cross(o: V3) = V3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    val length: Float get() = sqrt(this dot this)

    fun normalized(): V3 {
        val l = length
        return if (l < 1e-6f) ZERO else this * (1f / l)
    }

    /** The same point on the other side of the body. */
    fun mirror() = V3(x, y, -z)

    companion object {
        val ZERO = V3(0f, 0f, 0f)
        val UP = V3(0f, 1f, 0f)
        val DOWN = V3(0f, -1f, 0f)
        val FWD = V3(1f, 0f, 0f)
        val BACK = V3(-1f, 0f, 0f)
        val RIGHT = V3(0f, 0f, 1f)
    }
}

fun v(x: Number, y: Number, z: Number = 0) = V3(x.toFloat(), y.toFloat(), z.toFloat())

fun rad(deg: Float) = deg * (PI.toFloat() / 180f)

fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t
fun lerp(a: V3, b: V3, t: Float) = a + (b - a) * t

/** Smooth start and stop, so movements look controlled rather than mechanical. */
fun ease(t: Float): Float {
    val c = t.coerceIn(0f, 1f)
    return c * c * (3f - 2f * c)
}

/** Direction in the x/y plane at [deg] degrees: 0 = forward, 90 = up, -90 = down. */
fun dirXY(deg: Float) = V3(cos(rad(deg)), sin(rad(deg)), 0f)

/** Rotates [v] around the unit [axis] by [deg] degrees (Rodrigues' formula). */
fun rotate(v: V3, axis: V3, deg: Float): V3 {
    val a = rad(deg)
    val c = cos(a)
    val s = sin(a)
    return v * c + (axis cross v) * s + axis * ((axis dot v) * (1f - c))
}

fun anyPerpendicular(d: V3): V3 =
    if (abs(d.y) < 0.9f) (d cross V3.UP).normalized() else (d cross V3.FWD).normalized()

/**
 * An orthographic camera. [yaw] turns the view from a pure side view (0) towards the
 * person's front (90); [pitch] raises the camera so it looks down a little.
 */
class Camera(val yaw: Float = 25f, val pitch: Float = 10f) {
    private val cy = cos(rad(yaw))
    private val sy = sin(rad(yaw))
    private val cp = cos(rad(pitch))
    private val sp = sin(rad(pitch))

    /** Screen x (right), screen y (down) and depth (larger = closer to the viewer). */
    fun project(p: V3): FloatArray {
        val x1 = p.x * cy - p.z * sy
        val z1 = p.x * sy + p.z * cy
        val y2 = p.y * cp - z1 * sp
        val z2 = p.y * sp + z1 * cp
        return floatArrayOf(x1, -y2, z2)
    }

    /** Depth component of a direction; positive means the surface faces the camera. */
    fun facing(n: V3): Float {
        val z1 = n.x * sy + n.z * cy
        return n.y * sp + z1 * cp
    }
}
