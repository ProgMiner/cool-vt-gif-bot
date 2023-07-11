package ru.byprogminer.coolvtgifbot.gif.factory

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Java2DFrameConverter
import org.springframework.core.io.Resource
import ru.byprogminer.coolvtgifbot.gif.GifMetadata
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import ru.byprogminer.coolvtgifbot.utils.clone
import ru.byprogminer.coolvtgifbot.utils.placeText
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path


abstract class AbstractGifFactory(
    final override val originalGif: Resource,
    private val thumbnailGif: Resource = originalGif,
) : GifFactory {

    final override val metadata: GifMetadata

    // TODO remove frames caching and replace with overlaying by ffmpeg internally,
    //      in order to do that all videos must be encoded in non-compressing format like AVI
    private val frames: List<BufferedImage>
    private val frameRate: Double

    init {
        val cvt = Java2DFrameConverter()

        val grabber = FFmpegFrameGrabber(originalGif.inputStream)
        grabber.start()

        grabber.use {
            metadata = GifMetadata(
                width = grabber.imageWidth,
                height = grabber.imageHeight,
                duration = (grabber.lengthInTime / 1000000).toInt(),
            )

            // TODO maybe it could be moved into another thread to parallel caching all videos?
            frames = List(grabber.lengthInVideoFrames) {
                cvt.getBufferedImage(grabber.grabImage()).clone()
            }

            frameRate = grabber.frameRate
        }
    }

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

    protected suspend fun placeText(options: PlaceTextOptions, resultPath: Path) = coroutineScope {
        val overlay = BufferedImage(metadata.width, metadata.height, BufferedImage.TYPE_4BYTE_ABGR)
        overlay.placeText(options)

        val frames = frames.map { frame ->
            async {
                val newFrame = frame.clone()

                newFrame.graphics.drawImage(overlay, 0, 0, null)
                return@async newFrame
            }
        }

        val rec = FFmpegFrameRecorder(resultPath.toFile(), metadata.width, metadata.height)
        rec.videoCodec = AV_CODEC_ID_H264
        rec.frameRate = frameRate
        rec.format = "mp4"

        val cvt = Java2DFrameConverter()
        withContext(Dispatchers.IO) {
            rec.start()

            rec.use {
                frames.forEach { frame ->
                    rec.record(cvt.getFrame(frame.await()))
                }
            }
        }
    }
}
