plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.1-fabric"
stonecutter {
    parameters {
        constants {
            match(
                current.project.substringAfterLast("-"),
                "fabric",
                "neoforge"
            )
            val catalogue = rootProject.getForwardingVersionCatalog(current)

            this["moul_compat"] = catalogue.has("moulconfig")
            this["yacl_compat"] = catalogue.has("yacl")
            this["modmenu_compat"] = catalogue.has("modmenu")
            this["rconfig_compat"] = catalogue.has("rconfig")
            this["cinnabar"] = catalogue.has("cinnabar") && rootProject.hasProperty("minecraft.vulkan")
        }

        replacements {

            string(eval(current.version, ">= 26.1"), "gui_graphics") {
                replace("GuiGraphics", "GuiGraphicsExtractor")
            }

            regex(eval(current.version, "< 26.1")) {
                replace(
                    "import net.minecraft.client.gui.GuiGraphicsExtractor(?!;)",
                    "import net.minecraft.client.gui.GuiGraphics as GuiGraphicsExtractor",
                    "import net.minecraft.client.gui.GuiGraphics as GuiGraphicsExtractor",
                    "import net.minecraft.client.gui.GuiGraphicsExtractor",
                )
            }
            string(eval(current.version, ">= 1.21.11"), "identifier") {
                replace("ResourceLocation", "Identifier")
            }
            regex(eval(current.version, "< 1.21.11")) {
                replace(
                    "import net.minecraft.resources.Identifier(?!;)",
                    "import net.minecraft.resources.ResourceLocation as Identifier",
                    "import net.minecraft.resources.ResourceLocation as Identifier",
                    "import net.minecraft.resources.Identifier",
                )
            }
        }
    }
}