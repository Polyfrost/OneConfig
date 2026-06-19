plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
    id("oneconfig-bootstrap")
}

ploceus {
    setIntermediaryGeneration(2)
}

dependencies {
    mappings(ploceus.featherMappings(versionedCatalog.versions["feather.build"].requiredVersion))
}
