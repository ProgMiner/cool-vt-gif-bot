package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.Resource
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import java.awt.Color
import java.nio.file.Path


abstract class BackgroundGifFactory(
    name: String,
    originalGif: Resource,
    private val color: Color = Color.WHITE,
    private val backgroundColor: Color = Color.BLACK,
    private val borderWidth: Int = 5,
) : AbstractGifFactory(name, originalGif) {

    private companion object {

        val FRACTION: Pair<Int, Int> = 1 to 3
    }

    private val options by lazy {
        val x = borderWidth
        val width = metadata.width - borderWidth * 2
        val height = metadata.height * FRACTION.first / FRACTION.second
        val y = metadata.height - height

        return@lazy PlaceTextOptions(
            text = "",
            x = x,
            y = y,
            width = width,
            height = height,
            color = color,
            borderWidth = borderWidth,
            backgroundColor = backgroundColor,
        )
    }

    override suspend fun createOriginalGif(text: String, resultPath: Path) {
        placeText(options.copy(text = text), resultPath)
    }
}
