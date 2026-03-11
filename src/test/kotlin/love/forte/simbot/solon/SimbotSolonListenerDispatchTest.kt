package love.forte.simbot.solon

import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import love.forte.simbot.application.Application
import love.forte.simbot.common.id.ID
import love.forte.simbot.common.id.UUID
import love.forte.simbot.common.time.Timestamp
import love.forte.simbot.event.Event
import love.forte.simbot.quantcat.common.annotations.Listener
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.noear.solon.Solon
import org.noear.solon.annotation.Component
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

private val invokedCount = AtomicInteger(0)
private val injectedServiceRef = AtomicReference<TestService?>()

class TestEvent : Event {
    override val id: ID = UUID.random()
    override val time: Timestamp = Timestamp.now()
}

@Component
class TestService

@Component
class TestListenerBean {
    @Listener
    fun onTest(event: TestEvent, service: TestService) {
        invokedCount.incrementAndGet()
        injectedServiceRef.set(service)
    }
}

class SimbotSolonListenerDispatchTest {

    @Test
    fun `listener should be registered and invoked with injected bean`() {
        invokedCount.set(0)
        injectedServiceRef.set(null)

        val solonApp = Solon.start(SimbotSolonListenerDispatchTest::class.java, arrayOf()) { app ->
            app.enableHttp(false)
        }

        try {
            val context = solonApp.context()
            val application = context.getBean("simbotApplication") as Application?
            assertNotNull(application, "Solon context should contain bean: simbotApplication")

            runBlocking {
                withTimeout(5_000) {
                    application!!.eventDispatcher.push(TestEvent()).collect { /* drain */ }
                }
            }

            assertEquals(1, invokedCount.get(), "listener should be invoked exactly once")
            val injectedService = injectedServiceRef.get()
            assertNotNull(injectedService, "listener should receive injected TestService")
            assertSame(context.getBean(TestService::class.java), injectedService)
        } finally {
            Solon.stopBlock()
        }
    }
}
