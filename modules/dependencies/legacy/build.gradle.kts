import org.polyfrost.gradle.provideIncludedDependencies

plugins {
    id("dev.deftu.gradle.loom")
}

repositories {
    maven("https://maven.bawnorton.com/releases") {
        content { includeGroup("com.github.bawnorton.mixinsquared") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.16.5")
    mappings("net.fabricmc:yarn:1.16.5+build.10:v2")

    provideIncludedDependencies(null, "forge").forEach {
        val dep = if (it.mod) modCompileOnly(it.dep) else compileOnly(it.dep)
        if (dep != null) {
            include(dep)
        }
    }
}

