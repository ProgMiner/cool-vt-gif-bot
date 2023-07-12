package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import java.awt.Color
import java.nio.file.Path


@Service
class WomanGifFactory : AbstractGifFactory(
    "woman",
    ClassPathResource("gif/woman.mp4"),
) {

    override suspend fun createOriginalGif(text: String, resultPath: Path) {
        placeText(PlaceTextOptions(
            text = text,
            x = 5,
            y = 5,
            width = 310,
            height = 55,
            color = Color.BLACK,
            borderWidth = 5,
            backgroundColor = Color(0, 0, 0, 0),
        ), resultPath)
    }
}
