plugins {
    kotlin("jvm") version "2.2.10"
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
    maven("https://repo.polyfrost.org/releases")
    maven("https://maven.modmuss50.me/releases")
}
fun plugin(provider: Provider<PluginDependency>): Provider<String> = provider.map {
    "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}"
}

fun plugin(supplier: ProviderConvertible<PluginDependency>): Provider<String> = plugin(supplier.asProvider())

dependencies {
    // Mirroring https://github.com/EssentialGG/essential-gradle-toolkit/blob/master/build.gradle.kts
    implementation("org.ow2.asm:asm-commons:9.8")
    implementation("com.google.guava:guava:33.0.0-jre")

    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.2.10")
    implementation(plugin(libs.plugins.kotlin))
    implementation(plugin(libs.plugins.kotlin.compose))
    implementation(plugin(libs.plugins.google.ksp))

    implementation(plugin(fabric.plugins.loom))
    implementation(plugin(fabric.plugins.loom.remap))

    implementation(plugin(neoforge.plugins.moddev))

    implementation("dev.kikugie.stonecutter:dev.kikugie.stonecutter.gradle.plugin:0.9")
    implementation("me.modmuss50.mod-publish-plugin:me.modmuss50.mod-publish-plugin.gradle.plugin:1.1.0")
}
