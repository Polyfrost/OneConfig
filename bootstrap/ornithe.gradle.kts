plugins {
    id("net.fabricmc.fabric-loom-remap")
    id("ploceus")
    id("oneconfig-bootstrap")
}

repositories {
    // Mojang's library repository only has STB natives for the platforms vanilla ships
    exclusiveContent {
        forRepository { mavenCentral() }
        filter { includeModule("org.lwjgl", "lwjgl-stb") }
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

dependencies {
    mappings(ploceus.featherMappings(versionedCatalog.versions["feather.build"].requiredVersion))
}
