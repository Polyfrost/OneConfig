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

version = "1.0.0-alpha21"

repositories {
    maven("https://repo.polyfrost.cc/releases")
}

val shadeCompileOnly: Configuration by configurations.creating
val shadeSeparate: Configuration by configurations.creating

dependencies {
    val lwjglVersion = if (platform.mcVersion >= 11600) {
        "3.2.1"
    } else {
        "3.3.1"
    }
    if (platform.isLegacyForge || platform.isLegacyFabric) {
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion")

        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-windows")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-windows")
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-linux")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-linux")
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-macos")
        shadeCompileOnly("org.lwjgl:lwjgl-stb:$lwjglVersion:natives-macos-arm64")
        shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:$lwjglVersion:natives-macos-arm64")
    }

    shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion")
    shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-windows")
    shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-linux")
    shadeCompileOnly("org.lwjgl:lwjgl:$lwjglVersion:natives-macos")
    shadeSeparate("org.lwjgl:lwjgl:3.3.1:natives-macos-arm64")

    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion") {
        isTransitive = false
    }
    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-windows") {
        isTransitive = false
    }
    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-linux") {
        isTransitive = false
    }
    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:$lwjglVersion:natives-macos") {
        isTransitive = false
    }
    // force 3.3.1 for this, because
    // if the user is actually running M1+, LWJGL must be 3.3.0+
    shadeSeparate("org.lwjgl:lwjgl-nanovg:3.3.1:natives-macos-arm64") {
        isTransitive = false
    }
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        configurations = listOf(shadeCompileOnly, shadeSeparate)
        if (platform.isLegacyForge || platform.isLegacyFabric) {
            exclude("META-INF/versions/**")
            exclude("**/module-info.class")
            exclude("**/package-info.class")
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