import dev.detekt.gradle.Detekt
import net.ltgt.gradle.errorprone.errorprone
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

class OneConfigLintPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply("dev.detekt")
        pluginManager.apply("net.ltgt.errorprone")

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
                progressiveMode = true
            }
        }

        // the enabled Detekt rules need type resolution, which only the per-compilation tasks have
        tasks.named<Detekt>("detekt") {
            enabled = false
            dependsOn(tasks.withType<Detekt>().matching { it.name != "detekt" && !it.name.endsWith("SourceSet") })
        }
    }
}
