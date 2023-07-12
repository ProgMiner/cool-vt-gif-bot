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
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext


@Service
class GifFacade(
    @Value("\${tg.host}")
    private val host: String,
    @Value("\${gif.cache.size}")
    private val cacheSize: Int,
    @Value("\${gif.cache.path}")
    private val cachePath: Path,
    gifFactories: List<GifFactory>,
    private val coroutineContext: CoroutineContext,
) {

    companion object {

        const val ORIGINAL_KIND = "orig"
        const val THUMBNAIL_KIND = "thumb"
    }

    private val gifFactories: Map<String, GifFactory>

    private val cache: MutableMap<String, Deferred<Resource>> = ConcurrentHashMap()
    private val cacheKeys: Queue<String> = ConcurrentLinkedQueue()

    init {
        this.gifFactories = gifFactories.associateBy { it.name }

        require(this.gifFactories.size == gifFactories.size) { "names of GIF factories must be unique" }

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
        val tail = gifFactories.keys.drop(offset)

        val page = tail.take(maxSize)

        if (startMaking) {
            CoroutineScope(coroutineContext).run {
                page.forEach { key ->
                    launch { makeGif(key, text, false) }
                    launch { makeGif(key, text, true) }
                }
            }
        }

        val links = page.map { key ->
            Triple(
                makeLink(key, text, false),
                makeLink(key, text, true),
                gifFactories[key]!!.metadata,
            )
        }

        return links to tail.size
    }

    suspend fun makeGif(key: String, text: String?, thumbnail: Boolean): Resource? = withContext(coroutineContext) {
        if (key !in gifFactories) {
            return@withContext null
        }

        val cacheKey = "$key/$thumbnail/$text"

        val result = cache.computeIfAbsent(cacheKey) {
            cacheKeys.add(cacheKey)

            return@computeIfAbsent async { makeGif0(key, text, thumbnail) }
        }

        if (cache.size > cacheSize) {
            val deleted = cache.remove(cacheKeys.remove())!!.await()

            if (deleted is FileSystemResource) {
                withContext(Dispatchers.IO) {
                    Files.deleteIfExists(Paths.get(deleted.path))
                }
            }
        }

        return@withContext result.await()
    }

    private suspend fun makeGif0(key: String, text: String?, thumbnail: Boolean): Resource {
        if (text == null && !thumbnail) {
            val result = gifFactories[key]!!.originalGif

            if (result != null) {
                return result
            }
        }

        val uuid = UUID.randomUUID()
        val path = cachePath.resolve(uuid.toString())

        withContext(Dispatchers.IO) {
            Files.createDirectories(cachePath)
        }

        gifFactories[key]!!.createGif(text, thumbnail, path)

        return FileSystemResource(path)
    }

    private fun makeLink(key: String, text: String?, thumbnail: Boolean): String {
        val kind = when (thumbnail) {
            true -> THUMBNAIL_KIND
            else -> ORIGINAL_KIND
        }

        // preventing telegram caching
        // val time = LocalDateTime.now()

        return if (text != null) {
            // "$host/api/gif/$key/$kind/${text.urlEncoded}?$time"
            "$host/api/gif/$key/$kind/${text.urlEncoded}"
        } else {
            // "$host/api/gif/$key/$kind?$time"
            "$host/api/gif/$key/$kind"
        }
    }
}
