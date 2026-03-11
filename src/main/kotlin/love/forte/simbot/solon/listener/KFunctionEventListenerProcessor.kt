/*
 *     Copyright (c) 2024-2026. ForteScarlet and contributors.
 *
 *     This file is adapted from the Simple Robot (simbot) project.
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

import love.forte.simbot.annotations.InternalSimbotAPI
import love.forte.simbot.common.attribute.mutableAttributeMapOf
import love.forte.simbot.common.attribute.set
import love.forte.simbot.event.Event
import love.forte.simbot.event.EventListenerContext
import love.forte.simbot.logger.LoggerFactory
import love.forte.simbot.logger.logger
import love.forte.simbot.quantcat.common.annotations.ApplyBinder
import love.forte.simbot.quantcat.common.annotations.Listener
import love.forte.simbot.quantcat.common.binder.BinderManager
import love.forte.simbot.quantcat.common.binder.ParameterBinder
import love.forte.simbot.quantcat.common.binder.ParameterBinderFactory
import love.forte.simbot.quantcat.common.binder.ParameterBinderResult
import love.forte.simbot.quantcat.common.binder.impl.EmptyBinder
import love.forte.simbot.quantcat.common.binder.impl.MergedBinder
import love.forte.simbot.solon.common.MultipleIncompatibleTypesEventException
import org.noear.solon.core.AppContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.isAccessible


internal typealias MatcherFunc = suspend (EventListenerContext) -> Boolean

internal class KFunctionEventListenerProcessor {
    private val instanceCache = ConcurrentHashMap<KClass<*>, Any>()

    fun process(
        beanName: String?,
        beanInstance: Any,
        function: KFunction<*>,
        listenerAnnotation: Listener,
        applyBinder: ApplyBinder?,
        appContext: AppContext,
        binderManager: BinderManager,
    ): SimbotEventListenerResolver {
        val priority = listenerAnnotation.priority
        val listenTarget = function.listenTarget()
        val listenerAttributeMap = mutableAttributeMapOf(ConcurrentHashMap())
        val binders = function.resolveBinders(appContext, binderManager, applyBinder)

        listenerAttributeMap[KFunctionEventListener.RawFunctionAttribute] = function
        listenerAttributeMap[KFunctionEventListener.RawBindersAttribute] = binders.toList()
        listenerAttributeMap[KFunctionEventListener.RawListenTargetAttribute] = listenTarget

        val matchers = arrayOf<MatcherFunc>(
            { listenTarget.isInstance(it.event) }
        )

        return SimbotEventListenerResolver { application ->
            if (function.visibility != KVisibility.PUBLIC) {
                function.isAccessible = true
            }

            val listener = KFunctionEventListenerImpl(
                instance = beanInstance,
                caller = function,
                binders = binders.toTypedArray(),
                attributes = listenerAttributeMap,
                matcher = { c -> matchers.all { it.invoke(c) } }
            )

            application.eventDispatcher.register(
                { this.priority = priority },
                listener
            )

            if (beanName != null) {
                logger.debug("Registered listener {} from bean named {}", listener, beanName)
            } else {
                logger.debug("Registered listener {}", listener)
            }
        }
    }

    @OptIn(InternalSimbotAPI::class)
    private fun KFunction<*>.resolveBinders(
        appContext: AppContext,
        binderManager: BinderManager,
        applyBinder: ApplyBinder?,
    ): List<ParameterBinder> {
        val binderFactories = binderManager.globals.toMutableList()

        if (applyBinder != null) {
            for (bid in applyBinder.value) {
                val b = binderManager[bid]
                if (b != null) {
                    binderFactories.add(b)
                } else {
                    logger.warn("Applied binder factory id [{}] not found, skip it.", bid)
                }
            }

            for (factoryType in applyBinder.factories) {
                val b = resolveBinderFactoryInstance(instanceCache, appContext, factoryType)
                binderFactories.add(b)
            }
        }

        return binderFactoriesToBinders(binderFactories.apply { sortBy { it.priority } })
    }

    private fun KFunction<*>.binderFactoriesToBinders(
        factories: List<ParameterBinderFactory>,
    ): List<ParameterBinder> {
        val binders = parameters.map { parameter ->
            val bindList = mutableListOf<ParameterBinderResult.NotEmpty>()
            val bindSpareList = mutableListOf<ParameterBinderResult.NotEmpty>()

            val bindContext = ParameterBinderFactoryContextImpl(this, parameter)

            for (factory in factories) {
                when (val result = factory.resolveToBinder(bindContext)) {
                    is ParameterBinderResult.Empty -> continue
                    is ParameterBinderResult.NotEmpty -> {
                        when (result) {
                            is ParameterBinderResult.Normal -> {
                                if (bindList.isEmpty() || bindList.first() !is ParameterBinderResult.Only) {
                                    bindList.add(result)
                                }
                            }

                            is ParameterBinderResult.Only -> {
                                if (bindList.isNotEmpty() && bindList.first() is ParameterBinderResult.Only) {
                                    bindList[0] = result
                                } else {
                                    bindList.clear()
                                    bindList.add(result)
                                }
                            }

                            is ParameterBinderResult.Spare -> {
                                bindSpareList.add(result)
                            }
                        }
                    }
                }
            }

            bindList.sortBy { it.priority }
            bindSpareList.sortBy { it.priority }

            when {
                bindList.isEmpty() && bindSpareList.isEmpty() -> EmptyBinder(parameter)
                bindList.isEmpty() -> MergedBinder(bindSpareList.map { it.binder }, emptyList(), parameter)
                bindList.size == 1 && bindSpareList.isEmpty() -> bindList.first().binder
                else -> MergedBinder(
                    bindList.map { it.binder },
                    bindSpareList.map { it.binder },
                    parameter
                )
            }
        }
        return binders
    }

    private companion object {
        val logger = LoggerFactory.logger<KFunctionEventListenerProcessor>()
    }
}


private data class ParameterBinderFactoryContextImpl(
    override val source: KFunction<*>,
    override val parameter: KParameter,
) : ParameterBinderFactory.Context


private fun resolveBinderFactoryInstance(
    instanceCache: MutableMap<KClass<*>, Any>,
    appContext: AppContext,
    type: KClass<out love.forte.simbot.quantcat.common.binder.BaseParameterBinderFactory<*>>,
): ParameterBinderFactory {
    if (!type.isSubclassOf(ParameterBinderFactory::class)) {
        throw IllegalArgumentException(
            "The types in ApplyBinder.factories must be ParameterBinderFactory, but $type"
        )
    }

    @Suppress("UNCHECKED_CAST")
    val parameterBinderFactoryType = type as KClass<out ParameterBinderFactory>

    val instance = appContext.getBean(parameterBinderFactoryType.java)
    if (instance != null) {
        return instance
    }

    val objInstance = parameterBinderFactoryType.objectInstance
    if (objInstance != null) {
        return objInstance
    }

    return runCatching {
        instanceCache.computeIfAbsent(parameterBinderFactoryType) { it.createInstance() } as ParameterBinderFactory
    }.getOrElse { e ->
        throw IllegalArgumentException("Can't create instance for $parameterBinderFactoryType", e)
    }
}

@Suppress("UNCHECKED_CAST")
private fun KFunction<*>.listenTarget(): KClass<out Event> {
    val typeLink = mutableListOf<KClass<*>>()
    var minType: KClass<out Event>? = null

    parameters.asSequence()
        .filter { it.kind != KParameter.Kind.INSTANCE }
        .filter { (it.type.classifier as? KClass<*>)?.isSubclassOf(Event::class) == true }
        .forEach {
            val e = it.type.classifier as KClass<out Event>
            val m = minType
            when {
                m == null -> {
                    minType = e
                    typeLink.add(e)
                }

                e == m -> Unit

                e.isSubclassOf(m) -> {
                    minType = e
                    typeLink.add(e)
                }

                else -> {
                    throw MultipleIncompatibleTypesEventException(
                        buildString {
                            append("Current Event types link of function [${this@listenTarget}] is: \n[")
                            typeLink.forEachIndexed { index, t ->
                                append("(")
                                append(t)
                                append(")")
                                if (index != typeLink.lastIndex) {
                                    append(" -> ")
                                }
                            }
                            append("], \nbut now: ")
                            append(it.type.classifier).append("(").append(it)
                            append("), it !is ").append(typeLink.last())
                        }
                    )
                }
            }
        }

    return minType ?: Event::class
}
