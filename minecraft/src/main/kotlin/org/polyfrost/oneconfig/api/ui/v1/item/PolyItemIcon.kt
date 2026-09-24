package org.polyfrost.oneconfig.api.ui.v1.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import net.minecraft.client.Minecraft
//? if > 1.8.9
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.jetbrains.skia.Rect
import org.polyfrost.compose.composables.PolyCanvas
import org.polyfrost.compose.mc.McFontQueue
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.absoluteAt
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.hud.v1.LocalHud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog
import org.polyfrost.oneconfig.internal.ui.components.item.MinecraftItemCatalogService
import org.polyfrost.oneconfig.internal.ui.components.item.polyItemRenderSizePx
import org.polyfrost.oneconfig.internal.ui.components.item.rememberItemIconHandle
import kotlin.math.ceil

private const val ITEM_SIZE = 16f
private const val BAR_X = 2f
private const val BAR_Y = 13f
private const val BAR_WIDTH = 13f

private val BAR_BACKGROUND = PolyColor(0xFF000000.toInt())
//? if > 1.8.9 {
private val COOLDOWN_OVERLAY = PolyColor(0x7FFFFFFF)

private val clientDispatcher by lazy { Minecraft.getInstance().asCoroutineDispatcher() }

private val clientTicks = MutableStateFlow(0).also { ticks ->
    EventManager.register(TickEvent.End::class.java, Runnable { ticks.value++ })
}

private suspend fun awaitClientTick() {
    val seen = clientTicks.value
    clientTicks.first { it != seen }
}
//?}

/** Draws the item identified by its registry ID, or a placeholder while it is unavailable. */
@Composable
fun PolyItemIcon(
    id: String,
    size: Float = 16f,
    modifier: PolyModifier = PolyModifier,
    placeholderColor: PolyColor = PolyColor(0xFFAAAAAA.toInt()),
) {
    val itemIcon = rememberItemIconHandle(id)
    val hud = LocalHud.current
    SideEffect {
        itemIcon?.setRenderSizePx(polyItemRenderSizePx(size, hud))
    }
    PolyCanvas(modifier = modifier.size(size, size)) { x, y, w, h ->
        if (itemIcon == null) {
            rectStroke(x, y, w, h, placeholderColor, strokeWidth = 1f, radius = 1f)
            line(x, y, x + w, y + h, placeholderColor, strokeWidth = 1f)
        } else {
            itemIcon.setRenderSizePx(polyItemRenderSizePx(w, hud))
            itemIcon.draw(canvas, Rect.makeLTRB(x, y, x + w, y + h))
        }
    }
}

/** Draws the default icon for [item]. */
@Composable
fun PolyItemIcon(item: Item, size: Float = 16f, modifier: PolyModifier = PolyModifier) {
    //~ if = 1.8.9 'BuiltInRegistries.ITEM' -> 'Item.REGISTRY'
    val id = remember(item) { BuiltInRegistries.ITEM.getKey(item).toString() }
    PolyItemIcon(id, size, modifier)
}

/**
 * Draws a live stack including its optional count, durability, and cooldown decorations.
 *
 * @param countOverride text shown instead of the stack count, or `null` to show the normal vanilla count
 */
@Composable
fun PolyItemIcon(
    stack: ItemStack,
    size: Float = 16f,
    modifier: PolyModifier = PolyModifier,
    decorations: Boolean = true,
    countOverride: String? = null,
) {
    val hud = LocalHud.current
    val visible = hud == null || hud.isVisible.value
    val icon = rememberItemIconHandle(Unit) { forHud ->
        (ItemCatalog.platformService() as? MinecraftItemCatalogService)?.openStack(stack, forHud)
    }
    SideEffect {
        icon?.update(stack)
        icon?.setRenderSizePx(polyItemRenderSizePx(size, hud))
    }

    PolyBox(modifier = modifier.size(size, size)) {
        PolyCanvas(modifier = PolyModifier.size(size, size)) { x, y, w, h ->
            if (icon == null) return@PolyCanvas
            icon.setRenderSizePx(polyItemRenderSizePx(w, hud))
            icon.draw(canvas, Rect.makeLTRB(x, y, x + w, y + h))
        }
        if (!visible || !decorations || stack.isEmpty) return@PolyBox

        val scale = size / ITEM_SIZE
        ItemDurability(stack, scale)
        //? if > 1.8.9
        ItemCooldown(stack, scale)
        ItemCount(stack, countOverride, scale)
    }
}

@Composable
private fun ItemDurability(stack: ItemStack, scale: Float) {
    if (!stack.isBarVisible) return
    val filled = stack.barWidth.toFloat()
    val color = PolyColor(0xFF000000.toInt() or (stack.barColor and 0xFFFFFF))

    val size = ITEM_SIZE * scale
    PolyCanvas(PolyModifier.size(size, size)) { x, y, _, _ ->
        rect(x + BAR_X * scale, y + BAR_Y * scale, BAR_WIDTH * scale, 2f * scale, BAR_BACKGROUND)
        rect(x + BAR_X * scale, y + BAR_Y * scale, filled * scale, scale, color)
    }
}

//? if > 1.8.9 {
@Composable
private fun ItemCooldown(stack: ItemStack, scale: Float) {
    val size = ITEM_SIZE * scale
    val forHud = LocalHud.current != null
    val height = remember { intArrayOf(cooldownHeight(stack)) }

    LaunchedEffect(forHud, stack) {
        withContext(clientDispatcher) {
            while (true) {
                val updated = cooldownHeight(stack)
                if (height[0] != updated) {
                    height[0] = updated
                    // Previews render without read observation, so they redraw on their shared revision.
                    if (forHud) HudManager.invalidate() else HudManager.previewRevision.intValue++
                }
                if (updated > 0) withFrameNanos { } else awaitClientTick()
            }
        }
    }

    PolyCanvas(PolyModifier.size(size, size)) { x, y, _, _ ->
        val overlayHeight = height[0]
        if (overlayHeight > 0) {
            rect(x, y + (ITEM_SIZE - overlayHeight) * scale, size, overlayHeight * scale, COOLDOWN_OVERLAY)
        }
    }
}

private fun cooldownHeight(stack: ItemStack): Int {
    val mc = Minecraft.getInstance()
    val player = mc.player ?: return 0
    //? if < 1.21.4 {
    /*val cooldown = player.cooldowns.getCooldownPercent(
        stack.item,
        mc.timer.getGameTimeDeltaPartialTick(true),
    )
    *///?} else {
    val cooldown = player.cooldowns.getCooldownPercent(
        stack,
        mc.deltaTracker.getGameTimeDeltaPartialTick(true),
    )
    //?}
    return ceil(ITEM_SIZE * cooldown).toInt().coerceIn(0, ITEM_SIZE.toInt())
}
//?}

@Composable
private fun ItemCount(stack: ItemStack, countOverride: String?, scale: Float) {
    val text = countOverride ?: stack.count.takeIf { it > 1 }?.toString() ?: return
    val width = McFontQueue.measureTextWidth(text, 1f)
    PolyMcText(
        text = text,
        color = PolyColor.WHITE,
        shadow = true,
        scale = scale,
        modifier = PolyModifier.absoluteAt((17f - width) * scale, 9f * scale),
    )
}
