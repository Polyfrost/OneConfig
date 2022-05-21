import gg.essential.gradle.util.noServerRunConfigs
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import gg.essential.gradle.util.prebundle

plugins {
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom") version "1.3.0"
    id("io.github.juuxel.loom-quiltflower-mini")
    java
}

java {
    withJavadocJar()
    withSourcesJar()
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project

preprocess {
    vars.put("MODERN", if (project.platform.mcMinor >= 16) 1 else 0)
}

blossom {
    replaceToken("@VER@", mod_version)
    replaceToken("@NAME@", mod_name)
    replaceToken("@ID@", mod_id)
}

version = mod_version
group = "cc.polyfrost"
base {
    archivesName.set(mod_name)
}

loom.noServerRunConfigs()
loom {
    if (project.platform.isLegacyForge) {
        launchConfigs.named("client") {
            property("fml.coreMods.load", "cc.polyfrost.oneconfig.plugin.LoadingPlugin")
            property("mixin.debug.export", "true")
        }
    }
    if (project.platform.isForge) {
        forge {
            mixinConfig("mixins.${mod_id}.json")
        }
    }
    mixin.defaultRefmapName.set("mixins.${mod_id}.refmap.json")
}

repositories {
    maven("https://repo.woverflow.cc/")
}

val relocated = registerRelocationAttribute("relocate") {
    relocate("gg.essential", "cc.polyfrost.oneconfig.libs")
    relocate("com.google", "cc.polyfrost.oneconfig.libs")
}

val shadeRelocated: Configuration by configurations.creating {
    attributes { attribute(relocated, true) }
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val lwjgl: Configuration by configurations.creating

val lwjglNative: Configuration by configurations.creating {
    isTransitive = false
}

val dummyImpl: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

sourceSets {
    val dummy by creating {
        compileClasspath += dummyImpl
    }
    main {
        compileClasspath += dummy.output
        runtimeClasspath += lwjglNative
        output.setResourcesDir(java.classesDirectory)
    }
}

val lwjglJar by tasks.registering(ShadowJar::class) {
    group = "shadow"
    archiveClassifier.set("lwjgl")
    configurations = listOf(lwjgl)
    exclude ("META-INF/versions/**")
    exclude ("**/module-info.class")
    exclude ("**/package-info.class")
    relocate("org.lwjgl", "org.lwjgl3") {
        include ("org.lwjgl.PointerBuffer")
        include ("org.lwjgl.BufferUtils")
    }
}

dependencies {
    dummyImpl("gg.essential:vigilance-1.8.9-forge:222") {
        isTransitive = false
    }

    shadeRelocated("gg.essential:universalcraft-1.8.9-forge:209") {
        isTransitive = false
    }

    shadeRelocated("com.google.code.gson:gson:2.9.0")

    // for other mods and universalcraft
    shade("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
    shade("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.21")
    shade("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21")
    shade("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.1")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.1")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.3.2")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.3.2")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-cbor-jvm:1.3.2")

    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }

    lwjgl ("org.lwjgl:lwjgl:3.3.1")
    lwjgl ("org.lwjgl:lwjgl-stb:3.3.1")
    lwjgl ("org.lwjgl:lwjgl-tinyfd:3.3.1")
    lwjgl ("org.lwjgl:lwjgl-nanovg:3.3.1")
    lwjglNative ("org.lwjgl:lwjgl:3.3.1:natives-windows")
    lwjglNative ("org.lwjgl:lwjgl-stb:3.3.1:natives-windows")
    lwjglNative ("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-windows")
    lwjglNative ("org.lwjgl:lwjgl-nanovg:3.3.1:natives-windows")
    lwjglNative ("org.lwjgl:lwjgl:3.3.1:natives-linux")
    lwjglNative ("org.lwjgl:lwjgl-stb:3.3.1:natives-linux")
    lwjglNative ("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-linux")
    lwjglNative ("org.lwjgl:lwjgl-nanovg:3.3.1:natives-linux")
    lwjglNative ("org.lwjgl:lwjgl:3.3.1:natives-macos")
    lwjglNative ("org.lwjgl:lwjgl-stb:3.3.1:natives-macos")
    lwjglNative ("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-macos")
    lwjglNative ("org.lwjgl:lwjgl-nanovg:3.3.1:natives-macos")
    shade(lwjglJar.get().outputs.files)
    shade(prebundle(shadeRelocated))
}

tasks.processResources {
    inputs.property("id", mod_id)
    inputs.property("name", mod_name)
    val java = if (project.platform.mcMinor >= 18) {
        17
    } else {
        if (project.platform.mcMinor == 17) 16 else 8
    }
    val compatLevel = "JAVA_${java}"
    inputs.property("java", java)
    inputs.property("java_level", compatLevel)
    inputs.property("version", mod_version)
    inputs.property("mcVersionStr", project.platform.mcVersionStr)
    filesMatching(listOf("mcmod.info", "mixins.${mod_id}.json", "mods.toml")) {
        expand(
            mapOf(
                "id" to mod_id,
                "name" to mod_name,
                "java" to java,
                "java_level" to compatLevel,
                "version" to mod_version,
                "mcVersionStr" to project.platform.mcVersionStr
            )
        )
    }
    filesMatching("fabric.mod.json") {
        expand(
            mapOf(
                "id" to mod_id,
                "name" to mod_name,
                "java" to java,
                "java_level" to compatLevel,
                "version" to mod_version,
                "mcVersionStr" to project.platform.mcVersionStr.substringBeforeLast(".") + ".x"
            )
        )
    }
}

tasks {
    withType(Jar::class.java) {
        if (project.platform.isFabric) {
            exclude("mcmod.info", "mods.toml")
        } else {
            exclude("fabric.mod.json")
            if (project.platform.isLegacyForge) {
                exclude("mods.toml")
            } else {
                exclude("mcmod.info")
            }
        }
    }
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("dev")
        configurations = listOf(shade)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
    remapJar {
        input.set(shadowJar.get().archiveFile)
        archiveClassifier.set("")
    }
    jar {
        manifest {
            attributes(
                mapOf(
                    "ModSide" to "CLIENT",
                    "ForceLoadAsMod" to true,
                    "TweakOrder" to "0",
                    "MixinConfigs" to "mixins.oneconfig.json",
                    "FMLCorePlugin" to "cc.polyfrost.oneconfig.plugin.LoadingPlugin",
                    "TweakClass" to "org.spongepowered.asm.launch.MixinTweaker",
                    "FMLCorePluginContainsFMLMod" to "lol"
                )
            )
        }
        dependsOn(shadowJar)
        archiveClassifier.set("")
        enabled = false
    }
}

afterEvaluate {
    val checkFile = file(".gradle/loom-cache/SETUP")
    val lwjglJarDelayed by tasks.creating {
        dependsOn(lwjglJar)
    }

    @Suppress("UNUSED_VARIABLE")
    val setupGradle by tasks.creating {
        dependsOn(lwjglJarDelayed)
        val genSourcesWithQuiltflower = tasks.named("genSourcesWithQuiltflower").get()
        dependsOn(genSourcesWithQuiltflower)
        genSourcesWithQuiltflower.mustRunAfter(lwjglJarDelayed)
        doLast {
            checkFile.parentFile.mkdirs()
            checkFile.createNewFile()
        }
    }

    if (!checkFile.exists()) {
        logger.error("--------------")
        logger.error("PLEASE RUN THE `setupGradle` TASK, OR ELSE UNEXPECTED THING MAY HAPPEN!")
        logger.error("--------------")
    }
}