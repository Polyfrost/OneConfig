import dev.detekt.gradle.Detekt
import net.ltgt.gradle.errorprone.errorprone
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("dev.detekt")
    id("net.ltgt.errorprone")
}

dependencies {
    "errorprone"(rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("errorprone-core").get())
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disableAllChecks = true
        error("UnnecessarilyFullyQualified")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    finalizedBy("detekt")
}

// the enabled Detekt rules need type resolution, which only the per-compilation tasks have
tasks.named<Detekt>("detekt") {
    enabled = false
    dependsOn(tasks.withType<Detekt>().matching { it.name != "detekt" && !it.name.endsWith("SourceSet") })
}
