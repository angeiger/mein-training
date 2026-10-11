import de.meintraining.app.figure.Bounds
import de.meintraining.app.musclemap.BodyMap
import de.meintraining.app.musclemap.BodySide
import de.meintraining.app.figure.Dot2
import de.meintraining.app.figure.FigureRenderer
import de.meintraining.app.figure.Motions
import de.meintraining.app.figure.Part
import de.meintraining.app.figure.Poly2
import de.meintraining.app.figure.Seg2
import de.meintraining.app.figure.Shape2
import java.awt.BasicStroke
import java.awt.Color
import java.awt.Font
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

/**
 * Writes contact sheets of the exercise animations (and the muscle map) to
 * build/previews, so the drawings can be checked by eye without a phone.
 * Arguments: exercise ids to render only those, or "muscles" for the body map.
 */
fun main(args: Array<String>) {
    if (args.firstOrNull() == "debug") { debugSkeleton(args[1], args[2].toFloat()); return }
    if (args.firstOrNull() == "script") { renderScript(args[1]); return }
    val set = args.toSet()
    if ("muscles" in set) renderMuscles()
    val ids = set - "muscles"
    if (ids.isNotEmpty() || set.isEmpty()) renderFigures(ids.ifEmpty { null })
}

fun renderFigures(only: Set<String>?) {
    val entries = Motions.all.entries.filter { only == null || it.key in only }
    val tileW = 330
    val tileH = 260
    val ps = listOf(0f, 0.5f, 1f)
    entries.chunked(6).forEachIndexed { page, chunk ->
        val img = BufferedImage(tileW * ps.size, tileH * chunk.size, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = Color.WHITE
        g.fillRect(0, 0, img.width, img.height)
        chunk.forEachIndexed { row, (id, motion) ->
            val bounds = FigureRenderer.bounds(motion)
            ps.forEachIndexed { col, p ->
                val shapes = FigureRenderer.frame(motion, p, setOf(Part.THIGH, Part.TORSO))
                drawShapes(g, shapes, bounds, col * tileW, row * tileH, tileW, tileH)
                g.color = Color.DARK_GRAY
                g.font = Font("SansSerif", Font.BOLD, 13)
                g.drawString("$id  p=$p", col * tileW + 6, row * tileH + 16)
            }
            g.color = Color.LIGHT_GRAY
            g.drawLine(0, row * tileH, img.width, row * tileH)
        }
        g.dispose()
        val name = if (only == null) "figures-${page + 1}.png" else "figures-sel-${page + 1}.png"
        ImageIO.write(img, "png", File(name))
        println("wrote ${File(name).absolutePath}")
    }
}

/** Every step of a scripted loop, with the caption shown at that moment. */
fun renderScript(id: String) {
    val motion = Motions.all.getValue(id)
    val ps = (0 until 16).map { (it + 0.5f) / 16f }
    val tileW = 330
    val tileH = 270
    val cols = 4
    val img = BufferedImage(tileW * cols, tileH * ((ps.size + cols - 1) / cols), BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color.WHITE
    g.fillRect(0, 0, img.width, img.height)
    val bounds = FigureRenderer.bounds(motion)
    ps.forEachIndexed { i, p ->
        val x = (i % cols) * tileW
        val y = (i / cols) * tileH
        drawShapes(g, FigureRenderer.frame(motion, p, setOf(Part.TORSO)), bounds, x, y + 10, tileW, tileH - 10)
        g.color = Color.DARK_GRAY
        g.font = Font("SansSerif", Font.PLAIN, 11)
        g.drawString("%.2f %s".format(p, motion.caption(p * motion.timing.cycleMs)), x + 4, y + 14)
    }
    g.dispose()
    ImageIO.write(img, "png", File("script-$id.png"))
    println("wrote ${File("script-$id.png").absolutePath}")
}

fun renderMuscles() {
    val scale = 2.2f
    val gap = 40
    val img = BufferedImage(((BodyMap.W * 2) * scale + gap * 3).toInt(), (BodyMap.H * scale + 60).toInt(), BufferedImage.TYPE_INT_RGB)
    val g = img.createGraphics()
    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g.color = Color.WHITE
    g.fillRect(0, 0, img.width, img.height)
    fun poly(xs: FloatArray, ys: FloatArray, ox: Float, oy: Float): Path2D.Float {
        val path = Path2D.Float()
        for (i in xs.indices) {
            if (i == 0) path.moveTo(ox + xs[i] * scale, oy + ys[i] * scale) else path.lineTo(ox + xs[i] * scale, oy + ys[i] * scale)
        }
        path.closePath()
        return path
    }
    val colors = listOf(0xE4572E, 0x3D7EA6, 0x5BA55B, 0xC9A227, 0x8E5BA8, 0x2BA3A3, 0xB5525C, 0x6B8E23)
    BodySide.values().forEachIndexed { i, side ->
        val ox = gap + i * (BodyMap.W * scale + gap)
        val oy = 40f
        g.color = Color(0xE9E4DC)
        g.fill(poly(BodyMap.outline.first, BodyMap.outline.second, ox, oy))
        g.color = Color(0x9A8F84)
        g.stroke = BasicStroke(1.5f)
        g.draw(poly(BodyMap.outline.first, BodyMap.outline.second, ox, oy))
        BodyMap.regions(side).forEach { r ->
            g.color = Color(colors[r.muscle.ordinal % colors.size])
            g.fill(poly(r.xs, r.ys, ox, oy))
            g.color = Color.WHITE
            g.stroke = BasicStroke(1.5f)
            g.draw(poly(r.xs, r.ys, ox, oy))
        }
        g.color = Color.DARK_GRAY
        g.font = Font("SansSerif", Font.BOLD, 16)
        g.drawString(side.title, ox, 24f)
    }
    g.dispose()
    ImageIO.write(img, "png", File("muscles.png"))
    println("wrote ${File("muscles.png").absolutePath}")
}

fun drawShapes(g: Graphics2D, shapes: List<Shape2>, b: Bounds, x0: Int, y0: Int, w: Int, h: Int) {
    val pad = 0.06f
    val s = minOf(w * (1 - 2 * pad) / b.width, (h - 20) * (1 - 2 * pad) / b.height)
    val ox = x0 + (w - b.width * s) / 2 - b.minX * s
    val oy = y0 + 20 + ((h - 20) - b.height * s) / 2 - b.minY * s
    val clip = g.clip
    g.clipRect(x0, y0, w, h)
    for (sh in shapes) {
        g.color = Color(sh.color.toInt(), true)
        when (sh) {
            is Seg2 -> {
                g.stroke = BasicStroke(sh.w * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                g.draw(Line2D.Float(ox + sh.x1 * s, oy + sh.y1 * s, ox + sh.x2 * s, oy + sh.y2 * s))
            }
            is Dot2 -> g.fill(Ellipse2D.Float(ox + (sh.x - sh.r) * s, oy + (sh.y - sh.r) * s, 2 * sh.r * s, 2 * sh.r * s))
            is Poly2 -> {
                val path = Path2D.Float()
                for (i in sh.xs.indices) {
                    val px = ox + sh.xs[i] * s
                    val py = oy + sh.ys[i] * s
                    if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
                }
                path.closePath()
                g.fill(path)
                if (sh.stroke > 0) {
                    g.stroke = BasicStroke(sh.stroke * s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
                    g.draw(path)
                }
            }
        }
    }
    g.clip = clip
}
