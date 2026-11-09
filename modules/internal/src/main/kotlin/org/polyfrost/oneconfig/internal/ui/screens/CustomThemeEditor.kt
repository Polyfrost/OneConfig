package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.PopupProperties
import kotlinx.coroutines.delay
import org.polyfrost.oneconfig.api.ui.v1.components.TextField
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ShellPopup
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.glassHover
import org.polyfrost.oneconfig.internal.ui.components.keepSearchFocus
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerModel
import org.polyfrost.oneconfig.internal.ui.components.settings.ColorPickerPopup
import org.polyfrost.oneconfig.internal.ui.components.settings.SliderControl
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.CustomBase
import org.polyfrost.oneconfig.internal.ui.themes.CustomCorners
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemeSpec
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemes
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.contrastRatio
import org.polyfrost.oneconfig.internal.ui.themes.customSpec
import org.polyfrost.oneconfig.internal.ui.themes.isCustom
import org.polyfrost.oneconfig.internal.ui.themes.previewAsset
import org.polyfrost.oneconfig.internal.ui.themes.isLight
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper
import kotlin.math.roundToInt

private const val TEXT_CONTRAST = 4.5f

private const val SECONDARY_CONTRAST = 3f

private val EditorLabelWidth = 110.dp

private val EditorControlHeight = 30.dp

private val EditorFieldWidth = 320.dp

private val EditorGap = 8.dp

private val DangerColor = Color(0xFFE35B5B)

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
    val toward = if (background.luminance() < 0.5f) Color.White else Color.Black
    var t = 0f
    var fixed = color
    while (t < 1f && contrastRatio(fixed, background) < target) {
        t += 0.05f
        fixed = lerp(color, toward, t.coerceAtMost(1f))
    }
    return fixed
}

@Composable
internal fun CustomThemeCard(modifier: Modifier) {
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
                ThemeDropdown(specs, shown, { it.name }) { spec ->
                    pickedId = spec.id
                    if (active != null) activateCustom(spec) else UiSounds.play(UiSoundEvent.CLICK)
                }
            }
            if (specs.size < CustomThemeSpec.MAX) StartFromButton(startOpen) { startOpen = it }
        },
        footer = {
            ThemeCaption(translated("oneconfig.themes.caption.custom", "Make your own from any theme"))
        },
    )
}

@Composable
private fun StartFromButton(open: Boolean, onOpenChange: (Boolean) -> Unit) {
    val theme = LocalTheme.current
    Box {
        SquareControlButton("plus", translated("oneconfig.themes.tooltip.custom_new", "New custom theme")) { onOpenChange(!open) }
        SimpleDropdownMenu(
            expanded = open,
            onDismissRequest = { onOpenChange(false) },
            offset = DpOffset(0.dp, 8.dp),
        ) {
            Column(
                Modifier
                    .width(240.dp)
                    .clip(theme.popupShape)
                    .background(theme.popupBackground, theme.popupShape)
                    .border(1.dp, theme.borderColor, theme.popupShape)
                    .padding(6.dp),
            ) {
                StartFromHeading(translated("oneconfig.themes.custom.start_from", "Start from"))
                Column(
                    Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    ThemeRegistry.registry.filterNot { it.isCustom }.forEach { source ->
                        StartFromRow(source.name, {
                            @Suppress("DEPRECATION")
                            Image(
                                painter = painterResource("/assets/oneconfig/images/${source.previewAsset}.svg"),
                                contentDescription = null,
                                modifier = Modifier.size(StartFromPreviewWidth, StartFromPreviewHeight),
                            )
                        }) {
                            onOpenChange(false)
                            createCustomFrom(source)
                        }
                    }
                    if (CustomThemes.specs.isNotEmpty()) {
                        StartFromHeading(translated("oneconfig.themes.custom.start_from.yours", "Your custom themes"))
                        CustomThemes.specs.forEach { spec ->
                            StartFromRow(spec.name, {
                                CustomThemePreview(spec, Modifier.size(StartFromPreviewWidth, StartFromPreviewHeight))
                            }) {
                                onOpenChange(false)
                                createCustomFrom(customTheme(spec))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StartFromHeading(text: String) {
    Text(
        text,
        color = LocalTheme.current.textColorSecondary,
        fontSize = 12.sp,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
    )
}

@Composable
private fun StartFromRow(name: String, preview: @Composable () -> Unit, onPick: () -> Unit) {
    val theme = LocalTheme.current
    val source = rememberInteractionSource()
    val hovered by source.collectIsHoveredAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (hovered) theme.modCardBackground else Color.Transparent, theme.buttonShape)
            .hoverable(source)
            .keepSearchFocus()
            .clickable(source, indication = null, onClick = onPick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        preview()
        Text(
            name,
            color = theme.textColor,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
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
    val accent = spec.accent ?: Accent
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

private sealed interface EditorNotice {
    class Message(val text: String) : EditorNotice
    class Import(val code: String, val name: String) : EditorNotice
}

@Composable
internal fun CustomThemeEditor(spec: CustomThemeSpec) {
    val theme = LocalTheme.current
    var notice by remember { mutableStateOf<EditorNotice?>(null) }
    var confirmDelete by remember(spec.id) { mutableStateOf(false) }
    LaunchedEffect(notice) {
        if (notice is EditorNotice.Message) {
            delay(2500)
            notice = null
        }
    }
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
                CustomBase.entries.forEach { base ->
                    EditorButton(baseLabel(base), selected = spec.base == base) {
                        if (spec.base != base) update(spec.copy(base = base))
                    }
                }
                SegmentGroup {
                    ToggleSegment("moon", lightHint(false), !spec.light) { update(spec.copy(light = false)); UiSounds.play(UiSoundEvent.CLICK) }
                    ToggleSegment("sun", lightHint(true), spec.light) { update(spec.copy(light = true)); UiSounds.play(UiSoundEvent.CLICK) }
                }
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
                    EditorButton(translated("oneconfig.themes.custom.contrast.fix", "Fix")) {
                        update(
                            spec.copy(
                                text = if (textLow) fixContrast(spec.text, spec.background, TEXT_CONTRAST) else spec.text,
                                textSecondary = if (secondaryLow) fixContrast(spec.textSecondary, spec.background, SECONDARY_CONTRAST) else spec.textSecondary,
                            )
                        )
                    }
                }
            }
        }
        EditorRow(translated("oneconfig.themes.custom.accent", "Accent")) {
            Row(horizontalArrangement = Arrangement.spacedBy(EditorGap), verticalAlignment = Alignment.CenterVertically) {
                ColorField(null, spec.accent ?: Accent, spec.accent != null) { update(spec.copy(accent = it)) }
                EditorButton(translated("oneconfig.themes.custom.accent.player", "Use my accent"), selected = spec.accent == null) {
                    if (spec.accent != null) update(spec.copy(accent = null))
                }
            }
        }
        EditorRow(translated("oneconfig.themes.custom.corners", "Corners")) {
            FlexibleLayout(horizontalSpacing = EditorGap, verticalSpacing = EditorGap) {
                CustomCorners.entries.forEach { corners ->
                    EditorButton(cornersLabel(corners), selected = spec.corners == corners) {
                        if (spec.corners != corners) update(spec.copy(corners = corners))
                    }
                }
            }
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
                ThemeHint(translated("oneconfig.themes.tooltip.custom_copy", "Copy a code you can share")) {
                    EditorButton(translated("oneconfig.themes.custom.copy", "Copy code"), icon = "copy") {
                        notice = if (ClipboardHelper.setString(spec.encode())) {
                            EditorNotice.Message(translated("oneconfig.themes.custom.copied", "Code copied"))
                        } else {
                            EditorNotice.Message(translated("oneconfig.themes.custom.copy_failed", "Couldn't reach the clipboard"))
                        }
                    }
                }
                ThemeHint(translated("oneconfig.themes.tooltip.custom_paste", "Add a theme from a code")) {
                    EditorButton(translated("oneconfig.themes.custom.paste", "Paste code"), icon = "clipboard") {
                        val code = ClipboardHelper.getString()?.trim().orEmpty()
                        val decoded = CustomThemeSpec.decode(code, "preview")
                        notice = if (decoded == null) {
                            EditorNotice.Message(translated("oneconfig.themes.custom.paste_invalid", "That isn't a theme code"))
                        } else {
                            EditorNotice.Import(code, decoded.name)
                        }
                    }
                }
                EditorButton(translated("oneconfig.themes.custom.duplicate", "Duplicate"), icon = "copy-plus") {
                    val copy = CustomThemes.import(spec.encode())
                    if (copy == null) {
                        notice = EditorNotice.Message(fullMessage())
                    } else {
                        val named = copy.copy(name = customCopyName(spec.name))
                        update(named)
                        activateCustom(named)
                    }
                }
                ThemeHint(translated("oneconfig.themes.tooltip.custom_reset", "Put the colours back to the base theme's")) {
                    EditorButton(translated("oneconfig.themes.custom.reset", "Reset to base"), icon = "refresh") {
                        update(CustomThemeSpec.fromBase(spec.id, spec.name, spec.base, spec.light))
                    }
                }
                EditorButton(
                    if (confirmDelete) translated("oneconfig.themes.custom.delete_confirm", "Click again to delete") else translated("oneconfig.themes.custom.delete", "Delete"),
                    icon = "trash",
                    danger = true,
                    selected = confirmDelete,
                ) {
                    if (!confirmDelete) {
                        confirmDelete = true
                    } else {
                        val fallback = ThemeRegistry.registry.firstOrNull { !it.isCustom && baseOf(it) == spec.base && it.isLight == spec.light }
                            ?: ThemeRegistry.registry.firstOrNull { !it.isCustom }
                        fallback?.let(ThemeRegistry::activate)
                        CustomThemes.remove(spec.id)
                    }
                }
            }
        }
        when (val current = notice) {
            is EditorNotice.Message -> EditorRow("") {
                Text(current.text, color = theme.textColorSecondary, fontSize = 13.sp)
            }
            is EditorNotice.Import -> EditorRow("") {
                Row(horizontalArrangement = Arrangement.spacedBy(EditorGap), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        translated("oneconfig.themes.custom.import_prompt", "Add \"%s\" to your themes?", current.name),
                        color = theme.textColor,
                        fontSize = 13.sp,
                    )
                    EditorButton(translated("oneconfig.themes.custom.import", "Add"), selected = true) {
                        val imported = CustomThemes.import(current.code)
                        if (imported == null) {
                            notice = EditorNotice.Message(fullMessage())
                        } else {
                            notice = null
                            activateCustom(imported)
                        }
                    }
                    EditorButton(translated("oneconfig.themes.custom.cancel", "Cancel")) { notice = null }
                }
            }
            null -> {}
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

@Composable
private fun EditorButton(
    label: String,
    icon: String? = null,
    selected: Boolean = false,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val shape = theme.buttonShape
    val fill = when {
        selected && danger -> DangerColor
        selected -> Accent
        else -> theme.componentBackground
    }
    val ink = when {
        selected && danger -> Color.White
        selected -> theme.accentTextColor
        danger -> DangerColor
        else -> theme.textColor
    }
    Row(
        Modifier
            .height(EditorControlHeight)
            .liquidGlass(shape)
            .background(fill, shape)
            .border(1.dp, if (danger) DangerColor.copy(alpha = 0.6f) else if (selected) Color.Transparent else theme.borderColor, shape)
            .glassHover(shape, pull = 0.75.dp)
            .keepSearchFocus()
            .clickable(interactionSource, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Icon(icon, color = ink, modifier = Modifier.size(14.dp))
        Text(label, color = ink, fontSize = 13.sp, maxLines = 1)
    }
}

private fun opaqueHex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

@Composable
private fun ColorField(label: String?, color: Color, selected: Boolean, onChange: (Color) -> Unit) {
    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = rememberInteractionSource()
    val shape = theme.buttonShape
    Box {
        Row(
            Modifier
                .height(EditorControlHeight)
                .liquidGlass(shape)
                .background(theme.componentBackground, shape)
                .border(if (selected) 2.dp else 1.dp, if (selected) Accent else theme.borderColor, shape)
                .glassHover(shape, pull = 0.75.dp)
                .keepSearchFocus()
                .clickable(interactionSource, indication = null) { expanded = !expanded }
                .pointerHoverIcon(PointerIcon.Hand)
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(color.copy(alpha = 1f), CircleShape)
                    .border(1.dp, theme.borderColor, CircleShape)
            )
            if (label != null) Text(label, color = theme.textColor, fontSize = 13.sp, maxLines = 1)
            Text(opaqueHex(color), color = theme.textColorSecondary, fontSize = 13.sp, maxLines = 1)
        }
        if (expanded) {
            ShellPopup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, 38),
                onDismissRequest = { expanded = false },
                properties = PopupProperties(focusable = true),
            ) {
                val model = remember { ColorPickerModel(color.copy(alpha = 1f), false) }
                ColorPickerPopup(
                    model = model,
                    onColorChanged = { onChange(it.copy(alpha = color.alpha)) },
                    onClose = { expanded = false },
                    chromaCapable = false,
                )
            }
        }
    }
}
