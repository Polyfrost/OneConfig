import org.jetbrains.kotlin.gradle.plugin.KotlinApiPlugin

plugins {
    kotlin("jvm")
}

apply<KotlinApiPlugin>()

repositories {
    maven(url = "https://maven.teamresourceful.com/repository/maven-public/")
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("com.google.devtools.ksp:symbol-processing-api:2.0.20-1.0.25")

    implementation("me.owdding.kotlinpoet:kotlinpoet-jvm:1.0.1")
    implementation("me.owdding.kotlinpoet:ksp:1.0.1")
}

tasks.test {
    useJUnitPlatform()
}