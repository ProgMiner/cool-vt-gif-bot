package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.Resource
import ru.byprogminer.coolvtgifbot.gif.GifMetadata
import java.nio.file.Path


interface GifFactory {

    /**
     * Name of GIF factory
     */
    val name: String

    /**
     * Metadata of producing GIFs
     */
    val metadata: GifMetadata

    /**
     * Resource or null if not supported
     */
    val originalGif: Resource?

    /**
     * Creates GIF with placed text
     */
    suspend fun createGif(text: String?, thumbnail: Boolean, resultPath: Path)
}
