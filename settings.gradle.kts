@file:Suppress("UnstableApiUsage")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenLocal()
        mavenCentral()
        maven("https://repo.polyfrost.org/releases") {
            name = "Polyfrost Releases"
        }
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

val name: String by settings
rootProject.name = name
if (rootDir.name != name) {
    logger.error("""
        Root directory name (${rootDir.absolutePath}) does not match project name ($name)! 
        This may cause issues with indexing and other tools (see https://youtrack.jetbrains.com/issue/IDEA-317606/Changing-only-the-case-of-the-Gradle-root-project-name-causes-exception-while-importing-project-java.lang.IllegalStateException#focus=Comments-27-7257761.0-0 and https://stackoverflow.com/questions/77878944/what-to-do-when-the-java-lang-illegalsateexception-module-entity-with-name ). 
        If you are experiencing issues, please rename the root directory to match the project name, re-import the project, and invalidate caches if you are on IntelliJ.
    """.trimIndent())
}

include(":modules")
project(":modules").apply {
    buildFileName = "root.gradle.kts"
}

include(":platform")
project(":platform").apply {
    projectDir = file("versions/")
    buildFileName = "preprocessor.gradle.kts"
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
    "dependencies:legacy",
    "dependencies:modern",
    "dependencies:bundled",
    "utils"
).forEach { module ->
    include(":modules:$module")
}

// FOR ALL NEW VERSIONS MAKE SURE TO INCLUDE THEM IN preprocessor.gradle.kts !
listOf(
    "1.8.9-forge",
    "1.8.9-fabric",
    "1.12.2-fabric",
    "1.12.2-forge",
    "1.16.5-forge",
    "1.16.5-fabric",
    "1.17.1-forge",
    "1.17.1-fabric",
    "1.18.2-forge",
    "1.18.2-fabric",
    "1.19.4-forge",
    "1.19.4-fabric",
    "1.20.4-fabric",
    "1.20.4-forge"
).forEach { version ->
    val proj = ":platform:$version"
    include(proj)
    project(proj).apply {
        projectDir = file("versions/$version")
        buildFileName = "../build.gradle.kts"
    }
}
