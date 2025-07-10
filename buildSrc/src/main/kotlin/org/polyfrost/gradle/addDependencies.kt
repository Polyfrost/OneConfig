package org.polyfrost.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

private val legacyFabricApiModules = listOf(
    "api-base",
    "lifecycle-events-v1",
    "keybindings-api-v1",
    "rendering-api-v1"
)

private val fabricApiModules = listOf( // command-api-v2 introduced in 1.19, transitive-access-wideners-v1 introduced in 1.18
    "api-base",
    "lifecycle-events-v1",
    "key-binding-api-v1",
    "rendering-v1",
    "screen-api-v1"
)

// Yes, this is genuinely worth the effort of maintaining. Without this, syncing would require having to remap significantly more modules.
private val fabricApiModuleVersions = mapOf(
    "api-base-common" to "1.1.0+7c545fdb81e6", // Common (Legacy Fabric API)
    "api-base-10809" to "1.1.0+1.8.9+2c3f108c81e6", // 1.8.9
    "api-base-11202" to "1.1.0+1.8.9+2c3f108c81e6", // 1.12.2
    "api-base-11605" to "0.4.0+3cc0f0907d", // 1.16.5
    "api-base-11701" to "0.4.0+cf39a74318", // 1.17.1
    "api-base-11802" to "0.4.5+64b7c69360", // 1.18.2
    "api-base-11902" to "0.4.15+8f4e8eb390", // 1.19.2
    "api-base-11904" to "0.4.28+737a6ee8f4", // 1.19.4
    "api-base-12001" to "0.4.32+1802ada577", // 1.20.1
    "api-base-12004" to "0.4.37+78d798af4f", // 1.20.4
    "api-base-12006" to "0.4.40+80f8cf51b0", // 1.20.6
    "api-base-12101" to "0.4.42+6573ed8c19", // 1.21.1
    "api-base-12102" to "0.4.48+c47b9d4373", // 1.21.2
    "api-base-12103" to "0.4.50+119c825640", // 1.21.3
    "api-base-12104" to "0.4.54+b47eab6b04", // 1.21.4
    "api-base-12105" to "0.4.62+73a52b4b49", // 1.21.5
    "lifecycle-events-v1-common" to "1.1.0+2c3f108c81e6", // Common (Legacy Fabric API)
    "lifecycle-events-v1-10809" to "1.1.0+1.8.9+2c3f108c81e6", // 1.8.9
    "lifecycle-events-v1-11202" to "1.1.0+1.12.2+2c3f108c81e6", // 1.12.2
    "lifecycle-events-v1-11605" to "1.2.2+3cc0f0907d", // 1.16.5
    "lifecycle-events-v1-11701" to "1.4.6+0392f3a618", // 1.17.1
    "lifecycle-events-v1-11802" to "2.1.1+cc71601c60", // 1.18.2
    "lifecycle-events-v1-11902" to "2.2.4+1b46dc7890", // 1.19.2
    "lifecycle-events-v1-11904" to "2.2.19+10ce000ff4", // 1.19.4
    "lifecycle-events-v1-12001" to "2.2.23+1802ada577", // 1.20.1
    "lifecycle-events-v1-12004" to "2.3.1+a67ffb5d4f", // 1.20.4
    "lifecycle-events-v1-12006" to "2.3.4+c5fc38b3b0", // 1.20.6
    "lifecycle-events-v1-12101" to "2.6.0+0865547519", // 1.21.1
    "lifecycle-events-v1-12102" to "2.3.22+c47b9d4373", // 1.21.2
    "lifecycle-events-v1-12103" to "2.5.1+6da5ef6940", // 1.21.3
    "lifecycle-events-v1-12104" to "2.5.4+bf2a60eb04", // 1.21.4
    "lifecycle-events-v1-12105" to "2.6.0+230071a049", // 1.21.5
    "keybindings-api-v1-common" to "1.1.1+281301ea81e6", // Common (Legacy Fabric API)
    "keybindings-api-v1-10809" to "1.1.1+1.8.9+2c3f108c81e6", // 1.8.9
    "keybindings-api-v1-11202" to "1.1.1+1.12.2+2c3f108c81e6", // 1.12.2
    "key-binding-api-v1-11605" to "1.0.5+3cc0f0907d", // 1.16.5
    "key-binding-api-v1-11701" to "1.0.6+2a2bb57318", // 1.17.1
    "key-binding-api-v1-11802" to "1.0.12+54e5b2ec60", // 1.18.2
    "key-binding-api-v1-11902" to "1.0.25+5c4fce2890", // 1.19.2
    "key-binding-api-v1-11904" to "1.0.35+504944c8f4", // 1.19.4
    "key-binding-api-v1-12001" to "1.0.38+1802ada577", // 1.20.1
    "key-binding-api-v1-12004" to "1.0.42+78d798af4f", // 1.20.4
    "key-binding-api-v1-12006" to "1.0.45+80f8cf51b0", // 1.20.6
    "key-binding-api-v1-12101" to "1.0.47+0af3f5a719", // 1.21.1
    "key-binding-api-v1-12102" to "1.0.53+c47b9d4373", // 1.21.2
    "key-binding-api-v1-12103" to "1.0.53+fd37071f40", // 1.21.3
    "key-binding-api-v1-12104" to "1.0.57+7d48d43904", // 1.21.4
    "key-binding-api-v1-12105" to "1.0.63+ecf51cdc49", // 1.21.5
    "rendering-api-v1-common" to "1.0.0+7c545fdb81e6", // Common (Legacy Fabric API)
    "rendering-api-v1-10809" to "1.0.0+1.8.9+2c3f108c81e6", // 1.8.9
    "rendering-api-v1-11202" to "1.0.0+1.8.9+2c3f108c81e6", // 1.12.2
    "rendering-v1-11605" to "1.6.1+3cc0f0907d", // 1.16.5
    "rendering-v1-11701" to "1.10.1+377137cc18", // 1.17.1
    "rendering-v1-11802" to "1.11.0+b7f3cf3460", // 1.18.2
    "rendering-v1-11902" to "1.13.0+526f2c6790", // 1.19.2
    "rendering-v1-11904" to "2.1.5+10ce000ff4", // 1.19.4
    "rendering-v1-12001" to "3.0.9+1802ada577", // 1.20.1
    "rendering-v1-12004" to "3.2.1+6fd945a04f", // 1.20.4
    "rendering-v1-12006" to "4.2.5+850ef40bb0", // 1.20.6
    "rendering-v1-12101" to "5.0.5+df16efd019", // 1.21.1
    "rendering-v1-12102" to "8.0.5+c47b9d4373", // 1.21.2
    "rendering-v1-12103" to "8.0.8+6922831640", // 1.21.3
    "rendering-v1-12104" to "10.2.1+0d31b09f04", // 1.21.4
    "rendering-v1-12105" to "11.2.0+acfc689549", // 1.21.5
    "screen-api-v1-11605" to "1.0.1+3cc0f0907d", // 1.16.5
    "screen-api-v1-11701" to "1.0.5+cf39a74318", // 1.17.1
    "screen-api-v1-11802" to "1.0.11+d882b91560", // 1.18.2
    "screen-api-v1-11902" to "1.0.32+4d0d570390", // 1.19.2
    "screen-api-v1-11904" to "1.0.49+10ce000ff4", // 1.19.4
    "screen-api-v1-12001" to "2.0.9+1802ada577", // 1.20.1
    "screen-api-v1-12004" to "2.0.18+78d798af4f", // 1.20.4
    "screen-api-v1-12006" to "2.0.21+7b70ea8ab0", // 1.20.6
    "screen-api-v1-12101" to "2.0.25+8b68f1c719", // 1.21.1
    "screen-api-v1-12102" to "2.0.32+c47b9d4373", // 1.21.2
    "screen-api-v1-12103" to "2.0.34+fd37071f40", // 1.21.3
    "screen-api-v1-12104" to "2.0.38+7feeb73304", // 1.21.4
    "screen-api-v1-12105" to "2.0.46+86c3a9f149", // 1.21.5
    "command-api-v1-11605" to "1.1.3+3cc0f0907d", // 1.16.5
    "command-api-v1-11701" to "1.1.4+cf39a74318", // 1.17.1
    "command-api-v1-11802" to "1.1.10+d7c144a860", // 1.18.2
    "command-api-v2-11902" to "2.2.1+413cbbc790", // 1.19.2
    "command-api-v2-11904" to "2.2.10+10ce000ff4", // 1.19.4
    "command-api-v2-12001" to "2.2.14+1802ada577", // 1.20.1
    "command-api-v2-12004" to "2.2.21+78d798af4f", // 1.20.4
    "command-api-v2-12006" to "2.2.24+80f8cf51b0", // 1.20.6
    "command-api-v2-12101" to "2.2.28+6ced4dd919", // 1.21.1
    "command-api-v2-12102" to "2.2.35+c47b9d4373", // 1.21.2
    "command-api-v2-12103" to "2.2.37+c9d82ab240", // 1.21.3
    "command-api-v2-12104" to "2.2.41+e496eb1504", // 1.21.4
    "command-api-v2-12105" to "2.2.49+73a52b4b49", // 1.21.5
    "transitive-access-wideners-v1-11802" to "1.1.0+e747827960", // 1.18.2
    "transitive-access-wideners-v1-11902" to "1.3.3+08b73de490", // 1.19.2
    "transitive-access-wideners-v1-11904" to "3.3.0+1b5f819af4", // 1.19.4
    "transitive-access-wideners-v1-12001" to "4.3.2+1802ada577", // 1.20.1
    "transitive-access-wideners-v1-12004" to "5.0.15+78d798af4f", // 1.20.4
    "transitive-access-wideners-v1-12006" to "6.0.10+74e2f560b0", // 1.20.6
    "transitive-access-wideners-v1-12101" to "6.2.0+45b9699719", // 1.21.1
    "transitive-access-wideners-v1-12102" to "6.1.8+c47b9d4373", // 1.21.2
    "transitive-access-wideners-v1-12103" to "6.2.0+54a41b1c40", // 1.21.3
    "transitive-access-wideners-v1-12104" to "6.3.2+56e78b9b04", // 1.21.4
    "transitive-access-wideners-v1-12105" to "6.3.17+f17a180c49" // 1.21.5
)

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

fun Project.provideFabricApiDependency(version: Triple<Int, Int, Int>): List<OCDependency> {
    val deps = mutableListOf<OCDependency>()

    if (version.second <= 12) {
        // Legacy Fabric
        for (module in legacyFabricApiModules) {
            val commonVersionValue = fabricApiModuleVersions["$module-common"] ?: error("No version found for $module-common")
            deps.add(OCDependency("net.legacyfabric.legacy-fabric-api:legacy-fabric-$module-common:$commonVersionValue", true))
            val mcVersionValue = fabricApiModuleVersions["$module-${version.toPreprocessorNumber()}"] ?: error("No version found for $module-${version.toMCVer()}")
            deps.add(OCDependency("net.legacyfabric.legacy-fabric-api:legacy-fabric-$module:$mcVersionValue", true))
        }
    } else {
        // Modern Fabric
        val finalList = mutableListOf<String>()
        finalList.addAll(fabricApiModules)
        if (version.second >= 18) {
            finalList.add("transitive-access-wideners-v1")
        }
        if (version.second >= 19) {
            finalList.add("command-api-v2")
        } else {
            finalList.add("command-api-v1")
        }
        for (module in finalList) {
            val mcVersionValue = fabricApiModuleVersions["$module-${version.toPreprocessorNumber()}"] ?: error("No version found for $module-${version.toMCVer()}")
            deps.add(OCDependency("net.fabricmc.fabric-api:fabric-$module:$mcVersionValue", true))
        }
    }
    return deps
}

private fun Triple<Int, Int, Int>.toMCVer() = listOf(first, second, third).dropLastWhile { it == 0 }.joinToString(".")
private fun Triple<Int, Int, Int>.toPreprocessorNumber() = first * 10000 + second * 100 + third

data class OCDependency(val dep: Any, val mod: Boolean = false)