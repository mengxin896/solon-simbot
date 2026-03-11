/*
 *     Copyright (c) 2026.
 *
 *     This file is part of the Simbot Solon Starter.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     Lesser GNU General Public License for more details.
 *
 *     You should have received a copy of the Lesser GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package love.forte.simbot.solon

import kotlinx.coroutines.runBlocking
import love.forte.simbot.application.Application
import love.forte.simbot.solon.bot.SimbotSolonBotAutoLoader
import love.forte.simbot.component.ComponentFactory
import love.forte.simbot.component.findAndInstallAllComponents
import love.forte.simbot.core.application.Simple
import love.forte.simbot.plugin.PluginFactory
import love.forte.simbot.plugin.findAndInstallAllPlugins
import love.forte.simbot.quantcat.common.annotations.Binder
import love.forte.simbot.quantcat.common.binder.BinderManager
import love.forte.simbot.quantcat.common.binder.ParameterBinderFactory
import love.forte.simbot.quantcat.common.binder.SimpleBinderManager
import love.forte.simbot.quantcat.common.binder.impl.EventParameterBinderFactory
import love.forte.simbot.solon.binder.DuplicateBinderIdException
import love.forte.simbot.solon.binder.SolonAutoInjectBinderFactory
import love.forte.simbot.solon.config.SimbotSolonProperties
import love.forte.simbot.solon.listener.SimbotSolonListenerScanner
import org.noear.solon.core.AppContext
import org.noear.solon.core.Plugin
import org.noear.solon.core.event.AppLoadEndEvent
import org.noear.solon.core.util.LogUtil
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Simbot Solon starter 的 Solon Plugin 入口。
 *
 * 注意：Solon 插件启动顺序早于 Bean 扫描，因此实际初始化逻辑会延迟到 [AppLoadEndEvent] 触发后执行。
 */
public class SimbotSolonPlugin : Plugin {

    private val initialized = AtomicBoolean(false)

    @Volatile
    private var application: Application? = null

    override fun start(context: AppContext) {
        context.onEvent(AppLoadEndEvent::class.java) {
            if (!initialized.compareAndSet(false, true)) return@onEvent
            onAppLoadEnd(context)
        }
    }

    override fun stop() {
        runCatching { application?.cancel() }
            .onFailure { e: Throwable ->
                LogUtil.global().warn("Close simbot application failed: ${e.message}", e)
            }
    }

    private fun onAppLoadEnd(context: AppContext) {
        val properties = resolveProperties(context)
        if (!properties.enabled) {
            LogUtil.global().info("SimbotSolonPlugin disabled by config: simbot.enabled=false")
            return
        }

        // binder manager
        val binderManager = buildBinderManager(context)
        registerBean(context, "simbotBinderManager", binderManager)

        // scan listeners
        val resolvers = SimbotSolonListenerScanner(context, binderManager).scan()

        // build + launch application
        val launcher = Simple.create {
            // components
            if (properties.components.autoInstallProviders) {
                findAndInstallAllComponents(properties.components.autoInstallProviderConfigures)
            }

            @Suppress("UNCHECKED_CAST")
            context.getBeansOfType(ComponentFactory::class.java).forEach { factory ->
                install(factory as ComponentFactory<love.forte.simbot.component.Component, Any>)
            }

            // plugins
            if (properties.plugins.autoInstallProviders) {
                findAndInstallAllPlugins(properties.plugins.autoInstallProviderConfigures)
            }

            @Suppress("UNCHECKED_CAST")
            context.getBeansOfType(PluginFactory::class.java).forEach { factory ->
                install(factory as PluginFactory<love.forte.simbot.plugin.Plugin, Any>)
            }
        }

        val app = runBlocking { launcher.launch() }
        application = app

        registerBean(context, "simbotApplication", app)

        // register listeners
        resolvers.forEach { it.resolve(app) }

        // auto register bots from config resources
        val bots = SimbotSolonBotAutoLoader(context, properties.bots, app).loadAndRegister()

        LogUtil.global().info("SimbotSolonPlugin initialized. listeners=${resolvers.size}, bots=${bots.size}")
    }

    private fun resolveProperties(context: AppContext): SimbotSolonProperties {
        val existed = context.getBean(SimbotSolonProperties::class.java)
        if (existed != null) return existed

        val bw = context.beanMake(SimbotSolonProperties::class.java)
        val made = bw?.get() as? SimbotSolonProperties
        return made ?: SimbotSolonProperties()
    }

    private fun registerBean(context: AppContext, name: String, bean: Any) {
        val bw = context.wrapAndPut(name, bean, true)
        context.beanRegister(bw, name, true)
    }

    @OptIn(love.forte.simbot.annotations.ExperimentalSimbotAPI::class)
    private fun buildBinderManager(context: AppContext): BinderManager {
        val globals = mutableListOf<ParameterBinderFactory>()
        val ids = mutableMapOf<String, ParameterBinderFactory>()

        // default globals
        globals.add(EventParameterBinderFactory)
        globals.add(SolonAutoInjectBinderFactory(context))

        // binder factory beans
        context.getBeansOfType(ParameterBinderFactory::class.java).forEach { factory ->
            val binder = factory.javaClass.getAnnotation(Binder::class.java)

            when (binder?.scope ?: Binder.Scope.DEFAULT) {
                Binder.Scope.GLOBAL -> globals.add(factory)
                Binder.Scope.SPECIFY -> {
                    val id = binder?.id?.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException(
                            "Scope of @Binder on ${factory.javaClass.name} is SPECIFY, but id is empty"
                        )
                    if (ids.containsKey(id)) throw DuplicateBinderIdException("Duplicate binder factory id: $id")
                    ids[id] = factory
                }

                Binder.Scope.DEFAULT -> {
                    val id = binder?.id?.takeIf { it.isNotBlank() }
                    if (id != null) {
                        if (ids.containsKey(id)) throw DuplicateBinderIdException("Duplicate binder factory id: $id")
                        ids[id] = factory
                    } else {
                        globals.add(factory)
                    }
                }
            }
        }

        globals.sortBy { it.priority }
        return SimpleBinderManager(globals, ids)
    }
}
