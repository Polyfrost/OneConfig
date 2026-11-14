package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.ui.v1.components.ArmedConfirmButton
import org.polyfrost.oneconfig.api.ui.v1.components.Chip
import org.polyfrost.oneconfig.api.ui.v1.components.ColorPickerPopup
import org.polyfrost.oneconfig.api.ui.v1.components.Confirm
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.ControlStyle
import org.polyfrost.oneconfig.api.ui.v1.components.DropdownMenu
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.IconButtonStyle
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuHeading
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.SegmentedChips
import org.polyfrost.oneconfig.api.ui.v1.components.TextButton
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.api.ui.v1.components.rememberColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.glassHover
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.settings.SliderControl
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.CustomBase
import org.polyfrost.oneconfig.internal.ui.themes.CustomCorners
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemeSpec
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemes
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.PlayerAccent
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.contrastRatio
import org.polyfrost.oneconfig.internal.ui.themes.customSpec
import org.polyfrost.oneconfig.internal.ui.themes.isCustom
import org.polyfrost.oneconfig.internal.ui.themes.previewAsset
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper
import kotlin.math.roundToInt

private const val TEXT_CONTRAST = 4.5f

private const val SECONDARY_CONTRAST = 3f

private val EditorLabelWidth = 110.dp

private val EditorControlHeight = ControlSize.Compact.height

private val EditorFieldWidth = 320.dp

private val EditorGap = 8.dp

private fun baseOf(theme: UITheme): CustomBase = theme.customSpec?.base ?: when {
    theme.liquidGlass -> CustomBase.POLYGLASS
    theme.isMinecraftStyle -> CustomBase.MINECRAFT
    else -> CustomBase.POLYFROSTED
}

private val StartFromPreviewWidth = 42.dp

private val StartFromPreviewHeight = 27.dp

private fun customTheme(spec: CustomThemeSpec): UITheme =
    ThemeRegistry.registry.firstOrNull { it.previewImage == spec.themeId } ?: spec.toUITheme()

private fun activateCustom(spec: CustomThemeSpec) {
    ThemeRegistry.activate(customTheme(spec))
}

private val CopySuffix = Regex(""" copy(?: #\d+)?$""")

internal fun customCopyName(source: String): String {
    val root = source.replace(CopySuffix, "").ifBlank { source }
    val taken = CustomThemes.specs.mapTo(HashSet()) { it.name }
    var n = 1
    while (true) {
        var base = root
        fun format(b: String) = if (n == 1) {
            translated("oneconfig.themes.custom.copy_name", "%s copy", b)
        } else {
            translated("oneconfig.themes.custom.copy_name_numbered", "%s copy #%d", b, n)
        }
        var name = format(base)
        while (name.length > CustomThemeSpec.NAME_LIMIT && base.isNotEmpty()) {
            base = base.dropLast(1).trimEnd()
            name = format(base)
        }
        if (name !in taken) return name
        n++
        if (n > CustomThemeSpec.MAX + 1) return "${root.take(CustomThemeSpec.NAME_LIMIT - 4)} #$n"
    }
}

internal fun createCustomFrom(source: UITheme): Boolean {
    val spec = CustomThemes.createFrom(source, customCopyName(source.customSpec?.name ?: source.name)) ?: return false
    activateCustom(spec)
    return true
}

private fun duplicateCustom(spec: CustomThemeSpec): EditorNotice? {
    val copy = CustomThemes.import(spec.encode()) ?: return EditorNotice.Message(fullMessage())
    val named = copy.copy(name = customCopyName(spec.name))
    CustomThemes.update(named)
    activateCustom(named)
    return null
}

private fun copyCodeNotice(spec: CustomThemeSpec): EditorNotice =
    if (ClipboardHelper.setString(spec.encode())) {
        EditorNotice.Message(translated("oneconfig.themes.custom.copied", "Code copied"))
    } else {
        EditorNotice.Message(translated("oneconfig.themes.custom.copy_failed", "Couldn't reach the clipboard"))
    }

private fun deleteCustom(spec: CustomThemeSpec) {
    if (ThemeRegistry.activeTheme?.customSpec?.id == spec.id) {
        val fallback = ThemeRegistry.registry.firstOrNull { !it.isCustom && baseOf(it) == spec.base && it.isLight == spec.light }
            ?: ThemeRegistry.registry.firstOrNull { !it.isCustom }
        fallback?.let(ThemeRegistry::activate)
    }
    CustomThemes.remove(spec.id)
}

private fun askDeleteCustom(spec: CustomThemeSpec) {
    Confirm.ask(
        translated("oneconfig.themes.custom.delete_title", "Delete \"%s\"?", spec.name),
        translated("oneconfig.themes.custom.delete_text", "This custom theme will be gone for good. Copy its code first if you want to keep it."),
        translated("oneconfig.themes.custom.delete", "Delete"),
        { deleteCustom(spec) },
        icon = "trash",
    )
}

private fun customThemeMenu(spec: CustomThemeSpec, active: Boolean, onNotice: (EditorNotice?) -> Unit): List<MenuEntry> {
    val full = CustomThemes.specs.size >= CustomThemeSpec.MAX
    return listOf(
        MenuAction(
            translated("oneconfig.themes.menu.activate", "Activate"),
            { activateCustom(spec) },
            icon = "check-circle",
            enabled = !active,
        ),
        MenuAction(
            translated("oneconfig.themes.menu.edit", "Edit"),
            { if (!active) activateCustom(spec) },
            icon = "paintbrush",
        ),
        MenuAction(
            translated("oneconfig.themes.custom.duplicate", "Duplicate"),
            { duplicateCustom(spec)?.let { onNotice(it) } },
            icon = "copy-plus",
            enabled = !full,
            disabledReason = if (full) fullMessage() else null,
        ),
        MenuAction(
            translated("oneconfig.themes.custom.copy", "Copy code"),
            { onNotice(copyCodeNotice(spec)) },
            icon = "copy",
        ),
        MenuSeparator,
        MenuAction(
            translated("oneconfig.themes.custom.delete", "Delete"),
            { askDeleteCustom(spec) },
            icon = "trash",
            danger = true,
        ),
    )
}

private fun baseLabel(base: CustomBase): String = when (base) {
    CustomBase.POLYFROSTED -> translated("oneconfig.themes.custom.base.polyfrosted", "Polyfrosted")
    CustomBase.POLYGLASS -> translated("oneconfig.themes.custom.base.polyglass", "PolyGlass")
    CustomBase.MINECRAFT -> translated("oneconfig.themes.custom.base.minecraft", "Minecraft")
}

private fun cornersLabel(corners: CustomCorners): String = when (corners) {
    CustomCorners.ROUNDED -> translated("oneconfig.themes.custom.corners.rounded", "Rounded")
    CustomCorners.SOFT -> translated("oneconfig.themes.custom.corners.soft", "Soft")
    CustomCorners.SQUARE -> translated("oneconfig.themes.custom.corners.square", "Square")
}

private fun fixContrast(color: Color, background: Color, target: Float): Color {
    val toward = if (contrastRatio(Color.White, background) >= contrastRatio(Color.Black, background)) Color.White else Color.Black
    var t = 0f
    var fixed = color
    while (t < 1f && contrastRatio(fixed, background) < target) {
        t += 0.05f
        fixed = lerp(color, toward, t.coerceAtMost(1f))
    }
    return fixed
}

@Composable
internal fun CustomThemeCard(modifier: Modifier, onNotice: (EditorNotice?) -> Unit) {
    val theme = LocalTheme.current
    val specs = CustomThemes.specs
    val active = theme.customSpec
    var pickedId by rememberSaveable { mutableStateOf(active?.id ?: specs.firstOrNull()?.id) }
    LaunchedEffect(active?.id) {
        if (active != null) pickedId = active.id
    }
    val shown = active ?: specs.firstOrNull { it.id == pickedId } ?: specs.firstOrNull()
    var startOpen by remember { mutableStateOf(false) }
    ThemeCardShell(
        title = translated("oneconfig.themes.custom", "Custom"),
        selected = active != null,
        modifier = modifier,
        onClick = { if (shown != null) activateCustom(shown) else startOpen = true },
        preview = {
            if (shown != null) {
                CustomThemePreview(shown, Modifier.size(ThemePreviewWidth, ThemePreviewHeight))
            } else {
                EmptyCustomPreview(Modifier.size(ThemePreviewWidth, ThemePreviewHeight))
            }
        },
        controls = {
            if (shown != null) {
                ThemeSelect(specs, shown, { it.name }) { spec ->
                    pickedId = spec.id
                    if (active != null) activateCustom(spec)
                }
            }
            if (specs.size < CustomThemeSpec.MAX) StartFromButton(startOpen) { startOpen = it }
            if (active == null) {
                IconButton(
                    "clipboard",
                    translated("oneconfig.themes.tooltip.custom_paste", "Add a theme from a code"),
                    { onNotice(pastedThemeNotice()) },
                    style = IconButtonStyle.Framed,
                    size = ControlSize.Compact,
                    tooltipPlacement = TooltipPlacement.AboveCenter,
                )
            }
        },
        footer = {
            ThemeCaption(translated("oneconfig.themes.caption.custom", "Make your own from any theme"))
        },
        menu = {
            if (shown == null) emptyList() else customThemeMenu(shown, active?.id == shown.id, onNotice)
        },
    )
}

@Composable
private fun StartFromButton(open: Boolean, onOpenChange: (Boolean) -> Unit) {
    Box {
        IconButton(
            "plus",
            translated("oneconfig.themes.tooltip.custom_new", "New custom theme"),
            { onOpenChange(!open) },
            style = IconButtonStyle.Framed,
            size = ControlSize.Compact,
            tooltipPlacement = TooltipPlacement.AboveCenter,
        )
        if (open) DropdownMenu(true, { onOpenChange(false) }, startFromEntries())
    }
}

private fun startFromEntries(): List<MenuEntry> = buildList {
    add(MenuHeading(translated("oneconfig.themes.custom.start_from", "Start from")))
    ThemeRegistry.registry.filterNot { it.isCustom }.forEach { source ->
        add(MenuAction(source.name, { createCustomFrom(source) }, leading = { StartFromPreview(source.previewAsset) }))
    }
    val yours = CustomThemes.specs.toList()
    if (yours.isNotEmpty()) {
        add(MenuHeading(translated("oneconfig.themes.custom.start_from.yours", "Your custom themes")))
        yours.forEach { spec ->
            add(
                MenuAction(
                    spec.name,
                    { createCustomFrom(customTheme(spec)) },
                    leading = { CustomThemePreview(spec, Modifier.size(StartFromPreviewWidth, StartFromPreviewHeight)) },
                )
            )
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun StartFromPreview(asset: String) {
    Image(
        painter = painterResource("/assets/oneconfig/images/$asset.svg"),
        contentDescription = null,
        modifier = Modifier.size(StartFromPreviewWidth, StartFromPreviewHeight),
    )
}

@Composable
private fun EmptyCustomPreview(modifier: Modifier) {
    val theme = LocalTheme.current
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val radius = 8.dp.toPx()
            drawRoundRect(
                theme.borderColor,
                cornerRadius = CornerRadius(radius),
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
            )
        }
        Icon("plus", color = theme.textColorSecondary, modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun CustomThemePreview(spec: CustomThemeSpec, modifier: Modifier) {
    val accent = spec.accent ?: PlayerAccent
    val corner = when (spec.corners) {
        CustomCorners.ROUNDED -> 4.dp
        CustomCorners.SOFT -> 2.dp
        CustomCorners.SQUARE -> 0.dp
    }
    Canvas(modifier) {
        val r = CornerRadius(corner.toPx())
        val line = CornerRadius(1.dp.toPx())
        val pad = size.width * 0.04f
        drawRoundRect(spec.background, cornerRadius = CornerRadius(corner.toPx() * 1.5f))
        drawRoundRect(spec.border, cornerRadius = CornerRadius(corner.toPx() * 1.5f), style = Stroke(1f))
        val sideW = size.width * 0.24f
        drawRoundRect(spec.surface, Offset(pad, pad), Size(sideW, size.height - pad * 2), r)
        val barH = size.height * 0.05f
        repeat(5) { i ->
            drawRoundRect(
                if (i == 1) accent else spec.textSecondary,
                Offset(pad * 2, pad * 2.5f + i * size.height * 0.13f),
                Size(sideW - pad * 2, barH),
                line,
            )
        }
        val x0 = pad * 2 + sideW
        drawRoundRect(spec.text, Offset(x0, pad * 1.5f), Size(size.width * 0.28f, barH * 1.2f), line)
        val top = pad * 2.5f + barH * 1.2f
        val cols = 3
        val rows = 2
        val gap = pad * 0.8f
        val gw = (size.width - x0 - pad - gap * (cols - 1)) / cols
        val gh = (size.height - top - pad - gap * (rows - 1)) / rows
        for (row in 0 until rows) for (col in 0 until cols) {
            val o = Offset(x0 + col * (gw + gap), top + row * (gh + gap))
            drawRoundRect(spec.surface, o, Size(gw, gh), r)
            drawRoundRect(spec.border, o, Size(gw, gh), r, style = Stroke(1f))
            drawRoundRect(accent, Offset(o.x + gw * 0.15f, o.y + gh * 0.7f), Size(gw * 0.7f, barH * 0.9f), line)
            drawRoundRect(spec.textSecondary, Offset(o.x + gw * 0.15f, o.y + gh * 0.3f), Size(gw * 0.5f, barH * 0.7f), line)
        }
    }
}

internal sealed interface EditorNotice {
    class Message(val text: String) : EditorNotice
    class Import(val code: String, val name: String) : EditorNotice
}

internal fun pastedThemeNotice(): EditorNotice {
    val code = ClipboardHelper.getString()?.trim().orEmpty()
    val decoded = CustomThemeSpec.decode(code, "preview")
        ?: return EditorNotice.Message(translated("oneconfig.themes.custom.paste_invalid", "That isn't a theme code"))
    return EditorNotice.Import(code, decoded.name)
}

@Composable
internal fun rememberThemeCodeNotice(): MutableState<EditorNotice?> {
    val state = remember { mutableStateOf<EditorNotice?>(null) }
    LaunchedEffect(state.value) {
        if (state.value is EditorNotice.Message) {
            delay(2500)
            state.value = null
        }
    }
    return state
}

@Composable
internal fun ThemeCodeNotice(notice: EditorNotice, onNotice: (EditorNotice?) -> Unit) {
    val theme = LocalTheme.current
    when (notice) {
        is EditorNotice.Message -> Text(notice.text, color = theme.textColorSecondary, fontSize = 13.sp)
        is EditorNotice.Import -> Row(horizontalArrangement = Arrangement.spacedBy(EditorGap), verticalAlignment = Alignment.CenterVertically) {
            Text(
                translated("oneconfig.themes.custom.import_prompt", "Add \"%s\" to your themes?", notice.name),
                color = theme.textColor,
                fontSize = 13.sp,
            )
            TextButton(
                translated("oneconfig.themes.custom.import", "Add"),
                {
                    val imported = CustomThemes.import(notice.code)
                    if (imported == null) {
                        onNotice(EditorNotice.Message(fullMessage()))
                    } else {
                        onNotice(null)
                        activateCustom(imported)
                    }
                },
                style = ControlStyle.Primary,
                size = ControlSize.Compact,
            )
            TextButton(translated("oneconfig.themes.custom.cancel", "Cancel"), { onNotice(null) }, size = ControlSize.Compact)
        }
    }
}

@Composable
internal fun CustomThemeEditor(spec: CustomThemeSpec, noticeState: MutableState<EditorNotice?>) {
    val theme = LocalTheme.current
    var notice by noticeState
    val update: (CustomThemeSpec) -> Unit = { CustomThemes.update(it) }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = PageGutter)
            .background(theme.modCardBackground, theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        EditorRow(translated("oneconfig.themes.custom.name", "Name")) {
            var text by remember(spec.id) { mutableStateOf(spec.name) }
            TextField(
                text,
                { value ->
                    val clean = value.filter { it != '§' && !it.isISOControl() }.take(CustomThemeSpec.NAME_LIMIT)
                    text = clean
                    if (clean.isNotBlank()) update(spec.copy(name = clean))
                },
                Modifier.width(EditorFieldWidth),
                translated("oneconfig.themes.custom.name.placeholder", "Theme name"),
            )
        }
        EditorRow(translated("oneconfig.themes.custom.base", "Base")) {
            FlexibleLayout(horizontalSpacing = EditorGap, verticalSpacing = EditorGap) {
                SegmentedChips(
                    CustomBase.entries,
                    spec.base,
                    { if (it != spec.base) update(spec.copy(base = it)) },
                    label = ::baseLabel,
                    size = ControlSize.Compact,
                )
                IconSegments(
                    listOf(IconSegment(false, "moon", lightHint(false)), IconSegment(true, "sun", lightHint(true))),
                    spec.light,
                ) { update(spec.copy(light = it)) }
            }
        }
        EditorRow(translated("oneconfig.themes.custom.colours", "Colours")) {
            FlexibleLayout(horizontalSpacing = EditorGap, verticalSpacing = EditorGap) {
                ColorField(translated("oneconfig.themes.custom.colour.background", "Background"), spec.background, false) {
                    update(spec.copy(background = it))
                }
                ColorField(translated("oneconfig.themes.custom.colour.surface", "Surface"), spec.surface, false) {
                    update(spec.copy(surface = it))
                }
                ColorField(translated("oneconfig.themes.custom.colour.text", "Text"), spec.text, false) {
                    update(spec.copy(text = it))
                }
                ColorField(translated("oneconfig.themes.custom.colour.text_secondary", "Secondary text"), spec.textSecondary, false) {
                    update(spec.copy(textSecondary = it))
                }
                ColorField(translated("oneconfig.themes.custom.colour.border", "Border"), spec.border, false) {
                    update(spec.copy(border = it))
                }
            }
        }
        val textLow = contrastRatio(spec.text, spec.background) < TEXT_CONTRAST
        val secondaryLow = contrastRatio(spec.textSecondary, spec.background) < SECONDARY_CONTRAST
        if (textLow || secondaryLow) {
            EditorRow("") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon("info", color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
                    Text(
                        if (textLow) {
                            translated("oneconfig.themes.custom.contrast.text", "Text is hard to read on this background")
                        } else {
                            translated("oneconfig.themes.custom.contrast.secondary", "Secondary text is hard to read on this background")
                        },
                        color = theme.textColorSecondary,
                        fontSize = 13.sp,
                    )
                    TextButton(
                        translated("oneconfig.themes.custom.contrast.fix", "Fix"),
                        {
                            update(
                                spec.copy(
                                    text = if (textLow) fixContrast(spec.text, spec.background, TEXT_CONTRAST) else spec.text,
                                    textSecondary = if (secondaryLow) fixContrast(spec.textSecondary, spec.background, SECONDARY_CONTRAST) else spec.textSecondary,
                                )
                            )
                        },
                        size = ControlSize.Compact,
                    )
                }
            }
        }
        EditorRow(translated("oneconfig.themes.custom.accent", "Accent")) {
            Row(horizontalArrangement = Arrangement.spacedBy(EditorGap), verticalAlignment = Alignment.CenterVertically) {
                ColorField(null, spec.accent ?: PlayerAccent, spec.accent != null) { update(spec.copy(accent = it)) }
                Chip(
                    translated("oneconfig.themes.custom.accent.player", "Use my accent"),
                    spec.accent == null,
                    { if (spec.accent != null) update(spec.copy(accent = null)) },
                    size = ControlSize.Compact,
                )
            }
        }
        EditorRow(translated("oneconfig.themes.custom.corners", "Corners")) {
            SegmentedChips(
                CustomCorners.entries,
                spec.corners,
                { if (it != spec.corners) update(spec.copy(corners = it)) },
                label = ::cornersLabel,
                size = ControlSize.Compact,
            )
        }
        if (spec.base == CustomBase.POLYGLASS) {
            EditorRow(translated("oneconfig.themes.custom.glass", "Glass")) {
                Row(
                    Modifier.height(EditorControlHeight),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SliderControl(
                        spec.glass.toFloat(),
                        { value -> value.roundToInt().let { if (it != spec.glass) update(spec.copy(glass = it)) } },
                        0f,
                        100f,
                        1f,
                        Modifier.width(EditorFieldWidth),
                    )
                    Text("${spec.glass}%", color = theme.textColor, fontSize = 13.sp)
                }
            }
        }
        EditorRow("") {
            FlexibleLayout(horizontalSpacing = EditorGap, verticalSpacing = EditorGap) {
                TooltipBox(translated("oneconfig.themes.tooltip.custom_copy", "Copy a code you can share"), placement = TooltipPlacement.AboveCenter) {
                    TextButton(translated("oneconfig.themes.custom.copy", "Copy code"), "copy", { notice = copyCodeNotice(spec) }, size = ControlSize.Compact)
                }
                TooltipBox(translated("oneconfig.themes.tooltip.custom_paste", "Add a theme from a code"), placement = TooltipPlacement.AboveCenter) {
                    TextButton(translated("oneconfig.themes.custom.paste", "Paste code"), "clipboard", { notice = pastedThemeNotice() }, size = ControlSize.Compact)
                }
                TextButton(
                    translated("oneconfig.themes.custom.duplicate", "Duplicate"),
                    "copy-plus",
                    { duplicateCustom(spec)?.let { notice = it } },
                    size = ControlSize.Compact,
                )
                TooltipBox(translated("oneconfig.themes.tooltip.custom_reset", "Put the colours back to the base theme's"), placement = TooltipPlacement.AboveCenter) {
                    TextButton(
                        translated("oneconfig.themes.custom.reset", "Reset to base"),
                        "refresh",
                        { update(CustomThemeSpec.fromBase(spec.id, spec.name, spec.base, spec.light)) },
                        size = ControlSize.Compact,
                    )
                }
                key(spec.id) {
                    ArmedConfirmButton(
                        translated("oneconfig.themes.custom.delete", "Delete"),
                        { deleteCustom(spec) },
                        armedText = translated("oneconfig.themes.custom.delete_confirm", "Click again to delete"),
                        size = ControlSize.Compact,
                    )
                }
            }
        }
        notice?.let { current ->
            EditorRow("") { ThemeCodeNotice(current) { notice = it } }
        }
    }
}

internal fun fullMessage(): String =
    translated("oneconfig.themes.custom.full", "You can keep up to %d custom themes", CustomThemeSpec.MAX)

@Composable
private fun EditorRow(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.width(EditorLabelWidth).height(EditorControlHeight), contentAlignment = Alignment.CenterStart) {
            Text(label, color = LocalTheme.current.textColorSecondary, fontSize = 13.sp)
        }
        Box(Modifier.weight(1f)) { content() }
    }
}

private fun opaqueHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

@Composable
private fun ColorField(label: String?, color: Color, selected: Boolean, onChange: (Color) -> Unit) {
    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }
    val shape = theme.buttonShape
    val circle = theme.shapes.circle
    val hex = opaqueHex(color)
    Box {
        Row(
            Modifier
                .height(EditorControlHeight)
                .liquidGlass(shape)
                .background(theme.componentBackground, shape)
                .border(if (selected) 2.dp else 1.dp, if (selected) Accent else theme.borderColor, shape)
                .glassHover(shape, pull = 0.75.dp)
                .oneConfigClickable(
                    if (label == null) hex else "$label $hex",
                    { expanded = !expanded },
                    shape = shape,
                    indication = false,
                )
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(color.copy(alpha = 1f), circle)
                    .border(1.dp, theme.borderColor, circle)
            )
            if (label != null) Text(label, color = theme.textColor, fontSize = 13.sp, maxLines = 1)
            Text(hex, color = theme.textColorSecondary, fontSize = 13.sp, maxLines = 1)
        }
        if (expanded) {
            val model = rememberColorPickerModel(color.copy(alpha = 1f), hasAlpha = false)
            ColorPickerPopup(
                model,
                { onChange(it.copy(alpha = color.alpha)) },
                { expanded = false },
            )
        }
    }
}
