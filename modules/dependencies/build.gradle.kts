description = "Shared external libraries and dependencies"

allprojects {
    with(tasks) {
        arrayOf("javadocJar", "sourcesJar").forEach {
            findByName(it)?.enabled = false
        }
    }
}

repositories {
    mavenLocal()
    maven("https://repo.polyfrost.org/snapshots")
}

dependencies {
    api(libs.polyui)

    api(libs.bundles.kotlin)
    api(libs.bundles.kotlinx)

    api(libs.hypixel.modapi)

    api(libs.bundles.nightconfig)

    api(libs.isolated.lwjgl3.loader)
}