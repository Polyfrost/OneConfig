val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")

dependencies {
	val lwjglVersion = libs.versions.lwjgl.get()
	for (dep in listOf("-nanovg", "-tinyfd", "-stb", "")) {
		val lwjglDep = "org.lwjgl:lwjgl$dep:$lwjglVersion"
		api(lwjglDep) {
			isTransitive = false
		}
		for (native in natives) {
			implementation("$lwjglDep:natives-$native") {
				isTransitive = false
			}
		}
	}
}