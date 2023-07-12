package ru.byprogminer.coolvtgifbot.utils

import java.awt.*
import java.awt.font.TextAttribute
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.text.AttributedString
import kotlin.math.max


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

fun BufferedImage.placeText(options: PlaceTextOptions) = PlaceTextContext(options).run {
    placeText()
}

// object to encapsulate internal methods and prevent wasting namespace
private class PlaceTextContext(
    private val placeTextOptions: PlaceTextOptions,
) {

    fun BufferedImage.placeText() {
        val g = createGraphics()

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        g.paint = Color.WHITE

        g.font = defaultFont
        var textFonts = fontStack.apply(placeTextOptions.text)

        val fontSize = g.calcMaxFontSize(
            placeTextOptions.text,
            placeTextOptions.width,
            placeTextOptions.height,
            textFonts,
        )

        g.font = g.font.deriveFont(fontSize)
        textFonts = textFonts.map { (i, f) -> i to f.deriveFont(fontSize) }

        val text = g.placeLineBreaks(placeTextOptions.text, placeTextOptions.width, textFonts)

        val fontStack = fontStack.deriveFont(fontSize)
        val splitTextFonts = text.map { line -> fontStack.apply(line) }

        val x = placeTextOptions.x + (placeTextOptions.width.toFloat() / 2)
        val y = placeTextOptions.y + (placeTextOptions.height.toFloat() / 2)

        val linesDims = g.calcLinesDimensions(text, splitTextFonts)

        if (placeTextOptions.backgroundColor.alpha > 0) {
            g.color = placeTextOptions.backgroundColor
            g.drawBackground(linesDims, x, y, placeTextOptions.borderWidth)
        }

        g.color = placeTextOptions.color
        g.drawText(text, x, y, splitTextFonts, linesDims)
    }

    companion object {

        val defaultFont: Font
        val emojiFont: Font

        val fontStack: FontStack

        init {
            val defaultFontStream = PlaceTextContext::class.java
                .getResourceAsStream("/Roboto/Roboto-Regular.ttf")

            defaultFontStream.use {
                defaultFont = Font.createFont(Font.TRUETYPE_FONT, defaultFontStream)
            }

            val emojiFontStream = PlaceTextContext::class.java
                .getResourceAsStream("/NotoColorEmoji/NotoColorEmoji-Regular.ttf")

            emojiFontStream.use {
                emojiFont = Font.createFont(Font.TRUETYPE_FONT, emojiFontStream)
            }

            fontStack = FontStack(defaultFont, emojiFont)
        }

        fun Graphics.calcMaxFontSize(
            text: CharSequence,
            width: Int,
            height: Int,
            textFonts: List<Pair<Int, Font>>,
        ): Float {
            fun check(fontSize: Float): Boolean {
                var metrics = getFontMetrics(font.deriveFont(fontSize))
                var textFontsIndex = -1

                var i = 0
                var lastSpace = -1
                var requiredHeight = 0F
                var accumulatedWidth = 0
                var lastSpaceLine = ""
                var lastSpaceFontsIndex = 0
                val line = StringBuilder()
                while (i < text.length && requiredHeight < height) {
                    if (textFontsIndex + 1 < textFonts.size && textFonts[textFontsIndex + 1].first == i) {
                        accumulatedWidth += metrics.stringWidth(line.toString())
                        line.clear()

                        ++textFontsIndex
                        metrics = getFontMetrics(textFonts[textFontsIndex].second.deriveFont(fontSize))
                    }

                    if (text[i] == '\n') {
                        requiredHeight += metrics.getLineMetrics(line.toString(), this).height
                        line.clear()

                        ++i
                        lastSpace = -1
                        accumulatedWidth = 0
                        continue
                    }

                    line.append(text[i])
                    val currentLine = line.toString()
                    val lineWidth = accumulatedWidth + metrics.stringWidth(currentLine)

                    if (lineWidth > width) {
                        if (lastSpace < 0) {
                            return false
                        }

                        requiredHeight += metrics.getLineMetrics(lastSpaceLine, this).height
                        line.clear()

                        i = lastSpace + 1
                        lastSpace = -1
                        accumulatedWidth = 0
                        textFontsIndex = lastSpaceFontsIndex
                        metrics = getFontMetrics(textFonts[textFontsIndex].second.deriveFont(fontSize))
                        continue
                    }

                    if (text[i].isWhitespace()) {
                        lastSpace = i
                        lastSpaceLine = currentLine
                        lastSpaceFontsIndex = textFontsIndex
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

        fun Graphics.placeLineBreaks(text: String, width: Int, textFonts: List<Pair<Int, Font>>): List<String> {
            // TODO think about more centered algorithm

            var metrics = fontMetrics
            var textFontsIndex = -1

            var i = 0
            var lastSpace = -1
            var accumulatedWidth = 0
            var lastSpaceLine = ""
            var lastSpaceFontsIndex = 0
            val line = StringBuilder()
            val lines = mutableListOf<String>()
            while (i < text.length) {
                if (textFontsIndex + 1 < textFonts.size && textFonts[textFontsIndex + 1].first == i) {
                    accumulatedWidth += metrics.stringWidth(line.toString())
                    line.clear()

                    ++textFontsIndex
                    metrics = getFontMetrics(textFonts[textFontsIndex].second)
                }

                if (text[i] == '\n') {
                    lines.add(line.toString())
                    line.clear()

                    ++i
                    lastSpace = -1
                    accumulatedWidth = 0
                    continue
                }

                line.append(text[i])
                val currentLine = line.toString()
                val lineWidth = accumulatedWidth + metrics.stringWidth(currentLine)

                if (lineWidth > width) {
                    require(lastSpace >= 0) { "text doesn't fit in the given width" }

                    lines.add(lastSpaceLine)
                    line.clear()

                    i = lastSpace + 1
                    lastSpace = -1
                    accumulatedWidth = 0
                    textFontsIndex = lastSpaceFontsIndex
                    metrics = getFontMetrics(textFonts[textFontsIndex].second)
                    continue
                }

                if (text[i].isWhitespace()) {
                    lastSpace = i
                    lastSpaceLine = currentLine
                    lastSpaceFontsIndex = textFontsIndex
                }

                ++i
            }

            lines.add(line.toString())
            return lines.toList()
        }

        fun Graphics.calcLinesDimensions(text: List<String>, textFonts: List<List<Pair<Int, Font>>>): List<Point> =
            text.mapIndexed { index, line ->
                val lineChars = line.toCharArray()

                var lineWidth = 0F
                var lineHeight = 0F

                var prevI = 0
                var metrics = fontMetrics
                for ((i, font) in textFonts[index]) {
                    lineWidth += metrics.charsWidth(lineChars, prevI, i - prevI)

                    val lineMetrics = metrics.getLineMetrics(line, prevI, i - prevI, this)
                    lineHeight = max(lineHeight, lineMetrics.height)

                    prevI = i
                    metrics = getFontMetrics(font)
                }

                lineWidth += metrics.charsWidth(lineChars, prevI, line.length - prevI)

                val lineMetrics = metrics.getLineMetrics(line, prevI, line.length - prevI, this)
                lineHeight = max(lineHeight, lineMetrics.height)

                return@mapIndexed Point(lineWidth, lineHeight)
            }

        fun Graphics2D.drawBackground(
            linesDims: List<Point>,
            x: Float,
            y: Float,
            borderWidth: Int,
        ) {
            if (linesDims.isEmpty()) {
                return
            }

            val points = calcTextPoints(linesDims, x, y)

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

        fun calcTextPoints(
            linesDims: List<Point>,
            x: Float,
            y: Float,
        ): List<Point> {
            if (linesDims.isEmpty()) {
                return listOf()
            }

            var lineY = y - linesDims.map { it.y }.sum() / 2
            val deque = ArrayDeque<Point>()

            for (i in linesDims.indices) {
                val lineWidth = linesDims[i].x
                val lineHeight = linesDims[i].y

                val lineX = x - lineWidth / 2
                deque.addFirst(Point(lineX, lineY))
                deque.addFirst(Point(lineX, lineY + lineHeight))
                deque.addLast(Point(lineX + lineWidth, lineY))
                deque.addLast(Point(lineX + lineWidth, lineY + lineHeight))

                lineY += lineHeight
            }

            return deque.toList()
        }

        fun Graphics2D.drawText(
            text: List<String>,
            x: Float,
            y: Float,
            textFonts: List<List<Pair<Int, Font>>>,
            linesDims: List<Point>,
        ) {
            var lineY = y - linesDims.map { it.y }.sum() / 2

            for (i in text.indices) {
                val lineHeight = linesDims[i].y
                val lineWidth = linesDims[i].x
                val lineX = x - lineWidth / 2
                
                val ascent: Float
                val attributedLine = AttributedString(text[i])

                if (textFonts[i].isEmpty()) {
                    ascent = fontMetrics.getLineMetrics(text[i], this).ascent
                } else {
                    var ascentAccumulator = 0F

                    var prevJ = 0
                    var metrics = fontMetrics
                    for ((j, font) in textFonts[i]) {
                        ascentAccumulator = max(
                            ascentAccumulator,
                            metrics.getLineMetrics(text[i], prevJ, j - prevJ, this).ascent,
                        )

                        if (prevJ < j) {
                            attributedLine.addAttribute(TextAttribute.FONT, font, prevJ, j)
                        }

                        prevJ = j
                        metrics = getFontMetrics(font)
                    }

                    ascent = max(
                        ascentAccumulator,
                        metrics.getLineMetrics(text[i], prevJ, text.size - prevJ, this).ascent,
                    )

                    attributedLine.addAttribute(TextAttribute.FONT, font, prevJ, text.size)
                }

                drawString(attributedLine.iterator, lineX, lineY + ascent)
                lineY += lineHeight
            }
        }
    }

    data class Point(
        val x: Float,
        val y: Float,
    )
}
