// These inspections trigger because we're using the library versions that
// are bundled with Minecraft, which are not the latest versions.
@file:Suppress("GradlePackageUpdate", "VulnerableLibrariesLocal", "DSL_SCOPE_VIOLATION")

import org.polyfrost.gradle.multiversion.StripReferencesTransform.Companion.registerStripReferencesAttribute
import org.polyfrost.gradle.util.RelocationTransform.Companion.registerRelocationAttribute

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlinx.abi)
    alias(libs.plugins.pgt.defaults.repo)
    alias(libs.plugins.blossom)
    id("maven-publish")
    id("signing")
    java
}

kotlin.jvmToolchain {
    this.languageVersion.set(JavaLanguageVersion.of(8))
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
    mavenLocal()
    mavenCentral()
    maven("https://repo.polyfrost.org/releases")
}

dependencies {
    compileOnly("com.google.code.gson:gson:2.2.4")
    compileOnly("org.ow2.asm:asm-debug-all:5.0.3")

    compileOnly(libs.bundles.lwjgl)

    compileOnly(libs.polyui)
    compileOnly(libs.bundles.core)
    compileOnly(project(":modules:config"))
    compileOnly(project(":modules:commands"))
    compileOnly(project(":modules:events"))
    compileOnly(project(":modules:config-impl"))
    compileOnly(project(":modules:utils"))
    compileOnly(project(":modules:ui")) {
        isTransitive = false
    }

    compileOnly("org.polyfrost:universalcraft-1.8.9-forge:${libs.versions.universalcraft.get()}") {
        attributes {
            // prevent errors with classpath conflicts
            attribute(registerStripReferencesAttribute("strip-uc") {
                excludes.add("net.minecraft")
                excludes.add("net.minecraftforge")
            }, true)
        }
        isTransitive = false
    }

    compileOnly(libs.caffeine) {
        isTransitive = false
        attributes {
            attribute(registerRelocationAttribute("relocate-caffeine") {
                relocate("com.github.benmanes", "org.polyfrost.oneconfig.libs")
                remapStringsIn("com.github.benmanes.caffeine.cache.LocalCacheFactory")
                remapStringsIn("com.github.benmanes.caffeine.cache.NodeFactory")
            }, true)
        }
    }

    compileOnly(libs.bundles.kotlin)
    compileOnly(libs.bundles.kotlinx)

    compileOnly(libs.mixin) {
        isTransitive = false
    }
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