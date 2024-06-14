plugins {
	id("com.github.johnrengelman.shadow")
}

val shade by configurations.creating {
	configurations.api.get().extendsFrom(this)
}

allprojects {
	apply(plugin = "com.github.johnrengelman.shadow")

	base.archivesName = "deps"
	configurations {
		listOf(apiElements, runtimeElements).forEach {
			it.get().outgoing.artifacts.removeIf { it.buildDependencies.getDependencies(null).contains(tasks["jar"]) }
			it.get().outgoing.artifact(tasks["shadowJar"])
		}
	}

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
	shadowJar {
		archiveClassifier.set("")
		configurations = listOf(shade)
	}
}