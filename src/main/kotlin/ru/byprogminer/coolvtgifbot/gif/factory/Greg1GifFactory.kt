package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import java.awt.Color
import java.nio.file.Path


@Service
class Greg1GifFactory : AbstractGifFactory(
    "greg1",
    ClassPathResource("gif/greg1.mp4"),
) {

    override suspend fun createOriginalGif(text: String, resultPath: Path) {
        placeText(PlaceTextOptions(
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
