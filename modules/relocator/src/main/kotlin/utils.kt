package org.polyfrost.oneconfig.relocator

import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.readText

internal enum class SourceKind(val fileExtension: String) {
    JAVA("java"),
    KOTLIN("kt"),
    ;
}

internal fun convertStringToIntVersion(version: String): Int {
    val version = if (version.substringBefore('.') != "1") {
        version.substringBefore("-")
    } else version

    return version.let { if (it.count { char -> char == '.' } == 1) "$it.0" else it }.split(".")
        .joinToString("") { it.padStart(2, '0') }.toInt()
}

internal enum class SourceLocation(val path: String) {
    PRE_PROCESSED("build/preprocessed/main"),
    NORMAL("src/main"),
    ;
}

internal object SourceFileHelper {
    lateinit var dir: Path
    fun readSourceFile(kind: SourceKind, path: String) = SourceLocation.entries.map { dir.resolve(it.path) }
        .map { it.resolve(kind.name.lowercase()) }
        .map { it.resolve(path.replace(".", "/")) }
        .flatMap { listOf(it.resolveSibling("${it.nameWithoutExtension}.${kind.fileExtension}")) }
        .firstOrNull { it.exists() }?.readText()

    fun String.replacePatterns(target: String): String = this.replace("/*!target!*/", "_$target")
}

/**
 * Rewrites [sourcePackage] to [targetPackage] in both the dotted (imports, references) and the
 * internal/slash form. The slash form appears inside mixin member descriptors
 * (e.g. `method = "<init>(Lio/github/notenoughupdates/moulconfig/Config;)V"`); leaving it untouched
 * makes relocated injections silently fail to bind.
 */
internal fun String.relocatePackage(sourcePackage: String, targetPackage: String): String =
    this.replace(sourcePackage, targetPackage)
        .replace(sourcePackage.replace('.', '/'), targetPackage.replace('.', '/'))