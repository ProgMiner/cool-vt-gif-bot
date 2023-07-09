package ru.byprogminer.coolvtgifbot.gif.factory

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import ru.byprogminer.coolvtgifbot.utils.placeText
import java.awt.Color
import java.nio.file.Path


@Service
class GregGifFactory: AbstractGifFactory(
    ClassPathResource("gif/greg.mp4"),
) {

    override fun createOriginalGif(text: String, resultPath: Path) {
        FFmpegFrameGrabber(originalGif.inputStream).placeText(PlaceTextOptions(
            text = text,
            x = 5,
            y = 125,
            width = 198,
            height = 35,
            color = Color.WHITE,
            borderWidth = 5,
            backgroundColor = Color(0, 0, 0, 0),
        ), resultPath)
    }
}
