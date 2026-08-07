import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.kotlin.dsl.getByName

plugins {
    id("oneconfig-setup")
}

dependencies {
    "minecraft"("com.mojang:minecraft:${versionedCatalog.versions["minecraft"]}")

    val libsCatalog = rootProject.extensions.getByType<VersionCatalogsExtension>().named("libs")
    // libs.bundles.test-core would pull in log4j-core, which collides with the log4j Loom
    // already places on the test classpath.
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

    onlyIf { !project.hasProperty("skipMixinAudit") }

    testLogging {
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

val loom = extensions.getByName<LoomGradleExtensionAPI>("loom")
loom.apply {
    runConfigs["client"].apply {
        ideConfigGenerated(true)
        runDir = "../../run"
        // -Pdevauth=false launches offline, for runs that do not need a real account.
        property("devauth.enabled", (project.findProperty("devauth") ?: "true").toString())
        property("oneconfig.test", "true")
//        if (project.hasProperty("gpuprofile")) {
//            property("oneconfig.debug.gpuprofile", "true")
//            if (project.hasProperty("gpuprofile.sections")) {
//                property("oneconfig.debug.gpuprofile.sections", "true")
//            }
//        }
        //vmArg("-Dfabric.modsFolder=" + '"' + rootProject.projectDir.resolve("run/${mcVersion}Mods").absolutePath + '"')
    }
}
