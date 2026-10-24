import dev.detekt.gradle.Detekt

plugins {
    kotlin("jvm") version "2.3.20" // keep compatible with kotlin-dsl
    `kotlin-dsl`
    alias(libs.plugins.detekt)
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://maven.ornithemc.net/releases")
    maven("https://repo.polyfrost.org/releases")
    maven("https://maven.modmuss50.me/releases")
}
fun plugin(provider: Provider<PluginDependency>): Provider<String> = provider.map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

fun plugin(supplier: ProviderConvertible<PluginDependency>): Provider<String> = plugin(supplier.asProvider())

dependencies {
    // Mirroring https://github.com/EssentialGG/essential-gradle-toolkit/blob/master/build.gradle.kts
    implementation("org.ow2.asm:asm-commons:9.11.1")
    implementation("com.google.guava:guava:33.0.0-jre")

    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.2.10")
    implementation(plugin(libs.plugins.kotlin))
    implementation(plugin(libs.plugins.kotlin.compose))
    implementation(plugin(libs.plugins.google.ksp))
    implementation(plugin(libs.plugins.detekt))
    implementation(plugin(libs.plugins.errorprone))

    implementation(plugin(fabric.plugins.loom))
    implementation(plugin(fabric.plugins.loom.remap))

    implementation(plugin(ornithe.plugins.ploceus))

    implementation(plugin(neoforge.plugins.moddev))

    implementation("dev.kikugie.stonecutter:dev.kikugie.stonecutter.gradle.plugin:0.9.8")
    implementation("me.modmuss50.mod-publish-plugin:me.modmuss50.mod-publish-plugin.gradle.plugin:1.1.0")
}

detekt {
    config.setFrom("../config/detekt/detekt.yml")
}

tasks {
    // buildSrc's classpath covers the plugins used by the rest of the build's scripts
    register<Detekt>("detektScripts") {
        description = "Run detekt analysis for the Gradle scripts outside of buildSrc with type resolution"
        source(fileTree("..") {
            include("*.gradle.kts", "*/*.gradle.kts", "modules/**/*.gradle.kts")
            exclude("**/build/**")
        })
        classpath.from(sourceSets.main.get().compileClasspath, sourceSets.main.get().output)
    }

    // skips the kotlin-dsl generated sources
    withType<Detekt>().configureEach {
        val buildDir = layout.buildDirectory.get().asFile
        exclude { it.file.startsWith(buildDir) }
    }

    // mirrors oneconfig-lint.gradle.kts, which buildSrc can't apply to itself
    named<Detekt>("detekt") {
        enabled = false
        dependsOn(withType<Detekt>().matching { it.name != "detekt" && !it.name.endsWith("SourceSet") })
    }
}
