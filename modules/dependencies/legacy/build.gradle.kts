val shade by configurations.creating {
	configurations.api.get().extendsFrom(this)
}

val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")

dependencies {
	val lwjglVersion = libs.versions.lwjgl.get()
	for (dep in listOf("-nanovg", "-tinyfd", "-stb", "")) {
		val lwjglDep = "org.lwjgl:lwjgl$dep:$lwjglVersion"
		shade(lwjglDep) {
			isTransitive = false
		}
		for (native in natives) {
			shade("$lwjglDep:natives-$native") {
				isTransitive = false
			}
		}
	}
}

tasks {
	shadowJar {
		archiveClassifier = "legacy"
		configurations = listOf(shade)
	}
}
