package love.forte.simbot.solon.demo

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import love.forte.simbot.application.Application
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.UUID
import love.forte.simbot.common.time.Timestamp
import love.forte.simbot.event.Event
import love.forte.simbot.quantcat.common.annotations.Listener
import org.noear.solon.Solon
import org.noear.solon.annotation.Component
import java.util.concurrent.atomic.AtomicInteger

/**
 * 第四档：真实 Solon 应用集成测试
 *
 * 运行方式（先在仓库根目录执行一次 install）：
 * 1) `cd E:\project\simbot-solon-starter`
 * 2) `mvn -q -DskipTests install`
 * 3) `cd examples/demo-app`
 * 4) `mvn -q -DskipTests compile exec:java`
 */
object DemoApp {

    @JvmStatic
    fun main(args: Array<String>) {
        val solonApp = Solon.start(DemoApp::class.java, args) { app ->
            app.enableHttp(false)
        }

        try {
            val context = solonApp.context()
            val simbotApp = context.getBean("simbotApplication") as Application?
                ?: error("simbotApplication bean not found. Is simbot-solon-starter on classpath?")

            runBlocking {
                withTimeout(5_000) {
                    simbotApp.eventDispatcher.push(DemoEvent()).collect { /* drain */ }
                }
            }

            println("Demo done. invoked=${invokedCount.get()}")
        } finally {
            Solon.stopBlock()
        }
    }
}

private val invokedCount = AtomicInteger(0)

class DemoEvent : Event {
    override val id: ID = UUID.random()
    override val time: Timestamp = Timestamp.now()
}

@Component
class DemoService {
    fun ping(): String = "pong"
}

@Component
class DemoListeners {
    @Listener
    fun onDemo(event: DemoEvent, service: DemoService) {
        invokedCount.incrementAndGet()
        println("onDemo invoked. eventId=${event.id}, service=${service.ping()}")
    }
}

