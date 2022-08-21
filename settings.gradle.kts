pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        mavenCentral()
        maven("https://repo.polyfrost.cc/releases")
        maven("https://maven.architectury.dev/")
    }
    plugins {
        val egtVersion = "0.1.1314241"
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

val buildingCi = System.getProperty("BUILDING_CI") == "true"

rootProject.name = mod_name

if (!buildingCi) {
    include(":lwjgl")
    project(":lwjgl").apply {
        projectDir = file("lwjgl/")
        buildFileName = "root.gradle.kts"
    }
}

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
    "1.16.2-forge",
    "1.16.2-fabric"
).forEach { version ->
    include(":platform:$version")
    project(":platform:$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../build.gradle.kts"
    }
    if (!buildingCi) {
        include(":lwjgl:$version")
        project(":lwjgl:$version").apply {
            projectDir = file("lwjgl/$version")
            buildFileName = "../build.gradle.kts"
        }
    }
}
