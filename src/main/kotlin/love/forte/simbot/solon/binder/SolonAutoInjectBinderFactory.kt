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

package love.forte.simbot.solon.binder

import love.forte.simbot.quantcat.common.binder.BindException
import love.forte.simbot.quantcat.common.binder.ParameterBinder
import love.forte.simbot.quantcat.common.binder.ParameterBinderFactory
import love.forte.simbot.quantcat.common.binder.ParameterBinderResult
import org.noear.solon.core.AppContext
import kotlin.reflect.KParameter

/**
 * 在监听函数参数解析阶段，尝试从 Solon [AppContext] 中按参数类型获取 bean 进行注入。
 *
 * - 仅当容器中存在目标类型的 [org.noear.solon.core.BeanWrap] 时才会绑定（避免无意义的失败堆栈）
 * - 绑定结果使用 [ParameterBinderResult.Spare]：尽量不抢占更具体的 binder
 */
public class SolonAutoInjectBinderFactory(
    private val appContext: AppContext
) : ParameterBinderFactory {

    override fun resolveToBinder(context: ParameterBinderFactory.Context): ParameterBinderResult {
        val parameter = context.parameter

        // skip instance
        if (parameter.kind == KParameter.Kind.INSTANCE) return ParameterBinderResult.empty()

        val type = context.parameterType ?: return ParameterBinderResult.empty()

        // always support injecting AppContext itself
        if (AppContext::class.java.isAssignableFrom(type)) {
            return ParameterBinderResult.spare(AppContextBinder(appContext))
        }

        // only bind when beanWrap exists to avoid meaningless failures
        if (appContext.getWrap(type) == null) {
            return ParameterBinderResult.empty()
        }

        return ParameterBinderResult.spare(SolonBeanByTypeBinder(appContext, type))
    }

    private class AppContextBinder(
        private val appContext: AppContext
    ) : ParameterBinder {
        override fun arg(context: love.forte.simbot.event.EventListenerContext): Result<Any?> =
            Result.success(appContext)
    }

    private class SolonBeanByTypeBinder(
        private val appContext: AppContext,
        private val type: Class<*>,
    ) : ParameterBinder {
        override fun arg(context: love.forte.simbot.event.EventListenerContext): Result<Any?> {
            val bean = appContext.getBean(type)
            return if (bean != null) {
                Result.success(bean)
            } else {
                Result.failure(BindException("No bean found for type: $type"))
            }
        }
    }
}

