@file:Suppress("UnstableApiUsage")
plugins {
    id("net.fabricmc.fabric-loom-remap")
    `oneconfig-fabric`
}

dependencies {
    mappings(loom.officialMojangMappings())

    modRuntimeOnly(rootProject.fileTree("minecraft/run/extra_mods").include("*.jar"))
}
