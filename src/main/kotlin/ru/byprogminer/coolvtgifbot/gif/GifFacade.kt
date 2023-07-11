package ru.byprogminer.coolvtgifbot.gif

import kotlinx.coroutines.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.gif.factory.GifFactory
import ru.byprogminer.coolvtgifbot.utils.urlEncoded
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue


@Service
class GifFacade(
    @Value("\${tg.host}")
    private val host: String,
    @Value("\${gif.cache.size}")
    private val cacheSize: Int,
    @Value("\${gif.cache.path}")
    private val cachePath: Path,
    private val gifFactories: List<GifFactory>,
) {

    companion object {

        const val ORIGINAL_KIND = "orig"
        const val THUMBNAIL_KIND = "thumb"
    }

    private val cache: MutableMap<String, Deferred<Resource>> = ConcurrentHashMap()
    private val cacheKeys: Queue<String> = ConcurrentLinkedQueue()

    init {
        if (Files.isDirectory(cachePath)) {
            Files.list(cachePath).use { files ->
                files.forEach {
                    if (Files.isRegularFile(it)) {
                        Files.deleteIfExists(it)
                    }
                }
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
            CoroutineScope(Dispatchers.Default).run {
                page.forEach { index ->
                    launch { makeGif(index, text, false) }
                    launch { makeGif(index, text, true) }
                }
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

    suspend fun makeGif(index: Int, text: String?, thumbnail: Boolean): Resource? = coroutineScope {
        if (index < 0) {
            throw IllegalArgumentException("index cannot be negative")
        }

        if (index >= gifFactories.size) {
            return@coroutineScope null
        }

        val cacheKey = "$index/$thumbnail/$text"

        val result = cache.computeIfAbsent(cacheKey) {
            cacheKeys.add(cacheKey)

            return@computeIfAbsent async { makeGif0(index, text, thumbnail) }
        }

        if (cache.size > cacheSize) {
            val deleted = cache.remove(cacheKeys.remove())!!.await()

            if (deleted is FileSystemResource) {
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(Paths.get(deleted.path))
                }
            }
        }

        return@coroutineScope result.await()
    }

    private suspend fun makeGif0(index: Int, text: String?, thumbnail: Boolean): Resource {
        if (text == null && !thumbnail) {
            val result = gifFactories[index].originalGif

            if (result != null) {
                return result
            }
        }

        val uuid = UUID.randomUUID()
        val path = cachePath.resolve(uuid.toString())

        withContext(Dispatchers.IO) {
            Files.createDirectories(cachePath)
        }

        gifFactories[index].createGif(text, thumbnail, path)

        return FileSystemResource(path)
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
}
