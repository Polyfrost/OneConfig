import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val shade by configurations.creating {
	configurations.api.get().extendsFrom(this)
}

allprojects {
	base.archivesName = "deps"
	tasks.jar.get().enabled = false
	tasks.javadocJar.get().enabled = false
	tasks.sourcesJar.get().enabled = false
}

dependencies {
	shade(libs.polyui)

	shade(libs.bundles.kotlin)
	shade(libs.bundles.kotlinx)

	shade(libs.hypixel.modapi)

	shade(libs.bundles.nightconfig)
}

tasks {
	create("deps", ShadowJar::class.java) {
		configurations = listOf(shade)
	}
	build {
		dependsOn("deps")
	}
}
