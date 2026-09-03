plugins {
    id("dev.kikugie.stonecutter")
}

stonecutter active "26.2-fabric"
stonecutter {
    parameters {
        constants {
            match(
                current.project.substringAfterLast("-"),
                "fabric",
                "neoforge",
                "ornithe"
            )
            val catalogue = rootProject.getForwardingVersionCatalog(current)

            this["moul_compat"] = current.project.endsWith("-fabric") &&
                eval(current.version, "> 1.21.10") &&
                catalogue.has("moulconfig")
            this["yacl_compat"] = catalogue.has("yacl")
            this["clothconfig_compat"] = catalogue.has("clothconfig")
            this["midnightlib_compat"] = catalogue.has("midnightlib")
            this["walksylib_compat"] = catalogue.has("walksylib")
            this["modmenu_compat"] = catalogue.has("modmenu")
            this["rconfig_compat"] = catalogue.has("rconfig")
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
            this["wwaypoints_compat"] = current.project.endsWith("-fabric")
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
            string(eval(current.version, "< 1.21.11", "> 1.8.9")) {
                replace(
                    "import net.minecraft.resources.Identifier\n",
                    "import net.minecraft.resources.ResourceLocation as Identifier\n",
                )
            }
            string(eval(current.version, "= 1.8.9")) {
                replace(
                    "import net.minecraft.resources.Identifier\n",
                    "import net.minecraft.resource.Identifier\n",
                )
            }

            string(eval(current.version, "< 1.21.2")) {
                replace("Minecraft.getInstance().schedule(", "Minecraft.getInstance().tell(")
            }

            string(eval(current.version, "= 1.8.9")) {
                replace(
                    "com.mojang.blaze3d.platform.InputConstants",
                    "org.polyfrost.oneconfig.internal.legacy.InputConstants"
                )
                replace(
                    "com.mojang.blaze3d.platform.NativeImage",
                    "org.polyfrost.oneconfig.internal.legacy.NativeImage"
                )
                replace(
                    "net.kyori.adventure.platform.fabric.FabricClientAudiences",
                    "org.polyfrost.oneconfig.internal.legacy.FabricClientAudiences"
                )
                replace(
                    "net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource",
                    "org.polyfrost.oneconfig.internal.legacy.command.FabricClientCommandSource"
                )
                replace(
                    "net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback",
                    "org.polyfrost.oneconfig.internal.legacy.command.ClientCommandRegistrationCallback"
                )
                replace(
                    "net.minecraft.client.resources.sounds.SimpleSoundInstance",
                    "org.polyfrost.oneconfig.internal.legacy.SimpleSoundInstance"
                )
                replace(
                    "net.minecraft.sounds.SoundEvents",
                    "org.polyfrost.oneconfig.internal.legacy.SoundEvents"
                )
                replace(
                    "net.minecraft.network.chat.Component",
                    "org.polyfrost.oneconfig.internal.legacy.chat.Component"
                )
                replace(
                    "net.minecraft.network.chat.MutableComponent",
                    "org.polyfrost.oneconfig.internal.legacy.chat.MutableComponent"
                )
                replace(
                    "net.minecraft.network.chat.Style",
                    "org.polyfrost.oneconfig.internal.legacy.chat.Style"
                )
                replace(
                    "net.minecraft.ChatFormatting",
                    "org.polyfrost.oneconfig.internal.legacy.chat.ChatFormatting"
                )
                replace(
                    "net.minecraft.network.chat.FormattedText",
                    "org.polyfrost.oneconfig.internal.legacy.chat.FormattedText"
                )
                replace(
                    "net.minecraft.util.FormattedCharSequence",
                    "org.polyfrost.oneconfig.internal.legacy.chat.FormattedCharSequence"
                )
                replace(
                    "net.minecraft.util.FormattedCharSink",
                    "org.polyfrost.oneconfig.internal.legacy.chat.FormattedCharSink"
                )
                replace(
                    "net.minecraft.server.Bootstrap",
                    "net.minecraft.Bootstrap"
                )
                replace(
                    "net.minecraft.client.gui.screens.Screen",
                    "net.minecraft.client.gui.screen.Screen"
                )
                replace(
                    "net.minecraft.client.gui.screens.TitleScreen",
                    "net.minecraft.client.gui.screen.TitleScreen"
                )
                replace(
                    "net.minecraft.client.gui.screens.ChatScreen",
                    "net.minecraft.client.gui.screen.ChatScreen"
                )
                replace(
                    "com.mojang.blaze3d.pipeline.RenderTarget",
                    "net.minecraft.client.render.pipeline.RenderTarget"
                )
                replace(
                    "net.minecraft.server.packs.repository.RepositorySource",
                    "net.ornithemc.osl.resource.loader.api.resource.repository.ResourcePackRepository"
                )
                replace(
                    "net.minecraft.world.phys.HitResult",
                    "net.minecraft.world.HitResult"
                )
                replace(
                    "import net.minecraft.network.protocol.Packet",
                    "import net.minecraft.network.packet.Packet"
                )
                replace(
                    "net.minecraft.client.resources.sounds.SoundInstance",
                    "net.minecraft.client.sound.instance.SoundInstance"
                )
                replace(
                    "net.minecraft.client.resources.sounds.AbstractTickableSoundInstance",
                    "net.minecraft.client.sound.instance.AbstractTickableSoundInstance"
                )
                replace(
                    "net.minecraft.client.resources.language.I18n",
                    "net.ornithemc.osl.localization.api.L10n"
                )
                replace(
                    "net.minecraft.client.sounds.SoundManager",
                    "net.minecraft.client.sound.system.SoundManager"
                )
                replace(
                    "net.minecraft.client.sounds.SoundEngine",
                    "net.minecraft.client.sound.system.SoundEngine"
                )
                replace(
                    "net.minecraft.sounds.SoundSource",
                    "net.minecraft.client.sound.SoundCategory"
                )
                replace(
                    "net.minecraft.client.renderer.entity.LivingEntityRenderer",
                    "net.minecraft.client.render.entity.LivingEntityRenderer"
                )
                replace(
                    "net.minecraft.world.entity.LivingEntity",
                    "net.minecraft.entity.living.LivingEntity"
                )
                replace(
                    "net.minecraft.world.item",
                    "net.minecraft.item"
                )
                replace(
                    "net.minecraft.server.packs.resources.PreparableReloadListener",
                    "net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReloadListener"
                )
                replace(
                    "net.minecraft.server.packs.resources.ReloadableResourceManager",
                    "net.ornithemc.osl.resource.loader.api.resource.manager.ReloadableResourceManager"
                )
                replace(
                    "net.minecraft.server.packs.resources.ResourceManager",
                    "net.ornithemc.osl.resource.loader.api.resource.manager.ResourceManager"
                )
                replace(
                    "net.minecraft.server.packs.resources.SimpleReloadInstance",
                    "net.ornithemc.osl.resource.loader.impl.resource.manager.SimpleReloadableResourceManager"
                )
                replace(
                    "net.minecraft.core.Direction",
                    "net.minecraft.util.math.Direction"
                )
                replace(
                    "net.minecraft.server.packs.resources.ReloadInstance",
                    "net.ornithemc.osl.resource.loader.api.resource.reload.ResourceReload"
                )
                replace(
                    "net.minecraft.client.gui.components.DebugScreenOverlay",
                    "net.minecraft.client.gui.overlay.DebugOverlay"
                )
                replace(
                    "net.minecraft.client.renderer.texture.AbstractTexture",
                    "net.minecraft.client.render.texture.Texture"
                )
                replace(
                    "net.minecraft.client.renderer.texture.DynamicTexture",
                    "net.minecraft.client.render.texture.HttpTexture"
                )
                replace(
                    "net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents",
                    "net.ornithemc.osl.networking.api.client.ClientConnectionEvents"
                )
            }
        }
    }
}
