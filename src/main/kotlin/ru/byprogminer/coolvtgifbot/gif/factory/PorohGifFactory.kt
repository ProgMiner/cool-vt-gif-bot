package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service


@Service
class PorohGifFactory : BackgroundGifFactory(
    "poroh",
    ClassPathResource("gif/poroh.mp4"),
)
