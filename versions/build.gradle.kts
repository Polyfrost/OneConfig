@file:Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")

import net.fabricmc.loom.task.RemapSourcesJarTask
import org.polyfrost.gradle.util.noServerRunConfigs
import org.polyfrost.gradle.util.prebundle
import java.text.SimpleDateFormat
import java.util.concurrent.atomic.AtomicReference

plugins {
    alias(libs.plugins.kotlin)
    id(libs.plugins.pgt.main.get().pluginId)
    id(libs.plugins.pgt.default.get().pluginId)
    id(libs.plugins.shadow.get().pluginId)
    id("maven-publish")
    id("signing")
    java
}

java {
    withSourcesJar()
    withJavadocJar()
}


val modName = properties["mod_name"] as String
val modMajor = properties["mod_major_version"] as String
val modMinor = properties["mod_minor_version"] as String
val modId = properties["mod_id"] as String
version = "$modMajor$modMinor"
group = "org.polyfrost"

val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")
val tweakClass = "org.polyfrost.oneconfig.internal.init.OneConfigTweaker"

base {
    archivesName.set("$modId-$platform")
}

loom {
    noServerRunConfigs()
    runConfigs {
        "client" {
            if (project.platform.isLegacyForge) {
                programArgs("--tweakClass", tweakClass)
            }
            property("mixin.debug.export", "true")
            property("debugBytecode", "true")
            property("forge.logging.console.level", "debug")
            if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
                property("fml.earlyprogresswindow", "false")
            }
        }
    }
    if (project.platform.isForge) {
        forge {
            mixinConfig("mixins.${modId}.json")
        }
    }
    @Suppress("UnstableApiUsage")
    mixin.defaultRefmapName.set("mixins.${modId}.refmap.json")
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.polyfrost.org/releases")
}

val shade: Configuration by configurations.creating {
    configurations.api.get().extendsFrom(this)
}

val shadeMod: Configuration by configurations.creating {
    configurations.modApi.get().extendsFrom(this)
}

dependencies {
    compileOnly("gg.essential:vigilance-1.8.9-forge:295") {
        isTransitive = false
    }

    shadeMod("org.polyfrost:universalcraft-$platform:${libs.versions.universalcraft.get()}")

    shade(libs.polyui)
    if (platform.isLegacyForge || platform.isFabric) {
        shade(libs.bundles.nightconfig)
    } else {
        // modern forge includes nightconfig-core and nightconfig-toml
        // so, we can just include the ones we need
        shade(libs.nightconfig.json) {
            isTransitive = false
        }
        shade(libs.nightconfig.yaml) {
            isTransitive = false
        }
    }

    // for other mods and universalcraft
    shade(libs.bundles.kotlin)
    shade(libs.bundles.kotlinx)

    for (project in rootProject.project(":modules").subprojects) {
        shade(project(project.path)) {
            isTransitive = false
        }
    }

    if (platform.isLegacyForge) {
        shade(libs.mixin) {
            isTransitive = false
        }
    }


    val isLegacy = platform.isLegacyForge || platform.isLegacyFabric
    val lwjglVersion = libs.versions.lwjgl.get()
    if (isLegacy) {
        val cfg = configurations.create("lwjglBundleForLegacy")
        for (dep in listOf("-nanovg", "-tinyfd", "-stb", "")) {
            val lwjglDep = "org.lwjgl:lwjgl$dep:$lwjglVersion"
            compileOnly(cfg(lwjglDep) {
                isTransitive = false
            })
            for (native in natives) {
                runtimeOnly(cfg("$lwjglDep:natives-$native") {
                    isTransitive = false
                })
            }
        }
        shade(prebundle(cfg, "lwjgl-legacy.jar"))
    } else {
        val nvgDep = "org.lwjgl:lwjgl-nanovg:$lwjglVersion"
        shade(nvgDep) {
            isTransitive = false
        }
        for(native in natives) {
            shade("$nvgDep:natives-$native") {
                isTransitive = false
            }
        }
    }

    if (!platform.isLegacyFabric) {
        modRuntimeOnly(
            "me.djtheredstoner:DevAuth-" +
                    (if (platform.isForge) {
                        if (platform.isLegacyForge) "forge-legacy" else "forge-latest"
                    } else "fabric")
                    + ":1.1.2"
        )
    }
}

tasks {
    withType(Jar::class) {
        val atomicLines = AtomicReference(listOf<String>())

        // This removes the 22rd line in fabric.mod.json,
        // aka the TestMod entrypoint, for production builds.
        val fabricModJson = layout.buildDirectory.get().asFile.resolve("resources")
            .resolve("main")
            .resolve("fabric.mod.json")
        doFirst {
            if (fabricModJson.exists()) {
                val lines = fabricModJson.readLines()
                if (lines[21].contains("TestMod")) {
                    atomicLines.set(lines)

                    fabricModJson.delete()
                    fabricModJson.writeText(
                        lines.subList(0, 21).joinToString("\n") + "\n" + lines.subList(
                            22,
                            lines.size
                        ).joinToString("\n")
                    )
                }
            }
        }

        doLast {
            val lines = atomicLines.get()
            if (lines.isEmpty()) return@doLast
            fabricModJson.delete()
            fabricModJson.writeText(lines.joinToString("\n"))
        }
    }
    processResources {
        inputs.property("id", modId)
        inputs.property("name", modName)
        val compatLevel = "JAVA_8"
        inputs.property("java", 8)
        inputs.property("java_level", compatLevel)
        inputs.property("version", project.version)
        inputs.property("mcVersionStr", project.platform.mcVersionStr)

        filesMatching(listOf("mcmod.info", "mixins.${modId}.json", "**/mods.toml")) {
            expand(
                mapOf(
                    "id" to modId,
                    "name" to modName,
                    "java" to 8,
                    "java_level" to compatLevel,
                    "version" to project.version,
                    "mcVersionStr" to project.platform.mcVersionStr
                )
            )
        }
        filesMatching("fabric.mod.json") {
            expand(
                mapOf(
                    "id" to modId,
                    "name" to modName,
                    "java" to 8,
                    "java_level" to compatLevel,
                    "version" to project.version,
                    "mcVersionStr" to project.platform.mcVersionStr.substringBeforeLast(".") + ".x"
                )
            )
        }
    }

    withType(Jar::class.java) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        exclude("META-INF/com.android.tools/**")
        exclude("META-INF/proguard/**")
        if (project.platform.isFabric) {
            exclude("mcmod.info", "META-INF/mods.toml")
        } else {
            exclude("fabric.mod.json")
            if (project.platform.isLegacyForge) {
                exclude("**/mods.toml")
                exclude("META-INF/versions/**")
                exclude("**/module-info.class")
                exclude("**/package-info.class")
            } else {
                exclude("mcmod.info")
            }
        }
        if (!name.contains("sourcesjar", ignoreCase = true) || !name.contains("javadoc", ignoreCase = true)) {
            exclude("**/**_Test.**")
            exclude("**/**_Test$**.**")
            exclude("testmod_dark.svg")
        }
    }

    shadowJar {
        archiveClassifier.set("full-dev")
        configurations = listOf(shade, shadeMod)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)
    }

    remapJar {
        inputFile.set(shadowJar.get().archiveFile)
        archiveClassifier = "full"
    }
    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        manifest {
            attributes(
                if (platform.isForge) {
                    if (platform.isLegacyForge) {
                        mapOf(
                            "ModSide" to "CLIENT",
                            "ForceLoadAsMod" to true,
                            "TweakOrder" to "0",
                            "MixinConfigs" to "mixins.$modId.json",
                            "TweakClass" to tweakClass
                        )
                    } else {
                        mapOf(
                            "MixinConfigs" to "mixins.$modId.json",
                            "Specification-Title" to modId,
                            "Specification-Vendor" to "Polyfrost",
                            "Specification-Version" to "1", // We are version 1 of ourselves, whatever the hell that means
                            "Implementation-Title" to modName,
                            "Implementation-Version" to project.version,
                            "Implementation-Vendor" to "Polyfrost",
                            "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(`java.util`.Date())
                        )
                    }
                } else {
                    mapOf()
                }
            )
        }
        exclude("**/internal/**")
        archiveClassifier.set("")
    }
    named<Jar>("sourcesJar") {
        exclude("**/internal/**")
        archiveClassifier.set("sources")
        doFirst {
            archiveClassifier.set("sources")
        }
        doLast {
            archiveFile.orNull?.asFile?.let {
                it.copyTo(
                    File(
                        it.parentFile,
                        it.nameWithoutExtension + "-dev" + it.extension.let { if (it.isBlank()) "" else ".$it" }),
                    overwrite = true
                )
            }
            archiveClassifier.set("sources")
        }
    }
    named<Jar>("javadocJar") {
        archiveClassifier.set("javadoc")
    }
    withType<RemapSourcesJarTask> {
        enabled = false
    }
}

publishing {
    publications {
        register<MavenPublication>("$modId-$platform") {
            groupId = group.toString()
            artifactId = base.archivesName.get()

            artifact(tasks["jar"])
            artifact(tasks["remapJar"])
            artifact(tasks["sourcesJar"])
            artifact(tasks["javadocJar"])
        }
    }

    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.polyfrost.org/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "snapshots"
            url = uri("https://repo.polyfrost.org/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "private"
            url = uri("https://repo.polyfrost.org/private")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}

