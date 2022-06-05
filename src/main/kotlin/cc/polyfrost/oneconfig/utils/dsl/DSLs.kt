package cc.polyfrost.oneconfig.utils.dsl

import gg.essential.universal.UMinecraft

/**
 * Gets the current [net.minecraft.client.Minecraft] instance.
 */
val mc
    get() = UMinecraft.getMinecraft()