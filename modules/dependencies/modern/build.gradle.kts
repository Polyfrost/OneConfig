import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

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
	create("deps", ShadowJar::class.java) {
		archiveClassifier = "modern"
		configurations = listOf(shade)
	}
	build {
		dependsOn("deps")
	}
}
