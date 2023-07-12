package ru.byprogminer.coolvtgifbot.utils

import java.awt.Font


class FontStack(
    private val fonts: List<Font>,
) {

    constructor(vararg fonts: Font): this(fonts.toList())

    fun apply(text: String): List<Pair<Int, Font>> {
        val result = mutableListOf<Pair<Int, Font>>()

        var lastFont: Font? = null
        text.forEachIndexed { i, c ->
            val font = fonts.firstOrNull { font -> font.canDisplay(c) } ?: fonts.last()

            if (lastFont != font) {
                lastFont = font

                result.add(i to font)
            }
        }

        return result
    }

    fun deriveFont(fontSize: Float): FontStack = FontStack(fonts.map { it.deriveFont(fontSize) })
}
