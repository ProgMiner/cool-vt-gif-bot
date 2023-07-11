package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service


@Service
class TsoyGifFactory : BackgroundGifFactory(
    "tsoy",
    ClassPathResource("gif/tsoy.mp4"),
)
