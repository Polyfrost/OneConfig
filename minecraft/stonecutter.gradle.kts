plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.3-fabric"
stonecutter {
    parameters {
        constants {
            match(
                current.project.substringAfterLast("-"),
                "fabric",
                "neoforge"
            )
            val catalogue = rootProject.getForwardingVersionCatalog(current)

            this["sdl_keycodes"] = eval(current.version, ">= 26.3")

            this["moul_compat"] = current.project.endsWith("-fabric") &&
                eval(current.version, "> 1.21.10") &&
                catalogue.has("moulconfig")
            this["yacl_compat"] = catalogue.has("yacl")
            this["clothconfig_compat"] = catalogue.has("clothconfig")
            this["midnightlib_compat"] = catalogue.has("midnightlib")
            this["walksylib_compat"] = catalogue.has("walksylib")
            this["modmenu_compat"] = catalogue.has("modmenu")
            this["rconfig_compat"] = catalogue.has("rconfig")
            this["osl_config_compat"] = current.project.endsWith("-ornithe")
            this["dandelion_compat"] = catalogue.has("dandelion")
            this["odin_compat"] = current.project.endsWith("-fabric") && catalogue.has("odin")
            this["skycubed_compat"] = current.project.endsWith("-fabric") && catalogue.has("skycubed")
            this["skyblocker_compat"] = current.project.endsWith("-fabric") && catalogue.has("skyblocker")
            this["skyblocker_hud_v2"] = current.project.endsWith("-fabric") &&
                catalogue.has("skyblocker") &&
                eval(current.version, ">= 26.1")
            this["skyblocker_legacy_hud"] = current.project.endsWith("-fabric") &&
                catalogue.has("skyblocker") &&
                eval(current.version, "< 26.1")
            this["stella_compat"] = current.project.endsWith("-fabric") && catalogue.has("stella")
            this["apec_compat"] = current.project.endsWith("-fabric") && catalogue.has("apec")
            this["tr7zw_compat"] = true
            this["ukulib_compat"] = true
            this["axolotlclient_config_compat"] = true
            this["wwaypoints_compat"] = true
            this["cinnabar"] = catalogue.has("cinnabar") && rootProject.hasProperty("minecraft.vulkan")
            this["vulkanmod"] = current.project.endsWith("-fabric") && catalogue.has("vulkanmod")
        }

        replacements {

            string(eval(current.version, ">= 26.1"), "gui_graphics") {
                replace("GuiGraphics", "GuiGraphicsExtractor")
            }

            string(eval(current.version, "< 26.1")) {
                replace(
                    "import net.minecraft.client.gui.GuiGraphicsExtractor\n",
                    "import net.minecraft.client.gui.GuiGraphics as GuiGraphicsExtractor\n",
                )
            }
            // 26.2 moved the main render target from Minecraft onto GameRenderer
            string(eval(current.version, ">= 26.2"), "main_render_target") {
                replace("mainRenderTarget", "gameRenderer.mainRenderTarget()")
            }
            string(eval(current.version, "< 1.21.11")) {
                replace(
                    "import net.minecraft.resources.Identifier\n",
                    "import net.minecraft.resources.ResourceLocation as Identifier\n",
                )
            }

            string(eval(current.version, "< 1.21.2")) {
                replace("Minecraft.getInstance().schedule(", "Minecraft.getInstance().tell(")
            }

            string(eval(current.version, ">= 26.3")) {
                replace("com.mojang.blaze3d.opengl", "com.mojang.renderpearl.backend.opengl")
                replace("com.mojang.blaze3d.vulkan", "com.mojang.renderpearl.backend.vulkan")
                replace("com.mojang.blaze3d.textures", "com.mojang.renderpearl.api.textures")
                replace("com.mojang.blaze3d.buffers", "com.mojang.renderpearl.api.buffers")
                replace("com.mojang.blaze3d.GpuFormat", "com.mojang.renderpearl.api.GpuFormat")
                // mixin descriptors
                replace("com/mojang/blaze3d/systems/GpuSurface", "com/mojang/renderpearl/api/device/GpuSurface")
                replace("com/mojang/blaze3d/systems/CommandEncoder", "com/mojang/renderpearl/api/commands/CommandEncoder")
                replace("com/mojang/blaze3d/textures/GpuTextureView", "com/mojang/renderpearl/api/textures/GpuTextureView")
            }
        }
    }
}
