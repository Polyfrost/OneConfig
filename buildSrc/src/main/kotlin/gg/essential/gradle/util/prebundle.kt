/*
 * This file is part of essential-gradle-toolkit, licensed under the GPL-3.0.
 *
 * Copyright (C) 2025 EssentialGG and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package gg.essential.gradle.util

import com.google.common.base.Stopwatch
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import java.io.File
import java.io.OutputStream
import java.security.MessageDigest
import java.util.jar.JarOutputStream
import java.util.zip.ZipEntry

/**
 * Bundles all dependencies from the given [configuration] into a single dedicated jar and returns a file collection
 * containing that jar
 * Primarily for use in dependency declarations so fat jars of certain dependencies with potentially relocated
 * transitive dependencies can be created and then depended upon as usual
 * Compared to simply relocating in a later shadow task this has the advantage that IDEA will see the relocated
 * dependency rather than the original which for example allows one to use two different versions of the same
 * dependency at dev time
 *
 * If [jijName] is provided the fat jar will additionally be wrapped in an outer jar so that the classes are not
 * actually visible if the file collection is put onto the classpath
 * This may be useful when the jar is never meant to directly be on the classpath but rather only in a dedicated
 * class loader or JVM
 * The given [jijName] determines the path+name of the inner jar within the outer jar
 */
fun Project.prebundle(configuration: Configuration, jijName: String? = null, configure: PatternFilterable.() -> Unit = {}): FileCollection {
    val output = projectDir
        .resolve(".gradle")
        .resolve("prebundled-jars")
        .resolve("${configuration.name}.jar")

    val filter = PatternSet().apply(configure)

    // Delay resolving the configuration in case it is not yet fully configured
    afterEvaluate {
        bundle(configuration, filter, jijName, output, logger)
    }

    return files(output)
}

private fun Project.bundle(configuration: Configuration, filter: PatternSet, jijName: String?, output: File, logger: Logger) {
    output.parentFile.mkdirs()

    val hash = configuration.computeHash().apply {
        update(filter.hashCode().toBigInteger().toByteArray())
        update(jijName?.toByteArray() ?: byteArrayOf())
        update(byteArrayOf(0, 0, 0, 2)) // code version incremented with each semantic change
    }.digest()
    val hashFile = output.resolveSibling(output.name + ".md5")
    if (hashFile.exists() && hashFile.readBytes().contentEquals(hash) && output.exists()) {
        return
    }
    hashFile.delete()
    output.delete()

    val stopwatch = Stopwatch.createStarted()
    logger.lifecycle(":preparing ${configuration.name} jar")

    val spec = filter.asSpec
    val visitedEntries = mutableSetOf<String>()
    output.outputStream().use { fileOut_ ->
        var fileOut: OutputStream = fileOut_
        if (jijName != null) {
            fileOut = JarOutputStream(fileOut).apply {
                putNextEntry(ZipEntry(jijName))
            }
        }
        JarOutputStream(fileOut).use { jarOut ->
            for (sourceFile in configuration.files) {
                project.zipTree(sourceFile).visit {
                    if (!visitedEntries.add(path)) return@visit
                    if (!spec.isSatisfiedBy(this)) return@visit

                    jarOut.putNextEntry(ZipEntry(if (isDirectory) "$path/" else path).apply { time = CONSTANT_TIME_FOR_ZIP_ENTRIES })
                    open().use { copyTo(jarOut) }
                    jarOut.closeEntry()
                }
            }
        }
    }
    hashFile.writeBytes(hash)

    logger.lifecycle(":prepared ${configuration.name} jar in $stopwatch")
}

private fun Configuration.computeHash(): MessageDigest = files
    .sortedBy { it.name }
    .fold(MessageDigest.getInstance("MD5")) { digest, file ->
        // if the file path already contains a hash that is good enough otherwise we need to read its contents
        digest.update(file.findHashInPath()?.toByteArray() ?: file.readBytes())
        digest
    }

private fun File.findHashInPath(): String? {
    val path = absolutePath.replace('\\', '/')
    if ("/caches/modules-2/files-2.1/" in path && parentFile.name.length == 40) {
        return parentFile.name
    }
    if ("/caches/transforms-3/" in path && parentFile.parentFile.name.length == 32) {
        return parentFile.parentFile.name
    }
    return null
}