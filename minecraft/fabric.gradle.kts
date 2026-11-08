plugins {
    id("net.fabricmc.fabric-loom")
    id("oneconfig-bridge") // creates the modImplementation and friends configurations
    id("oneconfig-fabric")
}

dependencies {
    if (versionedCatalog.has("modmenu"))
    implementation(versionedCatalog["modmenu"])
}