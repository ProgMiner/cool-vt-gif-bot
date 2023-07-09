package ru.byprogminer.coolvtgifbot.gif.factory

import org.springframework.core.io.Resource
import ru.byprogminer.coolvtgifbot.gif.GifMetadata
import java.nio.file.Path


interface GifFactory {

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
    fun createGif(text: String?, thumbnail: Boolean, resultPath: Path)
}
