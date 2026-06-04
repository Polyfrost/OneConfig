// Build script for bootstrap nodes < 26.1 (obfuscated, needs remapping).
plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("oneconfig-bootstrap")
}

dependencies {
    mappings(loom.layered {
        officialMojangMappings()
        if (versionedCatalog.has("parchment")) {
            parchment(variantOf(versionedCatalog["parchment"]) {
                artifactType("zip")
            })
        }
    })
}
