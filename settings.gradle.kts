pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.polyfrost.cc/releases")
        maven("https://maven.architectury.dev/")
    }
    plugins {
        val egtVersion = "0.1.10"
        id("gg.essential.multi-version.root") version egtVersion
        id("gg.essential.defaults.repo") version egtVersion
        id("gg.essential.defaults.java") version egtVersion
        id("gg.essential.multi-version.api-validation") version egtVersion
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "io.github.juuxel.loom-quiltflower-mini") {
                useModule("com.github.wyvest:loom-quiltflower-mini:${requested.version}")
            }
        }
    }
}

val mod_name: String by settings

rootProject.name = mod_name

include(":lwjgl")

include(":platform")
project(":platform").apply {
    projectDir = file("versions/")
    buildFileName = "root.gradle.kts"
}

listOf(
    "1.8.9-forge",
    "1.12.2-forge",
    "1.16.2-forge"
).forEach { version ->
    include(":platform:$version")
    project(":platform:$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../build.gradle.kts"
    }
}
