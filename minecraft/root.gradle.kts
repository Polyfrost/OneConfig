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
    // FOR ALL NEW VERSIONS ENSURE TO UPDATE settings.gradle.kts !

    "26.1-fabric"(26_01_00, "srg") {
        "1.21.11-fabric"(1_21_11, "srg") {
            "1.21.11-neoforge"(1_21_11, "srg") {
                "1.21.10-neoforge"(1_21_10, "srg") {
                    "1.21.10-fabric"(1_21_10, "srg") {
                        "1.21.8-fabric"(1_21_08, "srg") {
                            "1.21.8-neoforge"(1_21_08, "srg") {
                                "1.21.5-neoforge"(1_21_05, "srg") {
                                    "1.21.5-fabric"(1_21_05, "srg") {
                                        "1.21.4-fabric"(1_21_04, "srg") {
                                            "1.21.4-neoforge"(1_21_04, "srg") {
                                                "1.21.1-neoforge"(1_21_01, "srg", file("mappings/1.21.4-forge+1.21.1-forge.txt")) {
                                                    "1.21.1-fabric"(1_21_01, "srg")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
