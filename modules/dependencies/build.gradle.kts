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
			it.get().outgoing.artifacts.removeIf { artifact ->
				artifact.buildDependencies.getDependencies(null).contains(tasks["jar"])
			}
			it.get().outgoing.artifact(tasks["shadowJar"])
		}
	}

	tasks {
		val jar by getting { enabled = false }
		val javadocJar by getting { enabled = false }
		val sourcesJar by getting { enabled = false }
	}
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