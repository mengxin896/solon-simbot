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

import org.noear.solon.core.util.ResourceUtil
import java.net.URI
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path

internal object BotConfigurationResourceResolver {
    fun resolveResourceUris(
        classLoader: ClassLoader,
        expr: String,
        workingDirectory: Path = defaultWorkingDirectory(),
    ): List<String> {
        if (ResourceUtil.hasFile(expr)) {
            return resolveFileResourceUris(expr, workingDirectory)
        }

        val schemaPrefix = when {
            ResourceUtil.hasClasspath(expr) -> ResourceUtil.TAG_classpath
            ResourceUtil.hasFile(expr) -> ResourceUtil.TAG_file
            else -> ""
        }

        return ResourceUtil.scanResources(classLoader, expr)
            .map { path -> schemaPrefix + path }
    }

    fun loadResourceContent(
        classLoader: ClassLoader,
        uri: String,
        workingDirectory: Path = defaultWorkingDirectory(),
    ): String? {
        if (ResourceUtil.hasFile(uri)) {
            return loadFileResourceContent(uri, workingDirectory)
        }

        val url = ResourceUtil.findResourceOrFile(classLoader, uri) ?: return null
        return ResourceUtil.getResourceAsString(url)
    }

    internal fun resolveFileResourceUris(
        expr: String,
        workingDirectory: Path = defaultWorkingDirectory(),
    ): List<String> {
        val rawPath = expr.removePrefix(ResourceUtil.TAG_file)
        val normalizedPath = normalizeFilePathExpression(rawPath)

        if (!containsGlob(normalizedPath)) {
            val patternPath = resolveFilePath(normalizedPath, workingDirectory)
            return if (Files.isRegularFile(patternPath)) {
                listOf(patternPath.toUri().toString())
            } else {
                emptyList()
            }
        }

        val (searchRoot, relativePattern) = resolveFileGlob(normalizedPath, workingDirectory)
        if (!Files.exists(searchRoot)) {
            return emptyList()
        }

        val matcher = FileSystems.getDefault().getPathMatcher("glob:$relativePattern")

        Files.walk(searchRoot).use { stream ->
            return stream
                .filter { path -> Files.isRegularFile(path) }
                .map { path -> path.normalize() }
                .filter { path -> matcher.matches(searchRoot.relativize(path)) }
                .map { path -> path.toUri().toString() }
                .sorted()
                .toList()
        }
    }

    internal fun loadFileResourceContent(
        uri: String,
        workingDirectory: Path = defaultWorkingDirectory(),
    ): String? {
        val path = runCatching { Path.of(URI.create(uri)) }
            .getOrElse {
                val rawPath = normalizeFilePathExpression(uri.removePrefix(ResourceUtil.TAG_file))
                resolveFilePath(rawPath, workingDirectory)
            }

        return if (Files.isRegularFile(path)) {
            Files.readString(path)
        } else {
            null
        }
    }

    internal fun resolveFileGlob(rawPath: String, workingDirectory: Path): Pair<Path, String> {
        val normalized = rawPath.replace('\\', '/')
        val isDriveAbsolute = normalized.length >= 3 && normalized[1] == ':' && normalized[2] == '/'
        val isUnixAbsolute = normalized.startsWith("/") && !isDriveAbsolute
        val basePath = when {
            isDriveAbsolute -> Path.of(normalized.substring(0, 3))
            isUnixAbsolute -> Path.of(FileSystems.getDefault().separator)
            else -> workingDirectory
        }

        val segments = normalized.split('/')
            .filter { segment -> segment.isNotEmpty() }
        val startIndex = if (isDriveAbsolute) 1 else 0

        val literalSegments = mutableListOf<String>()
        val globSegments = mutableListOf<String>()
        var wildcardReached = false

        for (index in startIndex until segments.size) {
            val segment = segments[index]
            if (!wildcardReached && !containsGlob(segment)) {
                literalSegments.add(segment)
            } else {
                wildcardReached = true
                globSegments.add(segment)
            }
        }

        val searchRoot = literalSegments.fold(basePath) { current, segment -> current.resolve(segment) }.normalize()
        val relativePattern = globSegments.joinToString(FileSystems.getDefault().separator)

        return searchRoot to relativePattern
    }

    private fun resolveFilePath(rawPath: String, workingDirectory: Path): Path {
        val path = Path.of(rawPath)
        return if (path.isAbsolute) {
            path.normalize()
        } else {
            workingDirectory.resolve(path).normalize()
        }
    }

    private fun normalizeFilePathExpression(rawPath: String): String {
        return when {
            rawPath.startsWith("///") && rawPath.getOrNull(3)?.isLetter() == true && rawPath.getOrNull(4) == ':' -> {
                rawPath.removePrefix("///")
            }

            rawPath.startsWith("//") && rawPath.getOrNull(2)?.isLetter() == true && rawPath.getOrNull(3) == ':' -> {
                rawPath.removePrefix("//")
            }

            rawPath.startsWith("/") && rawPath.getOrNull(1)?.isLetter() == true && rawPath.getOrNull(2) == ':' -> {
                rawPath.removePrefix("/")
            }

            else -> rawPath
        }
    }

    private fun containsGlob(text: String): Boolean = text.any { ch ->
        ch == '*' || ch == '?' || ch == '[' || ch == '{'
    }

    private fun defaultWorkingDirectory(): Path = Path.of("").toAbsolutePath().normalize()
}
