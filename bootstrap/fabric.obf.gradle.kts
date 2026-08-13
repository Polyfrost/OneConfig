// bootstrap nodes < 26.1 are obfuscated so they need remapping
plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("oneconfig-bootstrap")
}

dependencies {
    mappings(loom.officialMojangMappings())
}
