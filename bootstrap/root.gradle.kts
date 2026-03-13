import dev.deftu.gradle.utils.ModData
import dev.deftu.gradle.utils.ProjectData

plugins {
    id(libs.plugins.dgt.multiversion.root.get().pluginId)
}

subprojects {
    val projectData = ProjectData.from(rootProject)
    ModData.populateFrom(project, projectData)
}

preprocess {
    strictExtraMappings.set(true)
    val fabric12101 = createNode("bootstrap-1.21.1-fabric", 1_21_01, "yarn")
    val fabric12104 = createNode("bootstrap-1.21.4-fabric", 1_21_04, "yarn")
    val fabric12105 = createNode("bootstrap-1.21.5-fabric", 1_21_05, "yarn")
    val fabric12108 = createNode("bootstrap-1.21.8-fabric", 1_21_08, "yarn")
    val fabric12110 = createNode("bootstrap-1.21.10-fabric", 1_21_10, "yarn")
    val fabric12111 = createNode("bootstrap-1.21.11-fabric", 1_21_11, "yarn")
    val fabric261 = createNode("bootstrap-26.1-fabric", 26_01_00, "yarn")

    fabric12104.link(fabric12101)
    fabric12105.link(fabric12104)
    fabric12108.link(fabric12105)
    fabric12110.link(fabric12108)
    fabric12111.link(fabric12110)
    fabric261.link(fabric12111)

    strictExtraMappings.set(true)
}
