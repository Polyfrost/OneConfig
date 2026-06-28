plugins {
    id("net.fabricmc.fabric-loom")
    `oneconfig-bridge` // creates modImplementation, .. configuration
    `oneconfig-fabric`
}

dependencies {
    if (versionedCatalog.has("modmenu"))
    implementation(versionedCatalog["modmenu"])
}