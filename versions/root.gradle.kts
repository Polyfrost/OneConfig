/**
 * WHATEVER HAPPENS HERE MUST ALSO BE APPLIED IN `lwjgl/root.gradle.kts`
 */
plugins {
    kotlin("jvm") version "1.6.21" apply false
    id("gg.essential.multi-version.root")
    id("io.github.juuxel.loom-quiltflower-mini") version "171a6e2e49" apply false
}

preprocess {
    val forge10809 = createNode("1.8.9-forge", 10809, "srg")
    val fabric10809 = createNode("1.8.9-fabric", 10809, "yarn")
    val forge11202 = createNode("1.12.2-forge", 11202, "srg")
    val fabric11202 = createNode("1.12.2-fabric", 11202, "yarn")
    val forge11602 = createNode("1.16.2-forge", 11602, "srg")
    val fabric11602 = createNode("1.16.2-fabric", 11602, "yarn")

    fabric11602.link(forge11602)
    forge11602.link(forge11202, file("1.12.2-1.16.2.txt"))
    fabric11202.link(forge11202)
    forge11202.link(forge10809)
    fabric10809.link(forge10809, file("forge-fabric-legacy.txt"))
}