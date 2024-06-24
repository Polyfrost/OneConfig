val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")

dependencies {
	implementation(libs.lwjgl.nvg) {
		isTransitive = false
	}
	val nvg = libs.lwjgl.nvg.get()
	val nvgDep = "${nvg.module}:${nvg.version}:natives-"
	for (native in natives) {
		implementation("$nvgDep$native") {
			isTransitive = false
		}
	}
}