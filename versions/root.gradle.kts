/**
 * WHATEVER HAPPENS HERE MUST ALSO BE APPLIED IN `lwjgl/root.gradle.kts`
 */
plugins {
    kotlin("jvm") version "1.6.21" apply false
    id("gg.essential.multi-version.root")
    id("io.github.juuxel.loom-quiltflower-mini") version "171a6e2e49" apply false
}

preprocess {
    "1.8.9-forge"(10809, "srg") {
        "1.12.2-forge"(11202, "srg", file("1.8.9-1.12.2.txt")) {
            "1.16.2-forge"(11602, "srg", file("1.12.2-1.16.2.txt")) {
                "1.16.2-fabric"(11602, "yarn")
            }
        }
    }
}