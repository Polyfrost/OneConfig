pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven("https://repo.polyfrost.cc/releases")
    }
    plugins {
        val pgtVersion = "0.1.23"
        id("cc.polyfrost.multi-version.root") version pgtVersion
        id("cc.polyfrost.defaults.repo") version pgtVersion
        id("cc.polyfrost.defaults.java") version pgtVersion
        id("cc.polyfrost.multi-version.api-validation") version pgtVersion
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

include(":platform")
project(":platform").apply {
    projectDir = file("versions/")
    buildFileName = "root.gradle.kts"
}

listOf(
    "1.8.9-forge",
    "1.8.9-fabric",
    "1.12.2-fabric",
    "1.12.2-forge",
    "1.16.5-forge",
    "1.16.5-fabric"
).forEach { version ->
    include(":platform:$version")
    project(":platform:$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../build.gradle.kts"
    }
}
