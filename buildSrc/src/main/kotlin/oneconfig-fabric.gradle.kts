import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.kotlin.dsl.getByName

plugins {
    id("oneconfig-setup")
}

dependencies {
    "minecraft"("com.mojang:minecraft:${versionedCatalog.versions["minecraft"]}")
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
