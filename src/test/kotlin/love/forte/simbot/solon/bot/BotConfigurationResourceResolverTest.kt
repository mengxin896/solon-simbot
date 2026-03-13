package love.forte.simbot.solon.bot

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator

class BotConfigurationResourceResolverTest {

    @Test
    fun `should resolve and read relative file bot resources`() {
        val workingDirectory = Files.createTempDirectory("simbot-solon-bot-resolver")

        try {
            val botDirectory = Files.createDirectories(workingDirectory.resolve("simbot-bots"))
            val botFile = botDirectory.resolve("sample.bot.json")
            Files.writeString(botFile, """{"component":"demo.bot"}""")

            val uris = BotConfigurationResourceResolver.resolveFileResourceUris(
                "file:./simbot-bots/*.bot.json",
                workingDirectory
            )

            assertEquals(listOf(botFile.toUri().toString()), uris)

            val content = BotConfigurationResourceResolver.loadFileResourceContent(uris.single(), workingDirectory)
            assertEquals("""{"component":"demo.bot"}""", content)
        } finally {
            deleteRecursively(workingDirectory)
        }
    }

    @Test
    fun `should resolve absolute file bot resources`() {
        val workingDirectory = Files.createTempDirectory("simbot-solon-bot-resolver-abs")

        try {
            val botDirectory = Files.createDirectories(workingDirectory.resolve("simbot-bots"))
            val botFile = botDirectory.resolve("sample.bot.json")
            Files.writeString(botFile, """{"component":"demo.bot"}""")

            val expr = "file:${botDirectory}\\*.bot.json"
            val uris = BotConfigurationResourceResolver.resolveFileResourceUris(expr, workingDirectory)

            assertEquals(listOf(botFile.toUri().toString()), uris)
            assertTrue(BotConfigurationResourceResolver.loadFileResourceContent(uris.single(), workingDirectory) != null)
        } finally {
            deleteRecursively(workingDirectory)
        }
    }

    @Test
    fun `unix style absolute glob should not be resolved against working directory`() {
        val workingDirectory = Files.createTempDirectory("simbot-solon-bot-resolver-unix")

        try {
            val (searchRoot, relativePattern) = BotConfigurationResourceResolver.resolveFileGlob(
                "/tmp/*.bot.json",
                workingDirectory
            )

            assertEquals("*.bot.json", relativePattern)
            assertTrue(searchRoot.normalize() != workingDirectory.resolve("tmp").normalize())
        } finally {
            deleteRecursively(workingDirectory)
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) {
            return
        }

        Files.walk(path)
            .sorted(Comparator.reverseOrder())
            .forEach { target -> Files.deleteIfExists(target) }
    }
}
