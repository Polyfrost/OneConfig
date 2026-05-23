dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("fabric") {
            from(files("../gradle/fabric.versions.toml"))
        }
        create("neoforge") {
            from(files("../gradle/neoforge.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
