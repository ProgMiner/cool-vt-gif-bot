package ru.byprogminer.coolvtgifbot.bot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.inlineQuery
import com.github.kotlintelegrambot.logging.LogLevel
import com.github.kotlintelegrambot.webhook
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration


@Configuration
class BotConfig(
    @Value("\${tg.token}")
    private val token: String,
    @Value("\${tg.host}")
    private val host: String,
    private val inlineHandler: InlineHandler,
) {

    @Bean
    fun bot(): Bot = bot {
        logLevel = LogLevel.Error

        token = this@BotConfig.token

        webhook {
            url = "$host/${BotController.WEBHOOK_URL}/$token"

            maxConnections = 5
            allowedUpdates = listOf("inline_query")
        }

        dispatch {
            inlineQuery {
                inlineHandler.run {
                    handle()
                }
            }
        }
    }
}
