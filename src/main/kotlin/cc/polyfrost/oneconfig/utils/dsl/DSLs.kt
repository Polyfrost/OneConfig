package cc.polyfrost.oneconfig.utils.dsl

import cc.polyfrost.oneconfig.libs.universal.UMinecraft

/**
 * Gets the current [net.minecraft.client.Minecraft] instance.
 */
val mc
    get() = UMinecraft.getMinecraft()