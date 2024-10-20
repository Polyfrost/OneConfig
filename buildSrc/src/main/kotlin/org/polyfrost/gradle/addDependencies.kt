package org.polyfrost.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

fun Project.provideIncludedDependencies(version: Triple<Int, Int, Int>?, loader: String): List<OCDependency> { // Either a String or ExternalModuleDependency
    val libs = project
        .extensions
        .getByType<VersionCatalogsExtension>()
        .named("libs")
    val deps = mutableListOf<Any>()
    deps.addAll(libs.findBundle("kotlin").get().get())
    deps.addAll(libs.findBundle("kotlinx").get().get())
    deps.addAll(libs.findBundle("nightconfig").get().get())
    deps.add(libs.findLibrary("isolated-lwjgl3-loader").get().get())
    deps.add(libs.findLibrary("deftu-filestream").get().get())
    deps.add(libs.findLibrary("polyui").get().get())
    deps.add(libs.findLibrary("hypixel-modapi").get().get())
    deps.add(libs.findLibrary("hypixel-data").get().get())
    if (loader == "fabric") {
        deps.add(libs.findLibrary("fabric-language-kotlin").get().get())
    } else if (version != null && version.second > 12) { // forge / neoforge
        // TODO add KFF
    }
    val actualDeps = mutableListOf<OCDependency>()
    for (dep in deps) {
        actualDeps.add(OCDependency(dep))
    }
    if (version != null) {
        actualDeps.add(OCDependency("org.polyfrost:universalcraft-${version.toMCVer()}-$loader:${libs.findVersion("universalcraft").get().displayName}", true))
    }
    return actualDeps
}

private fun Triple<Int, Int, Int>.toMCVer() = listOf(first, second, third).dropLastWhile { it == 0 }.joinToString(".")

data class OCDependency(val dep: Any, val mod: Boolean = false)