package love.forte.simbot.solon.onebot11.demo

import kotlinx.coroutines.runBlocking
import love.forte.simbot.application.Application
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotGroupMessageEvent
import love.forte.simbot.component.onebot.v11.core.event.message.OneBotPrivateMessageEvent
import love.forte.simbot.event.MessageEvent
import love.forte.simbot.quantcat.common.annotations.Listener
import org.noear.solon.Solon
import org.noear.solon.annotation.Component
import java.nio.file.Paths

object OneBot11NapCatDemoApp {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val solonApp = Solon.start(OneBot11NapCatDemoApp::class.java, args)
        val context = solonApp.context()
        val simbotApplication = context.getBean("simbotApplication") as? Application
            ?: error("simbotApplication bean not found. Is simbot-solon-starter loaded?")

        println("OneBot11 + NapCat demo started.")
        println("Current workdir: ${Paths.get("").toAbsolutePath()}")
        println("Bot configuration file: ./simbot-bots/napcat.bot.json")
        println("If NapCat is already connected, send `ping` to the bot and it should reply `pong`.")

        simbotApplication.join()
    }
}

@Component
class PingListener {
    @Listener
    suspend fun onMessage(event: MessageEvent) {
        val text = event.messageContent.plainText?.trim().orEmpty()
        if (text != "ping") {
            return
        }

        when (event) {
            is OneBotPrivateMessageEvent -> println("Private message: author=${event.authorId}, text=$text")
            is OneBotGroupMessageEvent -> println("Group message: group=${event.groupId}, user=${event.userId}, text=$text")
            else -> println("Message event: type=${event::class.qualifiedName}, author=${event.authorId}, text=$text")
        }

        event.reply("pong")
    }
}
