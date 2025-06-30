package org.polyfrost.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

/**
 * Provides the dependencies that should be included in the final jar.
 * All deps are marked NON-TRANSITIVE; thus, all transitive deps must be explicitly included.
 * @param version The version of Minecraft. If null, the method is running inside the `:dependencies:legacy` module.
 * @param loader The mod loader being used.
 */
fun Project.provideIncludedDependencies(version: Triple<Int, Int, Int>?, loader: String?): List<OCDependency> { // Either a String or ExternalModuleDependency
    project.logger.lifecycle("===> Adding dependencies for Minecraft ${version?.toMCVer()} & $loader")

    val libs = rootProject
        .extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
    val deps = mutableListOf<Any>()
    deps.addAll(libs.findBundle("kotlin").get().get())
    deps.addAll(libs.findBundle("kotlinx").get().get())
    deps.addAll(libs.findBundle("nightconfig").get().get())
    deps.add(libs.findLibrary("snakeyaml").get().get())
    deps.add(libs.findLibrary("isolated-lwjgl3-loader").get().get())
    deps.add(libs.findLibrary("textile").get().get())
    deps.add(libs.findLibrary("polyio").get().get())
    val copycat = libs.findLibrary("copycat").get().get()
    deps.add(copycat)
    setOf(
        "windows" to setOf("x64", "x86"),
        "linux" to setOf("x64", "x86", "arm", "arm64"),
        "osx" to setOf("x64", "arm64")
    ).forEach { (os, arches) ->
        arches.forEach { arch ->
            deps.add("${copycat.group}:${copycat.name}-natives-$os-$arch:${copycat.version}")
        }
    }

    deps.add(libs.findLibrary("copycat-image-awt").get().get())

    if (version != null && version.second >= 16) {
        logger.lifecycle("===> Adding LWJGL dependencies for Minecraft ${version.toMCVer()} & $loader")

        // Modern (1.16+)
        val lwjglBase = "org.lwjgl:lwjgl"
        val lwjglVersion = when (version.second) {
            in 16..18 -> "3.2.2"
            19 -> "3.3.1"
            20 -> "3.3.2"
            21 -> "3.3.3"
            else -> error("Unsupported Minecraft version: ${version.toMCVer()}")
        }

        deps.add("$lwjglBase-tinyfd:$lwjglVersion")
        deps.add("$lwjglBase-nanovg:$lwjglVersion")

        logger.lifecycle("===> LWJGL version: $lwjglVersion")
    }

    deps.add(libs.findLibrary("polyui").get().get())
    deps.add(libs.findLibrary("hypixel-modapi").get().get())
    deps.add(libs.findLibrary("hypixel-data").get().get())
    if (loader == "fabric") {
        deps.add(libs.findLibrary("fabric-language-kotlin").get().get())
    } else if (version != null && version.second > 12) { // forge / neoforge
        // TODO add KFF
    }
    if ((version == null && loader != null) // legacy dep module
        || (version != null && version.first == 1 && version.second <= 12 && loader == "forge")) {
        deps.add(libs.findLibrary("mixin").get().get()) // PolyMixin
    }
    if (version != null && version.second <= 12) {
        deps.add(libs.findLibrary("brigadier").get().get())
    }
    if (version == null && loader != null) {
        deps.add(libs.findLibrary("asm").get().get())
    }
    deps.add(libs.findLibrary("mixin-extras").get().get())
    val actualDeps = mutableListOf<OCDependency>()
    for (dep in deps) {
        actualDeps.add(OCDependency(dep))
    }
    if (version != null) {
        actualDeps.add(OCDependency("dev.deftu:textile-${version.toMCVer()}-$loader:${libs.findVersion("textile").get().displayName}", true))
        actualDeps.add(OCDependency("dev.deftu:omnicore-${version.toMCVer()}-$loader:${libs.findVersion("omnicore").get().displayName}", true))
    }
    return actualDeps
}

private fun Triple<Int, Int, Int>.toMCVer() = listOf(first, second, third).dropLastWhile { it == 0 }.joinToString(".")

data class OCDependency(val dep: Any, val mod: Boolean = false)