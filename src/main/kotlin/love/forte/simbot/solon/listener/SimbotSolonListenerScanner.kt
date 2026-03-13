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

package love.forte.simbot.solon.listener

import love.forte.simbot.logger.LoggerFactory
import love.forte.simbot.logger.logger
import love.forte.simbot.quantcat.common.annotations.ApplyBinder
import love.forte.simbot.quantcat.common.annotations.Listener
import love.forte.simbot.quantcat.common.binder.BinderManager
import org.noear.solon.core.AppContext
import org.noear.solon.core.BeanWrap
import kotlin.reflect.jvm.kotlinFunction

/**
 * 在 Solon [AppContext] 中扫描所有 bean，解析其 `@Listener` 函数并生成对应的注册器。
 */
public class SimbotSolonListenerScanner(
    private val appContext: AppContext,
    private val binderManager: BinderManager,
) {
    private val kotlinProcessor = KFunctionEventListenerProcessor()
    private val javaProcessor = JavaMethodEventListenerProcessor()

    public fun scan(): List<SimbotEventListenerResolver> {
        val resolvers = mutableListOf<SimbotEventListenerResolver>()

        appContext.beanForeach { bw: BeanWrap ->
            val rawType = bw.rawClz()
            if (rawType == null) return@beanForeach

            val instance = runCatching { bw.get<Any>() }.getOrNull() ?: return@beanForeach
            val beanName = bw.name()?.takeIf { it.isNotBlank() }

            rawType.declaredMethods.asSequence()
                .filterNot { it.isSynthetic }
                .forEach { method ->
                    val listenerAnnotation = method.getAnnotation(Listener::class.java) ?: return@forEach
                    val applyBinder = method.getAnnotation(ApplyBinder::class.java)
                    val function = method.kotlinFunction

                    if (function == null) {
                        if (applyBinder != null) {
                            logger.warn(
                                "Method {} on bean {} uses @ApplyBinder, but custom binder resolution is not " +
                                    "supported for Java listener methods yet. Ignore applyBinder.",
                                method,
                                rawType.name
                            )
                        }

                        resolvers.add(
                            javaProcessor.process(
                                beanName = beanName,
                                beanInstance = instance,
                                method = method,
                                listenerAnnotation = listenerAnnotation,
                                appContext = appContext
                            )
                        )
                        return@forEach
                    }

                    resolvers.add(
                        kotlinProcessor.process(
                            beanName = beanName,
                            beanInstance = instance,
                            function = function,
                            listenerAnnotation = listenerAnnotation,
                            applyBinder = applyBinder,
                            appContext = appContext,
                            binderManager = binderManager
                        )
                    )
                }
        }

        return resolvers
    }

    private companion object {
        val logger = LoggerFactory.logger<SimbotSolonListenerScanner>()
    }
}
