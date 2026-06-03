@file:Suppress("UnstableApiUsage")
plugins {
    id("net.fabricmc.fabric-loom-remap")
    `oneconfig-fabric`
}

dependencies {4
    mappings(loom.layered {
        officialMojangMappings()
        if (versionedCatalog.has("parchment")) {
            parchment(variantOf(versionedCatalog["parchment"]) {
                artifactType("zip")
            })
        }
    })

    modRuntimeOnly(rootProject.fileTree("minecraft/run/extra_mods").include("*.jar"))
}
