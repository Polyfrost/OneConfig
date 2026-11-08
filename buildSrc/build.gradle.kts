import dev.detekt.gradle.Detekt
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kotlin.sam.with.receiver)
    alias(libs.plugins.kotlin.assignment)
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
    compileOnly(gradleKotlinDsl())

    // Mirroring https://github.com/EssentialGG/essential-gradle-toolkit/blob/master/build.gradle.kts
    implementation("org.ow2.asm:asm-commons:9.11.1")
    implementation("com.google.guava:guava:33.0.0-jre")

    implementation("org.jetbrains.kotlin:kotlin-metadata-jvm:2.4.21")
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

gradlePlugin {
    mapOf(
        "oneconfig-bootstrap" to "OneConfigBootstrapPlugin",
        "oneconfig-bridge" to "OneConfigBridgePlugin",
        "oneconfig-compose-bundle-publish" to "OneConfigComposeBundlePublishPlugin",
        "oneconfig-fabric" to "OneConfigFabricPlugin",
        "oneconfig-lint" to "OneConfigLintPlugin",
        "oneconfig-neoforge" to "OneConfigNeoForgePlugin",
        "oneconfig-setup" to "OneConfigSetupPlugin",
    ).forEach { (pluginId, className) ->
        plugins.register(pluginId) {
            id = pluginId
            implementationClass = className
        }
    }
}

samWithReceiver {
    annotation("org.gradle.api.HasImplicitReceiver")
}

assignment {
    annotation("org.gradle.api.SupportsKotlinAssignmentOverloading")
}

kotlin {
    compilerOptions {
        // plugins run against the Kotlin stdlib embedded in Gradle, not the one from the Kotlin Gradle plugin
        apiVersion = KotlinVersion.fromVersion(embeddedKotlinVersion.substringBeforeLast('.'))
        allWarningsAsErrors = true
        extraWarnings = true
        progressiveMode = true
    }
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

    // mirrors OneConfigLintPlugin, which buildSrc can't apply to itself
    named<Detekt>("detekt") {
        enabled = false
        dependsOn(withType<Detekt>().matching { it.name != "detekt" && !it.name.endsWith("SourceSet") })
    }
}
