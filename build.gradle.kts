// These inspections trigger because we're using the library versions that
// are bundled with Minecraft, which are not the latest versions.
@file:Suppress("GradlePackageUpdate", "VulnerableLibrariesLocal", "DSL_SCOPE_VIOLATION")

import cc.polyfrost.gradle.multiversion.StripReferencesTransform.Companion.registerStripReferencesAttribute
import cc.polyfrost.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import cc.polyfrost.gradle.util.prebundle

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinAbi)
    alias(pgtLibs.plugins.pgtDefaultJava)
    alias(pgtLibs.plugins.pgtDefaultRepo)
    alias(libs.plugins.blossom)
    id("maven-publish")
    id("signing")
    java
}

kotlin.jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(8))
}

java {
    withSourcesJar()
}

val modName = project.properties["mod_name"]
val modMajor = project.properties["mod_major_version"]
val modMinor = project.properties["mod_minor_version"]
val modId = project.properties["mod_id"] as String

version = "$modMajor$modMinor"
group = "cc.polyfrost"

blossom {
    replaceToken("@VER@", project.version)
    replaceToken("@NAME@", modName)
    replaceToken("@ID@", modId)
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

val common = registerStripReferencesAttribute("common") {
    excludes.add("net.minecraft")
    excludes.add("net.minecraftforge")
}

dependencies {
    compileOnly("com.google.code.gson:gson:2.2.4")
    compileOnly("commons-io:commons-io:2.4")
    compileOnly("com.google.guava:guava:17.0")
    compileOnly("org.lwjgl:lwjgl-opengl:3.3.2")
    compileOnly("org.apache.logging.log4j:log4j-core:2.0-beta9")
    compileOnly("org.apache.logging.log4j:log4j-api:2.0-beta9")
    compileOnly("org.ow2.asm:asm-debug-all:5.0.3")
    compileOnly("org.apache.commons:commons-lang3:3.3.2")
    compileOnly("org.jetbrains:annotations:24.0.1")

    compileOnly(libs.vigilance) {
        attributes { attribute(common, true) }
        isTransitive = false
    }

    compileOnly("cc.polyfrost:universalcraft-1.8.9-forge:${libs.versions.universalcraft.get()}") {
        attributes { attribute(common, true) }
        isTransitive = false
    }

    shadeRelocated(libs.keventbus) {
        isTransitive = false
    }

    shadeRelocated(libs.deencapsulation) {
        isTransitive = false
    }

    @Suppress("GradlePackageUpdate")
    shadeRelocated(libs.caffeine)

    // for other mods and universalcraft
    shade(libs.bundles.kotlin)

    shade(libs.mixin) {
        isTransitive = false
    }
    compileOnly("cc.polyfrost:lwjgl-legacy:${libs.versions.lwjgl.get()}") {
        isTransitive = false
    }
    shadeNoPom(prebundle(shadeRelocated))

    configurations.named(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeNoPom) }
    configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME) { extendsFrom(shadeNoPom) }
}

tasks {
    withType(Jar::class.java) {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        if (!name.contains("sourcesjar", ignoreCase = true) || !name.contains("dokka", ignoreCase = true)) {
            exclude("**/**_Test.**")
            exclude("**/**_Test$**.**")
        }
    }
    named<Jar>("sourcesJar") {
        exclude("**/internal/**")
        archiveClassifier.set("sources")
        doFirst {
            archiveClassifier.set("sources")
        }
    }
}

apiValidation {
    ignoredPackages.add("org.lwjgl")
    ignoredPackages.add("cc.polyfrost.oneconfig.libs")
    ignoredPackages.add("cc.polyfrost.oneconfig.internal")
    ignoredPackages.add("cc.polyfrost.oneconfig.test")
}