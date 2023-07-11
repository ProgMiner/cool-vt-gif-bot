package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service


@Service
class PistoletovGifFactory : BackgroundGifFactory(
    "pistoletov",
    ClassPathResource("gif/pistoletov.mp4"),
)
