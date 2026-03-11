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

package love.forte.simbot.solon.bot

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import love.forte.simbot.application.Application
import love.forte.simbot.bot.Bot
import love.forte.simbot.bot.BotManager
import love.forte.simbot.bot.SerializableBotConfiguration
import love.forte.simbot.logger.Logger
import love.forte.simbot.logger.LoggerFactory
import love.forte.simbot.logger.logger
import love.forte.simbot.solon.config.BotAutoStartMode
import love.forte.simbot.solon.config.BotConfigResourceLoadFailurePolicy
import love.forte.simbot.solon.config.BotRegistrationFailurePolicy
import love.forte.simbot.solon.config.MismatchConfigurableBotManagerPolicy
import love.forte.simbot.solon.config.SimbotSolonProperties
import org.noear.solon.core.AppContext
import org.noear.solon.core.util.ResourceUtil
import java.io.IOException
import java.net.URL

internal class SimbotSolonBotAutoLoader(
    private val appContext: AppContext,
    private val properties: SimbotSolonProperties.Bots,
    private val application: Application,
) {
    private val logger = LoggerFactory.logger<SimbotSolonBotAutoLoader>()

    @OptIn(ExperimentalSerializationApi::class)
    private val json: Json = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowTrailingComma = true
        decodeEnumsCaseInsensitive = true
        allowSpecialFloatingPointValues = true
        prettyPrint = false
        serializersModule = application.components.serializersModule
    }

    fun loadAndRegister(): List<Bot> {
        val botManagers = application.botManagers

        val resourceUris = resolveAllResourceUris()
        if (resourceUris.isEmpty()) {
            logger.info("No bot configuration resources found. resources={}", properties.configurationJsonResources)
            return emptyList()
        }

        val bots = resourceUris.mapNotNull { uri ->
            logger.debug("Resolving bot auto-register configuration resource: {}", uri)

            val content = loadResourceContent(properties.autoRegistrationResourceLoadFailurePolicy, uri) ?: return@mapNotNull null

            val configuration = try {
                json.decodeFromString(SerializableBotConfiguration.serializer(), content)
            } catch (se: Throwable) {
                processDecodeFailure(uri, se, properties.autoRegistrationResourceLoadFailurePolicy)
                return@mapNotNull null
            }

            var bot: Bot? = null

            for (manager in botManagers) {
                try {
                    if (manager.configurable(configuration)) {
                        bot = manager.register(configuration)
                    }
                } catch (e: Throwable) {
                    processRegisterBotFailed(
                        properties.autoRegistrationFailurePolicy,
                        e,
                        uri,
                        configuration,
                        manager
                    )
                }
            }

            if (bot == null) {
                mismatchConfigurableManager(
                    properties.autoRegistrationMismatchConfigurableBotManagerPolicy,
                    uri,
                    configuration,
                    botManagers
                )
            }

            bot
        }

        logger.info("The number of registered bots is {}", bots.size)
        processedBotWithPolicy(bots)
        return bots
    }

    private fun resolveAllResourceUris(): List<String> {
        val ignoreIO = properties.ignoreIOExceptionForResourcesLoad

        return properties.configurationJsonResources
            .asSequence()
            .flatMap { expr ->
                val schemaPrefix = when {
                    ResourceUtil.hasClasspath(expr) -> ResourceUtil.TAG_classpath
                    ResourceUtil.hasFile(expr) -> ResourceUtil.TAG_file
                    else -> ""
                }

                try {
                    ResourceUtil.scanResources(appContext.classLoader, expr)
                        .asSequence()
                        .map { path -> schemaPrefix + path }
                } catch (e: Throwable) {
                    val io = (e as? IOException) ?: (e.cause as? IOException)
                    if (io != null && ignoreIO) {
                        logger.warn(
                            "Bot configuration resource expr [{}] scan failed with IOException(message={}), skip it.",
                            expr,
                            io.localizedMessage,
                        )
                        logger.debug(
                            "Bot configuration resource expr [{}] scan failed with IOException(message={}), skip it.",
                            expr,
                            io.localizedMessage,
                            io
                        )
                        emptySequence()
                    } else {
                        throw e
                    }
                }
            }
            .distinct()
            .toList()
    }

    private fun loadResourceContent(policy: BotConfigResourceLoadFailurePolicy, uri: String): String? {
        val url = ResourceUtil.findResourceOrFile(appContext.classLoader, uri)

        if (url == null) {
            return handleResourceLoadFailure(policy, uri, null)
        }

        val content = runCatching { ResourceUtil.getResourceAsString(url) }.getOrElse { e ->
            if (e is IOException) {
                return handleResourceLoadFailure(policy, uri, e)
            }
            throw e
        }

        if (content == null) {
            return handleResourceLoadFailure(policy, uri, null)
        }

        return content
    }

    private fun handleResourceLoadFailure(
        policy: BotConfigResourceLoadFailurePolicy,
        uri: String,
        cause: Throwable?
    ): String? {
        val ex = BotConfigResourceLoadOnFailureException("Cannot load text content for resource: $uri", cause)
        return when (policy) {
            BotConfigResourceLoadFailurePolicy.ERROR -> throw ex
            BotConfigResourceLoadFailurePolicy.ERROR_LOG -> {
                logger.error(ex.message, ex)
                null
            }

            BotConfigResourceLoadFailurePolicy.WARN -> {
                logger.warn(ex.message, ex)
                null
            }

            BotConfigResourceLoadFailurePolicy.IGNORE -> {
                logger.debug(ex.message, ex)
                null
            }
        }
    }

    private fun processDecodeFailure(
        uri: String,
        se: Throwable,
        policy: BotConfigResourceLoadFailurePolicy
    ) {
        val message = se.localizedMessage
        val errMsg = if (se is SerializationException && message?.contains("Polymorphic", ignoreCase = true) == true) {
            "JSON resource [$uri] fails to deserialize, and this may be because a component does not support " +
                "polymorphic configurations or missing serializers. The information: $message"
        } else {
            "JSON resource [$uri] fails to deserialize, The information: $message"
        }

        val ex = BotConfigResourceLoadOnFailureException(errMsg, se)

        when (policy) {
            BotConfigResourceLoadFailurePolicy.ERROR -> throw ex
            BotConfigResourceLoadFailurePolicy.ERROR_LOG -> logger.error(errMsg, ex)
            BotConfigResourceLoadFailurePolicy.WARN -> logger.warn(errMsg, ex)
            BotConfigResourceLoadFailurePolicy.IGNORE -> logger.debug(errMsg, ex)
        }
    }

    private fun processRegisterBotFailed(
        policy: BotRegistrationFailurePolicy,
        e: Throwable,
        uri: String,
        configuration: SerializableBotConfiguration,
        botManager: BotManager
    ) {
        val message =
            "Failed to register bot from resource [$uri] to manager [$botManager] " +
                "via configuration [$configuration]. The information: ${e.localizedMessage}"

        val ex = BotRegisterFailureException(message, e)
        when (policy) {
            BotRegistrationFailurePolicy.ERROR -> throw ex
            BotRegistrationFailurePolicy.ERROR_LOG -> logger.error(message, ex)
            BotRegistrationFailurePolicy.WARN -> logger.warn(message, ex)
            BotRegistrationFailurePolicy.IGNORE -> logger.debug(message, ex)
        }
    }

    private fun mismatchConfigurableManager(
        policy: MismatchConfigurableBotManagerPolicy,
        uri: String,
        configuration: SerializableBotConfiguration,
        botManagers: Iterable<BotManager>
    ) {
        val message = "No registrable BotManager matching configuration [$configuration] " +
            "(type: ${configuration::class}) from resource [$uri] was found in $botManagers"

        val ex = MismatchConfigurableBotManagerException(message)

        when (policy) {
            MismatchConfigurableBotManagerPolicy.ERROR -> throw ex
            MismatchConfigurableBotManagerPolicy.ERROR_LOG -> logger.error(message, ex)
            MismatchConfigurableBotManagerPolicy.WARN -> logger.warn(message, ex)
            MismatchConfigurableBotManagerPolicy.IGNORE -> logger.debug(message, ex)
        }
    }

    private fun processedBotWithPolicy(botList: List<Bot>) {
        val autoStartBots = properties.autoStartBots
        val policy = properties.autoRegistrationFailurePolicy
        val autoStartMode = properties.autoStartMode

        logger.debug(
            "Auto start bots is {} with onFailure policy {} and start mode {}",
            autoStartBots,
            policy,
            autoStartMode
        )

        if (!autoStartBots || botList.isEmpty()) return

        when (autoStartMode) {
            BotAutoStartMode.SYNC -> startBotsInBlocking(policy, botList)
            BotAutoStartMode.ASYNC -> startBotsInAsync(policy, botList)
        }
    }

    private fun startBotsInAsync(policy: BotRegistrationFailurePolicy, botList: List<Bot>) {
        when (policy) {
            BotRegistrationFailurePolicy.ERROR -> {
                application.launch {
                    try {
                        coroutineScope {
                            botList.forEach { bot ->
                                launch { bot.start() }
                                logger.debug("Launched to start bot {}", bot)
                            }
                        }
                    } catch (e: Throwable) {
                        val err = BotAutoStartOnFailureException(e)
                        logger.error(
                            "There are certain bots that have exceptions in asynchronous startups, " +
                                "application will be cancelled",
                            err
                        )
                        application.cancel(err)
                    }
                }
            }

            BotRegistrationFailurePolicy.ERROR_LOG -> startBotsInAsyncOnFutureWithLog(botList) { error(it.message, it) }
            BotRegistrationFailurePolicy.WARN -> startBotsInAsyncOnFutureWithLog(botList) { warn(it.message, it) }
            BotRegistrationFailurePolicy.IGNORE -> startBotsInAsyncOnFutureWithLog(botList) { debug(it.message, it) }
        }
    }

    private inline fun startBotsInAsyncOnFutureWithLog(
        botList: List<Bot>,
        crossinline onFailure: Logger.(e: Throwable) -> Unit
    ) {
        application.launch {
            supervisorScope {
                botList.forEach { bot ->
                    launch {
                        runCatching { bot.start() }
                            .getOrElse { e -> logger.onFailure(BotAutoStartOnFailureException(e)) }
                    }
                    logger.debug("Launched to start bot {}", bot)
                }
            }
        }
    }

    private fun startBotsInBlocking(policy: BotRegistrationFailurePolicy, botList: List<Bot>) {
        for (bot in botList) {
            logger.debug("Starting bot {}", bot)
            try {
                bot.startBlocking()
                logger.debug("Bot {} started successfully", bot)
            } catch (e: Throwable) {
                val message = "Bot $bot auto start on failure: ${e.localizedMessage}"
                val ex = BotAutoStartOnFailureException(message, e)
                when (policy) {
                    BotRegistrationFailurePolicy.ERROR -> throw ex
                    BotRegistrationFailurePolicy.ERROR_LOG -> logger.error(message, ex)
                    BotRegistrationFailurePolicy.WARN -> logger.warn(message, ex)
                    BotRegistrationFailurePolicy.IGNORE -> logger.debug(message, ex)
                }
            }
        }
    }
}

internal class BotConfigResourceLoadOnFailureException(message: String?, cause: Throwable? = null) : RuntimeException(message, cause)

internal class MismatchConfigurableBotManagerException(message: String?) : RuntimeException(message)

internal class BotRegisterFailureException(message: String?, cause: Throwable?) : RuntimeException(message, cause)

internal class BotAutoStartOnFailureException(message: String?, cause: Throwable?) : RuntimeException(message, cause) {
    constructor(cause: Throwable) : this(cause.message, cause)
}

