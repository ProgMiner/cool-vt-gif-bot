package ru.byprogminer.coolvtgifbot.gif.factory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    override suspend fun createGif(text: String?, thumbnail: Boolean, resultPath: Path) {
        if (text == null && !thumbnail) {
            withContext(Dispatchers.IO) {
                Files.newOutputStream(resultPath).use {
                    originalGif.inputStream.copyTo(it)
                }
            }

            return
        }

        if (text == null) {
            withContext(Dispatchers.IO) {
                Files.newOutputStream(resultPath).use {
                    thumbnailGif.inputStream.copyTo(it)
                }
            }

            return
        }

        createOriginalGif(text, resultPath)
    }

    protected abstract suspend fun createOriginalGif(text: String, resultPath: Path)
}
