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
    // serial: nothing relies on Java serialization, despite the Serializable on some public types
    // classfile: malformed signatures in third-party jars, most notably the 1.8.9 game jar
    // processing: Mixin's processors never claim the annotations that they don't care about
    options.compilerArgs.addAll(listOf("-Xlint:all,-serial,-classfile,-processing", "-Werror"))
    options.errorprone {
        disableAllChecks = true
        error("UnnecessarilyFullyQualified")
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions {
        allWarningsAsErrors = true
        extraWarnings = true
    }
}

// the enabled Detekt rules need type resolution, which only the per-compilation tasks have
tasks.named<Detekt>("detekt") {
    enabled = false
    dependsOn(tasks.withType<Detekt>().matching { it.name != "detekt" && !it.name.endsWith("SourceSet") })
}
