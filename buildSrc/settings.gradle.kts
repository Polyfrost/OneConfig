dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
        create("fabric") {
            from(files("../gradle/fabric.versions.toml"))
        }
        create("ornithe") {
            from(files("../gradle/ornithe.versions.toml"))
        }
    }
}

rootProject.name = "buildSrc"
