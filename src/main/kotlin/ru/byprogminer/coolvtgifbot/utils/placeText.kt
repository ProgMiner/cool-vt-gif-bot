package ru.byprogminer.coolvtgifbot.utils

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Java2DFrameConverter
import java.awt.Color
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.nio.file.Path


data class PlaceTextOptions(
    val text: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val color: Color,
    val borderWidth: Int,
    val backgroundColor: Color,
)

fun FFmpegFrameGrabber.placeText(options: PlaceTextOptions, resultPath: Path) = PlaceTextContext(options).run {
    placeText(resultPath)
}

// object to encapsulate internal methods and prevent wasting namespace
private class PlaceTextContext(
    private val placeTextOptions: PlaceTextOptions,
) {

    lateinit var renderedText: BufferedImage

    fun FFmpegFrameGrabber.placeText(resultPath: Path) = use {
        val cvt = Java2DFrameConverter()

        start()

        renderedText = BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_4BYTE_ABGR).apply {
            val g = createGraphics()

            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)

            val fontSize = g.calcMaxFontSize(placeTextOptions.text, placeTextOptions.width, placeTextOptions.height)
            val text = g.placeLineBreaks(placeTextOptions.text, placeTextOptions.width)

            // TODO change font
            g.font = g.font.deriveFont(fontSize)

            val x = placeTextOptions.x + (placeTextOptions.width.toFloat() / 2)
            val y = placeTextOptions.y + (placeTextOptions.height.toFloat() / 2)

            if (placeTextOptions.backgroundColor.alpha > 0) {
                g.color = placeTextOptions.backgroundColor
                g.drawBackground(text, x, y, placeTextOptions.borderWidth)
            }

            g.color = placeTextOptions.color
            g.drawText(text, x, y)
        }

        val rec = FFmpegFrameRecorder(resultPath.toFile(), imageWidth, imageHeight, audioChannels)
        rec.audioCodec = audioCodec
        rec.videoCodec = videoCodec
        rec.format = "mp4"
        rec.start()

        rec.use {
            for (i in 0 until lengthInFrames) {
                rec.record(cvt.convert(cvt.convert(grabImage()).placeText()))
            }
        }
    }

    fun BufferedImage.placeText(): BufferedImage {
        val result = clone()

        result.createGraphics().drawImage(renderedText, 0, 0, null)
        return result
    }

    companion object {

        fun Graphics.calcMaxFontSize(text: String, width: Int, height: Int): Float {
            fun check(fontSize: Float): Boolean {
                val metrics = getFontMetrics(font.deriveFont(fontSize))

                var i = 0
                var lineStart = 0
                var lastSpace = -1
                var requiredHeight = 0F
                val line = StringBuilder()
                while (i < text.length && requiredHeight < height) {
                    if (text[i] == '\n') {
                        requiredHeight += metrics.getLineMetrics(line.toString(), this).height
                        line.clear()

                        ++i
                        lineStart = i
                        lastSpace = -1
                        continue
                    }

                    line.append(text[i])
                    val lineWidth = metrics.stringWidth(line.toString())

                    if (lineWidth > width) {
                        if (lastSpace < 0) {
                            return false
                        }

                        line.delete(lastSpace - lineStart, line.length)
                        requiredHeight += metrics.getLineMetrics(line.toString(), this).height
                        line.clear()

                        i = lastSpace + 1
                        lineStart = i
                        lastSpace = -1
                        continue
                    }

                    if (text[i].isWhitespace()) {
                        lastSpace = i
                    }

                    ++i
                }

                requiredHeight += metrics.getLineMetrics(line.toString(), this).height
                return requiredHeight <= height
            }

            var l = 1.0F
            var r = height.toFloat()

            while (l < r - 1e-2) {
                val m = l + (r - l) / 2

                if (check(m)) {
                    l = m
                } else {
                    r = m
                }
            }

            return l
        }

        fun Graphics.placeLineBreaks(text: String, width: Int): List<String> {
            // TODO think about more centered algorithm

            val metrics = fontMetrics

            var i = 0
            var lineStart = 0
            var lastSpace = -1
            val line = StringBuilder()
            val lines = mutableListOf<String>()
            while (i < text.length) {
                if (text[i] == '\n') {
                    lines.add(line.toString())
                    line.clear()

                    ++i
                    lineStart = i
                    lastSpace = -1
                    continue
                }

                line.append(text[i])
                val lineWidth = metrics.stringWidth(line.toString())

                if (lineWidth > width) {
                    line.delete(lastSpace - lineStart, line.length)
                    lines.add(line.toString())
                    line.clear()

                    i = lastSpace + 1
                    lineStart = i
                    lastSpace = -1
                    continue
                }

                if (text[i].isWhitespace()) {
                    lastSpace = i
                }

                ++i
            }

            lines.add(line.toString())
            return lines.toList()
        }

        fun Graphics2D.drawBackground(
            text: List<String>,
            x: Float,
            y: Float,
            borderWidth: Int,
        ) {
            if (text.isEmpty()) {
                return
            }

            val points = calcTextPoints(text, x, y)

            val newPoints = points.indices.map { i ->
                val (px, py) = points[(i + points.size - 1) % points.size]
                val (cx, cy) = points[i]
                val (nx, ny) = points[(i + 1) % points.size]

                return@map when {
                    px == cx && py < cy && cx < nx && cy == ny -> Point(cx + borderWidth, cy - borderWidth)
                    px == cx && py < cy && cx > nx && cy == ny -> Point(cx + borderWidth, cy + borderWidth)
                    px == cx && py > cy && cx < nx && cy == ny -> Point(cx - borderWidth, cy - borderWidth)
                    px == cx && py > cy && cx > nx && cy == ny -> Point(cx - borderWidth, cy + borderWidth)
                    px < cx && py == cy && cx == nx && cy < ny -> Point(cx + borderWidth, cy - borderWidth)
                    px < cx && py == cy && cx == nx && cy > ny -> Point(cx - borderWidth, cy - borderWidth)
                    px > cx && py == cy && cx == nx && cy < ny -> Point(cx + borderWidth, cy + borderWidth)
                    px > cx && py == cy && cx == nx && cy > ny -> Point(cx - borderWidth, cy + borderWidth)
                    else -> throw RuntimeException("wtf")
                }
            }

            val path = Path2D.Float()
            path.moveTo(newPoints[0].x, newPoints[0].y)

            for (i in 1 until newPoints.size) {
                path.lineTo(newPoints[i].x, newPoints[i].y)
            }

            fill(path)
        }

        fun Graphics.calcTextPoints(text: List<String>, x: Float, y: Float): List<Point> {
            if (text.isEmpty()) {
                return listOf()
            }

            val metrics = fontMetrics

            val linesMetrics = text.map { metrics.getLineMetrics(it, this) }
            var lineY = y - linesMetrics.map { it.height }.sum() / 2

            val deque = ArrayDeque<Point>()

            for (i in text.indices) {
                val lineHeight = linesMetrics[i].height
                val lineWidth = metrics.stringWidth(text[i])
                val lineX = x - lineWidth / 2

                deque.addFirst(Point(lineX, lineY))
                deque.addFirst(Point(lineX, lineY + lineHeight))
                deque.addLast(Point(lineX + lineWidth, lineY))
                deque.addLast(Point(lineX + lineWidth, lineY + lineHeight))

                lineY += lineHeight
            }

            return deque.toList()
        }

        fun Graphics2D.drawText(text: List<String>, x: Float, y: Float) {
            val metrics = fontMetrics

            val linesMetrics = text.map { metrics.getLineMetrics(it, this) }
            var lineY = y - linesMetrics.map { it.height }.sum() / 2

            for (i in text.indices) {
                val lineHeight = linesMetrics[i].height
                val lineWidth = metrics.stringWidth(text[i])
                val lineX = x - lineWidth / 2

                drawString(text[i], lineX, lineY + linesMetrics[i].ascent)
                lineY += lineHeight
            }
        }
    }

    data class Point(
        val x: Float,
        val y: Float,
    )
}

