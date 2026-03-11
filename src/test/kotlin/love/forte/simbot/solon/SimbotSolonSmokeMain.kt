package love.forte.simbot.solon

import org.noear.solon.Solon

/**
 * 用于在本地直接跑一次 Solon + starter 的最小启动验证（避免 surefire fork 环境差异影响定位）。
 */
object SimbotSolonSmokeMain {
    @JvmStatic
    fun main(args: Array<String>) {
        runCatching {
            val app = Solon.start(SimbotSolonSmokeMain::class.java, args) { solonApp ->
                solonApp.enableHttp(false)
            }
            println("Solon started. context=${app.context()}")
            val simbotApp = app.context().getBean<Any>("simbotApplication")
            println("simbotApplication bean = $simbotApp")
        }.onFailure { e ->
            e.printStackTrace()
        }.also {
            runCatching { Solon.stopBlock() }
        }
    }
}
