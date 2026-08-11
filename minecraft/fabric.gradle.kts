plugins {
    id("net.fabricmc.fabric-loom")
    `oneconfig-bridge` // creates the modImplementation and friends configurations
    `oneconfig-fabric`
}

dependencies {
    if (versionedCatalog.has("modmenu"))
    implementation(versionedCatalog["modmenu"])
}