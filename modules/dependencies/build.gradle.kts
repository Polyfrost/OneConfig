allprojects {
	group = "${rootProject.group}.${rootProject.name}.dependencies"

	with(tasks) {
		arrayOf("jar", "javadocJar", "sourcesJar").forEach {
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