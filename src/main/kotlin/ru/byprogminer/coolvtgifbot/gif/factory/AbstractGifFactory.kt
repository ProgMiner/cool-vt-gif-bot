package ru.byprogminer.coolvtgifbot.gif.factory

import kotlinx.coroutines.*
import org.bytedeco.ffmpeg.global.avcodec.AV_CODEC_ID_H264
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.bytedeco.javacv.Java2DFrameConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import ru.byprogminer.coolvtgifbot.gif.GifMetadata
import ru.byprogminer.coolvtgifbot.utils.PlaceTextOptions
import ru.byprogminer.coolvtgifbot.utils.placeText
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.annotation.PostConstruct
import javax.imageio.ImageIO
import kotlin.coroutines.CoroutineContext


abstract class AbstractGifFactory(
    final override val name: String,
    final override val originalGif: Resource,
    private val thumbnailGif: Resource = originalGif,
) : GifFactory {

    final override lateinit var metadata: GifMetadata

    // TODO remove frames caching and replace with overlaying by ffmpeg internally,
    //      in order to do that all videos must be encoded in non-compressing format like AVI
    private lateinit var frames: List<Resource>
    private var frameRate: Double = .0

    @Value("\${gif.cache.path}/frames")
    private lateinit var cacheDir: Path

    @Autowired
    private lateinit var coroutineContext: CoroutineContext

    override suspend fun createGif(
        text: String?,
        thumbnail: Boolean,
        resultPath: Path,
    ) = withContext(coroutineContext) {
        if (text == null && !thumbnail) {
            withContext(Dispatchers.IO) {
                Files.newOutputStream(resultPath).use {
                    originalGif.inputStream.copyTo(it)
                }
            }

            return@withContext
        }

        if (text == null) {
            withContext(Dispatchers.IO) {
                Files.newOutputStream(resultPath).use {
                    thumbnailGif.inputStream.copyTo(it)
                }
            }

            return@withContext
        }

        createOriginalGif(text, resultPath)
    }

    protected abstract suspend fun createOriginalGif(text: String, resultPath: Path)

    protected suspend fun placeText(options: PlaceTextOptions, resultPath: Path) = withContext(coroutineContext) {
        val overlay = BufferedImage(metadata.width, metadata.height, BufferedImage.TYPE_4BYTE_ABGR)
        overlay.placeText(options)

        val rec = FFmpegFrameRecorder(resultPath.toFile(), metadata.width, metadata.height)
        rec.videoCodec = AV_CODEC_ID_H264
        rec.frameRate = frameRate
        rec.format = "mp4"

        val cvt = Java2DFrameConverter()
        rec.start()

        rec.use {
            frames.forEach { frame ->
                val newFrame = ImageIO.read(frame.inputStream)

                newFrame.graphics.drawImage(overlay, 0, 0, null)
                rec.record(cvt.getFrame(newFrame))
            }
        }
    }

    @PostConstruct
    private fun init() {
        val framesDir = cacheDir.resolve(name)
        val cvt = Java2DFrameConverter()

        val grabber = FFmpegFrameGrabber(originalGif.inputStream)

        CoroutineScope(coroutineContext).launch {
            grabber.start()

            grabber.use {
                metadata = GifMetadata(
                    width = grabber.imageWidth,
                    height = grabber.imageHeight,
                    duration = (grabber.lengthInTime / 1000000).toInt(),
                )

                frameRate = grabber.frameRate

                if (!Files.isDirectory(framesDir)) {
                    Files.createDirectories(framesDir)

                    frames = List(grabber.lengthInVideoFrames) { idx ->
                        val frame = cvt.getBufferedImage(grabber.grabImage())

                        val framePath = framesDir.resolve(idx.toString())
                        ImageIO.write(frame, "BMP", framePath.toFile())

                        return@List FileSystemResource(framePath)
                    }
                } else {
                    frames = List(grabber.lengthInVideoFrames) { idx ->
                        FileSystemResource(framesDir.resolve(idx.toString()))
                    }
                }
            }
        }
    }
}
