@file:Suppress("PropertyName")

import groovy.lang.MissingPropertyException

pluginManagement {
    repositories {
        // Releases
        maven("https://maven.deftu.dev/releases")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://server.bbkr.space/artifactory/libs-release/")
        maven("https://jitpack.io/")

        // Snapshots
        maven("https://maven.deftu.dev/snapshots")
        mavenLocal()

        // Default
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version("2.0.20")
        id("dev.deftu.gradle.multiversion-root") version("2.48.0") // Update in libs.versions.toml too!!!
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://repo.polyfrost.org/releases")
        maven("https://repo.hypixel.net/repository/Hypixel")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.8.+")
}

val projectName: String = extra["project.name"]?.toString()
    ?: throw MissingPropertyException("mod.name has not been set.")

rootProject.name = projectName
if (rootDir.name != projectName) {
    logger.error("""
        Root directory name (${rootDir.absolutePath}) does not match project name ($projectName)! 
        This may cause issues with indexing and other tools (see https://youtrack.jetbrains.com/issue/IDEA-317606#focus=Comments-27-7257761.0-0 and https://stackoverflow.com/questions/77878944 ). 
        If you are experiencing issues, please rename the root directory to match the project name, re-import the project, and invalidate caches if you are on IntelliJ.
    """.trimIndent())
}

include(":modules")
project(":modules").apply {
    buildFileName = "root.gradle.kts"
}

listOf(
    "config",
    "config-impl",
    "commands",
    "hud",
    "events",
    "ui",
    "internal",
    "dependencies",
    "dependencies:agnostic",
    "dependencies:legacy",
    "utils",
    "relocator"
).forEach { module ->
    include(":modules:$module")
}

// FOR ALL NEW VERSIONS MAKE SURE TO INCLUDE THEM IN root..gradle.kts !
include(":minecraft")
project(":minecraft").buildFileName = "root.gradle.kts"
listOf(
    "1.8.9-forge",
    "1.8.9-fabric",

    "1.12.2-forge",
    "1.12.2-fabric",

    "1.16.5-forge",
    "1.16.5-fabric",

    "1.17.1-forge",
    "1.17.1-fabric",

    "1.18.2-forge",
    "1.18.2-fabric",

    "1.19.2-forge",
    "1.19.2-fabric",

    "1.19.4-forge",
    "1.19.4-fabric",

    "1.20.1-forge",
    "1.20.1-fabric",

    "1.20.4-forge",
    "1.20.4-neoforge",
    "1.20.4-fabric",

    "1.20.6-neoforge",
    "1.20.6-fabric",

    "1.21.1-neoforge",
    "1.21.1-fabric",

    "1.21.2-neoforge",
    "1.21.2-fabric",

    "1.21.3-neoforge",
    "1.21.3-fabric",

    "1.21.4-neoforge",
    "1.21.4-fabric",

    "1.21.5-neoforge",
    "1.21.5-fabric"
).forEach { version ->
    val proj = ":minecraft:$version"
    include(proj)
    project(proj).apply {
        projectDir = file("minecraft/versions/$version")
        buildFileName = "../../build.gradle.kts"
    }
}
