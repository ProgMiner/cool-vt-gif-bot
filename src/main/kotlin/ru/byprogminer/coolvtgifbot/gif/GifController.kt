package ru.byprogminer.coolvtgifbot.gif

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController


@RestController
class GifController(
    private val gifFacade: GifFacade,
) {

    private companion object {

        @JvmStatic
        val log: Logger = LoggerFactory.getLogger(GifController::class.java)
    }

    @GetMapping("api/gif/{index}/${GifFacade.ORIGINAL_KIND}")
    suspend fun getOriginal(
        @PathVariable("index") key: String,
    ): ResponseEntity<*> = createResponse { gifFacade.makeGif(key, null, false) }

    @GetMapping("api/gif/{index}/${GifFacade.THUMBNAIL_KIND}")
    suspend fun getThumbnail(
        @PathVariable("index") key: String,
    ): ResponseEntity<*> = createResponse { gifFacade.makeGif(key, null, true) }

    @GetMapping("api/gif/{index}/${GifFacade.ORIGINAL_KIND}/{text}")
    suspend fun getOriginalWithText(
        @PathVariable("index") key: String,
        @PathVariable("text") text: String,
    ): ResponseEntity<*> = createResponse { gifFacade.makeGif(key, text, false) }

    @GetMapping("api/gif/{index}/${GifFacade.THUMBNAIL_KIND}/{text}")
    suspend fun getThumbnailWithText(
        @PathVariable("index") key: String,
        @PathVariable("text") text: String,
    ): ResponseEntity<*> = createResponse { gifFacade.makeGif(key, text, true) }

    private inline fun createResponse(block: () -> Resource?) = try {
        val result = block()

        if (result == null) {
            ResponseEntity.notFound().build<Nothing?>()
        } else {
            ResponseEntity.status(HttpStatus.OK)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(result)
        }
    } catch (e: IllegalArgumentException) {
        log.info("Bad request", e)

        ResponseEntity.badRequest().body(e.message)
    } catch (e: Exception) {
        log.info("Internal Server Error", e)

        ResponseEntity.internalServerError().body(e.message)
    }
}
