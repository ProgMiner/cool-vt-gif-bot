package ru.byprogminer.coolvtgifbot.gif

import org.apache.catalina.util.URLEncoder
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.gif.factory.GifFactory
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


@Service
class GifFacade(
    @Value("\${tg.host}")
    private val host: String,
    private val gifFactories: List<GifFactory>,
) {

    companion object {

        const val ORIGINAL_KIND = "orig"
        const val THUMBNAIL_KIND = "thumb"

        private const val CACHE_SIZE = 500
        private const val CACHE_PATH = "./cache"
    }

    private val cache = mutableMapOf<String, Resource>()
    private val cacheKeys = LinkedList<String>()

    init {
        val cacheDir = Paths.get(CACHE_PATH)

        if (Files.isDirectory(cacheDir)) {
            Files.list(cacheDir).use {
                it.forEach(Files::deleteIfExists)
            }
        }
    }

    /**
     * @param text to place on GIF
     * @param maxSize size of page
     * @param offset pagination offset
     *
     * @return (list of (original GIF, thumbnail GIF, GIF metadata), amount of remaining items)
     */
    fun getGifLinks(text: String?, maxSize: Int, offset: Int): Pair<List<Triple<String, String, GifMetadata>>, Int> {
        val tail = gifFactories.indices.drop(offset)

        val links = tail.take(maxSize).map {
            Triple(
                makeLink(it, text, false),
                makeLink(it, text, true),
                gifFactories[it].metadata,
            )
        }

        return links to tail.size
    }

    fun makeGif(index: Int, text: String?, thumbnail: Boolean): Resource? {
        if (index < 0) {
            throw IllegalArgumentException("index cannot be negative")
        }

        if (index >= gifFactories.size) {
            return null
        }

        val cacheKey = "$index/$thumbnail/$text"
        val result = cache.computeIfAbsent(cacheKey) {
            cacheKeys.push(cacheKey)

            return@computeIfAbsent makeGif0(index, text, thumbnail)
        }

        if (cache.size > CACHE_SIZE) {
            val deleted = cache.remove(cacheKeys.pop())

            if (deleted is FileSystemResource) {
                Files.deleteIfExists(Paths.get(deleted.path))
            }
        }

        return result
    }

    private fun makeGif0(index: Int, text: String?, thumbnail: Boolean): Resource {
        if (text == null && !thumbnail) {
            val result = gifFactories[index].originalGif

            if (result != null) {
                return result
            }
        }

        val uuid = UUID.randomUUID()
        val path = Paths.get("$CACHE_PATH/$uuid")

        Files.createDirectories(Paths.get(CACHE_PATH))
        gifFactories[index].createGif(text, thumbnail, path)

        return FileSystemResource(path)
    }

    private fun makeLink(index: Int, text: String?, thumbnail: Boolean): String {
        val kind = when (thumbnail) {
            true -> THUMBNAIL_KIND
            else -> ORIGINAL_KIND
        }

        return if (text != null) {
            "$host/api/gif/$index/$kind/${text.urlEncoded}"
        } else {
            "$host/api/gif/$index/$kind"
        }
    }

    private val Any.urlEncoded get() = URLEncoder.DEFAULT.encode(toString(), Charsets.UTF_8)
}
