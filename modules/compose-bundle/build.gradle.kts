repositories {
    maven("https://redirector.kotlinlang.org/maven/compose-dev")
}

val shade: Configuration by configurations.creating {
    exclude(group = "org.jetbrains.kotlin")
    exclude(group = "org.jetbrains.kotlinx")
    exclude(group = "org.jetbrains", module = "annotations")
}

dependencies {
    shade(libs.jetbrains.compose.foundation)
    shade(libs.jetbrains.compose.material)
    shade(libs.jetbrains.compose.runtime)
    shade(libs.jetbrains.compose.ui)
    shade(libs.jetbrains.compose.ui.tooling.preview)
    shade(libs.jetbrains.compose.ui.util)
    shade(libs.jetbrains.skiko.awt)
    shade(libs.jetbrains.skiko.awt.runtime.windows.x64)
    shade(libs.jetbrains.skiko.awt.runtime.linux.x64)
    shade(libs.jetbrains.skiko.awt.runtime.macos.x64)
    shade(libs.jetbrains.skiko.awt.runtime.macos.arm64)
    shade(libs.jetbrains.compose.navigation)
    shade(libs.jetbrains.lifecycle)
    shade(libs.jetbrains.viewmodel)
}

tasks.jar {
    from(shade.map { if (it.isDirectory) it else zipTree(it) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

