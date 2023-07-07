package ru.byprogminer.coolvtgifbot.bot

import com.github.kotlintelegrambot.Bot
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Service


@Service
class BotRunner(
    private val bot: Bot,
) : ApplicationRunner {

    override fun run(args: ApplicationArguments) {
        bot.startWebhook()
    }
}
