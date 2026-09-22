package org.polyfrost.oneconfig.internal.compat

import net.minecraft.client.AttackIndicatorStatus
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.player.Player
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.HudResize
import org.polyfrost.oneconfig.api.hud.v1.OneConfigHudWrapper
import org.polyfrost.oneconfig.api.hud.v1.events.HudEditorToggleEvent
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.hud.CompatOverlayRenderer
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer

object ArmorHudCompat {
    private val LOGGER = LogManager.getLogger("OneConfig/ArmorHud-Compat")

    private const val MOD_ID = "ukus-armor-hud"
    private const val MOD_CLASS = "ru.berdinskiybear.armorhud.ArmorHudMod"

    private const val SLOT_HINT_COLOR = 0x66FFFFFF

    private val TRANSLATION_KEY_GETTERS = listOf("getTranslationKey", "getKey", "method_7359")

    private const val WIDGET_SHOWN = "widgetShown"
    private const val SHOWN_ALWAYS = "ALWAYS"

    private const val LEGACY_SIZE = 22
    private const val LEGACY_STEP = 20
    private const val LEGACY_SLOTS = 4
    private const val LEGACY_HOTBAR_OFFSET = 98
    private const val LEGACY_OFFHAND_OFFSET = 29
    private const val LEGACY_ATTACK_INDICATOR_OFFSET = 23

    private var dirty = false
    private var dragging = false
    private var dragX = 0f
    private var dragY = 0f

    private var restoreFailed = false


    private class Slot(val anchor: Any, val side: Any, val x: Float, val y: Float, val w: Float, val h: Float)

    @Volatile
    private var slots: List<Slot> = emptyList()

    @Volatile
    private var current: Slot? = null

    @JvmStatic
    fun register() {
        if (!CompatLoader.hasMod(MOD_ID)) return
        CompatLoader.requireTranslations(skip = true) {
            runCatching {
                if (config() == null) error("armor hud config is unavailable")
                ArmorHudWrapper().register()
                CompatOverlayRenderer.register(::onOverlay)
                EventManager.register(HudEditorToggleEvent::class.java, Consumer { event ->
                    if (!event.open) {
                        dragging = false
                        flush()
                    }
                })
                LOGGER.info("Registered uku's Armor HUD into the OneConfig HUD editor")
            }.onFailure { LOGGER.warn("Failed to load uku's Armor HUD compat", it) }
        }
    }

    private fun onOverlay(ctx: GuiGraphicsExtractor) {
        measure(ctx)
        if (dragging) renderSlotHints(ctx)
    }

    private fun measure(ctx: GuiGraphicsExtractor) {
        val config = config() ?: return
        val player = callStatic("getCameraPlayer") ?: return
        val previousAnchor = call(config, "getAnchor") ?: return
        val previousSide = call(config, "getSide") ?: return

        if (!dragging) {
            val live = widgetRect(ctx, player)
                ?.let { Slot(previousAnchor, previousSide, it[0], it[1], it[2], it[3]) }
            slots = listOfNotNull(live)
            current = live
            return
        }

        val anchors = enumConstants(previousAnchor) ?: return
        val sides = enumConstants(previousSide) ?: return
        val found = ArrayList<Slot>(anchors.size * sides.size)
        var live: Slot? = null
        try {
            for (anchor in anchors) {
                for (side in sides) {
                    call(config, "setAnchor", anchor)
                    call(config, "setSide", side)
                    val rect = widgetRect(ctx, player) ?: continue
                    val slot = Slot(anchor, side, rect[0], rect[1], rect[2], rect[3])
                    if (anchor == previousAnchor && side == previousSide) live = slot
                    found.add(slot)
                }
            }
        } finally {
            restore(config, previousAnchor, previousSide)
        }

        val ordered = if (live == null) found else listOf(live) + found.filter { it !== live }
        val deduped = ArrayList<Slot>(ordered.size)
        for (slot in ordered) {
            if (deduped.none { it.x == slot.x && it.y == slot.y && it.w == slot.w && it.h == slot.h }) {
                deduped.add(slot)
            }
        }

        slots = deduped
        current = live
    }

    private fun restore(config: Any, anchor: Any?, side: Any?) {
        call(config, "setAnchor", anchor)
        call(config, "setSide", side)
        val restored = call(config, "getAnchor") == anchor && call(config, "getSide") == side
        if (!restored && !restoreFailed) {
            LOGGER.warn("Could not put the armor HUD anchor and side back after measuring, so it will not be saved")
        }
        restoreFailed = !restored
    }

    private var rectWarned = false

    private fun cannotRead(what: String): FloatArray? {
        if (!rectWarned) {
            rectWarned = true
            LOGGER.warn("Could not read the armor HUD widget rect ($what), so it cannot be placed in the editor")
        }
        return null
    }

    private fun widgetRect(ctx: Any, player: Any): FloatArray? {
        if (!hasWidgetRect) return legacyWidgetRect(player)
        val answer = callStatic("getWidgetRect", ctx, player)
            ?: return cannotRead("ArmorHudMod#getWidgetRect could not be called")
        val optional = answer as? Optional<*> ?: return cannotRead("ArmorHudMod#getWidgetRect gave $answer")
        val rect = optional.orElse(null) ?: return null
        val x = (call(rect, "getX") as? Number)?.toFloat() ?: return cannotRead("${rect.javaClass.name} has no x")
        val y = (call(rect, "getY") as? Number)?.toFloat() ?: return cannotRead("${rect.javaClass.name} has no y")
        val w = (call(rect, "getWidth") as? Number)?.toFloat()
            ?: return cannotRead("${rect.javaClass.name} has no width")
        val h = (call(rect, "getHeight") as? Number)?.toFloat()
            ?: return cannotRead("${rect.javaClass.name} has no height")
        return floatArrayOf(x, y, w, h)
    }

    private fun legacyWidgetRect(player: Any): FloatArray? {
        val config = config() ?: return cannotRead("the armor hud config is unavailable")
        val anchor = (call(config, "getAnchor") as? Enum<*>)?.name ?: return cannotRead("the anchor is not an enum")
        val side = (call(config, "getSide") as? Enum<*>)?.name ?: return cannotRead("the side is not an enum")
        val offsetX = (call(config, "getOffsetX") as? Number)?.toInt() ?: 0
        val offsetY = (call(config, "getOffsetY") as? Number)?.toInt() ?: 0
        val screenWidth = Platform.screen().guiWidth()
        val screenHeight = Platform.screen().guiHeight()

        val mirrored = if (anchor == "HOTBAR") side == "LEFT" else side == "RIGHT"
        val sideMultiplier = if (mirrored) -1 else 1
        val sideOffsetMultiplier = if (mirrored) -1 else 0

        val shown = (call(config, "getWidgetShown") as? Enum<*>)?.name
        val slots = if (shown != "NOT_EMPTY") LEGACY_SLOTS else {
            (player as? Player)?.let { armorWorn(it).coerceIn(1, LEGACY_SLOTS) } ?: LEGACY_SLOTS
        }
        val width = LEGACY_SIZE + (slots - 1) * LEGACY_STEP

        val x = offsetX * sideMultiplier + when (anchor) {
            "TOP_CENTER" -> screenWidth / 2 - width / 2
            "HOTBAR" ->
                screenWidth / 2 +
                    (LEGACY_HOTBAR_OFFSET + legacyHotbarOffset(config, player)) * sideMultiplier +
                    width * sideOffsetMultiplier

            else -> (width - screenWidth) * sideOffsetMultiplier
        }
        val top = anchor == "TOP" || anchor == "TOP_CENTER"
        val y = (if (top) offsetY else screenHeight - LEGACY_SIZE - offsetY)
        return floatArrayOf(x.toFloat(), y.toFloat(), width.toFloat(), LEGACY_SIZE.toFloat())
    }

    private fun legacyHotbarOffset(config: Any, player: Any): Int {
        val behavior = (call(config, "getOffhandSlotBehavior") as? Enum<*>)?.name
        if (behavior == "ALWAYS_IGNORE") return 0
        if (behavior == "ALWAYS_LEAVE_SPACE") return LEGACY_OFFHAND_OFFSET
        val arm = call(config, "getSide")?.let { call(it, "asArm") } ?: return 0
        val entity = player as? Player ?: return 0
        if (entity.mainArm.opposite != arm) return 0
        if (!entity.offhandItem.isEmpty) return LEGACY_OFFHAND_OFFSET
        val indicator = Minecraft.getInstance().options.attackIndicator().get()
        return if (indicator == AttackIndicatorStatus.HOTBAR) LEGACY_ATTACK_INDICATOR_OFFSET else 0
    }

    private fun renderSlotHints(ctx: GuiGraphicsExtractor) {
        val live = current
        for (slot in slots) {
            if (slot === live) continue
            ctx.fill(
                slot.x.toInt(),
                slot.y.toInt(),
                (slot.x + slot.w).toInt(),
                (slot.y + slot.h).toInt(),
                SLOT_HINT_COLOR,
            )
        }
    }

    @JvmStatic
    fun forcedWidgetShown(config: Any): Any? {
        if (!HudManager.isEditing) return null
        val player = callStatic("getCameraPlayer") as? Player ?: return null
        if (!armorEmpty(player)) return null
        val current = fieldValue(config, WIDGET_SHOWN) ?: return null
        return enumConstants(current)?.firstOrNull { enumName(it) == SHOWN_ALWAYS }?.takeIf { it != current }
    }

    private fun enumName(value: Any?): String? = (value as? Enum<*>)?.name

    private val ARMOR_SLOTS = EquipmentSlot.values().filter { it.type == EquipmentSlot.Type.HUMANOID_ARMOR }

    private fun armorWorn(player: Player): Int = ARMOR_SLOTS.count { !player.getItemBySlot(it).isEmpty }

    private fun armorEmpty(player: Player): Boolean = armorWorn(player) == 0

    private fun snap(x: Float, y: Float) {
        val config = config() ?: return
        val live = current ?: return
        val cx = x + live.w / 2f
        val cy = y + live.h / 2f
        val target = slots.minByOrNull { slot ->
            val dx = cx - (slot.x + slot.w / 2f)
            val dy = cy - (slot.y + slot.h / 2f)
            dx * dx + dy * dy
        } ?: return
        if (target === live) return
        call(config, "setAnchor", target.anchor)
        call(config, "setSide", target.side)
        current = target
        dirty = true
    }

    private fun flush() {
        if (!dirty) return
        if (restoreFailed) {
            val live = current ?: return
            restore(config() ?: return, live.anchor, live.side)
            if (restoreFailed) return
        }
        dirty = false
        runCatching { manager()?.let { call(it, "saveConfig") } }
            .onFailure { LOGGER.warn("Failed to save the armor HUD config", it) }
    }

    private val LABEL_KEYS = mapOf(
        "pushStatusEffectIcons" to "pushIcons",
        "iconsShown" to "showIcons",
        "warningShown" to "showWarning",
        "warningBobIntensity" to "iconBobIntensity",
        "minDurabilityValue" to "minDuraValue",
        "minDurabilityPercentage" to "minDuraPercent",
    )

    internal fun buildSettings(): List<Property<*>> {
        val config = config() ?: return emptyList()
        val out = ArrayList<Property<*>>()
        for (field in config.javaClass.declaredFields) {
            if (Modifier.isStatic(field.modifiers) || field.isSynthetic) continue
            val name = field.name
            if (getter(config, name) == null || setter(config, name) == null) continue
            val id = "armorhud_${name.lowercase()}"
            val property = when {
                field.type == Boolean::class.javaPrimitiveType -> booleanProperty(name, id)
                field.type.isEnum -> enumProperty(name, field.type, id)
                field.type == Int::class.javaPrimitiveType -> intProperty(name, id)
                field.type == Double::class.javaPrimitiveType -> decimalProperty(name, id, wide = true)
                field.type == Float::class.javaPrimitiveType -> decimalProperty(name, id, wide = false)

                else -> null
            } ?: continue
            labelKey(name)?.let { property.addMetadata("titleKey", it) }
            property.addMetadata("subcategory", "Settings")
            out.add(property)
        }
        return out
    }

    private fun booleanProperty(field: String, id: String): Property<*> = Properties.functional(
        { read(field) == true },
        { value: Boolean -> write(field, value) },
        id,
        label(field),
        null,
        Boolean::class.javaObjectType,
    ).apply { addMetadata("visualizer", Visualizer.SwitchVisualizer::class.java) }

    private fun enumProperty(field: String, type: Class<*>, id: String): Property<*>? {
        val values = type.enumConstants ?: return null
        if (values.isEmpty()) return null
        return Properties.functional(
            { values.indexOfFirst { it == read(field) }.coerceAtLeast(0) },
            { value: Int -> values.getOrNull(value)?.let { write(field, it) } },
            id,
            label(field),
            null,
            Int::class.javaObjectType,
        ).apply {
            addMetadata("visualizer", Visualizer.DropdownVisualizer::class.java)
            addMetadata("options", values.map { enumLabel(it) })
        }
    }

    private fun intProperty(field: String, id: String): Property<*> = Properties.functional(
        { (read(field) as? Number)?.toInt() ?: 0 },
        { value: Int -> write(field, value) },
        id,
        label(field),
        null,
        Int::class.javaObjectType,
    ).apply {
        addMetadata("visualizer", Visualizer.NumberVisualizer::class.java)
        addMetadata("min", if (field.startsWith("offset")) -1000f else 0f)
        addMetadata("max", 1000f)
    }

    private fun decimalProperty(field: String, id: String, wide: Boolean): Property<*> = Properties.functional(
        { (read(field) as? Number)?.toFloat() ?: 0f },
        { value: Float -> write(field, if (wide) value.toDouble() else value) },
        id,
        label(field),
        null,
        Float::class.javaObjectType,
    ).apply {
        addMetadata("visualizer", Visualizer.SliderVisualizer::class.java)
        addMetadata("min", 0f)
        addMetadata("max", 1f)
        addMetadata("step", 0.01f)
    }

    private fun read(field: String): Any? {
        val config = config() ?: return null
        fieldValue(config, field)?.let { return it }
        val getter = getter(config, field) ?: return null
        return call(config, getter)
    }

    private fun fieldValue(config: Any, name: String): Any? {
        val field = field(config.javaClass, name) ?: return null
        return runCatching { field.get(config) }.getOrNull()
    }

    private fun write(field: String, value: Any?) {
        val config = config() ?: return
        val setter = setter(config, field) ?: return
        call(config, setter, value)
        dirty = true
    }

    private fun getter(config: Any, field: String): String? {
        val suffix = field.replaceFirstChar { it.uppercase() }
        return listOf("get$suffix", "is$suffix").firstOrNull { hasMethod(config.javaClass, it, 0) }
    }

    private fun setter(config: Any, field: String): String? {
        val name = "set" + field.replaceFirstChar { it.uppercase() }
        return name.takeIf { hasMethod(config.javaClass, it, 1) }
    }

    private fun labelKey(field: String): String? {
        val key = "armorhud.option.${LABEL_KEYS[field] ?: field}"
        return key.takeIf { translate(it) != null }
    }

    private fun label(field: String): String {
        labelKey(field)?.let { key -> translate(key)?.let { return it } }
        return field.replace(Regex("(?<=[a-z0-9])(?=[A-Z])"), " ").replaceFirstChar { it.uppercase() }
    }

    private fun enumLabel(value: Any): String {
        val key = fieldValue(value, "translationKey") as? String
            ?: TRANSLATION_KEY_GETTERS.firstNotNullOfOrNull { call(value, it) as? String }
        key?.let { translate(it)?.let { translated -> return translated } }
        return (value as? Enum<*>)?.name ?: value.toString()
    }

    private fun translate(key: String): String? {
        val translated = runCatching { Component.translatable(key).string }.getOrNull() ?: return null
        return translated.takeIf { it.isNotBlank() && it != key }
    }

    private val modClass: Class<*>? by lazy {
        runCatching { Class.forName(MOD_CLASS) }
            .onFailure { LOGGER.warn("Could not resolve $MOD_CLASS", it) }
            .getOrNull()
    }

    private val hasWidgetRect: Boolean by lazy {
        modClass?.methods?.any {
            it.name == "getWidgetRect" && it.parameterCount == 2 && Modifier.isStatic(it.modifiers)
        } == true
    }


    private fun manager(): Any? = callStatic("getManager")

    private fun config(): Any? = manager()?.let { call(it, "getConfig") }

    private fun enumConstants(value: Any?): Array<out Any>? {
        val cls = value?.javaClass ?: return null
        return cls.enumConstants ?: cls.superclass?.enumConstants
    }

    private val methodCache = ConcurrentHashMap<String, Optional<Method>>()
    private val fieldCache = ConcurrentHashMap<String, Optional<Field>>()

    private fun method(cls: Class<*>, name: String, params: Int): Method? =
        methodCache.computeIfAbsent("${cls.name}#$name/$params") {
            Optional.ofNullable(
                cls.methods.firstOrNull { it.name == name && it.parameterCount == params }
                    ?.apply { isAccessible = true },
            )
        }.orElse(null)

    private fun field(cls: Class<*>, name: String): Field? =
        fieldCache.computeIfAbsent("${cls.name}#$name") {
            Optional.ofNullable(
                runCatching { cls.getDeclaredField(name).apply { isAccessible = true } }.getOrNull(),
            )
        }.orElse(null)

    private fun hasMethod(cls: Class<*>, name: String, params: Int): Boolean = method(cls, name, params) != null

    private fun callStatic(name: String, vararg args: Any?): Any? {
        val cls = modClass ?: return null
        val method = method(cls, name, args.size) ?: return null
        return runCatching { method.invoke(null, *args) }.getOrNull()
    }

    private fun call(target: Any, name: String, vararg args: Any?): Any? {
        val method = method(target.javaClass, name, args.size) ?: return null
        return runCatching { method.invoke(target, *args) }.getOrNull()
    }

    private class ArmorHudWrapper : OneConfigHudWrapper {
        override var id: String = "ukus_armor_hud"

        override var name: String = ModInfo.loadedMods.firstOrNull { it.id == MOD_ID }?.name ?: "uku's Armor HUD"

        override val modId: String = MOD_ID

        override val ownsPlacement: Boolean get() = true

        override val supportsScale: Boolean get() = false

        override val resizeAxes: HudResize get() = HudResize.None

        override val placementReady: Boolean get() = current != null

        override var x: Float
            get() = if (dragging) dragX else current?.x ?: 0f
            set(value) {
                if (dragging) dragX = value
            }

        override var y: Float
            get() = if (dragging) dragY else current?.y ?: 0f
            set(value) {
                if (dragging) dragY = value
            }

        override var scale: Float
            get() = 1f
            set(_) {}

        override var scaledWidth: Float
            get() = current?.w ?: 0f
            set(_) {}

        override var scaledHeight: Float
            get() = current?.h ?: 0f
            set(_) {}

        private val settings: List<Property<*>> by lazy { buildSettings() }

        private val enabled: Property<Boolean>? by lazy { CompatHudToggle.find(settings) }

        override fun linkedProperties(): List<Property<*>> = settings

        override var hidden: Boolean
            get() = enabled?.get() == false
            set(value) {
                enabled?.set(!value)
            }

        override fun onDragStart() {
            dragX = current?.x ?: 0f
            dragY = current?.y ?: 0f
            dragging = true
        }

        override fun onDragEnd() {
            dragging = false
            snap(dragX, dragY)
            flush()
        }

        override fun save() = flush()
    }
}
