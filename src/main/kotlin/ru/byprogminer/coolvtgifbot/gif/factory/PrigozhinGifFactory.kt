package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service


@Service
class PrigozhinGifFactory : BackgroundGifFactory(
    "prigozhin",
    ClassPathResource("gif/prigozhin.mp4"),
)
