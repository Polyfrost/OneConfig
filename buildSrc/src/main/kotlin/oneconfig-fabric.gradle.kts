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
        property("devauth.enabled", "true")
        //vmArg("-Dfabric.modsFolder=" + '"' + rootProject.projectDir.resolve("run/${mcVersion}Mods").absolutePath + '"')
    }
}
