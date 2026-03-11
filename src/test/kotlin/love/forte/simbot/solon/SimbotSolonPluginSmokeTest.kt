package love.forte.simbot.solon

import love.forte.simbot.application.Application
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.noear.solon.Solon

class SimbotSolonPluginSmokeTest {

    @Test
    fun `plugin should initialize and register simbot beans`() {
        val solonApp = Solon.start(SimbotSolonPluginSmokeTest::class.java, arrayOf()) { app ->
            app.enableHttp(false)
        }

        try {
            val context = solonApp.context()
            val application = context.getBean("simbotApplication") as Application?
            assertNotNull(application, "Solon context should contain bean: simbotApplication")
        } finally {
            // Solon.stop() 会触发 System.exit；在测试环境用 stopBlock 仅停止容器本身
            Solon.stopBlock()
        }
    }
}
