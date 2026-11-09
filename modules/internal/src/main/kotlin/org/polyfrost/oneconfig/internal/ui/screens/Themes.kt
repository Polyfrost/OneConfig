package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.Tooltip
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.dropdown.SimpleDropdownMenu
import org.polyfrost.oneconfig.internal.ui.components.glassHover
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.keepSearchFocus
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
import org.polyfrost.oneconfig.internal.ui.components.liquidGlass
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.sound.SoundThemeSelection
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundTheme
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemeSpec
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemes
import org.polyfrost.oneconfig.internal.ui.themes.GlassTint
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.PaletteAccent
import org.polyfrost.oneconfig.internal.ui.themes.PaletteInfo
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.concentric
import org.polyfrost.oneconfig.internal.ui.themes.customSpec
import org.polyfrost.oneconfig.internal.ui.themes.isCustom
import org.polyfrost.oneconfig.internal.ui.themes.palette
import org.polyfrost.oneconfig.internal.ui.themes.syncNotificationTheme
import org.polyfrost.oneconfig.internal.ui.themes.previewAsset
import org.polyfrost.oneconfig.internal.ui.themes.isLight

const val THEMES_ID = "themes.json"

private val ThemeCardGap = 20.dp

private const val ThemeColumns = 3

private val VariantToggleHeight = 30.dp

private val CardActionSize = 22.dp

private val ToggleSegmentWidth = 28.dp

internal val ThemePreviewWidth = 112.dp

internal val ThemePreviewHeight = 72.dp

private val AccentSwatchSize = 16.dp

private val AccentSwatchGap = 3.dp

private val ThemeFooterHeight = 18.dp

private val ThemeFooterGap = 12.dp

private val SoundCardMinWidth = 240.dp

private const val DARK_SUFFIX = " Dark"

private const val LIGHT_SUFFIX = " Light"

private val UITheme.family: String get() = name.removeSuffix(DARK_SUFFIX).removeSuffix(LIGHT_SUFFIX)

@Composable
fun Themes() {
    val revision = ConfigRegistry.revision
    val tree = ConfigRegistry.findTree(THEMES_ID)
    if (tree == null) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            ThemesHeader()
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No preferences available.", color = LocalTheme.current.textColorSecondary)
            }
        }
        return
    }
    key(revision, tree) {
        ConfigScreen(tree, pageKey = THEMES_ID, header = { ThemesHeader() })
    }
}

@Composable
private fun ThemesHeader() {
    Column(
        modifier = Modifier.padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        val families = ThemeRegistry.registry.filterNot { it.isCustom }.groupBy { it.palette?.group ?: it.family }
        val cards: List<Pair<String, List<UITheme>>?> = families.map { (family, variants) -> family to variants } + null
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(ThemeCardGap),
        ) {
            cards.chunked(ThemeColumns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ThemeCardGap)) {
                    row.forEach { card ->
                        if (card == null) CustomThemeCard(Modifier.weight(1f)) else ThemeCard(card.first, card.second, Modifier.weight(1f))
                    }
                    repeat(ThemeColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        LocalTheme.current.customSpec?.let { CustomThemeEditor(it) }
        if (!Platform.screen().supportsBackdropGlass()) {
            Text(
                "PolyGlass shows without frosted glass while VulkanMod is installed on this Minecraft version.",
                color = LocalTheme.current.textColorSecondary,
                modifier = Modifier.padding(horizontal = PageGutter),
            )
        }
        SoundThemePicker()
    }
}

@Composable
private fun ThemeCard(family: String, variants: List<UITheme>, modifier: Modifier) {
    val theme = LocalTheme.current
    val active = if (theme.isCustom) null else variants.firstOrNull { it.name == theme.name }
    var pickedName by rememberSaveable(family) {
        mutableStateOf((active ?: variants.firstOrNull { it.isLight == theme.isLight } ?: variants.first()).name)
    }
    LaunchedEffect(active) {
        if (active != null) pickedName = active.name
    }
    val shown = active ?: variants.firstOrNull { it.name == pickedName } ?: variants.first()
    val isSelected = active != null
    val onVariant: (UITheme) -> Unit = { variant ->
        pickedName = variant.name
        if (isSelected) ThemeRegistry.activate(variant) else UiSounds.play(UiSoundEvent.CLICK)
    }
    val palette = shown.palette
    ThemeCardShell(
        title = family,
        selected = isSelected,
        modifier = modifier,
        onClick = { ThemeRegistry.activate(shown) },
        preview = {
            // the Compose resources library needs generated accessors, whereas these paths are only known at runtime
            @Suppress("DEPRECATION")
            Image(
                painter = painterResource("/assets/oneconfig/images/${shown.previewImage}.svg"),
                contentDescription = null,
                modifier = Modifier.size(ThemePreviewWidth, ThemePreviewHeight)
            )
        },
        controls = {
            if (palette != null && variants.size > 2) {
                ThemeDropdown(variants, shown, { it.palette?.label ?: it.name }, onVariant)
            } else {
                if (shown.liquidGlass) TintToggle()
                if (variants.size > 1) VariantToggle(variants, shown, onVariant)
            }
        },
        footer = {
            if (palette != null) {
                PaletteAccentRow(palette)
            } else {
                ThemeCaption(familyCaption(family))
            }
        },
        action = {
            val full = CustomThemes.specs.size >= CustomThemeSpec.MAX
            CardActionButton(
                "copy-plus",
                if (full) fullMessage() else translated("oneconfig.themes.tooltip.make_copy", "Make a custom copy"),
            ) {
                if (!createCustomFrom(shown)) UiSounds.play(UiSoundEvent.CLICK)
            }
        },
    )
}

@Composable
private fun CardActionButton(icon: String, hint: String, onClick: () -> Unit) {
    ThemeHint(hint) {
        val theme = LocalTheme.current
        val source = rememberInteractionSource()
        Box(
            Modifier
                .size(CardActionSize)
                .liquidGlass(theme.buttonShape)
                .background(theme.componentBackground, theme.buttonShape)
                .border(1.dp, theme.borderColor, theme.buttonShape)
                .glassHover(theme.buttonShape, pull = 0.5.dp)
                .keepSearchFocus()
                .clickable(source, indication = null, onClick = onClick)
                .pointerHoverIcon(PointerIcon.Hand),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, color = theme.textColorSecondary, modifier = Modifier.size(12.dp))
        }
    }
}

private val FamilyCaptions = mapOf(
    "polyfrosted" to "Frosted panels, the OneConfig look",
    "polyglass" to "See-through glass over your game",
    "minecraft" to "Blocky, like the vanilla menus",
)

private fun familyCaption(family: String): String {
    val key = family.lowercase().replace(' ', '_')
    val fallback = FamilyCaptions[key] ?: return ""
    return translated("oneconfig.themes.caption.$key", fallback)
}

@Composable
internal fun ThemeCaption(text: String) {
    Text(
        text,
        color = LocalTheme.current.textColorSecondary,
        fontSize = 12.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

@Composable
internal fun ThemeCardShell(
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    preview: @Composable () -> Unit,
    controls: @Composable () -> Unit,
    footer: @Composable () -> Unit,
    action: (@Composable () -> Unit)? = null,
) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current
    val hovered by interactionSource.collectIsHoveredAsState()
    var actionFocused by remember { mutableStateOf(false) }
    val actionAlpha by animateFloatAsState(if (hovered || actionFocused) 1f else 0f)
    val borderColor by animateColorAsState(
        if (selected) Accent else theme.borderColor
    )
    val borderWidth by animateDpAsState(
        if (selected) 3.dp else 1.dp
    )
    Column(
        modifier
            .hoverGlow(theme.backgroundShape)
            .background(theme.modCardBackground, theme.backgroundShape)
            .border(borderWidth, borderColor, theme.backgroundShape)
            .keepSearchFocus()
            .hoverable(interactionSource)
            .clickable(interactionSource, indication = null) {
                // Use clickable because onClick always plays CLICK. ThemeRegistry.activate chooses CLICK or OPEN
                onClick()
            }
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(ThemeFooterGap),
    ) {
        Row(
            Modifier.heightIn(min = ThemePreviewHeight),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            preview()
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    Modifier.fillMaxWidth().height(CardActionSize),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        title,
                        color = theme.textColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (action != null) {
                        Box(
                            Modifier
                                .graphicsLayer { alpha = actionAlpha }
                                .onFocusChanged { actionFocused = it.hasFocus }
                        ) {
                            action()
                        }
                    }
                }
                FlexibleLayout(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                    controls()
                }
            }
        }
        Box(Modifier.fillMaxWidth().height(ThemeFooterHeight), contentAlignment = Alignment.CenterStart) {
            footer()
        }
    }
}

@Composable
private fun VariantToggle(variants: List<UITheme>, shown: UITheme, onPick: (UITheme) -> Unit) {
    SegmentGroup {
        variants.forEach { variant ->
            ToggleSegment(if (variant.isLight) "sun" else "moon", lightHint(variant.isLight), variant == shown) { onPick(variant) }
        }
    }
}

@Composable
internal fun SegmentGroup(content: @Composable () -> Unit) {
    val theme = LocalTheme.current
    Row(
        Modifier
            .height(VariantToggleHeight)
            .liquidGlass(theme.buttonShape)
            .background(theme.componentBackground, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        content()
    }
}

private val TintIcons = listOf("layers", "paintbrush", "cloud")

private val TintHints = listOf("clear" to "Clear", "stained" to "Stained", "biome" to "Biome")

internal fun lightHint(light: Boolean): String =
    if (light) translated("oneconfig.themes.tooltip.light", "Light") else translated("oneconfig.themes.tooltip.dark", "Dark")

@Composable
private fun TintToggle() {
    val selected = GlassTint.mode
    SegmentGroup {
        TintIcons.forEachIndexed { index, icon ->
            val (key, fallback) = TintHints[index]
            ToggleSegment(icon, translated("oneconfig.themes.tooltip.tint.$key", fallback), index == selected) {
                GlassTint.mode = index
                ThemeConfig.selectGlassTint(index)
                UiSounds.play(UiSoundEvent.CLICK)
            }
        }
    }
}

@Composable
internal fun SquareControlButton(icon: String, hint: String, onClick: () -> Unit) {
    ThemeHint(hint) { SquareControlButtonBody(icon, onClick) }
}

@Composable
private fun SquareControlButtonBody(icon: String, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    Box(
        Modifier
            .size(VariantToggleHeight)
            .liquidGlass(theme.buttonShape)
            .background(theme.componentBackground, theme.buttonShape)
            .border(1.dp, theme.borderColor, theme.buttonShape)
            .glassHover(theme.buttonShape, pull = 0.75.dp)
            .keepSearchFocus()
            .clickable(interactionSource, indication = null, onClick = onClick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
    }
}

@Composable
internal fun ThemeHint(text: String, content: @Composable () -> Unit) {
    Tooltip(
        text = { Text(text, color = LocalTheme.current.textColor, fontSize = 12.sp) },
        modifier = Modifier.widthIn(max = 220.dp),
        anchor = Alignment.TopCenter,
        content = content,
    )
}

@Composable
internal fun <T> ThemeDropdown(items: List<T>, shown: T, label: (T) -> String, onPick: (T) -> Unit) {
    val theme = LocalTheme.current
    var expanded by remember { mutableStateOf(false) }
    val interactionSource = rememberInteractionSource()
    Box {
        Row(
            Modifier
                .height(VariantToggleHeight)
                .widthIn(max = 150.dp)
                .liquidGlass(theme.buttonShape)
                .background(theme.componentBackground, theme.buttonShape)
                .border(1.dp, theme.borderColor, theme.buttonShape)
                .padding(horizontal = 10.dp)
                .keepSearchFocus()
                .clickable(interactionSource, indication = null) { expanded = !expanded }
                .pointerHoverIcon(PointerIcon.Hand),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                label(shown),
                color = theme.textColor,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                "down",
                color = theme.textColorSecondary,
                modifier = Modifier.size(14.dp).graphicsLayer { rotationZ = if (expanded) 180f else 0f },
            )
        }
        SimpleDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(0.dp, 8.dp),
        ) {
            Column(
                Modifier
                    .width(200.dp)
                    .clip(theme.popupShape)
                    .background(theme.popupBackground, theme.popupShape)
                    .border(1.dp, theme.borderColor, theme.popupShape)
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items.forEach { item ->
                    val itemSource = rememberInteractionSource()
                    val hovered by itemSource.collectIsHoveredAsState()
                    val selected = item == shown
                    val shape = theme.buttonShape
                    Text(
                        label(item),
                        color = if (selected) theme.accentTextColor else theme.textColor,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                when {
                                    selected -> Accent
                                    hovered -> theme.modCardBackground
                                    else -> Color.Transparent
                                },
                                shape,
                            )
                            .hoverable(itemSource)
                            .clickable(itemSource, indication = null) {
                                expanded = false
                                if (!selected) onPick(item)
                            }
                            .pointerHoverIcon(PointerIcon.Hand)
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PaletteAccentRow(palette: PaletteInfo) {
    val choice = PaletteAccent.choice(palette.group)
    val pick: (Int) -> Unit = { value ->
        PaletteAccent.set(palette.group, value)
        ThemeConfig.savePaletteAccents()
        ThemeRegistry.activeTheme?.let(::syncNotificationTheme)
        UiSounds.play(UiSoundEvent.CLICK)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(AccentSwatchGap)) {
        palette.accents.forEachIndexed { index, accent ->
            val selected = choice == index || (choice == PaletteAccent.RECOMMENDED && index == palette.defaultAccent)
            AccentSwatch(accent.name, selected, { pick(index) }) {
                Box(Modifier.fillMaxSize().background(accent.color, CircleShape))
            }
        }
        AccentSwatch(translated("oneconfig.themes.tooltip.player_accent", "Use my accent"), choice == PaletteAccent.PLAYER, { pick(PaletteAccent.PLAYER) }) {
            Box(
                Modifier.fillMaxSize().background(LocalTheme.current.componentBackground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon("profiles", color = LocalTheme.current.textColor, modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun AccentSwatch(hint: String, selected: Boolean, onPick: () -> Unit, fill: @Composable () -> Unit) {
    ThemeHint(hint) { AccentSwatchBody(selected, onPick, fill) }
}

@Composable
private fun AccentSwatchBody(selected: Boolean, onPick: () -> Unit, fill: @Composable () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    Box(
        Modifier
            .size(AccentSwatchSize)
            .border(if (selected) 2.dp else 1.dp, if (selected) theme.textColor else Color.Transparent, CircleShape)
            .padding(if (selected) 3.dp else 2.dp)
            .then(if (selected) Modifier else Modifier.keepSearchFocus())
            .clickable(interactionSource, indication = null, enabled = !selected, onClick = onPick)
            .pointerHoverIcon(PointerIcon.Hand),
    ) {
        fill()
    }
}

@Composable
internal fun ToggleSegment(icon: String, hint: String, selected: Boolean, onPick: () -> Unit) {
    ThemeHint(hint) { ToggleSegmentBody(icon, selected, onPick) }
}

@Composable
private fun ToggleSegmentBody(icon: String, selected: Boolean, onPick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val shape = theme.buttonShape.concentric(3.dp)
    Box(
        Modifier
            .fillMaxHeight()
            .width(ToggleSegmentWidth)
            .glassHover(shape, pull = 0.75.dp)
            .background(if (selected) theme.textColor.copy(alpha = 0.1f) else Color.Transparent, shape)
            .then(if (selected) Modifier else Modifier.keepSearchFocus())
            .clickable(interactionSource, indication = null, enabled = !selected, onClick = onPick)
            .pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            color = if (selected) theme.textColor else theme.textColorSecondary,
            modifier = Modifier.size(14.dp),
        )
    }
}

private class SoundThemeOption(
    val value: Int,
    val key: String,
    val title: String,
    val caption: String,
)

private val SoundThemeOptions = listOf(
    SoundThemeOption(ThemeConfig.SOUND_THEME_MATCH, "match", "Match Theme", "Follows your theme, now %s"),
    SoundThemeOption(ThemeConfig.SOUND_THEME_ONECONFIG, "oneconfig", "OneConfig", "Soft, modern interface sounds"),
    SoundThemeOption(ThemeConfig.SOUND_THEME_MINECRAFT, "minecraft", "Minecraft", "Classic Minecraft interface sounds"),
)

@Composable
private fun SoundThemePicker() {
    val theme = LocalTheme.current
    val selected = SoundThemeSelection.value
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            translated("oneconfig.themes.sound_theme.title", "Sound Theme"),
            color = theme.textColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = PageGutter),
        )
        BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = PageGutter)) {
            val fit = ((maxWidth + ThemeCardGap) / (SoundCardMinWidth + ThemeCardGap)).toInt()
            val columns = fit.coerceIn(1, SoundThemeOptions.size)
            Column(verticalArrangement = Arrangement.spacedBy(ThemeCardGap)) {
                SoundThemeOptions.chunked(columns).forEach { rowOptions ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ThemeCardGap)) {
                        rowOptions.forEach { option ->
                            SoundThemeCard(option, option.value == selected, Modifier.weight(1f)) {
                                if (selected == option.value) {
                                    UiSounds.play(UiSoundEvent.CLICK)
                                } else {
                                    SoundThemeSelection.value = option.value
                                    ThemeConfig.selectSoundTheme(option.value)
                                }
                            }
                        }
                        repeat(columns - rowOptions.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SoundThemeCard(option: SoundThemeOption, isSelected: Boolean, modifier: Modifier, onPick: () -> Unit) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current
    val matched = UiSoundTheme.of(theme)
    val preview = when (option.value) {
        ThemeConfig.SOUND_THEME_ONECONFIG -> if (theme.isLight) "polyfrosted-light" else "polyfrosted-dark"
        ThemeConfig.SOUND_THEME_MINECRAFT -> if (theme.isLight) "minecraft-light" else "minecraft-dark"
        else -> theme.previewAsset
    }
    val caption = if (option.value == ThemeConfig.SOUND_THEME_MATCH) {
        val now = SoundThemeOptions.first {
            it.value == if (matched == UiSoundTheme.MINECRAFT) ThemeConfig.SOUND_THEME_MINECRAFT else ThemeConfig.SOUND_THEME_ONECONFIG
        }
        translated("oneconfig.themes.sound_theme.${option.key}.caption", option.caption, translated("oneconfig.themes.sound_theme.${now.key}", now.title))
    } else {
        translated("oneconfig.themes.sound_theme.${option.key}.caption", option.caption)
    }

    val borderColor by animateColorAsState(
        if (isSelected) Accent else theme.borderColor
    )
    val borderWidth by animateDpAsState(
        if (isSelected) 3.dp else 1.dp
    )

    Row(
        modifier = modifier
            .hoverGlow(theme.backgroundShape)
            .background(theme.modCardBackground, theme.backgroundShape)
            .border(borderWidth, borderColor, theme.backgroundShape)
            .keepSearchFocus()
            .clickable(interactionSource, indication = null, onClick = onPick)
            .pointerHoverIcon(PointerIcon.Hand)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            @Suppress("DEPRECATION")
            Image(
                painter = painterResource("/assets/oneconfig/images/$preview.svg"),
                contentDescription = null,
                modifier = Modifier.size(70.dp, 45.dp)
            )
            if (option.value == ThemeConfig.SOUND_THEME_MATCH) {
                Box(
                    Modifier
                        .padding(3.dp)
                        .size(18.dp)
                        .background(Accent, theme.buttonShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon("paintbrush", color = theme.accentTextColor, modifier = Modifier.size(11.dp))
                }
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                translated("oneconfig.themes.sound_theme.${option.key}", option.title),
                color = theme.textColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                caption,
                color = theme.textColorSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
