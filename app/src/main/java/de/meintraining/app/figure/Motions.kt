package de.meintraining.app.figure

import de.meintraining.app.figure.Palette.FRAME
import de.meintraining.app.figure.Palette.PAD
import de.meintraining.app.figure.Palette.PLATE
import de.meintraining.app.figure.Palette.WALL
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The movement of every exercise in the catalogue, keyed by exercise id.
 * Pose 0 is the starting position, pose 1 the end of the effortful part.
 */
object Motions {

    private const val SEAT = 51f        // height of a machine seat
    private const val SIT = 60f         // pelvis height when sitting on it

    private fun flat(x: Number, z: Number = 12, pole: V3 = V3.ZERO) = pair(v(x, Body.ANKLE_H, z), pole)

    /** Hands resting on handles beside the seat. */
    private fun seatHandles(x: Number = 10, y: Number = 50) = pair(v(x, y, 28), v(-0.3, -1, 0.4))

    private fun twoSided(r: Limb, l: Limb) = r to l

    val all: Map<String, Motion> = buildMap {

        // ---- Geräte: Beine ------------------------------------------------------------

        put("beinpresse", Motion(
            camera = Camera(28f, 12f),
            captions = "Wegdrücken – ausatmen" to "Langsam zurück – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(0, 56), pitch = -32f,
                    hands = pair(v(10, 47, 27), v(-0.3, -1, 0.4)),
                    feet = pair(v(lerp(54f, 80f, p), 74, 13), v(0.3, 1, 0.12)),
                    footAngle = 88f to 88f,
                )
            },
            props = { p, sk ->
                val x = lerp(54f, 80f, p) + 7f
                buildList {
                    addAll(Props.seat(-4f, 49f))
                    addAll(Props.backrest(sk.pelvis, -32f, length = 62f))
                    add(Box(v(x, 76, 0), v(4, 46, 56), PAD, bias = -25f))
                    add(Props.tube(v(x + 4, 66, 0), v(x + 28, 66, 0), 6f, bias = -40f))
                    add(Props.tube(v(30, 60, 0), v(118, 60, 0), 6f, bias = -45f))
                    add(Props.tube(v(118, 2, 0), v(118, 60, 0), 6f, bias = -45f))
                    add(Props.grip(v(10, 47, 27), V3.FWD)); add(Props.grip(v(10, 47, -27), V3.FWD))
                    addAll(Props.stack(-62f, lift = p, top = 120f))
                }
            },
        ))

        val knee = v(45, 58)
        fun shinOnArc(deg: Float) = knee + dirXY(deg) * Body.SHIN

        put("beinstrecker", Motion(
            camera = Camera(26f, 12f),
            captions = "Beine strecken – ausatmen" to "Langsam beugen – einatmen",
            pose = { p ->
                val a = shinOnArc(lerp(-100f, -6f, p))
                Pose(
                    pelvis = v(0, SIT), pitch = -10f,
                    hands = seatHandles(14, 50),
                    feet = twoSided(Limb(a + v(0, 0, 12), via = knee + v(0, 0, 11)), Limb(a + v(0, 0, -12), via = knee + v(0, 0, -11))),
                )
            },
            props = { p, sk ->
                val a = sk.ankle.first.copy(z = 0f)
                val pad = a + Props.front(sk.knee.first, sk.ankle.first) * 8f
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    addAll(Props.backrest(sk.pelvis, -10f))
                    add(Props.roller(pad))
                    add(Props.tube(pad + v(0, 0, 20), knee + v(0, 0, 21), 4.5f))
                    add(Disc(knee + v(0, 0, 22), V3.RIGHT, 5f, FRAME))
                    add(Props.grip(v(14, 50, 27), V3.FWD)); add(Props.grip(v(14, 50, -27), V3.FWD))
                    addAll(Props.stack(-60f, lift = p))
                }
            },
        ))

        put("beinbeuger_sitzend", Motion(
            camera = Camera(26f, 12f),
            captions = "Beine beugen – ausatmen" to "Langsam strecken – einatmen",
            pose = { p ->
                val a = shinOnArc(lerp(-8f, -112f, p))
                Pose(
                    pelvis = v(0, SIT), pitch = -14f,
                    hands = seatHandles(14, 50),
                    feet = twoSided(Limb(a + v(0, 0, 12), via = knee + v(0, 0, 11)), Limb(a + v(0, 0, -12), via = knee + v(0, 0, -11))),
                )
            },
            props = { p, sk ->
                val a = sk.ankle.first.copy(z = 0f)
                val pad = a - Props.front(sk.knee.first, sk.ankle.first) * 8f
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    addAll(Props.backrest(sk.pelvis, -14f))
                    add(Props.roller(v(32, 75, 0)))
                    add(Props.roller(pad))
                    add(Props.tube(pad + v(0, 0, 20), knee + v(0, 0, 21), 4.5f))
                    add(Props.tube(v(32, 75, 20), v(32, 40, 22), 4.5f, bias = -30f))
                    add(Disc(knee + v(0, 0, 22), V3.RIGHT, 5f, FRAME))
                    add(Props.grip(v(14, 50, 27), V3.FWD)); add(Props.grip(v(14, 50, -27), V3.FWD))
                    addAll(Props.stack(-60f, lift = p))
                }
            },
        ))

        val proneKnee = v(-45, 63)
        put("beinbeuger_liegend", Motion(
            camera = Camera(24f, 16f),
            captions = "Fersen zum Po – ausatmen" to "Langsam strecken – einatmen",
            pose = { p ->
                val a = proneKnee + dirXY(lerp(184f, 78f, p)) * Body.SHIN
                Pose(
                    pelvis = v(0, 66), pitch = 88f, headPitch = -18f,
                    hands = pair(v(60, 48, 17), v(-0.2, -0.3, 1)),
                    feet = twoSided(Limb(a + v(0, 0, 11), via = proneKnee + v(0, 0, 10)), Limb(a + v(0, 0, -11), via = proneKnee + v(0, 0, -10))),
                )
            },
            props = { p, sk ->
                val a = sk.ankle.first.copy(z = 0f)
                val pad = a - Props.front(sk.knee.first, sk.ankle.first) * 8f
                buildList {
                    add(Box(v(2, 52, 0), v(124, 8, 36), PAD, bias = -30f))
                    add(Props.tube(v(-45, 2, 0), v(-45, 48, 0), 6f, bias = -40f))
                    add(Props.tube(v(50, 2, 0), v(50, 48, 0), 6f, bias = -40f))
                    add(Props.roller(pad))
                    add(Props.tube(pad + v(0, 0, 20), proneKnee + v(0, -4, 21), 4.5f))
                    add(Disc(proneKnee + v(0, -4, 22), V3.RIGHT, 5f, FRAME))
                    add(Props.grip(v(60, 48, 17), V3.RIGHT)); add(Props.grip(v(60, 48, -17), V3.RIGHT))
                    addAll(Props.stack(-112f, lift = p, top = 120f))
                }
            },
        ))

        // Knees swing out (abduction) or in (adduction) around the hips.
        fun spreadPose(a: Float): Pose {
            val k = v(0, SIT, 10) + v(cos(rad(a)), -0.04f, sin(rad(a))).normalized() * Body.THIGH
            val ankle = k + v(5, -43, 1)
            val r = Limb(ankle, via = k)
            return Pose(
                pelvis = v(0, SIT), pitch = -12f,
                hands = seatHandles(8, 52),
                feet = r to r.mirror(),
            )
        }
        fun spreadProps(sk: Skeleton, outside: Boolean, lift: Float) = buildList {
            addAll(Props.seat(-2f, SEAT, width = 46f))
            addAll(Props.backrest(sk.pelvis, -12f, width = 46f))
            for ((k, hip) in listOf(sk.knee.first to sk.hip.first, sk.knee.second to sk.hip.second)) {
                val d = (k - hip).normalized()
                var n = V3(-d.z, 0f, d.x)
                if ((n.z > 0) != (k.z > 0)) n = -n
                if (!outside) n = -n
                add(Cap(k - d * 16f + n * 10f, k + d * 2f + n * 10f, 9f, PAD, bias = 0.2f))
                val a = if (k.z > 0) sk.ankle.first else sk.ankle.second
                add(Box(a + v(6, -9, 0), v(22, 3, 10), FRAME, bias = -10f))
            }
            add(Props.grip(v(8, 52, 28), V3.FWD)); add(Props.grip(v(8, 52, -28), V3.FWD))
            addAll(Props.stack(-58f, lift = lift))
        }
        put("abduktoren", Motion(
            camera = Camera(90f, 30f),
            captions = "Beine auseinander – ausatmen" to "Langsam zurück – einatmen",
            pose = { p -> spreadPose(lerp(3f, 30f, p)) },
            props = { p, sk -> spreadProps(sk, outside = true, lift = p) },
        ))
        put("adduktoren", Motion(
            camera = Camera(90f, 30f),
            captions = "Beine zusammen – ausatmen" to "Langsam öffnen – einatmen",
            pose = { p -> spreadPose(lerp(32f, 4f, p)) },
            props = { p, sk -> spreadProps(sk, outside = false, lift = p) },
        ))

        put("wadenheben", Motion(
            camera = Camera(26f, 8f),
            captions = "Auf die Zehen – ausatmen" to "Fersen langsam senken – einatmen",
            timing = Timing.PingPong(forwardMs = 1200, backMs = 1600),
            pose = { p ->
                val ang = lerp(18f, -32f, p)
                val toe = v(18, 12)
                val ankle = toe - dirXY(ang) * (Body.FOOT - 4f)
                val pelvis = v(ankle.x - 2f, ankle.y + 86f)
                val s = Body.shoulder(pelvis, 0f, R)
                Pose(
                    pelvis = pelvis,
                    hands = pair(s + v(8, 10, 9), v(-0.2, -1, 0.5)),
                    feet = pair(ankle.copy(z = 12f)),
                    footAngle = ang to ang,
                )
            },
            props = { p, sk ->
                val s = sk.shoulder.first
                val y = s.y + 9f
                buildList {
                    add(Box(v(12, 5, 0), v(30, 10, 56), PAD, bias = -30f))
                    add(Cap(v(s.x, y, -26), v(s.x, y, 26), 10f, PAD, bias = -0.5f))
                    add(Props.tube(v(s.x, y, 26), v(-34, y, 26), 5f, bias = -20f))
                    add(Props.tube(v(-34, 2, 26), v(-34, 200, 26), 6f, bias = -25f))
                    addAll(Props.stack(-52f, lift = p, top = 200f))
                }
            },
        ))

        // ---- Geräte: Oberkörper ----------------------------------------------------------

        put("brustpresse", Motion(
            camera = Camera(30f, 12f),
            captions = "Nach vorne drücken – ausatmen" to "Langsam zurück – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(0, SIT), pitch = -8f,
                    hands = pair(v(lerp(10f, 48f, p), 101, lerp(26f, 22f, p)), v(-0.4, -0.8, 1)),
                    feet = flat(40, 14),
                )
            },
            props = { p, sk ->
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    addAll(Props.backrest(sk.pelvis, -8f, length = 64f))
                    for (h in listOf(sk.hand.first, sk.hand.second)) {
                        val s = if (h.z > 0) 1f else -1f
                        add(Props.grip(h))
                        add(Props.tube(h + v(0, -6, 0), h + v(0, -6, 9 * s), 4f))
                        add(Props.tube(h + v(0, -6, 9 * s), h + v(-36, -6, 9 * s), 4f, bias = -5f))
                    }
                    addAll(Props.stack(-62f, lift = p))
                }
            },
        ))

        put("butterfly", Motion(
            camera = Camera(58f, 30f),
            captions = "Arme zusammenführen – ausatmen" to "Langsam öffnen – einatmen",
            pose = { p ->
                val pelvis = v(0, SIT)
                val s = Body.shoulder(pelvis, -5f, R)
                val b = rad(lerp(86f, 16f, p))
                val e = s + v(cos(b), -0.06f, sin(b)).normalized() * Body.UPPER_ARM
                val h = e + v(0, Body.FOREARM - 1f, 0)
                Pose(
                    pelvis = pelvis, pitch = -5f,
                    hands = Limb(h, via = e).let { it to it.mirror() },
                    feet = flat(40, 14),
                )
            },
            props = { p, sk ->
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    addAll(Props.backrest(sk.pelvis, -5f, length = 64f))
                    for (side in listOf(R, L)) {
                        val e = if (side == R) sk.elbow.first else sk.elbow.second
                        val h = if (side == R) sk.hand.first else sk.hand.second
                        val s = if (side == R) sk.shoulder.first else sk.shoulder.second
                        val arm = (e - s).normalized()
                        val t = V3(arm.z * side, 0f, -arm.x * side).let { if (it.x < 0) -it else it }
                        add(Cap(e + t * 6f, h + t * 6f, 10f, PAD, bias = 0.3f))
                        add(Props.tube(h + t * 6f, v(s.x, 150, s.z), 4f, bias = -5f))
                    }
                    addAll(Props.stack(-60f, lift = p, top = 160f))
                }
            },
        ))

        put("butterfly_reverse", Motion(
            camera = Camera(-58f, 34f),
            captions = "Arme nach hinten öffnen – ausatmen" to "Langsam zurück – einatmen",
            pose = { p ->
                val pelvis = v(0, SIT)
                val s = Body.shoulder(pelvis, 6f, R)
                val b = rad(lerp(14f, 92f, p))
                val h = s + v(cos(b), -0.06f, sin(b)).normalized() * 56f
                Pose(
                    pelvis = pelvis, pitch = 6f,
                    hands = pair(h, v(-0.2, -1, 0)),
                    feet = flat(40, 14),
                )
            },
            props = { p, sk ->
                val chest = Body.chest(sk.pelvis, 6f)
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    add(Box(chest + v(8.5, 0, 0), v(7, 36, 28), PAD, pitch = -6f, bias = -2f))
                    add(Props.tube(chest + v(12, -18, 0), v(chest.x + 12, 2, 0), 6f, bias = -30f))
                    for (h in listOf(sk.hand.first, sk.hand.second)) {
                        val s = if (h.z > 0) sk.shoulder.first else sk.shoulder.second
                        add(Props.grip(h))
                        add(Props.tube(h + v(0, 6, 0), v(s.x, 150, s.z), 4f, bias = -5f))
                    }
                    addAll(Props.stack(70f, lift = p, top = 160f))
                }
            },
        ))

        put("latzug", Motion(
            camera = Camera(62f, 6f),
            captions = "Stange zur Brust ziehen – ausatmen" to "Langsam nach oben – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(0, SIT - 2), pitch = lerp(-4f, -14f, p),
                    hands = pair(lerp(v(2, 160, 36), v(4, 113, 36), p), v(-0.2, -1, 0.7)),
                    feet = flat(46, 14),
                )
            },
            props = { p, sk ->
                val (hr, hl) = sk.hand
                val mid = (hr + hl) * 0.5f
                buildList {
                    addAll(Props.seat(-2f, SEAT - 2f))
                    add(Props.roller(v(38, 74, 0), half = 22f))
                    add(Props.tube(v(38, 74, 23), v(38, 40, 23), 4.5f, bias = -30f))
                    add(Props.bar(hl, hr, extra = 16f, w = 3.5f))
                    add(Props.cable(mid, v(mid.x, 200, 0)))
                    add(Props.pulley(v(mid.x, 200, 0)))
                    add(Props.tube(v(mid.x, 205, 0), v(-46, 205, 0), 6f, bias = -30f))
                    addAll(Props.stack(-46f, lift = p, top = 205f))
                }
            },
        ))

        put("rudern", Motion(
            camera = Camera(26f, 12f),
            captions = "Griffe zum Körper ziehen – ausatmen" to "Langsam zurück – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(0, SIT - 2),
                    hands = pair(lerp(v(54, 100, 17), v(8, 98, 23), p), v(-1, -0.4, 0.3)),
                    feet = flat(44, 14),
                )
            },
            props = { p, sk ->
                buildList {
                    addAll(Props.seat(-2f, SEAT - 2f))
                    add(Box(v(23, 96, 0), v(7, 34, 28), PAD, bias = -2f))
                    add(Props.tube(v(26, 79, 0), v(26, 2, 0), 6f, bias = -30f))
                    for (h in listOf(sk.hand.first, sk.hand.second)) {
                        add(Props.grip(h))
                        add(Props.tube(h + v(0, 6, 0), v(62, 168, h.z), 4f, bias = -5f))
                    }
                    add(Props.tube(v(62, 168, -24), v(62, 168, 24), 5f, bias = -10f))
                    addAll(Props.stack(84f, lift = p, top = 175f))
                }
            },
        ))

        put("schulterpresse", Motion(
            camera = Camera(56f, 6f),
            captions = "Nach oben drücken – ausatmen" to "Langsam senken – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(0, SIT), pitch = -6f,
                    hands = pair(lerp(v(2, 117, 32), v(2, 160, 24), p), v(-0.2, -1, 0.9)),
                    feet = flat(40, 16),
                )
            },
            props = { p, sk ->
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    addAll(Props.backrest(sk.pelvis, -6f, length = 66f))
                    for (h in listOf(sk.hand.first, sk.hand.second)) {
                        add(Props.grip(h, V3.FWD))
                        add(Props.tube(h - v(6, 0, 0), h + v(-40, -12, 0), 4f, bias = -5f))
                    }
                    addAll(Props.stack(-62f, lift = p, top = 175f))
                }
            },
        ))

        val curlElbow = Body.shoulder(v(0, 56), 15f, R) + dirXY(-52f) * Body.UPPER_ARM
        put("bizepsmaschine", Motion(
            camera = Camera(26f, 12f),
            captions = "Arme beugen – ausatmen" to "Langsam strecken – einatmen",
            pose = { p ->
                val h = curlElbow + dirXY(lerp(-28f, 96f, p)) * Body.FOREARM
                Pose(
                    pelvis = v(0, 56), pitch = 15f,
                    hands = pair(h.copy(z = 16f), via = curlElbow),
                    feet = flat(34, 14),
                )
            },
            props = { p, sk ->
                val s = Body.shoulder(v(0, 56), 15f, R)
                val mid = (s + curlElbow) * 0.5f
                val padC = (mid + dirXY(-142f) * 9.5f).copy(z = 0f)
                buildList {
                    addAll(Props.seat(-4f, 47f, depth = 36f))
                    add(Box(padC, v(7, 38, 48), PAD, pitch = -142f, bias = -3f))
                    add(Props.tube(padC + v(-4, -10, 0), v(padC.x - 4, 2, 0), 6f, bias = -30f))
                    val h = sk.hand.first
                    add(Cap(v(h.x, h.y, -24), v(h.x, h.y, 24), 3.5f, FRAME, bias = 0.5f))
                    add(Props.tube(v(h.x, h.y, 24), curlElbow.copy(z = 26f), 4f))
                    add(Props.tube(v(h.x, h.y, -24), curlElbow.copy(z = -26f), 4f))
                    add(Disc(curlElbow.copy(z = 27f), V3.RIGHT, 5f, FRAME))
                    addAll(Props.stack(-50f, lift = p))
                }
            },
        ))

        put("rueckenstrecker", Motion(
            camera = Camera(26f, 12f),
            captions = "Aufrichten – ausatmen" to "Langsam nach vorne – einatmen",
            pose = { p ->
                val pitch = lerp(38f, -12f, p)
                val pelvis = v(0, SIT)
                val f = torsoFrame(pitch)
                Pose(
                    pelvis = pelvis, pitch = pitch,
                    hands = twoSided(
                        Limb(Body.chest(pelvis, pitch, -6f) + f.fwd * 5f, v(0.2, -1, 1)),
                        Limb(Body.chest(pelvis, pitch, 6f) + f.fwd * 5f, v(0.2, -1, -1)),
                    ),
                    feet = flat(42, 14),
                )
            },
            props = { p, sk ->
                val f = sk.frame
                val pad = sk.pelvis + f.up * 38f - f.fwd * 14f
                val pitch = lerp(38f, -12f, p)
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    add(Box(pad, v(7, 26, 40), PAD, pitch = -pitch, bias = -2f))
                    add(Props.tube(pad + v(0, 0, 22), v(-8, SIT, 22), 4.5f))
                    add(Disc(v(-8, SIT, 23), V3.RIGHT, 5f, FRAME))
                    add(Props.roller(v(30, 76, 0)))
                    add(Box(v(46, 4, 0), v(22, 4, 40), FRAME, bias = -20f))
                    addAll(Props.stack(-62f, lift = p))
                }
            },
        ))

        put("bauchmaschine", Motion(
            camera = Camera(26f, 12f),
            captions = "Oberkörper einrollen – ausatmen" to "Langsam aufrichten – einatmen",
            pose = { p ->
                val pitch = lerp(-8f, 32f, p)
                val pelvis = v(0, SIT)
                val f = torsoFrame(pitch)
                val n = Body.neck(pelvis, pitch)
                Pose(
                    pelvis = pelvis, pitch = pitch, headPitch = lerp(0f, 18f, p),
                    hands = pair(n + f.up * 6f + f.fwd * 6f + f.right * 15f, v(1, -1, 0.6)),
                    feet = flat(42, 14),
                )
            },
            props = { p, sk ->
                val f = sk.frame
                val pitch = lerp(-8f, 32f, p)
                val pad = Body.chest(sk.pelvis, pitch) + f.fwd * 7.5f
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    addAll(Props.backrest(sk.pelvis, -8f, length = 30f))
                    add(Box(pad, v(6, 24, 34), PAD, pitch = -pitch, bias = 1f))
                    add(Props.tube(pad + v(0, 0, 18), v(6, SIT + 2, 24), 4.5f))
                    for (h in listOf(sk.hand.first, sk.hand.second)) add(Props.grip(h, f.right))
                    add(Props.roller(v(32, 76, 0)))
                    addAll(Props.stack(-62f, lift = p))
                }
            },
        ))

        put("rumpfrotation", Motion(
            camera = Camera(90f, 40f),
            floorX = null,
            timing = Timing.PingPong(forwardMs = 2000, backMs = 2000, holdStartMs = 400, holdEndMs = 400),
            captions = "Zur Seite drehen – ausatmen" to "Zur anderen Seite drehen – ausatmen",
            thumbP = 0.25f,
            pose = { p ->
                val tw = lerp(-35f, 35f, p)
                val pelvis = v(0, SIT)
                val f = torsoFrame(-4f, tw)
                val n = Body.neck(pelvis, -4f, tw)
                Pose(
                    pelvis = pelvis, pitch = -4f, twist = tw,
                    hands = twoSided(
                        Limb(n - f.up * 14f + f.fwd * 22f + f.right * 13f, f.right * 1f + V3.DOWN),
                        Limb(n - f.up * 14f + f.fwd * 22f - f.right * 13f, -f.right + V3.DOWN),
                    ),
                    feet = flat(42, 14),
                )
            },
            props = { _, sk ->
                val f = sk.frame
                val tw = Math.toDegrees(kotlin.math.atan2(-f.fwd.z, f.fwd.x).toDouble()).toFloat()
                buildList {
                    addAll(Props.seat(-2f, SEAT))
                    add(Box(sk.neck - f.up * 14f + f.fwd * 30f, v(5, 18, 40), PAD, yaw = tw, bias = 1f))
                    for (h in listOf(sk.hand.first, sk.hand.second)) add(Props.grip(h))
                    add(Props.tube(v(-2, 50, 0), v(-2, 115, 0), 6f, bias = -60f))
                    add(Props.roller(v(36, 74, 0)))
                }
            },
        ))

        put("hyperextension", Motion(
            camera = Camera(24f, 12f),
            captions = "Oberkörper heben – ausatmen" to "Langsam absenken – einatmen",
            pose = { p ->
                val pitch = lerp(148f, 42f, p)
                val pelvis = v(18, 88)
                val f = torsoFrame(pitch)
                Pose(
                    pelvis = pelvis, pitch = pitch, headPitch = lerp(10f, -8f, p),
                    hands = twoSided(
                        Limb(Body.chest(pelvis, pitch, -6f) + f.fwd * 5f, v(0.2, -1, 1)),
                        Limb(Body.chest(pelvis, pitch, 6f) + f.fwd * 5f, v(0.2, -1, -1)),
                    ),
                    feet = pair(v(-43, 27, 12), v(0.7, -0.7, 0)),
                )
            },
            props = { _, _ ->
                val legDir = v(-0.707, -0.707)
                val legFront = v(0.707, -0.707)
                val hipPad = v(18, 88) + legDir * 14f + legFront * 13f
                val ankle = v(-43, 27)
                buildList {
                    add(Props.roller(hipPad, half = 20f, w = 14f))
                    add(Props.roller(ankle - legFront * 8f, half = 18f, w = 9f))
                    add(Box(ankle + legDir * 6f + legFront * 7f, v(3, 24, 40), FRAME, pitch = -135f, bias = -5f))
                    add(Props.tube(v(-62, 4, 0), v(42, 4, 0), 6f, bias = -60f))
                    add(Props.tube(v(-55, 4, 0), ankle + legDir * 8f, 6f, bias = -60f))
                    add(Props.tube(v(30, 4, 0), hipPad + v(0, -6, 0), 6f, bias = -60f))
                    add(Props.tube(ankle + legDir * 8f, hipPad + v(-4, -8, 0), 6f, bias = -60f))
                }
            },
        ))

        // ---- Kabelzug -------------------------------------------------------------------------

        val pushElbow = Body.shoulder(v(-4, Body.STAND_H), 12f, R) + dirXY(-93f) * Body.UPPER_ARM
        put("trizeps_kabel", Motion(
            camera = Camera(28f, 8f),
            captions = "Nach unten drücken – ausatmen" to "Langsam nach oben – einatmen",
            pose = { p ->
                val h = pushElbow + dirXY(lerp(52f, -86f, p)) * Body.FOREARM
                Pose(
                    pelvis = v(-4, Body.STAND_H), pitch = 12f,
                    hands = pair(h.copy(z = 10f), via = pushElbow),
                    feet = flat(-4, 13),
                )
            },
            props = { p, sk ->
                val (hr, hl) = sk.hand
                val mid = (hr + hl) * 0.5f
                buildList {
                    add(Props.bar(hl, hr, extra = 4f))
                    add(Props.cable(mid, v(34, 200, 0)))
                    add(Props.pulley(v(34, 200, 0)))
                    add(Props.tube(v(40, 2, 0), v(40, 208, 0), 7f, bias = -30f))
                    addAll(Props.stack(54f, lift = p, top = 208f))
                }
            },
        ))

        // ---- Kurzhanteln & Langhantel ------------------------------------------------------------

        val curlE = v(-2, 113, 20)
        put("bizepscurl", Motion(
            camera = Camera(26f, 8f),
            captions = "Hanteln hochbeugen – ausatmen" to "Langsam senken – einatmen",
            pose = { p ->
                val h = curlE + dirXY(lerp(-84f, 72f, p)) * Body.FOREARM
                Pose(
                    pelvis = v(0, Body.STAND_H),
                    hands = pair(h, via = curlE),
                    feet = flat(0, 12),
                )
            },
            props = { _, sk -> Props.dumbbell(sk.hand.first, V3.RIGHT) + Props.dumbbell(sk.hand.second, V3.RIGHT) },
        ))

        put("seitheben", Motion(
            camera = Camera(72f, 4f),
            captions = "Arme seitlich heben – ausatmen" to "Langsam senken – einatmen",
            pose = { p ->
                val pelvis = v(0, Body.STAND_H)
                val s = Body.shoulder(pelvis, 0f, R)
                val a = rad(lerp(-80f, -4f, p))
                val h = s + v(0.18f, sin(a), cos(a)).normalized() * 56f
                Pose(pelvis = pelvis, hands = pair(h, v(-1, 0.4, 0)), feet = flat(0, 13))
            },
            props = { _, sk -> Props.dumbbell(sk.hand.first, V3.FWD) + Props.dumbbell(sk.hand.second, V3.FWD) },
        ))

        put("schulterdruecken_kh", Motion(
            camera = Camera(56f, 6f),
            captions = "Nach oben drücken – ausatmen" to "Langsam senken – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(0, SIT - 4), pitch = -4f,
                    hands = pair(lerp(v(0, 114, 34), v(0, 156, 22), p), v(-0.2, -1, 0.9)),
                    feet = flat(38, 22),
                )
            },
            props = { _, sk ->
                buildList {
                    addAll(Props.seat(-2f, SEAT - 4f))
                    addAll(Props.backrest(sk.pelvis, -4f, length = 62f))
                    addAll(Props.dumbbell(sk.hand.first, V3.RIGHT))
                    addAll(Props.dumbbell(sk.hand.second, V3.RIGHT))
                }
            },
        ))

        put("kh_rudern", Motion(
            camera = Camera(30f, 16f),
            captions = "Hantel zur Hüfte ziehen – ausatmen" to "Langsam senken – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(-12, 97), pitch = 75f,
                    hands = twoSided(
                        Limb(lerp(v(37, 54, 20), v(16, 94, 22), p), v(-0.6, 1, 0.2)),
                        Limb(v(37, 52, -15), v(-0.3, -1, -0.2)),
                    ),
                    feet = twoSided(
                        Limb(v(-6, Body.ANKLE_H, 24), v(1, 0.1, 0.2)),
                        Limb(v(-55, 53, -10), v(0.1, -1, 0)),
                    ),
                    footAngle = null to 180f,
                )
            },
            props = { _, sk ->
                buildList {
                    add(Box(v(-12, 44, -10), v(118, 8, 30), PAD, bias = -30f))
                    add(Props.tube(v(-60, 2, -10), v(-60, 40, -10), 6f, bias = -40f))
                    add(Props.tube(v(36, 2, -10), v(36, 40, -10), 6f, bias = -40f))
                    addAll(Props.dumbbell(sk.hand.first, V3.FWD))
                }
            },
        ))

        put("bankdruecken", Motion(
            camera = Camera(10f, 34f),
            captions = "Stange hochdrücken – ausatmen" to "Langsam senken – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(22, 61), pitch = -90f,
                    hands = pair(lerp(v(-18, 80, 30), v(-22, 116, 28), p), v(0.3, -1, 1)),
                    feet = pair(v(70, Body.ANKLE_H, 26), v(0.4, 1, 0.3)),
                )
            },
            props = { _, sk ->
                val (hr, hl) = sk.hand
                buildList {
                    add(Box(v(-8, 49, 0), v(112, 8, 30), PAD, bias = -30f))
                    add(Props.tube(v(-50, 2, 0), v(-50, 45, 0), 6f, bias = -40f))
                    add(Props.tube(v(36, 2, 0), v(36, 45, 0), 6f, bias = -40f))
                    for (z in listOf(-46, 46)) {
                        add(Props.tube(v(-30, 2, z), v(-30, 124, z), 5f, bias = -10f))
                        add(Props.tube(v(-30, 112, z), v(-24, 116, z), 4f, bias = -10f))
                    }
                    addAll(Props.barbell((hr + hl) * 0.5f))
                }
            },
        ))

        put("kreuzheben_kh", Motion(
            camera = Camera(26f, 8f),
            timing = Timing.PingPong(forwardMs = 2000, backMs = 1500),
            captions = "Langsam vorbeugen – einatmen" to "Aufrichten – ausatmen",
            pose = { p ->
                val pitch = lerp(0f, 72f, p)
                val pelvis = lerp(v(0, Body.STAND_H), v(-16, 89), p)
                val s = Body.shoulder(pelvis, pitch, R)
                Pose(
                    pelvis = pelvis, pitch = pitch, headPitch = lerp(0f, -12f, p),
                    hands = pair(s + v(3, -57, -5), v(-1, 0, 0.3)),
                    feet = flat(2, 12),
                )
            },
            props = { _, sk -> Props.dumbbell(sk.hand.first, V3.RIGHT) + Props.dumbbell(sk.hand.second, V3.RIGHT) },
        ))

        // ---- Ohne Geräte ----------------------------------------------------------------------------

        put("kniebeuge", Motion(
            camera = Camera(26f, 8f),
            timing = Timing.PingPong(forwardMs = 2000, backMs = 1500),
            captions = "Langsam in die Hocke – einatmen" to "Hochdrücken – ausatmen",
            pose = { p ->
                val pitch = lerp(3f, 38f, p)
                val pelvis = lerp(v(-2, Body.STAND_H), v(-30, 58), p)
                val s = Body.shoulder(pelvis, pitch, R)
                val hand = lerp(s + v(6, -56, -3), s + v(56, 2, -5), p)
                Pose(
                    pelvis = pelvis, pitch = pitch,
                    hands = pair(hand, v(-0.3, -1, 0.2)),
                    feet = flat(0, 15, v(1, 0.1, 0.35)),
                )
            },
        ))

        put("ausfallschritt", Motion(
            camera = Camera(28f, 8f),
            timing = Timing.PingPong(forwardMs = 2000, backMs = 1500),
            captions = "Langsam absenken – einatmen" to "Hochdrücken – ausatmen",
            pose = { p ->
                val pelvis = lerp(v(-4, 86), v(-8, 53), p)
                Pose(
                    pelvis = pelvis, pitch = lerp(0f, 4f, p),
                    hands = pair(pelvis + v(2, 14, 21), v(-0.3, 0, 1)),
                    feet = twoSided(
                        Limb(v(35, Body.ANKLE_H, 12), v(1, 0.1, 0.1)),
                        Limb(v(-50, 14, -12), v(0.2, -1, 0)),
                    ),
                    footAngle = null to -40f,
                )
            },
        ))

        put("aufstehen", Motion(
            camera = Camera(28f, 8f),
            timing = Timing.PingPong(forwardMs = 2200, backMs = 2600),
            captions = "Nach vorne lehnen und aufstehen – ausatmen" to "Langsam hinsetzen – einatmen",
            pose = { p ->
                val lean = (p / 0.35f).coerceIn(0f, 1f)
                val rise = ease(((p - 0.35f) / 0.65f).coerceIn(0f, 1f))
                val pitch = lerp(0f, 38f, lean) - 36f * rise
                val pelvis = lerp(v(-28, 56), v(-4, 94), rise)
                val f = torsoFrame(pitch)
                Pose(
                    pelvis = pelvis, pitch = pitch,
                    hands = twoSided(
                        Limb(Body.chest(pelvis, pitch, -7f) + f.fwd * 5f, v(0.2, -1, 0.8)),
                        Limb(Body.chest(pelvis, pitch, 7f) + f.fwd * 5f, v(0.2, -1, -0.8)),
                    ),
                    feet = flat(14, 13),
                )
            },
            props = { _, _ ->
                buildList {
                    add(Box(v(-32, 43.5, 0), v(44, 5, 44), PAD, bias = -30f))
                    add(Box(v(-53, 70, 0), v(4, 46, 44), PAD, bias = -30f))
                    for (x in listOf(-51, -13)) for (z in listOf(-19, 19)) {
                        add(Props.tube(v(x, 2, z), v(x, 41, z), 4f, bias = -40f))
                    }
                }
            },
        ))

        put("wand_liegestuetz", Motion(
            camera = Camera(28f, 10f),
            timing = Timing.PingPong(forwardMs = 2000, backMs = 1500),
            captions = "Langsam zur Wand – einatmen" to "Wegdrücken – ausatmen",
            pose = { p ->
                val b = lerp(12.4f, 25f, p)
                val ankle = v(-60, Body.ANKLE_H)
                val pelvis = ankle + v(sin(rad(b)), cos(rad(b))) * 87f
                Pose(
                    pelvis = pelvis, pitch = b,
                    hands = pair(v(24, 135, 24), v(-0.2, -1, 0.8)),
                    feet = pair(ankle.copy(z = 14f)),
                    footAngle = 0f to 0f,
                )
            },
            props = { _, _ -> listOf(Box(v(31, 100, 0), v(4, 200, 110), WALL, bias = -50f)) },
        ))

        put("crunch", Motion(
            camera = Camera(30f, 18f),
            captions = "Schultern anheben – ausatmen" to "Langsam ablegen – einatmen",
            pose = { p ->
                val pitch = lerp(-90f, -62f, p)
                val pelvis = v(0, 12)
                val f = torsoFrame(pitch)
                val n = Body.neck(pelvis, pitch)
                Pose(
                    pelvis = pelvis, pitch = pitch, headPitch = lerp(0f, 12f, p),
                    hands = pair(n + f.up * 13f - f.fwd * 2f + f.right * 13f, v(0, 0.3, 1)),
                    feet = pair(v(42, Body.ANKLE_H, 12), v(0.3, 1, 0.1)),
                )
            },
            props = { _, _ -> listOf(Props.mat(-10f)) },
        ))

        put("plank", Motion(
            camera = Camera(26f, 14f),
            timing = Timing.Still,
            captions = "Halten – ruhig weiteratmen" to "Halten – ruhig weiteratmen",
            pose = {
                val up = dirXY(10f)
                val shoulders = v(44, 37)
                val pelvis = shoulders - up * (Body.TORSO - Body.SHOULDER_DROP)
                val elbow = v(44, 6, 16)
                Pose(
                    pelvis = pelvis, pitch = 80f, headPitch = 0f,
                    hands = pair(v(72, 6, 10), via = elbow),
                    feet = pair((pelvis - up * 86f).copy(z = 10f), v(0, -1, 0)),
                    footAngle = -74f to -74f,
                )
            },
            props = { _, _ -> listOf(Props.mat(0f)) },
        ))

        put("beckenheben", Motion(
            camera = Camera(28f, 18f),
            captions = "Becken heben – ausatmen" to "Langsam ablegen – einatmen",
            pose = { p ->
                val pitch = lerp(-90f, -112f, p)
                val sCentre = v(-40, 14)
                val pelvis = sCentre - torsoFrame(pitch).up * (Body.TORSO - Body.SHOULDER_DROP)
                Pose(
                    pelvis = pelvis, pitch = pitch, headPitch = -(pitch + 90f),
                    hands = pair(v(14, 6, 23), v(0, 1, 0.3)),
                    feet = pair(v(46, Body.ANKLE_H, 13), v(0.2, 1, 0)),
                )
            },
            props = { _, _ -> listOf(Props.mat(-10f)) },
        ))

        put("vierfuessler", Motion(
            camera = Camera(22f, 16f),
            captions = "Arm und Bein strecken – ausatmen" to "Langsam zurück – einatmen",
            pose = { p ->
                Pose(
                    pelvis = v(-24, 58), pitch = 90f, headPitch = -6f,
                    hands = twoSided(
                        Limb(lerp(v(25, 6, 16), v(80, 62, 14), p), v(-0.3, -1, 0.2)),
                        Limb(v(25, 6, -16), v(-0.3, -1, -0.2)),
                    ),
                    feet = twoSided(
                        Limb(v(-66, 7, 10), v(0.2, -1, 0)),
                        Limb(lerp(v(-66, 7, -10), v(-112, 62, -10), p), v(0.2, -1, 0)),
                    ),
                    footAngle = 180f to lerp(180f, 120f, p),
                )
            },
            props = { _, _ -> listOf(Props.mat(-16f)) },
        ))

        // ---- Ausdauer ---------------------------------------------------------------------------------

        put("fahrrad", Motion(
            camera = Camera(24f, 8f),
            timing = Timing.Loop(1400),
            captions = "Gleichmäßig treten" to "Gleichmäßig treten",
            thumbP = 0.12f,
            pose = { t ->
                val c = v(18, 36)
                val phi = 90f - 360f * t
                val pr = c + dirXY(phi) * 16f
                val pl = c + dirXY(phi + 180f) * 16f
                Pose(
                    pelvis = v(-8, 100), pitch = 18f,
                    hands = pair(v(42, 112, 20), v(-0.3, -1, 0.3)),
                    feet = twoSided(
                        Limb((pr + v(-4, 7)).copy(z = 12f), v(1, 0.5, 0.1)),
                        Limb((pl + v(-4, 7)).copy(z = -12f), v(1, 0.5, -0.1)),
                    ),
                    footAngle = -12f to -12f,
                )
            },
            props = { t, _ ->
                val c = v(18, 36)
                val phi = 90f - 360f * t
                val pr = (c + dirXY(phi) * 16f).copy(z = 9f)
                val pl = (c + dirXY(phi + 180f) * 16f).copy(z = -9f)
                buildList {
                    add(Box(v(-10, 88, 0), v(24, 5, 14), PAD))
                    add(Props.tube(v(-8, 86, 0), c, 5f, bias = -8f))
                    add(Props.tube(c, v(48, 106, 0), 6f, bias = -8f))
                    add(Props.tube(v(48, 106, 0), v(44, 112, 0), 5f, bias = -8f))
                    add(Cap(v(44, 112, -22), v(44, 112, 22), 4f, FRAME))
                    add(Disc(v(56, 36, 0), V3.RIGHT, 24f, FRAME, bias = -12f))
                    add(Props.tube(v(-30, 4, 0), v(72, 4, 0), 6f, bias = -60f))
                    add(Props.tube(v(-30, 4, -22), v(-30, 4, 22), 6f, bias = -60f))
                    add(Props.tube(v(72, 4, -22), v(72, 4, 22), 6f, bias = -60f))
                    add(Props.tube(c.copy(z = 9f), pr, 3.5f, bias = -0.5f))
                    add(Props.tube(c.copy(z = -9f), pl, 3.5f, bias = -0.5f))
                    add(Cap(pr, pr + v(0, 0, 5), 4f, PLATE))
                    add(Cap(pl, pl - v(0, 0, 5), 4f, PLATE))
                }
            },
        ))

        put("laufband", Motion(
            camera = Camera(26f, 8f),
            timing = Timing.Loop(1300),
            captions = "Locker gehen, gleichmäßig atmen" to "Locker gehen, gleichmäßig atmen",
            thumbP = 0.2f,
            floorX = 6f,
            pose = { t ->
                fun ankle(ph: Float): Pair<V3, Float> {
                    val s = ((ph % 1f) + 1f) % 1f
                    return if (s < 0.6f) {
                        v(lerp(22f, -24f, s / 0.6f), 20) to 0f
                    } else {
                        val q = (s - 0.6f) / 0.4f
                        v(lerp(-24f, 22f, ease(q)), 20f + 9f * sin(PI.toFloat() * q)) to -12f * sin(PI.toFloat() * q)
                    }
                }
                val (ar, fr) = ankle(t)
                val (al, fl) = ankle(t + 0.5f)
                val pelvis = v(0, 104f + 1.5f * cos(4f * PI.toFloat() * t))
                val sR = Body.shoulder(pelvis, 3f, R)
                Pose(
                    pelvis = pelvis, pitch = 3f,
                    hands = twoSided(
                        Limb(sR + v(-0.5f * ar.x, -54, 2), v(-1, -0.4, 0.2)),
                        Limb(sR.mirror() + v(-0.5f * al.x, -54, -2), v(-1, -0.4, -0.2)),
                    ),
                    feet = twoSided(Limb(ar.copy(z = 11f)), Limb(al.copy(z = -11f))),
                    footAngle = fr to fl,
                )
            },
            props = { _, _ ->
                buildList {
                    add(Box(v(4, 6, 0), v(150, 10, 58), FRAME, bias = -50f))
                    add(Box(v(4, 11.5, 0), v(140, 1, 46), PLATE, bias = -49f))
                    for (z in listOf(-27, 27)) {
                        add(Props.tube(v(74, 10, z), v(64, 118, z), 5f, bias = -10f))
                        add(Props.tube(v(64, 112, z), v(28, 112, z), 4f, bias = -10f))
                    }
                    add(Box(v(66, 122, 0), v(8, 16, 56), FRAME, pitch = -20f, bias = -10f))
                }
            },
        ))

        put("crosstrainer", Motion(
            camera = Camera(26f, 8f),
            timing = Timing.Loop(1600),
            captions = "Gleichmäßig bewegen" to "Gleichmäßig bewegen",
            thumbP = 0.1f,
            pose = { t ->
                val c = v(0, 30)
                val phi = -360f * t
                fun pedal(ph: Float) = c + v(22f * cos(rad(ph)), 7f * sin(rad(ph)))
                val pr = pedal(phi)
                val pl = pedal(phi + 180f)
                val piv = v(44, 50)
                val hr = piv + dirXY(100f + 12f * cos(rad(phi))) * 70f
                val hl = piv + dirXY(100f + 12f * cos(rad(phi + 180f))) * 70f
                Pose(
                    pelvis = v(-4, 110f + 1.5f * sin(rad(2 * phi))), pitch = 4f,
                    hands = twoSided(Limb(hr.copy(z = 24f), v(-1, -0.5, 0.3)), Limb(hl.copy(z = -24f), v(-1, -0.5, -0.3))),
                    feet = twoSided(Limb((pr + v(-3, 7)).copy(z = 12f)), Limb((pl + v(-3, 7)).copy(z = -12f))),
                    footAngle = 0f to 0f,
                )
            },
            props = { _, sk ->
                val (hr, hl) = sk.hand
                val (ar, al) = sk.ankle
                buildList {
                    add(Props.tube(v(-48, 6, 0), v(60, 6, 0), 7f, bias = -60f))
                    add(Props.tube(v(-48, 6, -20), v(-48, 6, 20), 6f, bias = -60f))
                    add(Props.tube(v(56, 6, 0), v(48, 104, 0), 7f, bias = -30f))
                    add(Box(v(48, 108, 0), v(8, 12, 30), FRAME, pitch = -15f, bias = -30f))
                    add(Disc(v(-40, 30, 0), V3.RIGHT, 18f, FRAME, bias = -30f))
                    for ((h, a) in listOf(hr to ar, hl to al)) {
                        add(Props.tube(h, v(44, 50, h.z), 4.5f, bias = if (h.z > 0) 0f else -20f))
                        add(Box(a + v(2, -8, 0), v(30, 3, 12), PLATE, bias = 0f))
                        add(Props.tube(a + v(14, -9, 0), v(44, 50, h.z), 4f, bias = if (h.z > 0) 0f else -20f))
                        add(Props.tube(a + v(-12, -9, 0), v(-40, 30, h.z * 0.5f), 4f, bias = -20f))
                    }
                }
            },
        ))

        put("rudergeraet", Motion(
            camera = Camera(24f, 12f),
            timing = Timing.Loop(3200),
            captions = "Erst Beine, dann Oberkörper, dann Arme" to "Erst Beine, dann Oberkörper, dann Arme",
            thumbP = 0.36f,
            pose = { t ->
                val d = if (t < 0.36f) ease(t / 0.36f) else 1f - ease((t - 0.36f) / 0.64f)
                val legs = (d / 0.6f).coerceIn(0f, 1f)
                val back = ((d - 0.3f) / 0.4f).coerceIn(0f, 1f)
                val arms = ((d - 0.55f) / 0.45f).coerceIn(0f, 1f)
                val pelvis = v(lerp(-6f, -40f, legs), 40)
                val pitch = lerp(26f, -18f, back)
                val s = Body.shoulder(pelvis, pitch, R)
                val y = lerp(56f, 70f, d)
                val dy = s.y - y
                val straightX = s.x + sqrt((56f * 56f - dy * dy).coerceAtLeast(0f))
                val chest = Body.chest(pelvis, pitch)
                val hx = lerp(straightX, chest.x + 6f, arms)
                Pose(
                    pelvis = pelvis, pitch = pitch,
                    hands = pair(v(hx, y, 15), v(-1, -0.3, 0.4)),
                    feet = pair(v(48, 28, 12), v(0.2, 1, 0.1)),
                    footAngle = 62f to 62f,
                )
            },
            props = { _, sk ->
                val (hr, hl) = sk.hand
                val mid = (hr + hl) * 0.5f
                buildList {
                    add(Props.tube(v(-86, 22, 0), v(70, 22, 0), 7f, bias = -60f))
                    add(Props.tube(v(-86, 2, 0), v(-86, 22, 0), 6f, bias = -60f))
                    add(Box(v(sk.pelvis.x, 28, 0), v(28, 6, 26), PAD, bias = -20f))
                    add(Box(v(54, 30, 0), v(3, 28, 40), FRAME, pitch = -28f, bias = -15f))
                    add(Disc(v(80, 48, 0), V3.RIGHT, 22f, FRAME, bias = -30f))
                    add(Props.tube(v(80, 2, 0), v(80, 30, 0), 7f, bias = -60f))
                    add(Props.bar(hl, hr, extra = 5f))
                    add(Props.cable(mid, v(74, 54, 0)))
                }
            },
        ))
    }
}
