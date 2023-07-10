package ru.byprogminer.coolvtgifbot.gif.factory

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.springframework.core.io.Resource
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import ru.byprogminer.coolvtgifbot.utils.placeText
import java.awt.Color
import java.nio.file.Path


abstract class BackgroundGifFactory(
    originalGif: Resource,
    private val color: Color = Color.WHITE,
    private val backgroundColor: Color = Color.BLACK,
    private val borderWidth: Int = 5,
) : AbstractGifFactory(originalGif) {

    private companion object {

        val FRACTION: Pair<Int, Int> = 1 to 3
    }

    private val options by lazy {
        val meta = metadata

        val x = borderWidth
        val width = meta.width - borderWidth * 2
        val height = meta.height * FRACTION.first / FRACTION.second
        val y = meta.height - height

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
        FFmpegFrameGrabber(originalGif.inputStream).placeText(options.copy(text = text), resultPath)
    }
}
