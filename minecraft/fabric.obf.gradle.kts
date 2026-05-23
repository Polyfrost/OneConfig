plugins {
    id("net.fabricmc.fabric-loom-remap")
    `oneconfig-fabric`
}

dependencies {
    mappings(loom.officialMojangMappings())
}
