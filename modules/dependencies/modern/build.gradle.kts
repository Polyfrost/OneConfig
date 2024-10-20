description = "Dependencies for modern platforms (1.13+)"

val natives = listOf("windows", "windows-arm64", "linux", "macos", "macos-arm64")

dependencies {
    implementation(libs.lwjgl.nvg) {
        isTransitive = false
    }
}