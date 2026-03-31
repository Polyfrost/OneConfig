@file:Suppress("UnstableApiUsage")

import dev.deftu.gradle.utils.GameSide
import dev.deftu.gradle.utils.includeOrShade
import dev.deftu.gradle.utils.version.MinecraftDropVersion
import dev.deftu.gradle.utils.version.MinecraftReleaseVersion
import org.polyfrost.gradle.provideFabricApiDependency
import org.polyfrost.gradle.provideIncludedDependencies

plugins {
    java
    alias(libs.plugins.kotlin)
    alias(libs.plugins.google.ksp)
    id(libs.plugins.dgt.multiversion.platform.get().pluginId)
    id(libs.plugins.dgt.base.get().pluginId)
    id(libs.plugins.dgt.resources.get().pluginId)
    id(libs.plugins.dgt.bloom.get().pluginId)
    id(libs.plugins.dgt.shadow.get().pluginId)
    id(libs.plugins.dgt.loom.get().pluginId)
    signing
}

toolkitLoomHelper {
    disableRunConfigs(GameSide.BOTH)
    useTweaker("org.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker", GameSide.CLIENT)
}

repositories {
    maven("https://repo.polyfrost.org/releases")
    maven("https://repo.polyfrost.org/snapshots")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://maven.deftu.dev/releases")
    maven("https://jitpack.io") {
        content { includeGroupAndSubgroups("com.github") }
    }
    maven("https://maven.bawnorton.com/releases") {
        content { includeGroup("com.github.bawnorton.mixinsquared") }
    }
    maven("https://redirector.kotlinlang.org/maven/compose-dev")
    google()
}

dependencies {
    if (mcData.version.preprocessorKey >= 11300 || mcData.isFabric) {
        val mcVersion = mcData.version
        val tripleVersion = when (mcVersion) {
            is MinecraftDropVersion -> Triple(mcVersion.year, mcVersion.drop, mcVersion.patch)
            is MinecraftReleaseVersion -> Triple(mcVersion.major, mcVersion.minor, mcVersion.patch)
            else -> error("no")
        }

        provideIncludedDependencies(
            tripleVersion,
            mcData.loader.friendlyString,
            includeCompose = false
        ).forEach {
            includeOrShade(compileOnly(it.dep)!!)
        }

        includeOrShade(compileOnly(project(":modules:compose-bundle"))!!)

        for (project in rootProject.project(":modules").subprojects) {
            if ("dependencies" !in project.path && "relocator" !in project.path && "compose-bundle" !in project.path) {
                includeOrShade(project(project.path))
            }
        }
        includeOrShade(compileOnly(project(":minecraft:$mcData")) { isTransitive = false })
        if (mcData.isFabric) {
            provideFabricApiDependency(tripleVersion).forEach {
                @Suppress("USELESS_CAST")
                includeOrShade(maybeModCompileOnly(if (it.dep is String) it.dep as String else "${(it.dep as ExternalModuleDependency).group}:${(it.dep as ExternalModuleDependency).name}:${(it.dep as ExternalModuleDependency).version}") {
                    isTransitive = false
                })
            }
        }
    } else {
        val loaderModule = "all" // Shadowed dependencies
        val loaderDependency = "org.polyfrost.oneconfig:stage0"
        val fullLoaderDependency = "$loaderDependency:${libs.versions.oneconfig.loader.get()}:$loaderModule"
        includeOrShade(fullLoaderDependency)
    }
}

tasks {
    named<org.gradle.jvm.tasks.Jar>("remapJar") {
        archiveBaseName.set("OneConfigBootstrap")
    }
}

afterEvaluate {
    tasks {
        matching { it.group == "publishing" }.forEach {
            it.enabled = false
        }
    }
    publishing {
        publications {
            named<MavenPublication>("mavenJava") {
                artifactId = project.name
                groupId = project.group.toString()

                signing {
                    isRequired = project.properties["signing.keyId"] != null
                    sign(this@named)
                }
            }
        }
    }
}