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
                                                "1.21.1-fabric"(1_21_01, "srg") {
                                                    "1.20.4-fabric"(1_20_04, "srg") {
                                                        "1.20.4-neoforge"(1_20_04, "srg") {
                                                            "1.20.4-forge"(1_20_04, "srg") {
                                                                "1.20.1-forge"(1_20_01, "srg", file("mappings/1.20.4-forge+1.20.1-forge.txt")) {
                                                                    "1.20.1-fabric"(1_20_01, "srg") {
                                                                        "1.16.5-fabric"(1_16_05, "srg", file("mappings/1.20.1-fabric+1.16.5-fabric.txt")) {
                                                                            "1.16.5-forge"(1_16_05, "srg") {
                                                                                "1.12.2-forge"(1_12_02, "srg", file("mappings/1.16.5-forge+1.12.2-forge.txt")) {
                                                                                    "1.12.2-fabric"(1_12_02, "srg") {
                                                                                        "1.8.9-fabric"(1_08_09, "srg") {
                                                                                            "1.8.9-forge"(1_08_09, "srg")
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
