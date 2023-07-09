package ru.byprogminer.coolvtgifbot.gif.factory

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import ru.byprogminer.coolvtgifbot.utils.coroutinePlaceText
import java.awt.Color
import java.nio.file.Path


@Service
class Greg1GifFactory : AbstractGifFactory(
    ClassPathResource("gif/greg1.mp4"),
) {

    override suspend fun createOriginalGif(text: String, resultPath: Path) {
        FFmpegFrameGrabber(originalGif.inputStream).coroutinePlaceText(PlaceTextOptions(
            text = text,
            x = 5,
            y = 25,
            width = 470,
            height = 65,
            color = Color.WHITE,
            borderWidth = 5,
            backgroundColor = Color.BLACK,
        ), resultPath)
    }
}
