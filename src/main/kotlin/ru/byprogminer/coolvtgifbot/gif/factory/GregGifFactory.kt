package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import java.awt.Color
import java.nio.file.Path


@Service
class GregGifFactory : AbstractGifFactory(
    "greg",
    ClassPathResource("gif/greg.mp4"),
) {

    override suspend fun createOriginalGif(text: String, resultPath: Path) {
        placeText(PlaceTextOptions(
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
