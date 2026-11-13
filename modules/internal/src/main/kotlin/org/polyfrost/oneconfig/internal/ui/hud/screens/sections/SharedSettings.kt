package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.function.Consumer
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudGroup
import org.polyfrost.oneconfig.api.hud.v1.HudGroups
import org.polyfrost.oneconfig.api.hud.v1.HudResize
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.blockInteraction
import org.polyfrost.oneconfig.internal.ui.components.settings.Dropdown
import org.polyfrost.oneconfig.internal.ui.components.settings.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.components.settings.Radio
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.HudSettingsContent
import org.polyfrost.oneconfig.internal.ui.hud.repairHudStaticSize
import org.polyfrost.oneconfig.internal.ui.screens.ConfigListRow
import org.polyfrost.oneconfig.internal.ui.screens.HudCategoryList
import org.polyfrost.oneconfig.internal.ui.screens.HudOptionRow
import org.polyfrost.oneconfig.internal.ui.search.CategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.ConfigListEntry
import org.polyfrost.oneconfig.internal.ui.search.SettingNode
import org.polyfrost.oneconfig.internal.ui.search.SubcategoryGroup
import org.polyfrost.oneconfig.internal.ui.search.nodeGroup
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** Counts edits so rows read the HUDs again after one, since plain HUD fields cannot tell compose they changed */
private class SharedEdits {
    private var count by mutableIntStateOf(0)

    val revision: Int get() = count

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

/** The Style tab for [huds] */
@Composable
fun SharedStyle(huds: Collection<Hud>, query: String) {
    SharedContent(huds) { list, edits ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SharedCard(SharedSection.Text, list, query, edits)
            SharedCard(SharedSection.Background, list, query, edits)
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
    val scalable = huds.any { it.resizeAxes == HudResize.Both }
    val locked = group?.locked ?: resolveShared(huds) { it.locked }.isOn()
    val mixedLock = group == null && resolveShared(huds) { it.locked }.isMixed()
    CardSection(
        if (scalable) "Position & Scale" else "Position", query,
        "Lock position", *if (scalable) arrayOf("Scale", "Match scale") else emptyArray(),
    ) { col1 ->
        HudPositionFields(huds.first(), col1) {
            if (scalable) {
                LabeledControl("Scale") {
                    NumberSpinner(
                        HudGroups.scaleOf(huds), { HudGroups.setScale(huds, it) },
                        HudGroups.scaleRange(huds).start, HudGroups.scaleRange(huds).endInclusive, 0.25f,
                        width = GridCol3, unit = "x", enabled = !locked,
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
        if (scalable) {
            val scale = HudGroups.scaleOf(huds)
            if (!HudGroups.hasUniformScale(huds)) {
                GroupRow("Set every HUD to %.2fx".format(scale), "align") { HudGroups.matchScale(huds, scale) }
            }
        }
    }
}

/** The options of [section] that every HUD has, laid out in the panel grid with switches and segmented choices on rows of their own */
@Composable
private fun SharedCard(section: SharedSection, huds: List<Hud>, query: String, edits: SharedEdits) {
    val options = SharedOptions.shared(section, huds)
    if (options.isEmpty()) return
    // the background card's own switch turns the whole card on or off
    val master = options.firstOrNull { it.id == "showBackground" } as? SharedToggle
    // read so the card reads the HUDs again after an edit
    edits.revision
    val masterValue = master?.resolve(huds)
    CardSection(
        section.title, query, *options.map { it.label }.toTypedArray(),
        checked = masterValue?.isOn(),
        onCheckedChange = { master?.apply(huds, it); edits.bump() },
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

        for (option in options) {
            if (option === master || !SharedOptions.isShown(option, huds)) continue
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
    // read so the fields read the HUDs again after an edit
    var revision by remember { mutableIntStateOf(0) }
    revision
    val edit = { write: (Hud, Float) -> Unit, value: Float -> setShared(huds, value, write); revision++ }
    // opens with a field per side when the sides already differ
    var perSide by remember { mutableStateOf(huds.any { it.padLeft != it.padRight || it.padTop != it.padBottom }) }

    @Composable
    fun Pad(label: String, option: SharedAmount, width: Dp, write: (Hud, Float) -> Unit) {
        val value = option.resolve(huds)
        LabeledControl(label, value.isMixed()) {
            NumberSpinner(
                (value as? SharedValue.Single)?.value ?: option.read(huds.first()),
                { edit(write, it) }, 0f, 100f, 1f, width = width, unit = "px", mixed = value.isMixed(),
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
                            SwitchControl(perSide, {
                                perSide = it
                                // leaving per side mode evens the sides out so the two fields show what is applied
                                if (!it) {
                                    Snapshot.withMutableSnapshot { huds.forEach { h -> h.padRight = h.padLeft; h.padBottom = h.padTop } }
                                    revision++
                                }
                            })
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
    // read so the control reads the HUDs again after an edit
    edits.revision
    key(option.id) {
        when (option) {
            is SharedToggle -> Unit

            is SharedAmount -> {
                val value = option.resolve(huds)
                LabeledControl(option.label, value.isMixed()) {
                    NumberSpinner(
                        (value as? SharedValue.Single)?.value ?: option.read(huds.first()),
                        {
                            option.apply(huds, it)
                            edits.bump()
                        },
                        option.min, option.max, option.step,
                        width = width, unit = option.unit, mixed = value.isMixed(),
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
                        // a mix of both turns on first
                        ModifierToggle(flag.icon, value.isOn()) {
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
            Radio(option.values.map(option.name), if (mixed) -1 else selected, fillWidth = true, onSelect = onSelect)
        } else if (icon != null) {
            Radio(
                option.values.map { null }, if (mixed) -1 else selected,
                fillWidth = true, icons = option.values.map(icon), onSelect = onSelect,
            )
        } else {
            Dropdown(option.values.map(option.name), selected, onSelect, width, mixed)
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
    // read so the note reads the HUDs again after an edit
    edits.revision
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HudOptionRow(proxy)
        if (shared.resolve().isMixed()) {
            Text(
                "Mixed between these HUDs, changing it sets it for all of them.",
                color = LocalTheme.current.textColorSecondary,
                fontSize = 12.sp,
            )
            GroupRow("Set every HUD to the value shown", "align") { proxy.setReferential(proxy.get()) }
        }
    }
}
