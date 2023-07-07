package ru.byprogminer.coolvtgifbot.utils

import java.awt.image.BufferedImage


fun BufferedImage.clone(): BufferedImage {
    val cm = colorModel

    return BufferedImage(cm, copyData(null), cm.isAlphaPremultiplied, null)
}
