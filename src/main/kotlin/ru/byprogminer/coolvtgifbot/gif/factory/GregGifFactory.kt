package ru.byprogminer.coolvtgifbot.gif.factory

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import ru.byprogminer.coolvtgifbot.utils.coroutinePlaceText
import java.awt.Color
import java.nio.file.Path


@Service
class GregGifFactory : AbstractGifFactory(
    ClassPathResource("gif/greg.mp4"),
) {

    override suspend fun createOriginalGif(text: String, resultPath: Path) {
        FFmpegFrameGrabber(originalGif.inputStream).coroutinePlaceText(PlaceTextOptions(
            text = text,
            x = 5,
            y = 125,
            width = 198,
            height = 30,
            color = Color.WHITE,
            borderWidth = 5,
            backgroundColor = Color.BLACK,
        ), resultPath)
    }
}
