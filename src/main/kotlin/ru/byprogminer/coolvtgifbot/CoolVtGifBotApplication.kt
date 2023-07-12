package ru.byprogminer.coolvtgifbot

import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import org.bytedeco.javacv.FFmpegLogCallback
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.core.task.AsyncTaskExecutor
import kotlin.coroutines.CoroutineContext


@SpringBootApplication
class CoolVtGifBotApplication {

    @Bean
    fun coroutineContext(taskExecutor: AsyncTaskExecutor): CoroutineContext =
        taskExecutor.asCoroutineDispatcher() + SupervisorJob()
}

fun main(args: Array<String>) {
    FFmpegLogCallback.set()

    runApplication<CoolVtGifBotApplication>(*args)
}
