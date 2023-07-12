package ru.byprogminer.coolvtgifbot.bot

import com.github.kotlintelegrambot.dispatcher.handlers.InlineQueryHandlerEnvironment
import com.github.kotlintelegrambot.entities.inlinequeryresults.InlineQueryResult
import com.github.kotlintelegrambot.entities.inlinequeryresults.MimeType
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import ru.byprogminer.coolvtgifbot.gif.GifFacade
import java.util.*


@Service
class InlineHandler(
    @Value("\${tg.make_gif_immediately}")
    private val startMaking: Boolean,
    private val gifFacade: GifFacade,
) {

    private companion object {

        @JvmStatic
        val log: Logger = LoggerFactory.getLogger(InlineHandler::class.java)
    }

    suspend fun InlineQueryHandlerEnvironment.handle() {
        val query = inlineQuery.query.ifBlank { null }
        val offset = inlineQuery.offset.toIntOrNull() ?: 0

        log.info("New inline query: {}", inlineQuery)

        // 50 is current max size of page in TG inline query results
        val (links, size) = gifFacade.getGifLinks(query, 50, offset, startMaking)
        val result = links.map { (orig, thumb, meta) ->
            InlineQueryResult.Mpeg4Gif(
                id = UUID.randomUUID().toString(),
                mpeg4Url = orig,
                mpeg4Width = meta.width,
                mpeg4Height = meta.height,
                mpeg4Duration = meta.duration,
                thumbUrl = thumb,
                thumbMimeType = MimeType.VIDEO_MP4,
            )
        }

        val nextOffset = if (links.size < size) "" else (offset + links.size).toString()

        log.info("Results: {}", result)

        bot.answerInlineQuery(
            inlineQueryId = inlineQuery.id,
            inlineQueryResults = result,
            cacheTime = 1,
            isPersonal = false,
            nextOffset = nextOffset,
        )
    }
}
