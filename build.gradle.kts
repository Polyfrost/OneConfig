// These inspections trigger because we're using the library versions that
// are bundled with Minecraft, which are not the latest versions.
@file:Suppress("GradlePackageUpdate", "VulnerableLibrariesLocal", "DSL_SCOPE_VIOLATION")

import org.polyfrost.gradle.multiversion.StripReferencesTransform.Companion.registerStripReferencesAttribute
import org.polyfrost.gradle.util.RelocationTransform.Companion.registerRelocationAttribute
import org.polyfrost.gradle.util.prebundle

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
group = "org.polyfrost"

blossom {
    replaceToken("@VER@", project.version)
    replaceToken("@NAME@", modName)
    replaceToken("@ID@", modId)
}

repositories {
    maven("https://repo.polyfrost.org/releases")
}

val relocated = registerRelocationAttribute("relocate") {
    relocate("com.github.benmanes", "org.polyfrost.oneconfig.libs")
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
    compileOnly("org.ow2.asm:asm-debug-all:5.0.3")

    compileOnly(libs.bundles.lwjgl)

    shade(libs.polyui)
    shade(libs.bundles.core)
    compileOnly(project(":config"))
    compileOnly(project(":commands"))
    compileOnly(project(":events"))
    compileOnly(project(":config-impl"))
    compileOnly(project(":ui")) {
        isTransitive = false
    }

    compileOnly("org.polyfrost:universalcraft-1.8.9-forge:${libs.versions.universalcraft.get()}") {
        attributes { attribute(common, true) }
        isTransitive = false
    }

    @Suppress("GradlePackageUpdate")
    shadeRelocated(libs.caffeine)

    // for other mods and universalcraft
    shade(libs.bundles.kotlin)

    shade(libs.mixin) {
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
    ignoredPackages.add("org.polyfrost.oneconfig.libs")
    ignoredPackages.add("org.polyfrost.oneconfig.internal")
    ignoredPackages.add("org.polyfrost.oneconfig.test")
}