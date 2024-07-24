package org.polyfrost.utils.v1.dsl

import org.polyfrost.universal.UMinecraft

/**
 * Gets the current [net.minecraft.client.Minecraft] instance.
 */
val mc
    get() = UMinecraft.getMinecraft()