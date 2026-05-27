@file:Suppress("PropertyName")

pluginManagement {
    repositories {
        maven("https://maven.kikugie.dev/snapshots")

        mavenLocal()

        // Default
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version ("2.3.0")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
}


interface ModLoader {
    val name: String
    fun buildFile(version: String): String
    fun versionName(version: String): String
}

val stonecutterExt = stonecutter
val FABRIC: ModLoader = object : ModLoader {
    override val name: String = "fabric"
    override fun versionName(version: String) = "$version-fabric"
    override fun buildFile(version: String) = if (stonecutterExt.eval(version, ">= 26.1")) {
        "fabric.gradle.kts"
    } else {
        "fabric.obf.gradle.kts"
    }
}

val NEO_FORGE: ModLoader = object : ModLoader {
    override val name: String = "neoforge"
    override fun versionName(version: String) = "$version-neoforge"
    override fun buildFile(version: String) = "neoforge.gradle.kts"
}

val versions = buildList {
    fun fabric(version: String) = add(version to listOf(FABRIC))
    fun neoforge(version: String) = add(version to listOf(NEO_FORGE))
    fun both(version: String) {
        add(version to listOf(FABRIC, NEO_FORGE))
    }

    both("26.1")
    both("1.21.11")
    both("1.21.10")
    both("1.21.8")
    both("1.21.5")
    both("1.21.4")
    both("1.21.1")
}

stonecutter {
    create("minecraft") {
        versions.forEach { (version, loaders) ->
            loaders.forEach { loader ->
                version(loader.versionName(version), version).buildscript = loader.buildFile(version)
            }
        }
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("fabric") {
            from(files(rootProject.projectDir.resolve("gradle/fabric.versions.toml")))
        }
        create("neoforge") {
            from(files(rootProject.projectDir.resolve("gradle/neoforge.versions.toml")))
        }
        versions.forEach { (version, loaders) ->
            val common = "common$version".replace(".", "")
            create(common) {
                println("creating version catalogue $common")
                val file = rootProject.projectDir.resolve("gradle/common/$version.versions.toml")

                if (!file.exists()) {
                    file.parentFile.mkdirs()
                    file.createNewFile()
                }

                from(files(file))
            }

            loaders.forEach { loader ->
                val name = "${loader.name}$version".replace(".", "")
                create(name) {
                    println("creating version catalogue $name")
                    val file = rootProject.projectDir.resolve("gradle/${loader.name}/$version.versions.toml")

                    if (!file.exists()) {
                        file.parentFile.mkdirs()
                        file.createNewFile()
                    }

                    from(files(file))
                }
            }
        }
    }
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
    //"dependencies:legacy",
    "utils",
    "relocator",
    "poly-compose",
    "compose-bundle",
).forEach { module ->
    include(":modules:$module")
}

/*
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

// FOR ALL NEW VERSIONS MAKE SURE TO INCLUDE THEM IN root.gradle.kts !
include(":minecraft")
project(":minecraft").buildFileName = "root.gradle.kts"
/*
include(":bootstrap")
project(":bootstrap").buildFileName = "root.gradle.kts"
listOf(
    "1.21.1-neoforge",
    "1.21.1-fabric",

    "1.21.4-neoforge",
    "1.21.4-fabric",

    "1.21.5-neoforge",
    "1.21.5-fabric",

    "1.21.8-neoforge",
    "1.21.8-fabric",

    "1.21.10-neoforge",
    "1.21.10-fabric",

    "1.21.11-neoforge",
    "1.21.11-fabric",

    "26.1-fabric"
).forEach { version ->
    val proj = ":minecraft:$version"
    include(proj)
    project(proj).apply {
        projectDir = file("minecraft/versions/$version").also {
            if (!it.exists() && !it.mkdirs()) {
                throw IllegalStateException("Could not create project directory: ${it.absolutePath}")
            }
        }
        buildFileName = "../../build.gradle.kts"
    }
    val bootstrapProj = ":bootstrap:bootstrap-$version"
    if (listOf(
            "1.21.1-fabric",
            "1.21.4-fabric",
            "1.21.5-fabric",
            "1.21.8-fabric",
            "1.21.10-fabric",
            "1.21.11-fabric",
//            "26.1-fabric"
        ).contains(version)) {
        include(bootstrapProj)
        project(bootstrapProj).apply {
            projectDir = file("bootstrap/versions/$version").also {
                if (!it.exists() && !it.mkdirs()) {
                    throw IllegalStateException("Could not create project directory: ${it.absolutePath}")
                }
            }
            buildFileName = "../../build.gradle.kts"
        }
    }
}*/
