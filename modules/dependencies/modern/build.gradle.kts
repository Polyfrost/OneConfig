val shade by configurations.creating {
	configurations.api.get().extendsFrom(this)
}

val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")

dependencies {
	shade(libs.lwjgl.nvg) {
		isTransitive = false
	}
	val nvg = libs.lwjgl.nvg.get()
	val nvgDep = "${nvg.module}:${nvg.version}:natives-"
	for (native in natives) {
		shade("$nvgDep$native") {
			isTransitive = false
		}
	}
}

tasks {
	shadowJar {
		archiveClassifier = "modern"
		configurations = listOf(shade)
	}
}
