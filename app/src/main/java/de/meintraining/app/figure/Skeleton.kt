package de.meintraining.app.figure

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/** Body proportions of the figure, in the same units as the world (about cm). */
object Body {
    const val TORSO = 52f          // pelvis centre to the base of the neck
    const val SHOULDER_DROP = 5f
    const val SHOULDER_HALF = 18f
    const val HIP_HALF = 10f
    const val UPPER_ARM = 29f
    const val FOREARM = 31f        // elbow to the middle of the hand, where it grips
    const val THIGH = 45f
    const val SHIN = 44f
    const val FOOT = 19f
    const val NECK = 7f
    const val HEAD_R = 11f

    /** Height of the ankle joint above the floor when standing. */
    const val ANKLE_H = 8f

    /** Pelvis height of a person standing with soft knees. */
    const val STAND_H = 95f

    fun neck(pelvis: V3, pitch: Float, twist: Float = 0f) =
        pelvis + torsoFrame(pitch, twist).up * TORSO

    fun shoulder(pelvis: V3, pitch: Float, side: Int, twist: Float = 0f): V3 {
        val f = torsoFrame(pitch, twist)
        return pelvis + f.up * (TORSO - SHOULDER_DROP) + f.right * (SHOULDER_HALF * side)
    }

    /** A point on the front of the chest; [side] shifts it towards one shoulder. */
    fun chest(pelvis: V3, pitch: Float, side: Float = 0f, twist: Float = 0f): V3 {
        val f = torsoFrame(pitch, twist)
        return pelvis + f.up * (TORSO - 16f) + f.fwd * 12f + f.right * side
    }
}

const val R = 1   // right side of the body (+z)
const val L = -1  // left side of the body (-z)

class Frame(val up: V3, val fwd: V3, val right: V3)

/**
 * Orientation of the upper body. [pitch] 0 = upright, positive leans forward,
 * 90 = lying face down with the head forward, -90 = lying on the back with the head
 * towards -x. [twist] turns the shoulders around the spine.
 */
fun torsoFrame(pitch: Float, twist: Float = 0f): Frame {
    val up = dirXY(90f - pitch)
    val fwd = dirXY(-pitch)
    if (twist == 0f) return Frame(up, fwd, V3.RIGHT)
    return Frame(up, rotate(fwd, up, twist), rotate(V3.RIGHT, up, twist))
}

/**
 * Where a hand or foot should be. The elbow or knee is placed by inverse kinematics:
 * it bends towards [pole], or, when [via] is given, towards that exact point.
 */
data class Limb(val target: V3, val pole: V3 = V3.ZERO, val via: V3? = null) {
    fun mirror() = Limb(target.mirror(), pole.mirror(), via?.mirror())
}

/** Both sides from one description of the right side. */
fun pair(target: V3, pole: V3 = V3.ZERO, via: V3? = null): Pair<Limb, Limb> {
    val r = Limb(target, pole, via)
    return r to r.mirror()
}

data class Pose(
    val pelvis: V3,
    val pitch: Float = 0f,
    val twist: Float = 0f,
    /** Extra nod of the head; positive tips the chin towards the chest. */
    val headPitch: Float = 0f,
    val hands: Pair<Limb, Limb>,
    val feet: Pair<Limb, Limb>,
    /** Absolute foot direction in degrees (0 = toes forward, 90 = toes up); null follows the shin. */
    val footAngle: Pair<Float?, Float?> = null to null,
    /** Ankle bend added when the foot follows the shin; negative points the toes. */
    val footFlex: Float = 0f,
)

class Skeleton(
    val pelvis: V3,
    val neck: V3,
    val head: V3,
    val frame: Frame,
    val shoulder: Pair<V3, V3>,
    val elbow: Pair<V3, V3>,
    val hand: Pair<V3, V3>,
    val hip: Pair<V3, V3>,
    val knee: Pair<V3, V3>,
    val ankle: Pair<V3, V3>,
    val heel: Pair<V3, V3>,
    val toe: Pair<V3, V3>,
)

private val ARM_POLE = v(-0.5, -1, 0.35)
private val LEG_POLE = v(1, 0.1, 0.15)

fun solve(pose: Pose): Skeleton {
    val f = torsoFrame(pose.pitch, pose.twist)
    val pelvisFrame = torsoFrame(pose.pitch)
    val neck = pose.pelvis + f.up * Body.TORSO
    val headDir = rotate(f.up, f.right, -pose.headPitch)
    val head = neck + headDir * (Body.NECK + Body.HEAD_R - 2f) + f.fwd * 1.5f

    fun shoulder(side: Int) = neck - f.up * Body.SHOULDER_DROP + f.right * (Body.SHOULDER_HALF * side)
    fun hip(side: Int) = pose.pelvis + pelvisFrame.right * (Body.HIP_HALF * side)

    val sR = shoulder(R)
    val sL = shoulder(L)
    val (eR, hR) = limb(sR, pose.hands.first, Body.UPPER_ARM, Body.FOREARM, ARM_POLE)
    val (eL, hL) = limb(sL, pose.hands.second, Body.UPPER_ARM, Body.FOREARM, ARM_POLE.mirror())

    val pR = hip(R)
    val pL = hip(L)
    val (kR, aR) = limb(pR, pose.feet.first, Body.THIGH, Body.SHIN, LEG_POLE)
    val (kL, aL) = limb(pL, pose.feet.second, Body.THIGH, Body.SHIN, LEG_POLE.mirror())

    val fR = footDir(kR, aR, pose.footAngle.first, pose.footFlex)
    val fL = footDir(kL, aL, pose.footAngle.second, pose.footFlex)

    return Skeleton(
        pelvis = pose.pelvis,
        neck = neck,
        head = head,
        frame = f,
        shoulder = sR to sL,
        elbow = eR to eL,
        hand = hR to hL,
        hip = pR to pL,
        knee = kR to kL,
        ankle = aR to aL,
        heel = (aR - fR * 4f + V3.DOWN * 1.5f) to (aL - fL * 4f + V3.DOWN * 1.5f),
        toe = (aR + fR * (Body.FOOT - 4f)) to (aL + fL * (Body.FOOT - 4f)),
    )
}

private fun limb(base: V3, l: Limb, l1: Float, l2: Float, defaultPole: V3): Pair<V3, V3> {
    val pole = when {
        l.via != null -> l.via - (base + l.target) * 0.5f
        // Not `== V3.ZERO`: a mirrored zero has z = -0.0, which data-class equality
        // treats as different from 0.0.
        l.pole.length < 1e-6f -> defaultPole
        else -> l.pole
    }
    return ik(base, l.target, l1, l2, pole)
}

/** Two-bone inverse kinematics. Returns the middle joint and the reached end point. */
fun ik(base: V3, target: V3, l1: Float, l2: Float, pole: V3): Pair<V3, V3> {
    val d = target - base
    var dist = d.length
    val dir = if (dist < 1e-4f) V3.DOWN else d * (1f / dist)
    dist = dist.coerceIn(abs(l1 - l2) + 0.5f, l1 + l2 - 0.05f)
    val end = base + dir * dist
    val cosA = ((l1 * l1 + dist * dist - l2 * l2) / (2f * l1 * dist)).coerceIn(-1f, 1f)
    val sinA = sqrt(1f - cosA * cosA)
    var perp = pole - dir * (pole dot dir)
    if (perp.length < 1e-4f) perp = anyPerpendicular(dir)
    perp = perp.normalized()
    val mid = base + dir * (l1 * cosA) + perp * (l1 * sinA)
    return mid to end
}

private fun footDir(knee: V3, ankle: V3, absolute: Float?, flex: Float): V3 {
    if (absolute != null) return dirXY(absolute)
    val shin = ankle - knee
    val shinDeg = Math.toDegrees(atan2(shin.y, shin.x).toDouble()).toFloat()
    return dirXY(shinDeg + 90f + flex)
}
