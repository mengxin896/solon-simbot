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

package love.forte.simbot.solon.config

import org.noear.solon.annotation.BindProps
import org.noear.solon.annotation.Configuration

/**
 * Solon 下 Simbot starter 的基础配置。
 *
 * 通过 `@BindProps(prefix = "simbot")` 绑定，例如：
 *
 * ```properties
 * simbot.enabled=true
 * simbot.components.autoInstallProviders=true
 * simbot.plugins.autoInstallProviders=true
 * ```
 */
@BindProps(prefix = "simbot")
@Configuration
public open class SimbotSolonProperties {
    public open var enabled: Boolean = true

    public open var bots: Bots = Bots()
    public open var components: Components = Components()
    public open var plugins: Plugins = Plugins()

    /**
     * 与 bot 自动加载（配置文件 -> 注册 -> 启动）相关的配置。
     */
    public open class Bots {
        public companion object {
            /**
             * 默认扫描 classpath 下 `simbot-bots/\\*.bot.json`。
             *
             * 注意：如果你不希望将敏感信息打包进 jar，
             * 建议在用户工程里改成：`file:./simbot-bots/\\*.bot.json`
             */
            public const val DEFAULT_JSON_RESOURCE_PATTERN: String = "classpath:simbot-bots/*.bot.json"
        }

        /**
         * 是否忽略扫描/读取配置资源过程中出现的 IOException。
         */
        public open var ignoreIOExceptionForResourcesLoad: Boolean = true

        /**
         * 需要加载的 bot 配置文件资源表达式列表（支持 `classpath:` / `file:` 与 `*` 通配）。
         */
        public open var configurationJsonResources: MutableSet<String> =
            mutableSetOf(DEFAULT_JSON_RESOURCE_PATTERN)

        /**
         * 当加载用于注册 bot 的配置文件出现错误时的处理策略。
         */
        public open var autoRegistrationResourceLoadFailurePolicy: BotConfigResourceLoadFailurePolicy =
            BotConfigResourceLoadFailurePolicy.ERROR

        /**
         * 当无法为某个配置找到任何可供其注册的 BotManager 时的处理策略。
         */
        public open var autoRegistrationMismatchConfigurableBotManagerPolicy: MismatchConfigurableBotManagerPolicy =
            MismatchConfigurableBotManagerPolicy.ERROR_LOG

        /**
         * 是否在 bot 注册后自动调用 `bot.start()` 启动它们。
         */
        public open var autoStartBots: Boolean = true

        /**
         * 启动 bot 的方式。会先注册完所有 bot 再启动。
         */
        public open var autoStartMode: BotAutoStartMode = BotAutoStartMode.ASYNC

        /**
         * 当自动扫描的 bot 注册或启动失败时的处理策略。
         */
        public open var autoRegistrationFailurePolicy: BotRegistrationFailurePolicy = BotRegistrationFailurePolicy.ERROR
    }

    public open class Components {
        public open var autoInstallProviders: Boolean = true
        public open var autoInstallProviderConfigures: Boolean = true
    }

    public open class Plugins {
        public open var autoInstallProviders: Boolean = true
        public open var autoInstallProviderConfigures: Boolean = true
    }
}

/**
 * bot 启动模式。
 */
public enum class BotAutoStartMode {
    /**
     * 依次同步启动（阻塞）。
     */
    SYNC,

    /**
     * 每个 bot 独立异步启动。
     */
    ASYNC
}

/**
 * 当自动扫描的 bot 注册或启动失败时的处理策略。
 */
public enum class BotRegistrationFailurePolicy {
    /**
     * 抛出异常并终止流程。
     */
    ERROR,

    /**
     * 输出 error 日志并继续。
     */
    ERROR_LOG,

    /**
     * 输出 warn 日志并继续。
     */
    WARN,

    /**
     * 仅输出 debug 日志并继续。
     */
    IGNORE
}

/**
 * 当自动扫描的 bot 配置文件加载/解析失败时的处理策略。
 */
public enum class BotConfigResourceLoadFailurePolicy {
    ERROR,
    ERROR_LOG,
    WARN,
    IGNORE
}

/**
 * 被加载的 SerializableBotConfiguration 无法找到任何可供注册的 BotManager 时的策略。
 */
public enum class MismatchConfigurableBotManagerPolicy {
    ERROR,
    ERROR_LOG,
    WARN,
    IGNORE
}
