//? wwaypoints_compat {
package org.polyfrost.oneconfig.internal.compat

import com.mojang.blaze3d.platform.InputConstants
import java.nio.file.Files
import java.nio.file.Path
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.Supplier
import net.minecraft.client.KeyMapping
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import org.polyfrost.oneconfig.api.config.v1.CompatSnapshots
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.config.v1.dsl.category
import org.polyfrost.oneconfig.api.config.v1.dsl.noCache
import org.polyfrost.oneconfig.api.config.v1.dsl.saveFunction
import org.polyfrost.oneconfig.api.config.v1.dsl.subcategory
import org.polyfrost.oneconfig.api.config.v1.dsl.visualizer
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.api.TinyFdApi
import org.polyfrost.oneconfig.api.ui.v1.keybind.KeyModifiers
import org.polyfrost.oneconfig.api.ui.v1.keybind.OneConfigKeybind
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindCodec
import org.polyfrost.oneconfig.internal.ui.keybind.MinecraftKeybindRegistrar

/**
 * Renders [wWaypoints](https://modrinth.com/mod/wwaypoints) options as a native OneConfig page instead of the
 * Mod Menu Compat placeholder card
 *
 * The mod is closed-source and not a compile-time dependency so everything goes through [WWaypointsBridge]
 * Its own screens keep working because Mod Menu resolves each mod's own factory first and its `K` keybind
 * never routes through OneConfig
 *
 * Settings needing a widget OneConfig does not have yet become link-out buttons onto the first-party screen
 */
object WWaypointsCompat {

    private val LOGGER = org.apache.logging.log4j.LogManager.getLogger("OneConfig/wWaypoints-Compat")

    private const val MOD_ID = "wwaypoints"

    @JvmStatic
    fun register() {
        if (!CompatLoader.hasMod(MOD_ID)) return
        // priority 0 so this runs before ModMenuCompat.postLoad (1000) and claims the card first
        CompatLoader.requireTranslations(priority = 0, skip = true) {
            runCatching {
                if (!WWaypointsBridge.available) {
                    LOGGER.warn("wWaypoints is present but its config could not be resolved; leaving it to Mod Menu compat")
                    return@requireTranslations
                }
                val tree = buildTree()
                CompatSnapshots.register(tree)
                CompatLoader.nativeLoadedConfigs.add(MOD_ID)
            }.onFailure { LOGGER.warn("Failed to build the wWaypoints config tree", it) }
        }
    }

    private fun buildTree(): Tree {
        val tree = Tree.tree()
        tree.id = MOD_ID
        tree.title = "wWaypoints"
        // mod writes its config on every row change so OneConfig must never cache values on top of it
        tree.noCache = true
        tree.saveFunction = Runnable { WWaypointsBridge.saveConfig() }
        ModInfo.loadedMods.firstOrNull { it.id == MOD_ID }?.extractIconFile()?.let {
            tree.addMetadata("icon_path", it)
        }

        buildGeneral(tree)
        buildDefaults(tree)
        buildIntegrations(tree)
        buildControls(tree)
        buildData(tree)
        buildPresets(tree)
        return tree
    }

    private fun buildGeneral(tree: Tree) {
        tree.put(
            switch(
                field = "ignoreFov",
                name = "Zoom With FOV",
                description = "Allows zoom mods to zoom into labels. Uses a fixed FOV baseline.",
                tags = listOf("zoom", "fov", "field of view", "optifine", "spyglass"),
            ).onChange { enabled ->
                // mirrors the first-party row where enabling pins the baseline to the player's current FOV
                if (enabled == true) currentFovDegrees()?.let { WWaypointsBridge.set("ignoreFovBaselineDeg", it) }
            }
        )

        tree.put(
            switch(
                field = "deathWaypoints",
                name = "Death waypoints",
                description = "Create a temporary waypoint at your death location.",
                tags = DEATH_TAGS,
            ).onChange { WWaypointsBridge.syncDeathWaypointsHiddenStateAfterToggle(it == true) }
        )
        tree.put(
            linkOut(
                id = "death_waypoint_appearance",
                name = "Death waypoint appearance",
                description = "Style the label and block shown at your death location, and cap how many are kept.",
                text = "Configure",
                tags = DEATH_TAGS,
            ) { WWaypointsBridge.specialLabelStylist("DEATH", currentScreen()) }
        )

        tree.put(
            switch(
                field = "useBlockModel",
                name = "Use Block Model",
                description = "When ON, block highlights fit slabs, stairs, buttons, and similar block shapes.",
                tags = listOf("block", "model", "shape", "slab", "stairs", "highlight", "hitbox"),
            ).onChange {
                WWaypointsBridge.clearLastAirShapeCache()
                WWaypointsBridge.saveWaypoints()
            }
        )

        tree.put(
            enumDropdown(
                field = "waypointMenuSortOrder",
                enumName = "WaypointMenuSortOrder",
                fallback = "PROXIMITY",
                labels = listOf("Proximity", "Alphabetical", "Custom"),
                name = "Sort order",
                description = "Applies on new waypoints and when opening the Waypoints menu.",
                tags = listOf(
                    "waypoint list", "list order", "sort waypoints", "sorting",
                    "alphabetical", "abc", "nearest", "closest", "proximity", "custom", "manual",
                ),
            )
        )

        tree.put(
            uiScaleDropdown().apply { subcategory = "UI" }
        )
        tree.put(
            slider(
                field = "menuBackgroundDarknessPercent",
                name = "Menu background darkness",
                description = "How dark the backdrop around menus is.",
                min = 0f,
                max = 100f,
                tags = listOf(
                    "dark", "darken", "dim", "backdrop", "background",
                    "brightness", "transparent", "transparency", "opacity", "see through",
                ),
            ).apply { subcategory = "UI" }
        )
        tree.put(
            enumDropdown(
                field = "uiAnimationMode",
                enumName = "UiAnimationMode",
                fallback = "ALL",
                labels = listOf("All", "Reduced", "None"),
                name = "Animations",
                description = "All enables full UI motion. Reduced stays non-disruptive and avoids motion that " +
                    "slows you down. None snaps UI changes.",
                tags = listOf(
                    "less animations", "reduced motion", "no motion", "disable animations",
                    "instant", "snap", "snappy", "motion",
                ),
            ).apply { subcategory = "UI" }
        )
    }

    private fun uiScaleDropdown(): Property<*> {
        // mod stores a float and normalizes it on read while OneConfig's dropdown writes back the selected
        // index so translate between the two here rather than exposing the raw float
        val prop = Properties.functional(
            Supplier { uiScaleIndex(WWaypointsBridge.float("waypointMenuGuiScale", 2f)) },
            Consumer<Int> { index -> WWaypointsBridge.set("waypointMenuGuiScale", UI_SCALES.getOrElse(index) { 2f }) },
            "wwaypoints.waypointMenuGuiScale",
            "UI Scale",
            "Menu size. The middle option adapts for lower resolutions.",
            Int::class.java,
        )
        prop.visualizer = Visualizer.DropdownVisualizer::class.java
        prop.addMetadata("options", listOf("Small", "Medium", "Large"))
        prop.category = "General"
        prop.addMetadata(
            "searchTags",
            listOf("big", "bigger", "large", "small", "smaller", "tiny", "medium", "menu size", "ui size", "gui scale", "text size"),
        )
        return prop
    }

    private val UI_SCALES = floatArrayOf(1f, 2f, 3f)

    /** Mirrors `UiScale.normalizeMenuReferenceScale` mapped onto the dropdown's option indices */
    private fun uiScaleIndex(scale: Float): Int = when {
        !scale.isFinite() -> 1
        scale <= 1.5f -> 0
        scale < 2.75f -> 1
        else -> 2
    }

    private fun currentFovDegrees(): Float? = runCatching {
        (Minecraft.getInstance()?.options?.fov()?.get() as? Number)?.toFloat()
    }.getOrNull()

    /**
     * The mod's Defaults page and the Label section of its composer
     *
     * 0.7.3 always keeps the single/clump/group defaults identical (`UNIFIED` mode) so these edit the
     * single-waypoint set and mirror it like the first-party page does
     */
    private fun buildDefaults(tree: Tree) {
        defaultLabelPreset()?.let { tree.put(it.defaults("Label")) }
        tree.put(
            floatSlider(
                field = "labelEyeOffset",
                name = "Label height",
                description = "How far above a waypoint its label floats, in blocks.",
                min = 1f,
                max = 3f,
                step = 0.1f,
                tags = listOf("above waypoint", "above head", "vertical", "height", "raise", "lower", "move label"),
            ).defaults("Label")
        )
        tree.put(labelFadeRange().defaults("Label"))
        tree.put(
            switch(
                field = "labelStylistAlwaysCenterHorizontally",
                name = "Always Center X",
                description = "Keeps label layers horizontally centred as you edit them.",
                tags = listOf("center", "centre", "align", "horizontal", "layout"),
            ).defaults("Label")
        )
        tree.put(
            switch(
                field = "labelStylistCenterWhenDone",
                name = "Center When Done",
                description = "Re-centres the label composition once you finish editing it.",
                tags = listOf("center", "centre", "align", "layout", "finish"),
            ).defaults("Label")
        )

        tree.put(
            defaultsSwitch(
                field = "blockThroughWalls",
                name = "Block through walls",
                description = "Only affects block rendering.",
                tags = listOf("through walls", "wall", "xray", "visible behind blocks", "occluded", "block render"),
            ).defaults("Block")
        )
        tree.put(
            inheritablePercent(
                field = "outlineOpacityPercent",
                globalField = "outlineOpacityPercent",
                name = "Outline opacity",
                description = "How solid a waypoint block's outline is. Inherits the global outline opacity when unset.",
                tags = listOf("outline", "transparent", "transparency", "opacity", "alpha", "border", "see through"),
            ).defaults("Block")
        )
        tree.put(
            inheritablePercent(
                field = "fillOpacityPercent",
                globalField = "fillOpacityPercent",
                name = "Fill opacity",
                description = "How solid a waypoint block's fill is. Inherits the global fill opacity when unset.",
                tags = listOf("fill", "transparent", "transparency", "opacity", "alpha", "inside", "see through"),
            ).defaults("Block")
        )
        tree.put(
            inheritableThickness().defaults("Block")
        )
    }

    /**
     * The preset new waypoints get plus the mod's synthetic "Label Off" choice which hides the label instead of
     * picking a preset
     *
     * Built from the presets that exist at startup like the keybind rows
     */
    private fun defaultLabelPreset(): Property<String>? {
        val presets = WWaypointsBridge.visiblePresets()
        if (presets.isEmpty()) return null
        val values = presets.map { it.first } + LABEL_OFF
        val labels = presets.map { it.second } + "Label Off"

        val prop = Properties.functional(
            Supplier {
                if (WWaypointsBridge.defaults("hiddenLabel") == true) LABEL_OFF
                else (WWaypointsBridge.defaults("defaultLabelPresetId") as? String)?.takeIf { it in values }
                    ?: values.first()
            },
            Consumer<String> { selected ->
                if (selected == LABEL_OFF) {
                    WWaypointsBridge.setDefaults("hiddenLabel", true)
                } else {
                    WWaypointsBridge.setDefaults("defaultLabelPresetId", selected)
                    WWaypointsBridge.setDefaults("hiddenLabel", false)
                }
            },
            "wwaypoints.defaults.defaultLabelPresetId",
            "Default Label Preset",
            "Preset applied to newly created items.",
            String::class.java,
        )
        prop.visualizer = Visualizer.DropdownVisualizer::class.java
        prop.addMetadata("options", labels)
        prop.addMetadata("optionValues", values)
        prop.addMetadata(
            "searchTags",
            listOf("default preset", "new waypoint", "label style", "appearance", "label off", "hidden", "default label"),
        )
        return prop
    }

    private const val LABEL_OFF = "oneconfig:label_off"

    /**
     * `outlineOpacityPercent`/`fillOpacityPercent` on the defaults are `int`s where any negative value means
     * inherit so `-1` is the sentinel and the global field of the same name supplies the inherited value
     */
    private fun inheritablePercent(
        field: String,
        globalField: String,
        name: String,
        description: String,
        tags: List<String>,
    ): Property<Int> {
        val prop = Properties.functional(
            Supplier { (WWaypointsBridge.defaults(field) as? Number)?.toInt() ?: -1 },
            Consumer<Int> { WWaypointsBridge.setDefaults(field, if (it < 0) -1 else it.coerceAtMost(100)) },
            "wwaypoints.defaults.$field",
            name,
            description,
            Int::class.java,
        )
        prop.visualizer = Visualizer.InheritableSliderVisualizer::class.java
        prop.addMetadata("min", 0f)
        prop.addMetadata("max", 100f)
        prop.addMetadata("step", 1f)
        prop.addMetadata("inheritSentinel", -1)
        prop.addMetadata("inheritedValue", Supplier { WWaypointsBridge.int(globalField) })
        prop.addMetadata("searchTags", tags)
        return prop
    }

    /**
     * `outlineThickness` on the defaults is a boxed `Float` where `null` means inherit so this property is
     * nullable and carries no explicit sentinel because writing null hands the value back to the global
     */
    @Suppress("UNCHECKED_CAST")
    private fun inheritableThickness(): Property<Float?> {
        val prop = Properties.functional(
            Supplier { (WWaypointsBridge.defaults("outlineThickness") as? Number)?.toFloat() },
            Consumer<Float?> { WWaypointsBridge.setDefaults("outlineThickness", it?.coerceIn(0f, 10f)) },
            "wwaypoints.defaults.outlineThickness",
            "Outline thickness",
            "How thick a waypoint block's outline is. Inherits the global outline thickness when unset.",
            Float::class.javaObjectType as Class<Float?>,
        )
        prop.visualizer = Visualizer.InheritableSliderVisualizer::class.java
        prop.addMetadata("min", 0f)
        prop.addMetadata("max", 10f)
        prop.addMetadata("step", 0.5f)
        prop.addMetadata("inheritedValue", Supplier { WWaypointsBridge.float("outlineThickness", 2f) })
        prop.addMetadata("searchTags", listOf("outline", "width", "border", "thickness", "thick", "thin", "block outline"))
        // compat snapshots cannot record a null so a snapshotted value would resurrect itself on the next
        // profile apply and undo inherit
        // the opacity rows are unaffected because their -1 sentinel snapshots fine
        prop.addMetadata(CompatSnapshots.NO_SNAPSHOT_META, true)
        return prop
    }

    private fun defaultsSwitch(field: String, name: String, description: String, tags: List<String>): Property<Boolean> {
        val prop = Properties.functional(
            Supplier { WWaypointsBridge.defaults(field) as? Boolean ?: false },
            Consumer<Boolean> { WWaypointsBridge.setDefaults(field, it) },
            "wwaypoints.defaults.$field",
            name,
            description,
            Boolean::class.java,
        )
        prop.visualizer = Visualizer.SwitchVisualizer::class.java
        prop.addMetadata("searchTags", tags)
        return prop
    }

    /**
     * The two `labelFade*Blocks` fields as a single start/end pair
     *
     * They are nullable on disk and the mod reads a missing end of the range as `0` so that is what an unset
     * field surfaces as here
     */
    private fun labelFadeRange(): Property<FloatArray> {
        val prop = Properties.functional(
            Supplier {
                floatArrayOf(
                    WWaypointsBridge.get<Number>("labelFadeStartBlocks")?.toFloat() ?: 0f,
                    WWaypointsBridge.get<Number>("labelFadeEndBlocks")?.toFloat() ?: 0f,
                )
            },
            Consumer<FloatArray> { range ->
                WWaypointsBridge.set("labelFadeStartBlocks", range.getOrElse(0) { 0f })
                WWaypointsBridge.set("labelFadeEndBlocks", range.getOrElse(1) { 0f })
            },
            "wwaypoints.labelFadeRange",
            "Fade Range (Blocks)",
            "Fade labels out as you get close. Set where the fade starts and where it finishes.",
            FloatArray::class.java,
        )
        prop.visualizer = Visualizer.RangeSliderVisualizer::class.java
        prop.addMetadata("min", 0f)
        prop.addMetadata("max", 10f)
        prop.addMetadata("step", 0.1f)
        prop.addMetadata("searchTags", listOf("fade", "close", "near", "disappear", "distance", "hide near", "range", "visibility"))
        return prop
    }

    private fun buildIntegrations(tree: Tree) {
        val hopliteEnabled = switch(
            field = "hopliteEnabled",
            name = "Enable Hoplite features",
            description = "Master switch for Hoplite-related waypoints and tagging.",
            tags = HOPLITE_TAGS,
        )
        hopliteEnabled.category = "Integrations"
        hopliteEnabled.subcategory = "Hoplite"
        tree.put(hopliteEnabled)

        tree.put(
            switch(
                field = "hopliteAutoSupplyDropWaypoints",
                name = "Auto supply drop waypoints",
                description = "Automatically creates supply drop waypoints.",
                tags = HOPLITE_TAGS + listOf("supply drop", "crate", "airdrop", "loot"),
            ).integrations("Hoplite").requires(hopliteEnabled)
        )
        tree.put(
            linkOut(
                id = "hoplite_supply_drop_appearance",
                name = "Supply drop appearance",
                description = "Style the label and block used for automatic supply drop waypoints.",
                text = "Configure",
                tags = HOPLITE_TAGS + listOf("supply drop", "crate", "airdrop"),
            ) { WWaypointsBridge.specialLabelStylist("SUPPLY_DROP", currentScreen()) }
                .integrations("Hoplite").requires(hopliteEnabled)
        )

        tree.put(
            switch(
                field = "hopliteAutoPickCreatesWaypoint",
                name = "Auto pick waypoint",
                description = "Creates a waypoint when you auto-pick an item.",
                tags = HOPLITE_TAGS + listOf("auto pick", "autopick", "picked item"),
            ).integrations("Hoplite").requires(hopliteEnabled)
        )
        tree.put(
            linkOut(
                id = "hoplite_auto_pick_appearance",
                name = "Auto pick appearance",
                description = "Style the label and block used for auto-pick waypoints.",
                text = "Configure",
                tags = HOPLITE_TAGS + listOf("auto pick", "autopick"),
            ) { WWaypointsBridge.specialLabelStylist("AUTO_PICK", currentScreen()) }
                .integrations("Hoplite").requires(hopliteEnabled)
        )

        tree.put(
            switch(
                field = "signGuiPopup",
                name = "Sign GUI popup",
                description = "When OFF, placing signs will not open the sign editor.",
                tags = SIGN_TAGS,
            ).integrations("Extras")
        )
        tree.put(
            switch(
                field = "showHiddenPotionEffectsOnHud",
                name = "Show hidden potion effects (HUD)",
                description = "When ON, effects with hideParticles=true still show on the HUD.",
                tags = listOf("hidden potion effects", "hud", "status effects", "particles", "beacon"),
            ).integrations("Extras")
        )

        buildToggleSneak(tree)
    }

    /**
     * Native reimplementation of the mod's `ToggleSneakConfigScreen` which is a plain popup of scalar rows
     *
     * The two `Boolean` fields are tri-state on disk and the mod reads them with `Boolean.TRUE.equals` after
     * load so `null` is surfaced as off and only concrete values are ever written back
     */
    private fun buildToggleSneak(tree: Tree) {
        tree.put(
            switch(
                field = "toggleSneakStaySneakedInContainers",
                name = "Stay Sneaked in Containers",
                description = "Keeps Toggle Sneak active while inventory and container screens are open.",
                tags = SNEAK_TAGS + listOf("container", "inventory", "chest"),
            ).integrations("Toggle Sneak")
        )
        tree.put(
            switch(
                field = "toggleSneakBetterInteractions",
                name = "Better Sneak Interactions",
                description = "Lets container clicks work with items in hand and waterlogs slabs while toggle-sneaking.",
                tags = SNEAK_TAGS + listOf("interactions", "waterlog", "slab"),
            ).integrations("Toggle Sneak")
        )
        tree.put(
            switch(
                field = "sneakKeybindOverrides",
                name = "Sneak Keybind Overrides",
                description = "While Toggle Sneak is enabled, tapping Sneak will cancel Toggle Sneak when you release it.",
                tags = SNEAK_TAGS + listOf("override", "cancel", "release", "tap"),
            ).integrations("Toggle Sneak")
        )
        tree.put(
            enumDropdown(
                field = "toggleSneakIndicatorMode",
                enumName = "ToggleSneakIndicatorMode",
                fallback = "OFF",
                labels = listOf("Off", "When Crawling", "Always"),
                name = "Toggle Sneak Indicator",
                description = "Show a persistent \"Toggle Sneak Enabled\" indicator above the hotbar.",
                tags = SNEAK_TAGS + listOf("indicator", "crawl", "crawling", "hotbar"),
            ).onChange { mode ->
                // mod keeps this legacy flag in step with the mode because the crawl renderer still reads it
                WWaypointsBridge.set("crawlToggleSneakIndicator", (mode as? Enum<*>)?.name != "OFF")
            }.integrations("Toggle Sneak")
        )
    }

    private fun buildControls(tree: Tree) {
        WWaypointsBridge.syncPresetCreateKeybindings()

        for ((presetId, presetName) in WWaypointsBridge.myPresets()) {
            val mapping = WWaypointsBridge.presetCreateKey(presetId) ?: continue
            tree.put(
                keybind(
                    id = "preset_create.$presetId",
                    mapping = mapping,
                    name = "$presetName Waypoint",
                    description = "Creates a waypoint using the $presetName preset.",
                    subcategory = "Create wWaypoint",
                    tags = listOf("create waypoint", "new waypoint", "preset", presetName),
                )
            )
        }

        tree.put(
            keybindFor(
                accessor = "getOpenWaypointMenuKey",
                id = "open_menu",
                name = "Open waypoint menu",
                description = "Opens the waypoint list/menu.",
                subcategory = "Menus & Utility",
                tags = listOf("waypoint menu", "waypoint list", "open waypoints", "waypoint screen"),
            )
        )
        tree.put(
            keybindFor(
                accessor = "getOpenWaypointSettingsKey",
                id = "open_settings",
                name = "Open settings",
                description = "Opens wWaypoints settings.",
                subcategory = "Menus & Utility",
                tags = listOf("settings", "options", "config", "configure"),
            )
        )
        tree.put(
            keybindFor(
                accessor = "getClearUnlockedKey",
                id = "clear_unlocked",
                name = "Clear unlocked",
                description = "Prompts to clear unlocked waypoints.",
                subcategory = "Menus & Utility",
                tags = listOf("delete unlocked", "remove unlocked", "clear waypoints", "purge"),
            )
        )

        tree.put(
            keybindFor(
                accessor = "getHideWaypointsKey",
                id = "hide_waypoints",
                name = "Hide waypoints",
                description = "Cycles through waypoint visibility modes.",
                subcategory = "Hide Waypoints",
                tags = HIDE_TAGS,
            )
        )
        tree.put(hideWaypointCycle())

        tree.put(
            keybindFor(
                accessor = "getToggleSneakKey",
                id = "toggle_sneak",
                name = "Toggle Sneak",
                description = "Toggles sneak on or off.",
                subcategory = "Extras",
                tags = SNEAK_TAGS,
            )
        )
        tree.put(
            keybindFor(
                accessor = "getSignGuiPopupToggleKey",
                id = "sign_gui_popup_toggle",
                name = "Sign GUI popup",
                description = "Toggles the Sign GUI popup feature on or off.",
                subcategory = "Extras",
                tags = SIGN_TAGS,
            )
        )
    }

    /**
     * The visibility modes the hide-waypoints keybind steps through
     *
     * Every edit is round-tripped through the mod's own `HideWaypointCycle.normalize` which owns the rules
     * (fixed leading "Show All" / no duplicates / clamped distances / length between two and five)
     */
    private fun hideWaypointCycle(): Property<IntArray> {
        val prop = Properties.functional(
            Supplier { WWaypointsBridge.normalizedCycle(WWaypointsBridge.get<IntArray>("hideWaypointCycleThresholds")) },
            Consumer<IntArray> { WWaypointsBridge.set("hideWaypointCycleThresholds", WWaypointsBridge.normalizedCycle(it)) },
            "wwaypoints.hideWaypointCycleThresholds",
            "Cycle Options",
            "Press the keybind to cycle through these options from left to right.",
            IntArray::class.java,
        )
        prop.visualizer = Visualizer.NumberChainVisualizer::class.java
        prop.addMetadata("min", 0f)
        prop.addMetadata("max", WWaypointsBridge.cycleMaxBlocks().toFloat())
        prop.addMetadata("maxEntries", WWaypointsBridge.cycleMaxEntries())
        prop.addMetadata("unit", WWaypointsBridge.CYCLE_UNIT)
        // index 0 is always "Show All" and the mod refuses to move edit or drop it
        prop.addMetadata("lockedLeading", 1)
        prop.addMetadata("entryLabel", Function<Number, String> { WWaypointsBridge.cycleLabel(it.toInt()) })
        prop.addMetadata("nextValue", Function<List<Number>, Number> { current ->
            WWaypointsBridge.nextCycleThreshold(current.map { it.toInt() }.toIntArray())
        })
        prop.category = "Controls"
        prop.subcategory = "Hide Waypoints"
        prop.addMetadata("searchTags", HIDE_TAGS)
        return prop
    }

    private fun buildData(tree: Tree) {
        tree.put(
            linkOut(
                id = "import_export",
                name = "Import / Export wWaypoints",
                // importing needs the mod's merge/override decision flow and exporting every world needs its
                // private on-disk layout walk so both stay on the first-party screen
                description = "Import wWaypoints data, export every world, or use the clipboard.",
                text = "Open",
                tags = TRANSFER_TAGS,
            ) { WWaypointsBridge.screen(WWaypointsBridge.IMPORT_EXPORT_SCREEN, currentScreen()) }
                .data("Waypoints")
        )
        tree.put(
            linkOut(
                id = "import_from_world",
                name = "Import From Another World",
                description = "Import waypoints from a different world/server into the current one.",
                text = "Open",
                tags = TRANSFER_TAGS + listOf("world", "server", "realm"),
            ) { WWaypointsBridge.screen(WWaypointsBridge.IMPORT_FROM_WORLD_SCREEN, currentScreen()) }
                .data("Waypoints")
        )
        tree.put(
            linkOut(
                id = "import_xaero",
                name = "Import from Xaero's Minimap",
                description = "Imports Xaero minimap waypoints (xaero/minimap or mods/XaeroWaypoints).",
                text = "Open",
                tags = TRANSFER_TAGS + listOf("xaero", "minimap", "convert", "old waypoints"),
            ) { WWaypointsBridge.screen(WWaypointsBridge.XAERO_IMPORT_SCREEN, currentScreen()) }
                .data("Waypoints")
        )

        tree.put(
            action(
                id = "export_world",
                name = "Export this world",
                description = "Saves the current world's waypoints to a .wwaypoint file.",
                text = "Save",
                tags = TRANSFER_TAGS + listOf("wwaypoint", "world", "backup"),
            ) { exportCurrentWorld() }
                .data("Waypoints")
        )

        tree.put(
            action(
                id = "export_settings",
                name = "Export settings",
                description = "Saves settings, defaults, pinned icons, and Waypoint Presets to a .wsettings file.",
                text = "Save",
                tags = TRANSFER_TAGS + listOf("wsettings", "backup", "settings"),
            ) { exportSettings() }
                .data("Settings")
        )
        tree.put(
            action(
                id = "import_settings",
                name = "Import settings",
                description = "Loads settings, defaults, pinned icons, and Waypoint Presets from a .wsettings file.",
                text = "Open",
                tags = TRANSFER_TAGS + listOf("wsettings", "restore", "settings"),
            ) { importSettings() }
                .data("Settings")
        )
        tree.put(
            action(
                id = "reset_settings",
                name = "Reset All Settings",
                description = "Resets all wWaypoints settings to defaults.",
                text = "Reset All",
                tags = listOf("reset", "defaults", "restore defaults", "start over"),
            ) { resetAllSettings() }
                .data("Settings")
        )
        tree.put(
            linkOut(
                id = "factory_reset",
                name = "Factory Reset wWaypoints",
                description = "Deletes all wWaypoints save data, settings, presets, templates, custom icons, and backups.",
                text = "Open",
                tags = listOf("wipe", "delete everything", "delete all", "start over", "uninstall", "clean"),
            ) { WWaypointsBridge.settingsScreen(currentScreen(), page = "IMPORT", importSubTab = 1) }
                .data("Settings")
        )
    }

    /**
     * The mod builds this envelope in a private method so the row is limited to the current world
     *
     * The all-worlds variant would mean duplicating its on-disk layout walk which breaks silently on updates
     */
    private fun exportCurrentWorld() {
        val json = WWaypointsBridge.exportCurrentWorldJson(System.currentTimeMillis())
        if (json == null) {
            WWaypointsBridge.toast("Join a world before exporting waypoints.")
            return
        }
        val suggested = "${WWaypointsBridge.currentWorldId() ?: "wwaypoints"}.wwaypoint"
        saveFileInBackground("Export wWaypoints", suggested, WWAYPOINT_FILTER, "wWaypoints data") { path ->
            Files.writeString(path, json)
            WWaypointsBridge.toast("Exported waypoints to ${path.fileName}.")
        }
    }

    private fun exportSettings() {
        val json = WWaypointsBridge.settingsJson()
        if (json == null) {
            WWaypointsBridge.toast("Could not read wWaypoints settings.")
            return
        }
        saveFileInBackground("Export settings", "wwaypoints.wsettings", WSETTINGS_FILTER, "wWaypoints settings") { path ->
            Files.writeString(path, json)
            WWaypointsBridge.toast("Exported settings to ${path.fileName}.")
        }
    }

    private fun importSettings() {
        openFileInBackground("Import settings", WSETTINGS_FILTER, "wWaypoints settings") { path ->
            val json = Files.readString(path)
            onClientThread {
                if (WWaypointsBridge.applySettingsJson(json)) {
                    WWaypointsBridge.saveConfig()
                    WWaypointsBridge.toast("Imported settings from ${path.fileName}.")
                } else {
                    WWaypointsBridge.toast("That file is not a wWaypoints settings export.")
                }
            }
        }
    }

    private fun resetAllSettings() {
        inBackground {
            val confirmed = TinyFdApi.getInstance().showMessageBox(
                "Reset All Settings",
                "Reset every wWaypoints setting to its default?\nWaypoints and presets are not affected.",
                TinyFdApi.YES_NO_DIALOG,
                TinyFdApi.WARNING_ICON,
                false,
            )
            if (!confirmed) return@inBackground
            onClientThread {
                WWaypointsBridge.resetConfigToDefaults()
                WWaypointsBridge.saveConfig()
                WWaypointsBridge.toast("wWaypoints settings reset to defaults.")
            }
        }
    }

    private fun importTemplate() {
        openFileInBackground("Import template", WTEMPLATE_FILTER, "wWaypoints template") { path ->
            onClientThread {
                val name = WWaypointsBridge.importTemplateFile(path)
                if (name != null) WWaypointsBridge.toast("Imported template \"$name\".")
                else WWaypointsBridge.toast("That file is not a wWaypoints template.")
            }
        }
    }

    private val WWAYPOINT_FILTER = arrayOf("*.wwaypoint")
    private val WSETTINGS_FILTER = arrayOf("*.wsettings")
    private val WTEMPLATE_FILTER = arrayOf("*.wtemplate")

    /**
     * Button handlers run on the render thread where a native dialog would freeze the game so every dialog is
     * opened from a short-lived worker and the result marshalled back with [onClientThread]
     */
    private fun inBackground(block: () -> Unit) {
        Thread({
            runCatching(block).onFailure { LOGGER.warn("wWaypoints file action failed", it) }
        }, "oneconfig-wwaypoints-dialog").apply { isDaemon = true }.start()
    }

    private fun onClientThread(block: () -> Unit) {
        val client = Minecraft.getInstance()
        if (client != null) client.execute { runCatching(block).onFailure { LOGGER.warn("wWaypoints action failed", it) } }
        else runCatching(block).onFailure { LOGGER.warn("wWaypoints action failed", it) }
    }

    private fun saveFileInBackground(
        title: String,
        suggestedName: String,
        filters: Array<String>,
        filterName: String,
        write: (Path) -> Unit,
    ) = inBackground {
        val chosen = TinyFdApi.getInstance().openSaveSelector(title, suggestedName, filters, filterName) ?: return@inBackground
        val target = if (chosen.fileName.toString().contains('.')) chosen else chosen.resolveSibling("${chosen.fileName}${filters[0].removePrefix("*")}")
        write(target)
    }

    private fun openFileInBackground(
        title: String,
        filters: Array<String>,
        filterName: String,
        read: (Path) -> Unit,
    ) = inBackground {
        val chosen = TinyFdApi.getInstance().openFileSelector(title, null, filters, filterName) ?: return@inBackground
        read(chosen)
    }

    private fun buildPresets(tree: Tree) {
        tree.put(
            linkOut(
                id = "preset_library",
                name = "Manage Presets",
                description = "Create, rename, reorder, and style the label presets new waypoints use.",
                text = "Open",
                tags = listOf("preset", "presets", "library", "label", "stylist", "composer", "appearance"),
            ) { WWaypointsBridge.screen(WWaypointsBridge.PRESET_LIBRARY_SCREEN, currentScreen()) }
                .apply { category = "Presets"; subcategory = "Presets" }
        )
        tree.put(
            linkOut(
                id = "templates",
                name = "Manage Templates",
                description = "Apply, rename, reorder, and delete saved waypoint layout templates.",
                text = "Open",
                tags = listOf("template", "templates", "layout", "group", "structure"),
            ) { WWaypointsBridge.settingsScreen(currentScreen(), page = "TEMPLATES") }
                .apply { category = "Presets"; subcategory = "Templates" }
        )
        tree.put(
            action(
                id = "import_template",
                name = "Import Template",
                description = "Adds a waypoint layout template from a .wtemplate file.",
                text = "Open",
                tags = TRANSFER_TAGS + listOf("template", "templates", "layout", "structure"),
            ) { importTemplate() }
                .apply { category = "Presets"; subcategory = "Templates" }
        )
    }

    private fun switch(field: String, name: String, description: String, tags: List<String>): Property<Boolean> {
        val prop = Properties.functional(
            Supplier { WWaypointsBridge.bool(field) },
            Consumer<Boolean> { WWaypointsBridge.set(field, it) },
            "wwaypoints.$field",
            name,
            description,
            Boolean::class.java,
        )
        prop.visualizer = Visualizer.SwitchVisualizer::class.java
        prop.category = "General"
        prop.addMetadata("searchTags", tags)
        (WWaypointsBridge.configDefault(field) as? Boolean)?.let { prop.addMetadata("default", it) }
        return prop
    }

    private fun slider(
        field: String,
        name: String,
        description: String,
        min: Float,
        max: Float,
        tags: List<String>,
    ): Property<Int> {
        val prop = Properties.functional(
            Supplier { WWaypointsBridge.int(field) },
            Consumer<Int> { WWaypointsBridge.set(field, it.coerceIn(min.toInt(), max.toInt())) },
            "wwaypoints.$field",
            name,
            description,
            Int::class.java,
        )
        prop.visualizer = Visualizer.SliderVisualizer::class.java
        prop.addMetadata("min", min)
        prop.addMetadata("max", max)
        prop.addMetadata("step", 1f)
        prop.category = "General"
        prop.addMetadata("searchTags", tags)
        (WWaypointsBridge.configDefault(field) as? Number)?.let { prop.addMetadata("default", it.toInt()) }
        return prop
    }

    private fun floatSlider(
        field: String,
        name: String,
        description: String,
        min: Float,
        max: Float,
        step: Float,
        tags: List<String>,
    ): Property<Float> {
        val prop = Properties.functional(
            Supplier { WWaypointsBridge.float(field) },
            Consumer<Float> { WWaypointsBridge.set(field, it.coerceIn(min, max)) },
            "wwaypoints.$field",
            name,
            description,
            Float::class.java,
        )
        prop.visualizer = Visualizer.SliderVisualizer::class.java
        prop.addMetadata("min", min)
        prop.addMetadata("max", max)
        prop.addMetadata("step", step)
        prop.category = "General"
        prop.addMetadata("searchTags", tags)
        (WWaypointsBridge.configDefault(field) as? Number)?.let { prop.addMetadata("default", it.toFloat()) }
        return prop
    }

    /**
     * A dropdown over one of the mod's nested config enums
     *
     * [fallback] is the constant the mod substitutes when the stored value is `null` and [labels] must be in
     * the enum's declaration order
     */
    private fun enumDropdown(
        field: String,
        enumName: String,
        fallback: String,
        labels: List<String>,
        name: String,
        description: String,
        tags: List<String>,
    ): Property<Any> {
        val enumClass = WWaypointsBridge.enumClass(enumName)
            ?: error("wWaypoints enum ModConfig.$enumName is missing")
        val fallbackConstant = WWaypointsBridge.enumConstant(enumName, fallback)
            ?: error("wWaypoints enum constant ModConfig.$enumName.$fallback is missing")
        @Suppress("UNCHECKED_CAST")
        val prop = Properties.functional(
            Supplier { WWaypointsBridge.get<Any>(field) ?: fallbackConstant },
            Consumer<Any> { WWaypointsBridge.set(field, it) },
            "wwaypoints.$field",
            name,
            description,
            enumClass as Class<Any>,
        )
        prop.visualizer = Visualizer.DropdownVisualizer::class.java
        prop.addMetadata("options", labels)
        prop.category = "General"
        prop.addMetadata("searchTags", tags)
        prop.addMetadata("default", WWaypointsBridge.configDefault(field)?.takeIf(enumClass::isInstance) ?: fallbackConstant)
        return prop
    }

    private fun linkOut(
        id: String,
        name: String,
        description: String,
        text: String,
        tags: List<String>,
        screen: () -> Screen?,
    ): Property<Void> = action(id, name, description, text, tags) {
        screen()?.let { Platform.screen().display(it) }
    }

    /** A button row that runs [run] rather than handing off to one of the mod's own screens */
    private fun action(
        id: String,
        name: String,
        description: String,
        text: String,
        tags: List<String>,
        run: () -> Unit,
    ): Property<Void> {
        val prop = Properties.dummy("wwaypoints.$id", name, description)
        prop.visualizer = Visualizer.ButtonVisualizer::class.java
        prop.addMetadata("text", text)
        prop.addMetadata("runnable", Runnable { runCatching(run).onFailure { LOGGER.warn("wWaypoints action '{}' failed", id, it) } })
        prop.category = "General"
        prop.addMetadata("searchTags", tags)
        return prop
    }

    private fun keybindFor(
        accessor: String,
        id: String,
        name: String,
        description: String,
        subcategory: String,
        tags: List<String>,
    ): Property<*> {
        val mapping = WWaypointsBridge.keyMapping(accessor)
        if (mapping == null) {
            return unavailableKeybind(id, name, description, subcategory, tags)
        }
        return keybind(id, mapping, name, description, subcategory, tags)
    }

    /**
     * Bridges one of the mod's vanilla [KeyMapping]s onto OneConfig's keybind row
     *
     * The value never lives in OneConfig
     *
     * Reads convert the mapping's current key and writes go through the mod's own rebind entry point so its
     * conflict handling and `options.txt` persistence still run
     */
    private fun keybind(
        id: String,
        mapping: KeyMapping,
        name: String,
        description: String,
        subcategory: String,
        tags: List<String>,
    ): Property<OneConfigKeybind> {
        val prop = Properties.functional(
            Supplier { mapping.toOneConfigKeybind() },
            Consumer<OneConfigKeybind> { WWaypointsBridge.rebindKeyMapping(mapping, it.toInputKey()) },
            "wwaypoints.key.$id",
            name,
            description,
            OneConfigKeybind::class.java,
        )
        prop.visualizer = Visualizer.KeybindVisualizer::class.java
        prop.category = "Controls"
        prop.subcategory = subcategory
        prop.addMetadata("searchTags", tags)
        // mapping is already a real vanilla keybind so it must not be mirrored back into the Controls menu
        // and Minecraft keybinds are profile-managed by MinecraftKeybindProfiles rather than compat snapshots
        prop.addMetadata(MinecraftKeybindRegistrar.NO_MIRROR_METADATA, true)
        prop.addMetadata(CompatSnapshots.NO_SNAPSHOT_META, true)
        return prop
    }

    /** Shown when the mod has not registered a mapping yet because it only picks up new preset binds on restart */
    private fun unavailableKeybind(
        id: String,
        name: String,
        description: String,
        subcategory: String,
        tags: List<String>,
    ): Property<Void> {
        val prop = Properties.dummy("wwaypoints.key.$id", name, description)
        prop.visualizer = Visualizer.InfoVisualizer::class.java
        prop.addMetadata("type", "warning")
        prop.category = "Controls"
        prop.subcategory = subcategory
        prop.addMetadata("searchTags", tags)
        prop.addMetadata(CompatSnapshots.NO_SNAPSHOT_META, true)
        return prop
    }

    private fun KeyMapping.toOneConfigKeybind(): OneConfigKeybind {
        val key = runCatching { InputConstants.getKey(saveString()) }.getOrDefault(InputConstants.UNKNOWN)
        return when (key.type) {
            //~ if < 26.3 'Type.KEYBOARD' -> 'Type.KEYSYM'
            InputConstants.Type.KEYSYM if key.value > 0 ->
                OneConfigKeybind(intArrayOf(key.value), null, KeyModifiers.NONE, 0L) { true }
            //~ if < 26.3 'key.value > 0' -> 'key.value >= 0'
            InputConstants.Type.MOUSE if key.value >= 0 ->
                OneConfigKeybind(null, intArrayOf(key.value), KeyModifiers.NONE, 0L) { true }
            else -> OneConfigKeybind(null, null, KeyModifiers.NONE, 0L) { true }
        }
    }

    /** wWaypoints mappings are single-key so only the primary input of a combo survives the round trip */
    private fun OneConfigKeybind?.toInputKey(): InputConstants.Key {
        if (this == null || !isBound) return InputConstants.UNKNOWN
        //~ if < 26.3 'it > 0' -> 'it >= 0'
        mouseBtns?.firstOrNull { it >= 0 }?.let { return MinecraftKeybindCodec.mouse(it) }
        keyCodes?.firstOrNull { it > 0 }?.let { return MinecraftKeybindCodec.keysym(it) }
        return InputConstants.UNKNOWN
    }

    private fun currentScreen(): Screen? = runCatching { Platform.screen().current<Screen>() }.getOrNull()

    private fun <T> Property<T>.onChange(action: (T?) -> Unit): Property<T> = apply {
        addCallback { value ->
            runCatching { action(value) }.onFailure { LOGGER.warn("wWaypoints change hook failed for '{}'", id, it) }
            false
        }
    }

    private fun <T> Property<T>.integrations(subcategory: String): Property<T> = apply {
        category = "Integrations"
        this.subcategory = subcategory
    }

    private fun <T> Property<T>.data(subcategory: String): Property<T> = apply {
        category = "Data"
        this.subcategory = subcategory
    }

    private fun <T> Property<T>.defaults(subcategory: String): Property<T> = apply {
        category = "Defaults"
        this.subcategory = subcategory
    }

    /** Greys the row out rather than hiding it while [condition] is off matching the first-party screen */
    private fun <T> Property<T>.requires(condition: Property<Boolean>): Property<T> =
        apply { addDisplayCondition(condition, false) }

    // search vocabulary mirroring the mod's own SettingsSearchVocabulary
    private val DEATH_TAGS = listOf(
        "die", "died", "dead", "killed", "grave", "corpse", "death marker",
        "last death", "recover items", "respawn",
    )

    private val HOPLITE_TAGS = listOf("hoplite", "uhc", "event", "server", "integration")

    private val SNEAK_TAGS = listOf(
        "shift", "crouch", "crouching", "sneak", "sneaking", "toggle sneak", "hold sneak",
    )

    private val SIGN_TAGS = listOf(
        "sign", "signs", "sign popup", "sign editor", "edit sign", "placing signs", "pop up",
    )

    private val HIDE_TAGS = listOf(
        "hide", "show", "visibility", "visible", "invisible", "waypoint visibility",
        "visibility mode", "cycle", "labels", "blocks",
    )

    private val TRANSFER_TAGS = listOf(
        "import", "export", "backup", "restore", "share", "send", "receive",
        "copy", "paste", "clipboard", "file", "transfer", "migrate",
    )
}

//? }
