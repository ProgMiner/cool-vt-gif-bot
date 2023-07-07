package ru.byprogminer.coolvtgifbot.gif.factory

import org.bytedeco.javacv.FFmpegFrameGrabber
import org.springframework.core.io.Resource
import ru.byprogminer.coolvtgifbot.utils.gifMetadata
import java.nio.file.Files
import java.nio.file.Path


abstract class AbstractGifFactory(
    final override val originalGif: Resource,
    private val thumbnailGif: Resource = originalGif,
) : GifFactory {

    final override val metadata = FFmpegFrameGrabber(originalGif.inputStream).gifMetadata

    override fun createGif(text: String?, thumbnail: Boolean, resultPath: Path) {
        if (text == null && !thumbnail) {
            Files.newOutputStream(resultPath).use {
                originalGif.inputStream.copyTo(it)
            }
            return
        }

        if (text == null) {
            Files.newOutputStream(resultPath).use {
                thumbnailGif.inputStream.copyTo(it)
            }
            return
        }

        createOriginalGif(text, resultPath)
    }

    protected abstract fun createOriginalGif(text: String, resultPath: Path)
}