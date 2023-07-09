package ru.byprogminer.coolvtgifbot.gif

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

    @GetMapping("api/gif/{index}/${GifFacade.ORIGINAL_KIND}")
    fun getOriginal(
        @PathVariable("index") index: Int,
    ) = createResponse { gifFacade.makeGif(index, null, false) }

    @GetMapping("api/gif/{index}/${GifFacade.THUMBNAIL_KIND}")
    fun getThumbnail(
        @PathVariable("index") index: Int,
    ) = createResponse { gifFacade.makeGif(index, null, true) }

    @GetMapping("api/gif/{index}/${GifFacade.ORIGINAL_KIND}/{text}")
    fun getOriginalWithText(
        @PathVariable("index") index: Int,
        @PathVariable("text") text: String,
    ) = createResponse { gifFacade.makeGif(index, text, false) }

    @GetMapping("api/gif/{index}/${GifFacade.THUMBNAIL_KIND}/{text}")
    fun getThumbnailWithText(
        @PathVariable("index") index: Int,
        @PathVariable("text") text: String,
    ) = createResponse { gifFacade.makeGif(index, text, true) }

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
        ResponseEntity.badRequest().body(e.message)
    } catch (e: Exception) {
        ResponseEntity.internalServerError().body(e.message)
    }
}
