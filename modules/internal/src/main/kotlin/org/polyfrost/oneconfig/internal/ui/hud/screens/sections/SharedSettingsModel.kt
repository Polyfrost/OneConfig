package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.runtime.snapshots.Snapshot
import java.util.Arrays
import java.util.function.Supplier
import org.polyfrost.oneconfig.api.config.v1.Config
import org.polyfrost.oneconfig.api.config.v1.Properties
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.Visualizer
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.api.hud.v1.Weight
import org.polyfrost.oneconfig.internal.ui.hud.screens.comparable
import org.polyfrost.oneconfig.internal.ui.hud.screens.sameValue

/** What the HUDs of a selection hold for one option */
internal sealed interface SharedValue<out T> {
    /** Every HUD holds [value] */
    data class Single<out T>(val value: T) : SharedValue<T>

    /** The HUDs hold different values */
    data object Mixed : SharedValue<Nothing>
}

/** The value every HUD of [huds] holds for what [read] reads, or [SharedValue.Mixed] when they differ */
internal fun <T> resolveShared(huds: Collection<Hud>, read: (Hud) -> T): SharedValue<T> {
    val values = huds.map(read)
    val first = values.firstOrNull() ?: return SharedValue.Mixed
    return if (values.all { sameValue(first, it) }) SharedValue.Single(first) else SharedValue.Mixed
}

/** Writes [value] to every HUD of [huds] in one snapshot, so it is a single step in the editor's history */
internal fun <T> setShared(huds: Collection<Hud>, value: T, write: (Hud, T) -> Unit) {
    Snapshot.withMutableSnapshot { for (hud in huds) write(hud, value) }
}

/** The editor tab a [SharedSection] is drawn in */
internal enum class SharedTab { Layout, Style, Settings }

/** The cards of the shared settings view that [SharedOption]s are drawn in */
internal enum class SharedSection(val title: String, val tab: SharedTab) {
    Padding("Padding", SharedTab.Layout),
    Text("Text", SharedTab.Style),
    Background("Background", SharedTab.Style),
    Visibility("Visibility", SharedTab.Settings),
}

/**
 * A built-in option the HUDs of a selection can share
 *
 * Only options which the single HUD panels draw by hand are listed, anything else a HUD has is either
 * specific to that HUD, like its position, or a mod's own option and is found by [SharedProperties]
 */
internal sealed class SharedOption(
    val id: String,
    val label: String,
    val section: SharedSection,
    /** Whether a HUD has the option at all, which it only does for HUDs the single HUD panel draws it for */
    val appliesTo: (Hud) -> Boolean,
    /** Whether the option does anything for a HUD, which an option depending on a switch does not while it is off */
    val shownWhen: (Hud) -> Boolean,
)

internal class SharedToggle(
    id: String,
    label: String,
    section: SharedSection,
    val read: (Hud) -> Boolean,
    val write: (Hud, Boolean) -> Unit,
    appliesTo: (Hud) -> Boolean = ::drawsAppearance,
    shownWhen: (Hud) -> Boolean = { true },
) : SharedOption(id, label, section, appliesTo, shownWhen) {
    fun resolve(huds: Collection<Hud>): SharedValue<Boolean> = resolveShared(huds, read)

    fun apply(huds: Collection<Hud>, value: Boolean) = setShared(huds, value, write)
}

internal class SharedAmount(
    id: String,
    label: String,
    section: SharedSection,
    val min: Float,
    val max: Float,
    val step: Float,
    val read: (Hud) -> Float,
    val write: (Hud, Float) -> Unit,
    val unit: String? = "px",
    appliesTo: (Hud) -> Boolean = ::drawsAppearance,
    shownWhen: (Hud) -> Boolean = { true },
) : SharedOption(id, label, section, appliesTo, shownWhen) {
    fun resolve(huds: Collection<Hud>): SharedValue<Float> = resolveShared(huds, read)

    fun apply(huds: Collection<Hud>, value: Float) = setShared(huds, value, write)
}

/** A flag of [SharedModifiers] drawn as an icon */
internal class SharedFlag(
    val label: String,
    val icon: String,
    val read: (Hud) -> Boolean,
    val write: (Hud, Boolean) -> Unit,
) {
    fun resolve(huds: Collection<Hud>): SharedValue<Boolean> = resolveShared(huds, read)

    fun apply(huds: Collection<Hud>, value: Boolean) = setShared(huds, value, write)
}

/** Switches drawn as a row of icons that are lit while on, like the text modifiers of a single HUD */
internal class SharedModifiers(
    id: String,
    label: String,
    section: SharedSection,
    val flags: List<SharedFlag>,
    appliesTo: (Hud) -> Boolean = ::drawsAppearance,
    shownWhen: (Hud) -> Boolean = { true },
) : SharedOption(id, label, section, appliesTo, shownWhen)

/** A color along with whether it cycles through the rainbow and how fast, which are edited together */
internal class SharedColor(
    id: String,
    label: String,
    section: SharedSection,
    val read: (Hud) -> Int,
    val write: (Hud, Int) -> Unit,
    val readChroma: (Hud) -> Boolean,
    val writeChroma: (Hud, Boolean) -> Unit,
    val readSpeed: (Hud) -> Float,
    val writeSpeed: (Hud, Float) -> Unit,
    appliesTo: (Hud) -> Boolean = ::drawsAppearance,
    shownWhen: (Hud) -> Boolean = { true },
) : SharedOption(id, label, section, appliesTo, shownWhen) {
    fun resolve(huds: Collection<Hud>): SharedValue<Int> = resolveShared(huds, read)

    /** Mixed as soon as any of the color, chroma or its speed differs between the HUDs */
    fun isMixed(huds: Collection<Hud>): Boolean =
        resolve(huds) is SharedValue.Mixed ||
            resolveShared(huds, readChroma) is SharedValue.Mixed ||
            resolveShared(huds, readSpeed) is SharedValue.Mixed

    fun apply(huds: Collection<Hud>, argb: Int) = setShared(huds, argb, write)

    fun applyChroma(huds: Collection<Hud>, enabled: Boolean, speed: Float) {
        Snapshot.withMutableSnapshot {
            for (hud in huds) {
                writeChroma(hud, enabled)
                writeSpeed(hud, speed)
            }
        }
    }
}

/** One of [values], drawn as a dropdown or when [icon] is given as a row of icons */
internal class SharedChoice<E : Any>(
    id: String,
    label: String,
    section: SharedSection,
    val values: List<E>,
    val name: (E) -> String,
    val icon: ((E) -> String)?,
    val read: (Hud) -> E,
    val write: (Hud, E) -> Unit,
    /** Drawn as a row of named choices across the card rather than a dropdown */
    val segmented: Boolean = false,
    appliesTo: (Hud) -> Boolean = ::drawsAppearance,
    shownWhen: (Hud) -> Boolean = { true },
) : SharedOption(id, label, section, appliesTo, shownWhen) {
    fun resolve(huds: Collection<Hud>): SharedValue<E> = resolveShared(huds, read)

    fun apply(huds: Collection<Hud>, value: E) = setShared(huds, value, write)
}

/** Legacy HUDs draw their own content so the single HUD panel leaves out everything about how it looks */
internal fun drawsAppearance(hud: Hud): Boolean = hud !is LegacyHudMarker

private fun mergesBackground(hud: Hud): Boolean = hud !is LegacyHudMarker && hud.canMergeBackground()

private inline fun <reified E : Enum<E>> enumChoice(
    id: String,
    label: String,
    section: SharedSection,
    noinline icon: ((E) -> String)? = null,
    noinline read: (Hud) -> E,
    noinline write: (Hud, E) -> Unit,
): SharedChoice<E> = SharedChoice(id, label, section, enumValues<E>().toList(), { it.name }, icon, read, write)

internal object SharedOptions {
    /** The options in the order they are drawn */
    @JvmField
    val all: List<SharedOption> = listOf(
        SharedAmount("padLeft", "Left", SharedSection.Padding, 0f, 100f, 1f, { it.padLeft }, { h, v -> h.padLeft = v }),
        SharedAmount("padRight", "Right", SharedSection.Padding, 0f, 100f, 1f, { it.padRight }, { h, v -> h.padRight = v }),
        SharedAmount("padTop", "Top", SharedSection.Padding, 0f, 100f, 1f, { it.padTop }, { h, v -> h.padTop = v }),
        SharedAmount("padBottom", "Bottom", SharedSection.Padding, 0f, 100f, 1f, { it.padBottom }, { h, v -> h.padBottom = v }),
        enumChoice<Font>("font", "Font", SharedSection.Text, read = { it.font }, write = { h, v -> h.font = v }),
        SharedAmount(
            "textScale", "Size", SharedSection.Text, 25f, 500f, 5f,
            { it.textScale * 100f }, { h, v -> h.textScale = v / 100f }, unit = "%",
        ),
        SharedModifiers(
            "modifiers", "Modifiers", SharedSection.Text,
            listOf(
                SharedFlag("Bold", "bold", { it.textBold }, { h, v -> h.textBold = v }),
                SharedFlag("Italic", "italic", { it.textItalic }, { h, v -> h.textItalic = v }),
                SharedFlag("Underline", "underline", { it.textUnderline }, { h, v -> h.textUnderline = v }),
            ),
        ),
        enumChoice<Weight>("textWeight", "Weight", SharedSection.Text, read = { it.textWeight }, write = { h, v -> h.textWeight = v }),
        enumChoice<CaseType>(
            "caseType", "Case", SharedSection.Text, { it.icon },
            read = { CaseType.entries[it.caseType.coerceIn(0, CaseType.entries.lastIndex)] },
            write = { h, v -> h.caseType = v.ordinal },
        ),
        SharedColor(
            "textColor", "Text color", SharedSection.Text,
            { it.textColor }, { h, v -> h.textColor = v },
            { it.textChroma }, { h, v -> h.textChroma = v },
            { it.textChromaSpeed }, { h, v -> h.textChromaSpeed = v },
        ),
        SharedChoice(
            "shadow", "Shadow", SharedSection.Text, ShadowMode.entries, { it.title }, null,
            { if (!it.showShadow) ShadowMode.Off else if (it.fullShadow) ShadowMode.Full else ShadowMode.Normal },
            { h, v ->
                h.showShadow = v != ShadowMode.Off
                if (h.showShadow) h.fullShadow = v == ShadowMode.Full
            },
            segmented = true,
        ),
        SharedColor(
            "shadowColor", "Shadow color", SharedSection.Text,
            { it.shadowColor }, { h, v -> h.shadowColor = v },
            { it.shadowChroma }, { h, v -> h.shadowChroma = v },
            { it.shadowChromaSpeed }, { h, v -> h.shadowChromaSpeed = v },
            shownWhen = { it.showShadow },
        ),
        SharedToggle("showBackground", "Show background", SharedSection.Background, { it.showBackground }, { h, v -> h.showBackground = v }),
        SharedAmount(
            "bgRadius", "Radius", SharedSection.Background, 0f, 32f, 1f,
            { it.bgRadius }, { h, v -> h.bgRadius = v },
            shownWhen = { it.showBackground },
        ),
        SharedColor(
            "bgColor", "Color", SharedSection.Background,
            { it.bgColor }, { h, v -> h.bgColor = v },
            { it.bgChroma }, { h, v -> h.bgChroma = v },
            { it.bgChromaSpeed }, { h, v -> h.bgChromaSpeed = v },
            shownWhen = { it.showBackground },
        ),
        // HUDs drawing their own background opt out of merging so this has nothing to control
        SharedChoice(
            "merge", "Merge with neighbours", SharedSection.Background, MergeMode.entries, { it.title }, null,
            { if (!it.mergeBackground) MergeMode.Off else if (it.mergeDiagonally) MergeMode.Diagonal else MergeMode.Normal },
            { h, v ->
                h.mergeBackground = v != MergeMode.Off
                if (h.mergeBackground) h.mergeDiagonally = v == MergeMode.Diagonal
            },
            appliesTo = ::mergesBackground, shownWhen = { it.showBackground },
            segmented = true,
        ),
        SharedToggle(
            "keepBgWhenHidden", "Keep hidden", SharedSection.Background,
            { it.keepBgWhenHidden }, { h, v -> h.keepBgWhenHidden = v },
            appliesTo = ::mergesBackground, shownWhen = { it.showBackground && it.mergeBackground },
        ),
        SharedToggle("showInF3", "Show in F3 Screen", SharedSection.Visibility, { it.showInF3 }, { h, v -> h.showInF3 = v }),
        SharedToggle("showInTab", "Show in Tab List", SharedSection.Visibility, { it.showInTab }, { h, v -> h.showInTab = v }),
        SharedToggle("showInScreens", "Show in GUIs", SharedSection.Visibility, { it.showInScreens }, { h, v -> h.showInScreens = v }),
        SharedToggle("showInChat", "Show in Chat", SharedSection.Visibility, { it.showInChat }, { h, v -> h.showInChat = v }),
    )

    @JvmStatic
    fun find(id: String): SharedOption? = all.firstOrNull { it.id == id }

    /** Whether every HUD of [huds] has [option] */
    @JvmStatic
    fun isShared(option: SharedOption, huds: Collection<Hud>): Boolean = huds.isNotEmpty() && huds.all(option.appliesTo)

    /** Whether [huds] share any option drawn in [tab] */
    @JvmStatic
    fun anyShared(tab: SharedTab, huds: Collection<Hud>): Boolean =
        all.any { it.section.tab == tab && isShared(it, huds) }

    /** Whether [option] does anything for [huds], which is not so while every HUD has what it depends on switched off */
    @JvmStatic
    fun isShown(option: SharedOption, huds: Collection<Hud>): Boolean = huds.any(option.shownWhen)

    /** The options of [section] that every HUD of [huds] has */
    @JvmStatic
    fun shared(section: SharedSection, huds: Collection<Hud>): List<SharedOption> =
        all.filter { it.section == section && isShared(it, huds) }
}

/**
 * A mod's own option that every HUD of a selection has in the same form, with each HUD's property for it
 *
 * The properties are in the order of the HUDs
 */
internal class SharedProperty(val id: String, val props: List<Property<*>>) {
    /** The property standing in for all of them, which shows the first HUD's value and writes to every HUD */
    fun proxy(onWrite: () -> Unit = {}): Property<Any?> = SharedProperties.proxy(this, onWrite)

    fun resolve(): SharedValue<Any?> {
        val values = props.map(::comparable)
        val first = values.first()
        return if (values.all { sameValue(first, it) }) SharedValue.Single(first) else SharedValue.Mixed
    }
}

internal object SharedProperties {
    /** Identity text of a HUD, which sharing between HUDs would overwrite */
    private val IDENTITY_OPTIONS = setOf("prefix", "suffix")

    /** Options taking no single value, which are a command, a note or a list rather than a setting */
    private val UNSHARED_VISUALIZERS: Set<Class<*>> = setOf(
        Visualizer.ButtonVisualizer::class.java,
        Visualizer.InfoVisualizer::class.java,
        Visualizer.KeybindVisualizer::class.java,
        Visualizer.DraggableListVisualizer::class.java,
        Visualizer.MultiSelectDropdownVisualizer::class.java,
        Visualizer.NumberChainVisualizer::class.java,
        Visualizer.TextListVisualizer::class.java,
        Visualizer.ItemListVisualizer::class.java,
        Visualizer.EffectListVisualizer::class.java,
        Visualizer.FileListVisualizer::class.java,
        Visualizer.ColorListVisualizer::class.java,
        Visualizer.NumberListVisualizer::class.java,
        Visualizer.SliderListVisualizer::class.java,
        Visualizer.RangeSliderListVisualizer::class.java,
        Visualizer.RangeNumberListVisualizer::class.java,
    )

    /** What has to be the same for two options to be one setting, since each is drawn from it */
    private val MATCHED_METADATA = listOf("options", "optionLabels", "optionValues", "min", "max", "step")

    /** Everything the built-in options already cover, in case a HUD does not hide its own copy */
    private val BUILT_IN_OPTIONS: Set<String> by lazy {
        SharedOptions.all.mapTo(HashSet()) { it.id } + listOf("textBold", "textItalic", "textUnderline", "showShadow", "fullShadow", "mergeBackground", "mergeDiagonally")
    }

    private fun visualizerClass(prop: Property<*>): Class<*>? = when (val raw = prop.getMetadata<Any?>("visualizer")) {
        null -> null
        is Class<*> -> raw
        else -> raw.javaClass
    }

    /** Whether [prop] is a mod's option which can be shared with the same option of another HUD */
    @JvmStatic
    fun isShareable(prop: Property<*>): Boolean {
        val id = prop.id ?: return false
        if (id in IDENTITY_OPTIONS || id in BUILT_IN_OPTIONS) return false
        if (prop.type == Void::class.java || prop.type == Void.TYPE) return false
        if (prop.getMetadata<Any?>("hidden") != null) return false
        val visualizer = visualizerClass(prop) ?: return false
        return visualizer !in UNSHARED_VISUALIZERS
    }

    /** Whether [a] and [b] are the same option, so one control can edit both */
    @JvmStatic
    fun matches(a: Property<*>, b: Property<*>): Boolean =
        a.id == b.id && a.type == b.type && visualizerClass(a) == visualizerClass(b) &&
            MATCHED_METADATA.all { key ->
                Arrays.deepEquals(arrayOf(a.getMetadata<Any?>(key)), arrayOf(b.getMetadata<Any?>(key)))
            }

    /** The top level options of [hud] which mods defined and may be shared, whether they are shown right now or not */
    @JvmStatic
    fun candidates(hud: Hud): List<Property<*>> =
        hud.tree?.map?.values.orEmpty().filterIsInstance<Property<*>>().filter(::isShareable)

    /** The mod options that every HUD of [huds] has in the same form and that are shown for all of them, in the order of the first HUD */
    @JvmStatic
    fun find(huds: List<Hud>): List<SharedProperty> {
        val first = huds.firstOrNull() ?: return emptyList()
        val out = ArrayList<SharedProperty>()
        for (prop in candidates(first)) {
            val id = prop.id ?: continue
            val props = huds.map { hud -> hud.tree?.getProp(id)?.takeIf(::isShareable) }
            if (props.any { it == null }) continue
            val all = props.filterNotNull()
            if (all.any { it.display == Property.Display.HIDDEN }) continue
            if (all.any { !matches(prop, it) }) continue
            out.add(SharedProperty(id, all))
        }
        return out
    }

    /**
     * A property that shows the first HUD's value and writes every HUD's own property when it is set
     *
     * Each HUD is written through its property rather than its field so the option's callbacks run, and
     * [onWrite] runs once they all have
     */
    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun proxy(shared: SharedProperty, onWrite: () -> Unit = {}): Property<Any?> {
        val first = shared.props.first() as Property<Any?>
        val proxy = Properties.functional(
            Supplier { first.get() },
            { value: Any? ->
                Snapshot.withMutableSnapshot {
                    for (prop in shared.props) {
                        (prop as Property<Any?>).setAs(Config.copyDefault(prop.type, value))
                    }
                }
                onWrite()
            },
            first.id,
            first.title,
            first.description,
            first.type as Class<Any?>,
        )
        first.getMetadata()?.let { proxy.addMetadata(it) }
        proxy.addDisplayCondition(Supplier { worstDisplay(shared) })
        return proxy
    }

    /** Hidden when it is for any of the HUDs and disabled when it is for any of them, which is how the row should read */
    @JvmStatic
    fun worstDisplay(shared: SharedProperty): Property.Display {
        val displays = shared.props.map { it.display }
        return when {
            Property.Display.HIDDEN in displays -> Property.Display.HIDDEN
            Property.Display.DISABLED in displays -> Property.Display.DISABLED
            else -> Property.Display.SHOWN
        }
    }
}
