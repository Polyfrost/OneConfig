package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudGroup
import org.polyfrost.oneconfig.api.hud.v1.HudGroups
import org.polyfrost.oneconfig.api.ui.v1.components.NumberField
import org.polyfrost.oneconfig.api.ui.v1.components.RadioGroup
import org.polyfrost.oneconfig.api.ui.v1.components.Switch
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.settings.Dropdown
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.hud.screens.draggableHuds
import org.polyfrost.oneconfig.internal.ui.screens.ConfigListRow
import org.polyfrost.oneconfig.internal.ui.screens.HudCategoryList
import org.polyfrost.oneconfig.internal.ui.screens.HudOptionRow
import org.polyfrost.oneconfig.internal.ui.screens.rememberDisplay
import org.polyfrost.oneconfig.internal.ui.search.CategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.ConfigListEntry
import org.polyfrost.oneconfig.internal.ui.search.SettingNode
import org.polyfrost.oneconfig.internal.ui.search.SubcategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.nodeGroup
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import java.util.function.Consumer

/** Counts edits so rows read the HUDs again after one, since plain HUD fields cannot tell compose they changed */
private class SharedEdits {
    private var count by mutableIntStateOf(0)

    fun observe() {
        count
    }

    fun bump() {
        count++
    }
}

private fun SharedValue<Boolean>.isOn(): Boolean = (this as? SharedValue.Single)?.value == true

private fun SharedValue<*>.isMixed(): Boolean = this is SharedValue.Mixed

/**
 * What an edit of the shared tabs is written to
 *
 * A setting the HUDs hold differently reads as mixed until it is changed, which sets it for every HUD
 */
@Composable
private fun SharedContent(huds: Collection<Hud>, content: @Composable (List<Hud>, SharedEdits) -> Unit) {
    val list = huds.toList()
    if (list.isEmpty()) return

    LaunchedEffect(list) { list.forEach(::repairHudStaticSize) }

    HudSettingsContent(list) {
        val edits = remember { SharedEdits() }
        content(list, edits)
    }
}

/** The Layout tab for [huds], where a [group] has the lock and scale of its own */
@Composable
fun SharedLayout(huds: Collection<Hud>, query: String, group: HudGroup? = null) {
    SharedContent(huds) { list, _ ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PositionAndScale(list, query, group)
            SharedPadding(list, query)
        }
    }
}

private val SINGLE_BACKGROUND_HIDDEN = setOf("merge")

/** The Style tab for [huds] */
@Composable
fun SharedStyle(huds: Collection<Hud>, query: String, group: HudGroup? = null) {
    SharedContent(huds) { list, edits ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SharedCard(SharedSection.Text, list, query, edits)
            SharedCard(SharedSection.Background, list, query, edits, group)
        }
    }
}

/** The Settings tab for [huds], which is when they show and the options mods defined that all of them have */
@Composable
fun SharedSettings(huds: Collection<Hud>, query: String) {
    SharedContent(huds) { list, edits ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SharedPropertyRows(list, query, edits)
            SharedCard(SharedSection.Visibility, list, query, edits)
        }
    }
}

@Composable
private fun PositionAndScale(huds: List<Hud>, query: String, group: HudGroup?) {
    val locked = group?.locked ?: resolveShared(huds) { it.locked }.isOn()
    val movable = draggableHuds(huds).ifEmpty { huds }
    val scalable = HudGroups.scalable(movable).isNotEmpty()
    val mixedLock = group == null && resolveShared(huds) { it.locked }.isMixed()
    CardSection(
        if (scalable) "Position & Scale" else "Position", query,
        "Lock position", *if (scalable) arrayOf("Scale", "Match scale") else emptyArray(),
    ) { col1 ->
        HudPositionFields(huds.first(), col1) {
            if (scalable) {
                LabeledControl("Scale") {
                    NumberField(
                        HudGroups.scaleOf(movable), { HudGroups.setScale(movable, it) },
                        range = HudGroups.scaleRange(movable).let { it.start..it.endInclusive }, step = 0.25f, roundToDecimals = false, fractionDigits = 2,
                        width = GridCol3, unit = "x", enabled = !locked, fontSize = 13.sp,
                    )
                }
            }
        }
        SwitchRow("Lock position", locked, mixedLock) {
            if (group != null) HudGroups.setLocked(group, it) else HudGroups.setLocked(huds, it)
        }
        if (!locked && huds.any { it.locked } && group != null) {
            Text(
                "Some HUDs are locked on their own and hold still inside the group.",
                color = LocalTheme.current.textColorSecondary,
                fontSize = 12.sp,
            )
        }
        if (scalable && !locked) {
            val scale = HudGroups.scaleOf(movable)
            if (!HudGroups.hasUniformScale(movable)) {
                GroupRow("Set every HUD to %.2fx".format(scale), "align") { HudGroups.matchScale(movable, scale) }
            }
        }
    }
}

/** The options of [section] that every HUD has, laid out in the panel grid with switches and segmented choices on rows of their own */
@Composable
private fun SharedCard(section: SharedSection, huds: List<Hud>, query: String, edits: SharedEdits, group: HudGroup? = null) {
    val options = SharedOptions.shared(section, huds)
    if (options.isEmpty()) return
    val cardSwitch = options.firstOrNull { it.id == "showBackground" } as? SharedToggle
    edits.observe()
    val cardSwitchValue = cardSwitch?.resolve(huds)
    val groupBackgroundLabel = hudPanelText("group_background", "One background for the group")
    val singleBackground = section == SharedSection.Background && group != null && HudGroups.hasSingleBackground(group)
    val backgroundTakenOver = group != null && HudGroups.isBackgroundTakenOver(group)
    val canShareBackground = group != null && group.huds.all { mergesBackground(it) && !it.borderEnabled }
    CardSection(
        section.title, query, *options.map { it.label }.toTypedArray(),
        *if (section == SharedSection.Background && group != null) arrayOf(groupBackgroundLabel) else emptyArray(),
        checked = cardSwitchValue?.isOn(),
        mixed = cardSwitchValue?.isMixed() == true,
        onCheckedChange = { cardSwitch?.apply(huds, it); edits.bump() },
    ) { col1 ->
        val widths = listOf(col1, GridCol2, GridCol3)
        val cells = ArrayList<SharedOption>()

        @Composable
        fun flush() {
            for (row in cells.chunked(3)) {
                GridRow(
                    col1,
                    first = { SharedCell(row[0], huds, edits, widths[0]) },
                    second = { row.getOrNull(1)?.let { SharedCell(it, huds, edits, widths[1]) } },
                    third = { row.getOrNull(2)?.let { SharedCell(it, huds, edits, widths[2]) } },
                )
            }
            cells.clear()
        }

        if (section == SharedSection.Background && group != null) {
            SwitchRow(groupBackgroundLabel, singleBackground, false, enabled = !backgroundTakenOver && (singleBackground || canShareBackground)) {
                HudGroups.setSingleBackground(group, it)
                edits.bump()
            }
        }

        for (option in options) {
            if (option === cardSwitch || !SharedOptions.isShown(option, huds)) continue
            if (singleBackground && option.id in SINGLE_BACKGROUND_HIDDEN) continue
            if (singleBackground && option.id == "borderEnabled" && huds.none { it.borderEnabled }) continue
            when {
                option is SharedToggle -> {
                    flush()
                    key(option.id) {
                        val value = option.resolve(huds)
                        SwitchRow(option.label, value.isOn(), value.isMixed()) {
                            option.apply(huds, it)
                            edits.bump()
                        }
                    }
                }
                option is SharedChoice<*> && option.segmented -> {
                    flush()
                    GridRow(col1, wide = { SharedCell(option, huds, edits, col1 + GridGap + GridCol2) })
                }
                else -> cells += option
            }
        }
        flush()
    }
}

/** The padding of every HUD, which edits both sides of an axis together until it is split per side */
@Composable
private fun SharedPadding(huds: List<Hud>, query: String) {
    val sides = listOf("padLeft", "padRight", "padTop", "padBottom").map { SharedOptions.find(it) as? SharedAmount ?: return }
    if (!sides.all { SharedOptions.isShared(it, huds) }) return
    val (left, right, top, bottom) = sides
    var revision by remember { mutableIntStateOf(0) }
    revision
    val edit = { write: (Hud, Float) -> Unit, value: Float -> setShared(huds, value, write); revision++ }
    var perSide by remember { mutableStateOf(huds.any { it.padLeft != it.padRight || it.padTop != it.padBottom }) }

    @Composable
    fun Pad(label: String, option: SharedAmount, width: Dp, write: (Hud, Float) -> Unit) {
        val value = option.resolve(huds)
        LabeledControl(label, value.isMixed()) {
            NumberField(
                (value as? SharedValue.Single)?.value ?: option.read(huds.first()),
                { edit(write, it) }, range = 0f..100f, roundToDecimals = false, fractionDigits = 2,
                width = width, unit = "px", format = mixedFormat(value.isMixed()), fontSize = 13.sp,
            )
        }
    }

    CardSection("Padding", query, "Horizontal", "Vertical", "Per side", "Left", "Right", "Top", "Bottom") { col1 ->
        RowWithFold(
            perSide,
            row = {
                GridRow(
                    col1,
                    first = {
                        if (perSide) Pad("Left", left, col1) { h, v -> h.padLeft = v }
                        else Pad("Horizontal", left, col1) { h, v -> h.padLeft = v; h.padRight = v }
                    },
                    second = {
                        if (perSide) Pad("Top", top, GridCol2) { h, v -> h.padTop = v }
                        else Pad("Vertical", top, GridCol2) { h, v -> h.padTop = v; h.padBottom = v }
                    },
                    third = {
                        LabeledControl("Per side") {
                            Switch(perSide, {
                                perSide = it
                                if (!it) {
                                    Snapshot.withMutableSnapshot { huds.forEach { h -> h.padRight = h.padLeft; h.padBottom = h.padTop } }
                                    revision++
                                }
                            }, stateLabel = true)
                        }
                    },
                )
            },
            folded = {
                GridRow(
                    col1,
                    first = { Pad("Right", right, col1) { h, v -> h.padRight = v } },
                    second = { Pad("Bottom", bottom, GridCol2) { h, v -> h.padBottom = v } },
                )
            },
        )
    }
}

@Composable
private fun SharedCell(option: SharedOption, huds: List<Hud>, edits: SharedEdits, width: Dp) {
    edits.observe()
    key(option.id) {
        when (option) {
            is SharedToggle -> Unit

            is SharedAmount -> {
                val value = option.resolve(huds)
                LabeledControl(option.label, value.isMixed()) {
                    NumberField(
                        (value as? SharedValue.Single)?.value ?: option.read(huds.first()),
                        {
                            option.apply(huds, it)
                            edits.bump()
                        },
                        range = option.min..option.max, step = option.step, roundToDecimals = false, fractionDigits = 2,
                        width = width, unit = option.unit, format = mixedFormat(value.isMixed()), fontSize = 13.sp,
                    )
                }
            }

            is SharedColor -> {
                val first = huds.first()
                val color = (option.resolve(huds) as? SharedValue.Single)?.value ?: option.read(first)
                val chroma = (resolveShared(huds, option.readChroma) as? SharedValue.Single)?.value ?: option.readChroma(first)
                val speed = (resolveShared(huds, option.readSpeed) as? SharedValue.Single)?.value ?: option.readSpeed(first)
                LabeledControl(option.label, option.isMixed(huds)) {
                    ColorSwatch(
                        Color(color),
                        chroma = chroma, chromaSpeed = speed,
                        onColorChanged = {
                            option.apply(huds, it.toArgb())
                            edits.bump()
                        },
                        onChromaChanged = { enabled, chromaSpeed ->
                            option.applyChroma(huds, enabled, chromaSpeed)
                            edits.bump()
                        },
                    )
                }
            }

            is SharedModifiers -> LabeledControl(option.label) {
                ModifierButtons(
                    option.flags.map { flag ->
                        val value = flag.resolve(huds)
                        ModifierToggle(flag.icon, hudPanelText(flag.icon, flag.label), value.isOn()) {
                            flag.apply(huds, !value.isOn())
                            edits.bump()
                        }
                    },
                )
            }

            is SharedChoice<*> -> SharedChoiceControl(option, huds, edits, width)
        }
    }
}

@Composable
private fun <E : Any> SharedChoiceControl(option: SharedChoice<E>, huds: List<Hud>, edits: SharedEdits, width: Dp) {
    val value = option.resolve(huds)
    val mixed = value.isMixed()
    val shown = (value as? SharedValue.Single)?.value ?: option.read(huds.first())
    val selected = option.values.indexOf(shown)
    val onSelect = { index: Int ->
        option.apply(huds, option.values[index])
        edits.bump()
    }
    val icon = option.icon
    LabeledControl(option.label, mixed) {
        if (option.segmented) {
            RadioGroup(
                if (mixed) -1 else selected, option.values.indices.toList(), onSelect,
                label = { option.name(option.values[it]) }, orientation = Orientation.Horizontal, fillWidth = true,
            )
        } else if (icon != null) {
            RadioGroup(
                if (mixed) -1 else selected, option.values.indices.toList(), onSelect,
                label = { "" }, orientation = Orientation.Horizontal, fillWidth = true, icon = { icon(option.values[it]) },
            )
        } else {
            Dropdown(option.values.map(option.name), selected, onSelect, width, mixed = mixed)
        }
    }
}

/** Counts display changes of the options mods defined, since which of them every HUD shows depends on them */
@Composable
private fun rememberDisplayRevision(huds: List<Hud>): Int {
    var revision by remember(huds) { mutableIntStateOf(0) }
    DisposableEffect(huds) {
        val listener = Consumer<Property.Display> { revision++ }
        val props = huds.flatMap(SharedProperties::candidates)
        props.forEach { it.addDisplayListener(listener) }
        onDispose { props.forEach { it.removeDisplayListener(listener) } }
    }
    return revision
}

/** The options mods defined, which are grouped into the categories the mod gave them like the HUD editor does for a single HUD */
@Composable
private fun SharedPropertyRows(huds: List<Hud>, query: String, edits: SharedEdits) {
    val revision = rememberDisplayRevision(huds)
    val shared = remember(huds, revision) { SharedProperties.find(huds) }
    if (shared.isEmpty()) return
    val proxies = remember(shared) { shared.associateWith { it.proxy(onWrite = edits::bump) } }
    DisposableEffect(proxies) {
        val listeners = proxies.map { (option, proxy) ->
            Consumer<Property.Display> { proxy.revaluateDisplay() }.also { listener ->
                option.props.forEach { it.addDisplayListener(listener) }
            }
        }
        onDispose {
            proxies.keys.forEachIndexed { index, option -> option.props.forEach { it.removeDisplayListener(listeners[index]) } }
        }
    }
    val bySharedProp = remember(proxies) { proxies.entries.associate { (option, proxy) -> proxy to (option to proxy) } }
    val categories = remember(proxies) {
        val grouped = LinkedHashMap<String, LinkedHashMap<String, MutableList<SettingNode>>>()
        for (proxy in proxies.values) {
            val category = nodeGroup(proxy, "category", ConfigVisualizer.DEFAULT_CATEGORY)
            val subcategory = nodeGroup(proxy, "subcategory", ConfigVisualizer.DEFAULT_SUBCATEGORY)
            grouped.getOrPut(category) { LinkedHashMap() }.getOrPut(subcategory) { ArrayList() } += SettingNode.Leaf(proxy)
        }
        grouped.map { (category, subcategories) ->
            CategoryGroup(category, subcategories.map { (name, nodes) -> SubcategoryGroup(name, nodes) })
        }
    }
    HudCategoryList(categories, query) { entry ->
        val prop = ((entry as? ConfigListEntry.Item)?.node as? SettingNode.Leaf)?.prop
        val (option, proxy) = bySharedProp[prop] ?: (null to null)
        if (option != null && proxy != null) {
            key(option.id) { SharedPropertyRow(option, proxy, edits) }
        } else {
            ConfigListRow(entry)
        }
    }
}

@Composable
private fun SharedPropertyRow(shared: SharedProperty, proxy: Property<Any?>, edits: SharedEdits) {
    edits.observe()
    val display = rememberDisplay(proxy)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HudOptionRow(proxy)
        if (shared.resolve().isMixed()) {
            Text(
                "Mixed between these HUDs, changing it sets it for all of them.",
                color = LocalTheme.current.textColorSecondary,
                fontSize = 12.sp,
            )
            if (display == Property.Display.SHOWN) {
                GroupRow("Set every HUD to the value shown", "align") { proxy.setReferential(proxy.get()) }
            }
        }
    }
}

/** Reads "Mixed" in place of the number until the field is focused, for a setting that differs between several HUDs */
private fun mixedFormat(mixed: Boolean): ((Float) -> String)? = if (mixed) { _ -> "Mixed" } else null
