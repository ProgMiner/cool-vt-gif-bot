package ru.byprogminer.coolvtgifbot.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.Update
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController


@RestController
class BotController(
    private val bot: Bot,
) {

    companion object {

        const val WEBHOOK_URL = "api/webhook"
    }

    @PostMapping("/$WEBHOOK_URL/\${tg.token}")
    suspend fun webhook(@RequestBody body: Update) {
        bot.processUpdate(body)
    }
}
