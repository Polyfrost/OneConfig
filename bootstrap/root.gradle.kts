import com.replaymod.gradle.preprocess.ProjectGraphNode
import com.replaymod.gradle.preprocess.RootPreprocessExtension
import dev.deftu.gradle.utils.ModData
import dev.deftu.gradle.utils.ProjectData

plugins {
    id(libs.plugins.dgt.multiversion.root.get().pluginId)
}

subprojects {
    val projectData = ProjectData.from(rootProject)
    ModData.populateFrom(project, projectData)

    // Force evaluation of the previous project in the preprocess chain before this project configures.
    // Prevents "Resolution was attempted without an exclusive lock" when using configure-on-demand + parallel,
    // since preprocessCode needs to resolve the linked project's compileClasspath.
    (parent?.extensions?.findByType(RootPreprocessExtension::class.java))?.rootNode?.let { rootNode ->
        val nodes = mutableListOf<ProjectGraphNode>()
        fun recurse(node: ProjectGraphNode) {
            nodes.add(node)
            for ((child) in node.links) recurse(child)
        }
        recurse(rootNode)
        val prevByProject = (1 until nodes.size).associate { nodes[it].project to nodes[it - 1].project }
        prevByProject[project.name]?.let { prevProject ->
            evaluationDependsOn(":bootstrap:$prevProject")
        }
    }
}

preprocess {
    strictExtraMappings.set(true)
    val forge10809 = createNode("bootstrap-1.8.9-forge", 1_08_09, "srg")
    val forge11202 = createNode("bootstrap-1.12.2-forge", 1_12_02, "srg")
    val forge11605 = createNode("bootstrap-1.16.5-forge", 1_16_05, "srg")
    val fabric11605 = createNode("bootstrap-1.16.5-fabric", 1_16_05, "yarn")
    val fabric12101 = createNode("bootstrap-1.21.1-fabric", 1_21_01, "yarn")
    val fabric12104 = createNode("bootstrap-1.21.4-fabric", 1_21_04, "yarn")
    val fabric12105 = createNode("bootstrap-1.21.5-fabric", 1_21_05, "yarn")
    val fabric12108 = createNode("bootstrap-1.21.8-fabric", 1_21_08, "yarn")
    val fabric12110 = createNode("bootstrap-1.21.10-fabric", 1_21_10, "yarn")
    val fabric12111 = createNode("bootstrap-1.21.11-fabric", 1_21_11, "yarn")
    val fabric261 = createNode("bootstrap-26.1-fabric", 26_01_00, "yarn")

    forge11202.link(forge10809)
    forge11605.link(forge11202)
    fabric11605.link(forge11605)
    fabric12101.link(fabric11605)
    fabric12104.link(fabric12101)
    fabric12105.link(fabric12104)
    fabric12108.link(fabric12105)
    fabric12110.link(fabric12108)
    fabric12111.link(fabric12110)
    fabric261.link(fabric12111)

    strictExtraMappings.set(true)
}
