package ru.byprogminer.coolvtgifbot.gif

import org.apache.catalina.util.URLEncoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.core.task.AsyncListenableTaskExecutor
import org.springframework.scheduling.annotation.AsyncResult
import org.springframework.stereotype.Service
import org.springframework.util.concurrent.ListenableFuture
import ru.byprogminer.coolvtgifbot.gif.factory.GifFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.Callable


@Service
class GifFacade(
    @Value("\${tg.host}")
    private val host: String,
    private val gifFactories: List<GifFactory>,
    private val executor: AsyncListenableTaskExecutor,
    @Value("\${gif.cache.size}")
    private val cacheSize: Int,
    @Value("\${gif.cache.path}")
    private val cachePath: Path,
) {

    companion object {

        const val ORIGINAL_KIND = "orig"
        const val THUMBNAIL_KIND = "thumb"
    }

    private val cache = mutableMapOf<String, ListenableFuture<out Resource>>()
    private val cacheKeys = LinkedList<String>()

    init {
        if (Files.isDirectory(cachePath)) {
            Files.list(cachePath).use {
                it.forEach(Files::deleteIfExists)
            }
        }
    }

    /**
     * @param text to place on GIF
     * @param maxSize size of page
     * @param offset pagination offset
     * @param startMaking start making of GIFs
     *
     * @return (list of (original GIF, thumbnail GIF, GIF metadata), amount of remaining items)
     */
    fun getGifLinks(
        text: String?,
        maxSize: Int,
        offset: Int,
        startMaking: Boolean,
    ): Pair<List<Triple<String, String, GifMetadata>>, Int> {
        val tail = gifFactories.indices.drop(offset)

        val page = tail.take(maxSize)

        if (startMaking) {
            page.forEach { index ->
                makeGif(index, text, false)
                makeGif(index, text, true)
            }
        }

        val links = page.map { index ->
            Triple(
                makeLink(index, text, false),
                makeLink(index, text, true),
                gifFactories[index].metadata,
            )
        }

        return links to tail.size
    }

    fun makeGif(index: Int, text: String?, thumbnail: Boolean): ListenableFuture<out Resource?> {
        if (index < 0) {
            throw IllegalArgumentException("index cannot be negative")
        }

        if (index >= gifFactories.size) {
            return AsyncResult(null)
        }

        val cacheKey = "$index/$thumbnail/$text"
        val result = cache.computeIfAbsent(cacheKey) {
            cacheKeys.push(cacheKey)

            return@computeIfAbsent makeGif0(index, text, thumbnail)
        }

        if (cache.size > cacheSize) {
            val deletedFuture = cache.remove(cacheKeys.pop())!!

            deletedFuture.completable().thenAccept { deleted ->
                if (deleted is FileSystemResource) {
                    Files.deleteIfExists(Paths.get(deleted.path))
                }
            }
        }

        return result
    }

    private fun makeGif0(index: Int, text: String?, thumbnail: Boolean): ListenableFuture<out Resource> {
        if (text == null && !thumbnail) {
            val result = gifFactories[index].originalGif

            if (result != null) {
                return AsyncResult(result)
            }
        }

        val uuid = UUID.randomUUID()
        val path = cachePath.resolve(uuid.toString())

        Files.createDirectories(cachePath)

        return executor.submitListenable(Callable {
            gifFactories[index].createGif(text, thumbnail, path)

            return@Callable FileSystemResource(path)
        })
    }

    private fun makeLink(index: Int, text: String?, thumbnail: Boolean): String {
        val kind = when (thumbnail) {
            true -> THUMBNAIL_KIND
            else -> ORIGINAL_KIND
        }

        // preventing telegram caching
        val time = LocalDateTime.now()

        return if (text != null) {
            "$host/api/gif/$index/$kind/${text.urlEncoded}?$time"
        } else {
            "$host/api/gif/$index/$kind?$time"
        }
    }

    private val Any.urlEncoded get() = URLEncoder.DEFAULT.encode(toString(), Charsets.UTF_8)
}
