@file:Suppress("PropertyName")

pluginManagement {
    repositories {
        maven("https://maven.kikugie.dev/snapshots")

        mavenLocal()

        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        kotlin("jvm") version ("2.3.20")
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
        add(version to listOf(FABRIC))
    }

    both("26.2")
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

    // mirrors every Fabric node of the minecraft tree
    create("bootstrap") {
        versions.forEach { (version, loaders) ->
            loaders.filter { it == FABRIC }.forEach { loader ->
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
    "notifications",
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
