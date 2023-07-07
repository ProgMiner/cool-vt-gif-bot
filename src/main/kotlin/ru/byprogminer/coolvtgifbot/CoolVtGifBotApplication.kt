package ru.byprogminer.coolvtgifbot

import org.bytedeco.javacv.FFmpegLogCallback
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication


@SpringBootApplication
class CoolVtGifBotApplication

fun main(args: Array<String>) {
    FFmpegLogCallback.set()

    runApplication<CoolVtGifBotApplication>(*args)
}
