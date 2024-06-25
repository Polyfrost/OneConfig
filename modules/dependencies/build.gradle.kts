description = "Shared external libraries and dependencies"

allprojects {
    with(tasks) {
        arrayOf("javadocJar", "sourcesJar").forEach {
            findByName(it)?.enabled = false
        }
    }
}

dependencies {
    api(libs.polyui)

    api(libs.bundles.kotlin)
    api(libs.bundles.kotlinx)

    api(libs.hypixel.modapi)

    api(libs.bundles.nightconfig)
}