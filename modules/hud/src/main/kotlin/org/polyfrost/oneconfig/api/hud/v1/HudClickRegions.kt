@file:OptIn(ExperimentalOneConfigUiApi::class, InternalOneConfigUiApi::class)
@file:JvmName("HudClickRegions")

package org.polyfrost.oneconfig.api.hud.v1

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.oneconfig.api.ui.v1.ExperimentalOneConfigUiApi
import org.polyfrost.oneconfig.api.ui.v1.InternalOneConfigUiApi
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong

@ExperimentalOneConfigUiApi
public enum class HudMouseButton {
    Left,

    Right,

    Middle,
}

@ExperimentalOneConfigUiApi
public fun Hud.clickRegion(
    id: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    anyScreen: Boolean = false,
    button: HudMouseButton = HudMouseButton.Left,
    expireAfterMillis: Long = defaultRegionExpiryMillis(),
    onClick: () -> Unit,
): Boolean = HudClickInput.region(this, id, x, y, w, h, anyScreen, 1 shl button.ordinal, true, expireAfterMillis, null, onClick)

@ExperimentalOneConfigUiApi
public fun Hud.clickRegion(
    id: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    anyScreen: Boolean,
    buttons: Set<HudMouseButton>,
    expireAfterMillis: Long = defaultRegionExpiryMillis(),
    onClick: (HudMouseButton) -> Unit,
): Boolean = HudClickInput.region(this, id, x, y, w, h, anyScreen, buttonMask(buttons), true, expireAfterMillis, onClick, null)

@ExperimentalOneConfigUiApi
public fun Hud.hoverRegion(
    id: String,
    x: Float,
    y: Float,
    w: Float,
    h: Float,
    anyScreen: Boolean = false,
    expireAfterMillis: Long = defaultRegionExpiryMillis(),
): Boolean = HudClickInput.region(this, id, x, y, w, h, anyScreen, 0, false, expireAfterMillis, null, null)

@ExperimentalOneConfigUiApi
public fun Hud.defaultRegionExpiryMillis(): Long {
    val frequency = runCatching { updateFrequency() }.getOrDefault(-1L)
    return when {
        frequency <= 0L -> MIN_REGION_EXPIRY_MILLIS
        frequency >= Long.MAX_VALUE / REGION_EXPIRY_UPDATES -> Long.MAX_VALUE
        else -> {
            val nanos = frequency * REGION_EXPIRY_UPDATES
            maxOf(MIN_REGION_EXPIRY_MILLIS, nanos / NANOS_PER_MILLI + if (nanos % NANOS_PER_MILLI != 0L) 1L else 0L)
        }
    }
}

private const val MIN_REGION_EXPIRY_MILLIS = 250L
private const val REGION_EXPIRY_UPDATES = 2L
private const val NANOS_PER_MILLI = 1_000_000L

@ExperimentalOneConfigUiApi
public fun Hud.removeClickRegion(id: String): Boolean = HudClickInput.remove(this, id)

@ExperimentalOneConfigUiApi
public fun Hud.isClickRegionHovered(id: String): Boolean = HudClickInput.hovered(this, id)

@ExperimentalOneConfigUiApi
public fun Hud.hoveredClickRegion(): String? = HudClickInput.hoveredId(this)

private val ALL_BUTTONS = HudMouseButton.values()

private fun buttonMask(buttons: Set<HudMouseButton>): Int {
    var mask = 0
    for (button in ALL_BUTTONS) if (button in buttons) mask = mask or (1 shl button.ordinal)
    return mask
}

@InternalOneConfigUiApi
@ExperimentalOneConfigUiApi
@ApiStatus.Internal
public object HudClickInput {
    private class Region(val hud: Hud) {
        @Volatile var x = 0f
        @Volatile var y = 0f
        @Volatile var w = 0f
        @Volatile var h = 0f
        @Volatile var anyScreen = false
        @Volatile var buttons = 0
        @Volatile var consumes = true
        @Volatile var onClick: ((HudMouseButton) -> Unit)? = null
        @Volatile var onPlainClick: (() -> Unit)? = null
        @Volatile var declaredAt = 0L
        @Volatile var inPass = false
        @Volatile var epoch = 0L
        @Volatile var declaredNanos = 0L
        @Volatile var lifetime = Long.MAX_VALUE
        val hovered = mutableStateOf(false)
    }

    private const val STALE_NANOS = 250_000_000L

    private val BUTTONS = HudMouseButton.values()

    private val regions = IdentityHashMap<Hud, LinkedHashMap<String, Region>>()
    private val recorded = IdentityHashMap<Hud, Long>()
    private val looseEpochs = IdentityHashMap<Hud, Long>()
    private val clock = AtomicLong()
    @Volatile private var drawing: Hud? = null
    @Volatile private var lastPassStart = 0L
    @Volatile private var pointerX = Float.NaN
    @Volatile private var pointerY = Float.NaN
    @Volatile private var active = false
    @Volatile private var anyScreen = false
    @Volatile private var pointerAt = 0L
    private val consumedPress = BooleanArray(BUTTONS.size)
    private val pressedRegion = arrayOfNulls<Region>(BUTTONS.size)

    internal fun region(
        hud: Hud,
        id: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        anyScreen: Boolean,
        buttons: Int,
        consumes: Boolean,
        expireAfterMillis: Long,
        onClick: ((HudMouseButton) -> Unit)?,
        onPlainClick: (() -> Unit)?,
    ): Boolean {
        val inPass = drawing === hud
        val epoch = lastPassStart
        val region = synchronized(regions) {
            if (!inPass) {
                val loose = looseEpochs[hud]
                if (loose == null || loose < epoch) looseEpochs[hud] = epoch
            }
            regions.getOrPut(hud) { LinkedHashMap() }.getOrPut(id) { Region(hud) }
        }
        if (region.inPass != inPass) region.inPass = inPass
        region.epoch = epoch
        if (region.x != x) region.x = x
        if (region.y != y) region.y = y
        if (region.w != w) region.w = w
        if (region.h != h) region.h = h
        if (region.anyScreen != anyScreen) region.anyScreen = anyScreen
        if (region.buttons != buttons) region.buttons = buttons
        if (region.consumes != consumes) region.consumes = consumes
        if (region.onClick !== onClick) region.onClick = onClick
        if (region.onPlainClick !== onPlainClick) region.onPlainClick = onPlainClick
        region.declaredAt = clock.incrementAndGet()
        region.declaredNanos = System.nanoTime()
        region.lifetime = if (inPass) Long.MAX_VALUE else lifetimeNanos(expireAfterMillis)
        return region.hovered.value
    }

    private fun lifetimeNanos(millis: Long): Long = when {
        millis >= Long.MAX_VALUE / 1_000_000L -> Long.MAX_VALUE
        millis <= 0L -> 0L
        else -> millis * 1_000_000L
    }

    internal fun remove(hud: Hud, id: String): Boolean {
        val removed = synchronized(regions) {
            val byId = regions[hud] ?: return false
            val region = byId.remove(id) ?: return false
            if (byId.isEmpty()) {
                regions.remove(hud)
                recorded.remove(hud)
                looseEpochs.remove(hud)
            }
            region
        }
        if (removed.hovered.value) Snapshot.withMutableSnapshot { removed.hovered.value = false }
        return true
    }

    internal fun hovered(hud: Hud, id: String): Boolean {
        val region = synchronized(regions) { regions[hud]?.get(id) } ?: return false
        return region.hovered.value
    }

    internal fun hoveredId(hud: Hud): String? {
        val entries = synchronized(regions) { regions[hud]?.entries?.map { it.key to it.value } } ?: return null
        var found: String? = null
        for ((id, region) in entries) if (region.hovered.value) found = id
        return found
    }

    internal fun passStart(hud: Hud): Long {
        val pass = clock.incrementAndGet()
        lastPassStart = pass
        drawing = hud
        return pass
    }

    internal fun passEnd() {
        drawing = null
    }

    internal fun recorded(hud: Hud, pass: Long) {
        synchronized(regions) {
            if (regions.containsKey(hud)) recorded[hud] = pass
        }
    }

    public fun updatePointer(guiX: Float, guiY: Float, active: Boolean) {
        updatePointer(guiX, guiY, active, active)
    }

    public fun updatePointer(guiX: Float, guiY: Float, active: Boolean, anyScreen: Boolean) {
        pointerX = guiX
        pointerY = guiY
        this.active = active
        this.anyScreen = anyScreen || active
        pointerAt = System.nanoTime()
        if (!this.anyScreen || HudManager.isEditing || HudManager.isConfigUiOpen) {
            idlePointer()
            return
        }
        var changed: ArrayList<Region>? = null
        synchronized(regions) {
            hoverCleared = false
            if (regions.isEmpty()) return
            changed = sweep()
        }
        val targets = hoverTargets()
        synchronized(regions) {
            for (byId in regions.values) for (region in byId.values) if (region.hovered.value != (targets?.contains(region) == true)) changed = changed.with(region)
        }
        val flips = changed ?: return
        Snapshot.withMutableSnapshot {
            for (region in flips) region.hovered.value = !region.hovered.value
        }
    }

    private var hoverCleared = true

    private fun idlePointer() {
        var cleared: ArrayList<Region>? = null
        synchronized(regions) {
            if (regions.isEmpty()) {
                hoverCleared = true
                return
            }
            cleared = sweep()
            if (!hoverCleared) {
                for (byId in regions.values) for (region in byId.values) if (region.hovered.value) cleared = cleared.with(region)
                hoverCleared = true
            }
        }
        val unhover = cleared ?: return
        Snapshot.withMutableSnapshot {
            for (region in unhover) region.hovered.value = false
        }
    }

    private fun ArrayList<Region>?.with(region: Region): ArrayList<Region> =
        (this ?: ArrayList<Region>(2)).also { it.add(region) }

    private fun sweep(): ArrayList<Region>? {
        var changed: ArrayList<Region>? = null
        val iter = regions.entries.iterator()
        while (iter.hasNext()) {
            val (hud, byId) = iter.next()
            if (!isActive(hud)) {
                for (region in byId.values) if (region.hovered.value) changed = changed.with(region)
                iter.remove()
                recorded.remove(hud)
                looseEpochs.remove(hud)
                continue
            }
            val stale = byId.values.iterator()
            while (stale.hasNext()) {
                val region = stale.next()
                if (!stale(region)) continue
                if (region.hovered.value) changed = changed.with(region)
                stale.remove()
            }
            if (byId.isEmpty()) {
                iter.remove()
                recorded.remove(hud)
                looseEpochs.remove(hud)
            }
        }
        return changed
    }

    public fun consumeButton(button: Int, pressed: Boolean): Boolean {
        val kind = BUTTONS.getOrNull(button) ?: return false
        if (!pressed) {
            val target = pressedRegion[button]
            pressedRegion[button] = null
            if (!consumedPress[button]) return false
            consumedPress[button] = false
            if (target != null && topmostHit(kind) === target) click(target, kind)
            return true
        }
        pressedRegion[button] = null
        consumedPress[button] = false
        if (!(active || anyScreen) || HudManager.isEditing || HudManager.isConfigUiOpen) return false
        val target = topmostHit(kind) ?: return false
        consumedPress[button] = true
        pressedRegion[button] = target
        return true
    }

    private fun click(target: Region, button: HudMouseButton) {
        try {
            val plain = target.onPlainClick
            if (plain != null) plain() else target.onClick?.invoke(button)
        } catch (e: Throwable) {
            if (e is VirtualMachineError) throw e
            HudManager.LOGGER.error("Click handler of HUD ${target.hud.title} failed", e)
        }
    }

    private fun snapshot(): IdentityHashMap<Hud, List<Region>>? = synchronized(regions) {
        if (regions.isEmpty()) return null
        IdentityHashMap<Hud, List<Region>>(regions.size).also { copy ->
            for ((hud, byId) in regions) copy[hud] = byId.values.toList()
        }
    }

    private fun topmostHit(button: HudMouseButton): Region? {
        val byHud = snapshot() ?: return null
        val bit = 1 shl button.ordinal
        val order = HudManager.pointerOrder()
        val body = bodyScratch
        for (i in order.size - 1 downTo 0) {
            val hud = order[i]
            val list = byHud[hud]
            if (list != null) {
                for (j in list.size - 1 downTo 0) {
                    val region = list[j]
                    if (region.consumes && region.buttons and bit != 0 && hits(region)) return region
                }
            }
            if (coversPointer(hud, i, body)) return null
        }
        return null
    }

    private fun hoverTargets(): Set<Region>? {
        val byHud = snapshot() ?: return null
        var found: MutableSet<Region>? = null
        val order = HudManager.pointerOrder()
        val body = bodyScratch
        for (i in order.size - 1 downTo 0) {
            val hud = order[i]
            val list = byHud[hud]
            var blocked = false
            if (list != null) {
                for (j in list.size - 1 downTo 0) {
                    val region = list[j]
                    if (region.consumes && blocked) continue
                    if (!hits(region)) continue
                    (found ?: Collections.newSetFromMap(IdentityHashMap<Region, Boolean>()).also { found = it }).add(region)
                    if (region.consumes) blocked = true
                }
            }
            if (blocked || coversPointer(hud, i, body)) break
        }
        return found
    }

    private fun stale(region: Region): Boolean = synchronized(regions) {
        if (region.inPass) {
            region.declaredAt < (recorded[region.hud] ?: 0L)
        } else {
            region.epoch < (looseEpochs[region.hud] ?: 0L) || expired(region)
        }
    }

    private fun expired(region: Region): Boolean =
        region.lifetime != Long.MAX_VALUE && System.nanoTime() - region.declaredNanos > region.lifetime

    private fun isActive(hud: Hud): Boolean {
        val instances = HudManager.activeInstances
        for (i in instances.indices) if (instances[i] === hud) return true
        return false
    }

    private val bodyScratch = FloatArray(4)

    private fun coversPointer(hud: Hud, index: Int, bounds: FloatArray): Boolean {
        if (!hud.isVisible.value) return false
        if (!HudManager.pointerBody(index, bounds)) return false
        val px = pointerX
        val py = pointerY
        if (px.isNaN() || py.isNaN()) return false
        val x = hud.frameX
        val y = hud.frameY
        return px >= x && py >= y && px < x + bounds[2] && py < y + bounds[3]
    }

    private fun hits(region: Region): Boolean {
        if (!(if (region.anyScreen) anyScreen else active) || HudManager.isEditing || HudManager.isConfigUiOpen) return false
        if (System.nanoTime() - pointerAt > STALE_NANOS) return false
        if (region.w <= 0f || region.h <= 0f) return false
        val hud = region.hud
        if (stale(region)) return false
        if (!hud.isVisible.value) return false
        val scale = hud.frameEffectiveScale
        if (scale <= 0f) return false
        val px = pointerX
        val py = pointerY
        if (px.isNaN() || py.isNaN()) return false
        val lx = (px - hud.frameX) / scale
        val ly = (py - hud.frameY) / scale
        return lx >= region.x && ly >= region.y && lx < region.x + region.w && ly < region.y + region.h
    }
}
