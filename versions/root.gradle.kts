/**
 * WHATEVER HAPPENS HERE MUST ALSO BE APPLIED IN `lwjgl/root.gradle.kts`
 */
plugins {
    kotlin("jvm") version "1.6.21" apply false
    id("cc.polyfrost.multi-version.root")
    id("io.github.juuxel.loom-quiltflower-mini") version "171a6e2e49" apply false
}

preprocess {
    val forge10809 = createNode("1.8.9-forge", 10809, "srg")
    val fabric10809 = createNode("1.8.9-fabric", 10809, "yarn")
    val forge11202 = createNode("1.12.2-forge", 11202, "srg")
    val fabric11202 = createNode("1.12.2-fabric", 11202, "yarn")
    val forge11605 = createNode("1.16.5-forge", 11605, "srg")
    val fabric11605 = createNode("1.16.5-fabric", 11605, "yarn")

    fabric11605.link(forge11605)
    forge11605.link(forge11202, file("1.16.5-1.12.2.txt"))
    fabric11202.link(fabric10809)
    forge11202.link(forge10809, file("1.12.2-1.8.9.txt"))
    fabric10809.link(forge10809, file("fabric-forge-legacy.txt"))
}