plugins {
    kotlin("jvm")
    id("gg.essential.defaults.repo")
    id("gg.essential.defaults.java")
    id("com.github.johnrengelman.shadow")
    id("maven-publish")
    id("signing")
    java
}

val mod_name: String by project
val mod_version: String by project
val mod_id: String by project

version = "1.0.0-alpha1"

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
    shadeCompileOnly("org.lwjgl:lwjgl:3.3.1")
    shadeCompileOnly("org.lwjgl:lwjgl-stb:3.3.1")
    shadeCompileOnly("org.lwjgl:lwjgl-tinyfd:3.3.1")
    shadeCompileOnly("org.lwjgl:lwjgl-nanovg:3.3.1")

    shadeRuntimeOnly("org.lwjgl:lwjgl:3.3.1:natives-windows")
    shadeRuntimeOnly("org.lwjgl:lwjgl-stb:3.3.1:natives-windows")
    shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-windows")
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:3.3.1:natives-windows")
    shadeRuntimeOnly("org.lwjgl:lwjgl:3.3.1:natives-linux")
    shadeRuntimeOnly("org.lwjgl:lwjgl-stb:3.3.1:natives-linux")
    shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-linux")
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:3.3.1:natives-linux")
    shadeRuntimeOnly("org.lwjgl:lwjgl:3.3.1:natives-macos")
    shadeRuntimeOnly("org.lwjgl:lwjgl-stb:3.3.1:natives-macos")
    shadeRuntimeOnly("org.lwjgl:lwjgl-tinyfd:3.3.1:natives-macos")
    shadeRuntimeOnly("org.lwjgl:lwjgl-nanovg:3.3.1:natives-macos")
}

tasks {
    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveClassifier.set("")
        configurations = listOf(shadeCompileOnly, shadeRuntimeOnly)
        exclude("META-INF/versions/**")
        exclude("**/module-info.class")
        exclude("**/package-info.class")
        relocate("org.lwjgl", "org.lwjgl3") {
            include("org.lwjgl.PointerBuffer")
            include("org.lwjgl.BufferUtils")
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        dependsOn(jar)
    }
    jar {
        enabled = false
    }
}

publishing {
    publications {
        register<MavenPublication>("lwjgl") {
            groupId = "cc.polyfrost"
            artifactId = "lwjgl"
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