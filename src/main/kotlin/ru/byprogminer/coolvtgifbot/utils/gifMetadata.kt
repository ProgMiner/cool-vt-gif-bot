package ru.byprogminer.coolvtgifbot.utils

import org.bytedeco.javacv.FrameGrabber
import ru.byprogminer.coolvtgifbot.gif.GifMetadata


val FrameGrabber.gifMetadata: GifMetadata get() = use {
    start()

    return GifMetadata(
        width = imageWidth,
        height = imageHeight,
        duration = (lengthInTime / 1000000).toInt(),
    )
}
