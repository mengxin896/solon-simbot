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
 *     You should have received a copy of the Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package love.forte.simbot.solon.listener

import love.forte.simbot.event.Event
import love.forte.simbot.event.EventContext
import love.forte.simbot.event.EventListener
import love.forte.simbot.event.EventListenerContext
import love.forte.simbot.event.EventResult
import love.forte.simbot.logger.LoggerFactory
import love.forte.simbot.logger.logger
import love.forte.simbot.quantcat.common.annotations.Listener
import love.forte.simbot.quantcat.common.binder.BindException
import love.forte.simbot.solon.common.MultipleIncompatibleTypesEventException
import org.noear.solon.core.AppContext
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Parameter

internal class JavaMethodEventListenerProcessor {

    fun process(
        beanName: String?,
        beanInstance: Any,
        method: Method,
        listenerAnnotation: Listener,
        appContext: AppContext,
    ): SimbotEventListenerResolver {
        val priority = listenerAnnotation.priority
        val listenTarget = method.listenTarget()

        return SimbotEventListenerResolver { application ->
            if (!method.canAccess(beanInstance)) {
                method.trySetAccessible()
            }

            val listener = JavaMethodEventListener(
                instance = beanInstance,
                method = method,
                appContext = appContext,
                listenTarget = listenTarget
            )

            application.eventDispatcher.register(
                { this.priority = priority },
                listener
            )

            if (beanName != null) {
                logger.debug("Registered Java listener {} from bean named {}", listener, beanName)
            } else {
                logger.debug("Registered Java listener {}", listener)
            }
        }
    }

    private companion object {
        val logger = LoggerFactory.logger<JavaMethodEventListenerProcessor>()
    }
}

internal class JavaMethodEventListener(
    private val instance: Any,
    private val method: Method,
    private val appContext: AppContext,
    private val listenTarget: Class<out Event>,
) : EventListener {

    override suspend fun EventListenerContext.handle(): EventResult {
        if (!listenTarget.isInstance(event)) {
            return EventResult.invalid()
        }

        val args = resolveArguments(this)
        val result = try {
            method.invoke(instance, *args)
        } catch (e: InvocationTargetException) {
            throw e.targetException ?: e
        }

        return result.toEventResult()
    }

    private fun resolveArguments(context: EventListenerContext): Array<Any?> =
        Array(method.parameters.size) { index ->
            resolveArgument(context, method.parameters[index])
        }

    private fun resolveArgument(context: EventListenerContext, parameter: Parameter): Any? {
        val type = parameter.type

        return when {
            EventListenerContext::class.java.isAssignableFrom(type) -> context
            EventContext::class.java.isAssignableFrom(type) -> context.context
            EventListener::class.java.isAssignableFrom(type) -> context.listener
            Event::class.java.isAssignableFrom(type) -> {
                if (type.isInstance(context.event)) {
                    context.event
                } else {
                    throw BindException(
                        "The type of EventListenerContext.event ${context.event.javaClass.name} " +
                            "is inconsistent with the target type ${type.name}"
                    )
                }
            }

            AppContext::class.java.isAssignableFrom(type) -> appContext
            Method::class.java.isAssignableFrom(type) -> method
            else -> resolveBean(type)
        }
    }

    private fun resolveBean(type: Class<*>): Any {
        if (appContext.getWrap(type) == null) {
            throw BindException("No bean found for type: ${type.name} on Java listener method $method")
        }

        return appContext.getBean(type)
            ?: throw BindException("No bean found for type: ${type.name} on Java listener method $method")
    }

    override fun toString(): String = "JavaMethodEventListener(method=$method)"
}

private fun Any?.toEventResult(): EventResult = when (this) {
    is EventResult -> this
    null, Unit -> EventResult.empty()
    else -> EventResult.of(this)
}

@Suppress("UNCHECKED_CAST")
private fun Method.listenTarget(): Class<out Event> {
    val typeLink = mutableListOf<Class<out Event>>()
    var minType: Class<out Event>? = null

    parameters.asSequence()
        .map { it.type }
        .filter { Event::class.java.isAssignableFrom(it) }
        .forEach { rawEventType ->
            val eventType = rawEventType as Class<out Event>
            val current = minType

            when {
                current == null -> {
                    minType = eventType
                    typeLink.add(eventType)
                }

                eventType == current -> Unit

                current.isAssignableFrom(eventType) -> {
                    minType = eventType
                    typeLink.add(eventType)
                }

                eventType.isAssignableFrom(current) -> Unit

                else -> {
                    throw MultipleIncompatibleTypesEventException(
                        buildString {
                            append("Current Event types link of method [$this@listenTarget] is: \n[")
                            typeLink.forEachIndexed { index, type ->
                                append("(").append(type.name).append(")")
                                if (index != typeLink.lastIndex) {
                                    append(" -> ")
                                }
                            }
                            append("], \nbut now: ").append(eventType.name)
                        }
                    )
                }
            }
        }

    return minType ?: Event::class.java
}
