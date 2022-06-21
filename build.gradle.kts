import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import gg.essential.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import gg.essential.gradle.util.noServerRunConfigs
import gg.essential.gradle.util.prebundle
import net.fabricmc.loom.task.RemapSourcesJarTask


plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
    id("com.github.johnrengelman.shadow")
    id("net.kyori.blossom") version "1.3.0"
    id("io.github.juuxel.loom-quiltflower-mini")
    id("org.jetbrains.dokka") version "1.7.0"
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
            arg("--tweakClass", "cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker")
            property("mixin.debug.export", "true")
            property("debugBytecode", "true")
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

val relocated = registerRelocationAttribute("relocate") {
    relocate("gg.essential", "cc.polyfrost.oneconfig.libs")
    relocate("me.kbrewster", "cc.polyfrost.oneconfig.libs")
    relocate("com.github.benmanes", "cc.polyfrost.oneconfig.libs")
    relocate("com.google", "cc.polyfrost.oneconfig.libs")
    relocate("org.checkerframework", "cc.polyfrost.oneconfig.libs")
    remapStringsIn("com.github.benmanes.caffeine.cache.LocalCacheFactory")
    remapStringsIn("com.github.benmanes.caffeine.cache.NodeFactory")
}

val shadeRelocated: Configuration by configurations.creating {
    attributes { attribute(relocated, true) }
}

val shade: Configuration by configurations.creating {
    configurations.api.get().extendsFrom(this)
}

val shadeNoPom: Configuration by configurations.creating

sourceSets {
    main {
        output.setResourcesDir(java.classesDirectory)
    }
}

dependencies {
    compileOnly("gg.essential:vigilance-$platform:222") {
        isTransitive = false
    }

    shadeRelocated("gg.essential:universalcraft-$platform:211") {
        isTransitive = false
    }

    shadeRelocated("com.github.KevinPriv:keventbus:c52e0a2ea0") {
        isTransitive = false
    }

    @Suppress("GradlePackageUpdate")
    shadeRelocated("com.github.ben-manes.caffeine:caffeine:2.9.3")

    // for other mods and universalcraft
    val kotlinVersion: String by project
    val coroutinesVersion: String by project
    val serializationVersion: String by project
    val atomicfuVersion: String by project
    shade("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    shade("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    shade("org.jetbrains.kotlin:kotlin-stdlib-jdk7:$kotlinVersion")
    shade("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    shade("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$coroutinesVersion")
    shade("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:$coroutinesVersion")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:$serializationVersion")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:$serializationVersion")
    shade("org.jetbrains.kotlinx:kotlinx-serialization-cbor-jvm:$serializationVersion")
    shade("org.jetbrains.kotlinx:atomicfu-jvm:$atomicfuVersion")

    shade("org.spongepowered:mixin:0.7.11-SNAPSHOT") {
        isTransitive = false
    }
    shade("cc.polyfrost:lwjgl:1.0.0-alpha1")
    shadeNoPom(prebundle(shadeRelocated))

    dokkaHtmlPlugin("org.jetbrains.dokka:kotlin-as-java-plugin:1.6.21")

    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeNoPom) }
    configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeNoPom) }
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
                exclude("META-INF/versions/**")
                exclude("**/module-info.class")
                exclude("**/package-info.class")
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
        archiveClassifier.set("donotusethis")
        configurations = listOf(shade, shadeNoPom)
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)
    }
    remapJar {
        input.set(shadowJar.get().archiveFile)
        archiveClassifier.set("full")
    }
    fun Jar.excludeInternal() {
        exclude("**/internal/**")
    }
    jar {
        dependsOn(shadeNoPom)
        from({ shadeNoPom.map { if (it.isDirectory) it else zipTree(it) } })
        manifest {
            attributes(
                mapOf(
                    "ModSide" to "CLIENT",
                    "ForceLoadAsMod" to true,
                    "TweakOrder" to "0",
                    "MixinConfigs" to "mixins.oneconfig.json",
                    "TweakClass" to "cc.polyfrost.oneconfig.internal.plugin.asm.OneConfigTweaker"
                )
            )
        }
        excludeInternal()
        archiveClassifier.set("")
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
    named<Jar>("sourcesJar") {
        dependsOn(dokkaJar)
        excludeInternal()
        archiveClassifier.set("sources")
        doFirst {
            archiveClassifier.set("sources")
        }
    }
    withType<RemapSourcesJarTask> {
        enabled = false
    }
}

afterEvaluate {
    val checkFile = file(".gradle/loom-cache/SETUP")
    @Suppress("UNUSED_VARIABLE")
    val setupGradle by tasks.creating {
        group = "loom"
        description = "Setup OneConfig"
        dependsOn(tasks.named("genSourcesWithQuiltflower").get())
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

            artifact(tasks["jar"])
            artifact(tasks["remapJar"])
            artifact(tasks["sourcesJar"])
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