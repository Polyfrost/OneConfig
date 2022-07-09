@file:Suppress("GradlePackageUpdate")

plugins {
    kotlin("jvm")
    id("gg.essential.multi-version")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("gg.essential.defaults.loom")
    id("com.github.johnrengelman.shadow")
    id("maven-publish")
    id("signing")
    java
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project

version = "1.0.0-alpha6"

repositories {
    maven("https://repo.polyfrost.cc/releases")
}
val shadeCompileOnly: Configuration by configurations.creating
val shadeRuntimeOnly: Configuration by configurations.creating

sourceSets {
    main {
        runtimeClasspath += shadeRuntimeOnly
    }
}

dependencies {
    val lwjgl = if (platform.mcVersion >= 11600) {
        "3.2.1"
    } else {
        "3.3.1"
    }
    if (platform.isLegacyForge) {
        shadeCompileOnly("org.lwjgl:lwjgl:$lwjgl")
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjgl")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjgl")

        shadeRuntimeOnly("org.lwjgl:lwjgl:$lwjgl:natives-windows")
        shadeRuntimeOnly("org.lwjgl:lwjgl-stb:$lwjgl:natives-windows")
        shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjgl:natives-windows")
        shadeRuntimeOnly("org.lwjgl:lwjgl:$lwjgl:natives-linux")
        shadeRuntimeOnly("org.lwjgl:lwjgl-stb:$lwjgl:natives-linux")
        shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjgl:natives-linux")
        shadeRuntimeOnly("org.lwjgl:lwjgl:$lwjgl:natives-macos")
        shadeRuntimeOnly("org.lwjgl:lwjgl-stb:$lwjgl:natives-macos")
        shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:$lwjgl:natives-macos")
    }

    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjgl") {
        isTransitive = platform.isLegacyForge
    }
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:$lwjgl:natives-windows") {
        isTransitive = platform.isLegacyForge
    }
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:$lwjgl:natives-linux") {
        isTransitive = platform.isLegacyForge
    }
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:$lwjgl:natives-macos") {
        isTransitive = platform.isLegacyForge
    }
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        configurations = listOf(shadeCompileOnly, shadeRuntimeOnly)
        if (platform.isLegacyForge) {
            exclude("META-INF/versions/**")
            exclude("**/module-info.class")
            exclude("**/package-info.class")
            relocate("org.lwjgl", "org.lwjgl3") {
                include("org.lwjgl.PointerBuffer")
                include("org.lwjgl.BufferUtils")
            }
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)
    }
    jar {
        enabled = false
    }
    remapJar {
        enabled = false
    }
}

publishing {
    publications {
        register<MavenPublication>("lwjgl-$platform") {
            groupId = "cc.polyfrost"
            artifactId = "lwjgl-$platform"
            artifact(tasks["shadowJar"])
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