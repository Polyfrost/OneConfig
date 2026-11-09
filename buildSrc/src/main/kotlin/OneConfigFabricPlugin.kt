import dev.kikugie.stonecutter.build.StonecutterBuildExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class OneConfigFabricPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(OneConfigSetupPlugin::class.java)
        pluginManager.apply(OneConfigDistributionPlugin::class.java)

        dependencies {
            "minecraft"("com.mojang:minecraft:${versionedCatalog.versions["minecraft"]}")

            val libsCatalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
            // libs.bundles.test-core would pull in log4j-core which collides with the log4j
            // loom already puts on the test classpath
            "testImplementation"(platform(libsCatalog.findLibrary("junit-bom").get()))
            "testImplementation"(libsCatalog.findLibrary("junit").get())
            "testRuntimeOnly"("org.junit.platform:junit-platform-launcher")
            "testRuntimeOnly"(versionedCatalog["fabric-loader-junit"])
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()

            workingDir = layout.buildDirectory.dir("test-run").get().asFile
            doFirst { workingDir.mkdirs() }

            maxHeapSize = "2G"
            systemProperty("java.awt.headless", "true")
            systemProperty("mixin.debug.countInjections", "true")
            systemProperty("org.apache.logging.log4j.level", "INFO")

            val skipMixinAudit = project.hasProperty("skipMixinAudit")
            onlyIf { !skipMixinAudit }

            testLogging {
                showStackTraces = true
                exceptionFormat = TestExceptionFormat.FULL
                events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
            }
        }

        val sc = extensions.getByName<StonecutterBuildExtension>("stonecutter")
        val loom = extensions.getByName<LoomGradleExtensionAPI>("loom")
        loom.apply {
            accessWidenerPath = sc.process(
                rootProject.file("minecraft/src/main/resources/oneconfigv1.classtweaker"),
                "build/processed.classtweaker",
            )
            runConfigs["client"].apply {
                generateRunConfig = true
                runDirectory = layout.projectDirectory.dir("../../run")
                systemProperties.put("oneconfig.test", "true")
            }
        }
    }
}
