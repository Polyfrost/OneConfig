import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gg.essential.gradle.util.noServerRunConfigs

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom") version "1.3.0"
    id("io.github.juuxel.loom-quiltflower-mini")
    id("org.jetbrains.dokka") version "1.6.21"
    id("maven-publish")
    id("signing")
    java
}

java {
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
    archivesName.set("$mod_id-$platform")
}
loom {
    noServerRunConfigs()
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
    maven("https://repo.polyfrost.cc/releases")
}

val shade: Configuration by configurations.creating {
    configurations.implementation.get().extendsFrom(this)
}

val lwjgl: Configuration by configurations.creating

val lwjglNative: Configuration by configurations.creating {
    isTransitive = false
}

sourceSets {
    main {
        runtimeClasspath += lwjglNative
        output.setResourcesDir(java.classesDirectory)
    }
}

val lwjglJar by tasks.registering(ShadowJar::class) {
    group = "shadow"
    archiveClassifier.set("lwjgl")
    configurations = listOf(lwjgl)
    exclude("META-INF/versions/**")
    exclude("**/module-info.class")
    exclude("**/package-info.class")
    relocate("org.lwjgl", "org.lwjgl3") {
        include("org.lwjgl.PointerBuffer")
        include("org.lwjgl.BufferUtils")
    }
}

dependencies {
    compileOnly("gg.essential:vigilance-$platform:222") {
        isTransitive = false
    }

    shade("gg.essential:universalcraft-$platform:211") {
        isTransitive = false
    }

    shade("com.github.KevinPriv:keventbus:c52e0a2ea0") {
        isTransitive = false
    }

    // for other mods and universalcraft
    shade("org.jetbrains.kotlin:kotlin-stdlib:1.6.21")
    shade("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.21")
    shade("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.6.21")
    shade("org.jetbrains.kotlin:kotlin-reflect:1.6.21")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.6.1")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.1")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.3.3")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:1.3.3")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-cbor-jvm:1.3.3")

    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }

    lwjgl("org.lwjgl:lwjgl:3.3.1")
    lwjgl("org.lwjgl:lwjgl-stb:3.3.1")
    lwjgl("org.lwjgl:lwjgl-tinyfd:3.3.1")
    lwjgl("org.lwjgl:lwjgl-nanovg:3.3.1")
    lwjglNative("org.lwjgl:lwjgl:3.3.1:natives-windows")
    lwjglNative("org.lwjgl:lwjgl-stb:3.3.1:natives-windows")
    lwjglNative("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-windows")
    lwjglNative("org.lwjgl:lwjgl-nanovg:3.3.1:natives-windows")
    lwjglNative("org.lwjgl:lwjgl:3.3.1:natives-linux")
    lwjglNative("org.lwjgl:lwjgl-stb:3.3.1:natives-linux")
    lwjglNative("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-linux")
    lwjglNative("org.lwjgl:lwjgl-nanovg:3.3.1:natives-linux")
    lwjglNative("org.lwjgl:lwjgl:3.3.1:natives-macos")
    lwjglNative("org.lwjgl:lwjgl-stb:3.3.1:natives-macos")
    lwjglNative("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-macos")
    lwjglNative("org.lwjgl:lwjgl-nanovg:3.3.1:natives-macos")
    shade(lwjglJar.get().outputs.files)

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.6.21")
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
        if (!name.contains("sourcesjar", ignoreCase = true) || !name.contains("dokka", ignoreCase = true)) {
            exclude("**/**_Test.**")
            exclude("**/**_Test$**.**")
        }
    }
    named<ShadowJar>("shadowJar") {
        archiveClassifier.set("dev")
        configurations = listOf(shade, lwjglNative)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE

        relocate("gg.essential", "cc.polyfrost.oneconfig.libs")
        relocate("me.kbrewster", "cc.polyfrost.oneconfig.libs")
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
    dokkaHtml.configure {
        outputDirectory.set(buildDir.resolve("dokka"))
        moduleName.set("OneConfig $platform")
        moduleVersion.set(mod_version)
        dokkaSourceSets {
            configureEach {
                jdkVersion.set(8)
                //reportUndocumented.set(true)
            }
        }
        doLast {
            val outputFile = outputDirectory.get().resolve("images/logo-icon.svg")
            if (outputFile.exists()) {
                outputFile.delete()
            }
            val inputFile = project.rootDir.resolve("src/main/resources/assets/oneconfig/icons/OneConfig.svg")
            inputFile.copyTo(outputFile)
        }
    }
    val dokkaJar = create("dokkaJar", Jar::class.java) {
        archiveClassifier.set("dokka")
        group = "build"
        dependsOn(dokkaHtml)
        from(layout.buildDirectory.dir("dokka"))
    }
    named("sourcesJar").get().dependsOn(dokkaJar)
}

afterEvaluate {
    val checkFile = file(".gradle/loom-cache/SETUP")
    val lwjglJarDelayed by tasks.creating {
        dependsOn(lwjglJar)
    }

    @Suppress("UNUSED_VARIABLE")
    val setupGradle by tasks.creating {
        group = "loom"
        description = "Setup OneConfig"
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
        logger.error("`setupGradle` is in the loom category of your gradle project.")
        logger.error("--------------")
    }
}

publishing {
    publications {
        register<MavenPublication>("oneconfig-$platform") {
            groupId = "cc.polyfrost"
            artifactId = base.archivesName.get()

            from(components["java"])
            artifact(tasks["remapJar"])
            artifact(tasks["dokkaJar"])
        }
    }

    repositories {
        maven {
            name = "releases"
            url = uri("https://repo.polyfrost.cc/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "snapshots"
            url = uri("https://repo.polyfrost.cc/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "private"
            url = uri("https://repo.polyfrost.cc/private")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}