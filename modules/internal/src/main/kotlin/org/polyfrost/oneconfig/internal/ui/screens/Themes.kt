package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.a11y.A11yRole
import org.polyfrost.oneconfig.api.ui.v1.a11y.Motion
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.EmptyState
import org.polyfrost.oneconfig.api.ui.v1.components.IconButton
import org.polyfrost.oneconfig.api.ui.v1.components.IconButtonStyle
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.Select
import org.polyfrost.oneconfig.api.ui.v1.components.TabItem
import org.polyfrost.oneconfig.api.ui.v1.components.TabRow
import org.polyfrost.oneconfig.api.ui.v1.components.TabStyle
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipPlacement
import org.polyfrost.oneconfig.api.ui.v1.components.contextMenu
import org.polyfrost.oneconfig.api.ui.v1.components.hiddenUntilHover
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ThemeConfig
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.layout.FlexibleLayout
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.sound.SoundThemeSelection
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundTheme
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.animateAccentColorAsState
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemeSpec
import org.polyfrost.oneconfig.internal.ui.themes.CustomThemes
import org.polyfrost.oneconfig.internal.ui.themes.GlassTint
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.PaletteAccent
import org.polyfrost.oneconfig.internal.ui.themes.PaletteInfo
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.customSpec
import org.polyfrost.oneconfig.internal.ui.themes.isCustom
import org.polyfrost.oneconfig.internal.ui.themes.palette
import org.polyfrost.oneconfig.internal.ui.themes.syncNotificationTheme
import org.polyfrost.oneconfig.internal.ui.themes.previewAsset

const val THEMES_ID = "themes.json"

private val ThemeCardGap = 20.dp

private const val ThemeColumns = 3

private val CardActionSize = 22.dp

private val ThemeSelectMaxWidth = 150.dp

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
                EmptyState(translated("oneconfig.themes.empty", "No theme settings available"), icon = "paintbrush")
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
        val codeNotice = rememberThemeCodeNotice()
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = PageGutter),
            verticalArrangement = Arrangement.spacedBy(ThemeCardGap),
        ) {
            cards.chunked(ThemeColumns).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ThemeCardGap)) {
                    row.forEach { card ->
                        if (card == null) CustomThemeCard(Modifier.weight(1f)) { codeNotice.value = it } else ThemeCard(card.first, card.second, Modifier.weight(1f))
                    }
                    repeat(ThemeColumns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
        val customSpec = LocalTheme.current.customSpec
        if (customSpec != null) {
            CustomThemeEditor(customSpec, codeNotice)
        } else {
            codeNotice.value?.let { notice ->
                Box(Modifier.padding(horizontal = PageGutter)) { ThemeCodeNotice(notice) { codeNotice.value = it } }
            }
        }
        if (!Platform.screen().supportsBackdropGlass()) {
            Text(
                translated(
                    "oneconfig.themes.no_backdrop_glass",
                    "PolyGlass shows without frosted glass while VulkanMod is installed on this Minecraft version.",
                ),
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
        if (isSelected) ThemeRegistry.activate(variant)
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
                ThemeSelect(variants, shown, { it.palette?.label ?: it.name }, onVariant)
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
        action = { actionModifier ->
            val full = CustomThemes.specs.size >= CustomThemeSpec.MAX
            IconButton(
                "copy-plus",
                if (full) fullMessage() else translated("oneconfig.themes.tooltip.make_copy", "Make a custom copy"),
                { createCustomFrom(shown) },
                actionModifier.size(CardActionSize),
                style = IconButtonStyle.Framed,
                size = ControlSize.Compact,
                tooltipPlacement = TooltipPlacement.AboveCenter,
            )
        },
        menu = {
            val full = CustomThemes.specs.size >= CustomThemeSpec.MAX
            listOf(
                MenuAction(
                    translated("oneconfig.themes.menu.activate", "Activate"),
                    { ThemeRegistry.activate(shown) },
                    icon = "check-circle",
                    enabled = !isSelected,
                ),
                MenuAction(
                    translated("oneconfig.themes.tooltip.make_copy", "Make a custom copy"),
                    { createCustomFrom(shown) },
                    icon = "copy-plus",
                    enabled = !full,
                    disabledReason = if (full) fullMessage() else null,
                ),
            )
        },
    )
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
    action: (@Composable (Modifier) -> Unit)? = null,
    menu: (() -> List<MenuEntry>)? = null,
) {
    val interactionSource = rememberInteractionSource()
    val theme = LocalTheme.current
    val hovered by interactionSource.collectIsHoveredAsState()
    val focused by interactionSource.collectIsFocusedAsState()
    var actionFocused by remember { mutableStateOf(false) }
    val borderColor by animateAccentColorAsState(
        selected,
        if (selected) Accent else theme.borderColor,
        Motion.spec(spring<Float>()),
    )
    val borderWidth by Motion.animateDpAsState(
        if (selected) 3.dp else 1.dp
    )
    Column(
        modifier
            .hoverGlow(theme.backgroundShape)
            .background(theme.modCardBackground, theme.backgroundShape)
            .border(borderWidth, borderColor, theme.backgroundShape)
            .then(if (menu != null) Modifier.contextMenu(entries = menu) else Modifier)
            .hoverable(interactionSource)
            .oneConfigClickable(
                title,
                {
                    // Use clickable because onClick always plays CLICK. ThemeRegistry.activate chooses CLICK or OPEN
                    onClick()
                },
                selected = selected,
                shape = theme.backgroundShape,
                indication = false,
                sound = null,
                interactionSource = interactionSource,
            )
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
                        Box(Modifier.onFocusChanged { actionFocused = it.hasFocus }) {
                            action(Modifier.hiddenUntilHover(hovered || focused || actionFocused))
                        }
                    }
                }
                FlexibleLayout(horizontalSpacing = 6.dp, verticalSpacing = 6.dp) {
                    controls()
                }
            }
        }
        Box(Modifier.fillMaxWidth().heightIn(min = ThemeFooterHeight), contentAlignment = Alignment.CenterStart) {
            footer()
        }
    }
}

internal class IconSegment<T>(val value: T, val icon: String, val hint: String)

@Composable
internal fun <T> IconSegments(segments: List<IconSegment<T>>, selected: T, onSelect: (T) -> Unit) {
    TooltipBox(segments.firstOrNull { it.value == selected }?.hint, placement = TooltipPlacement.AboveCenter) {
        TabRow(
            segments.map { TabItem(it.value, "", icon = it.icon) },
            selected,
            { if (it != selected) onSelect(it) },
            style = TabStyle.Segmented,
            size = ControlSize.Compact,
            fill = false,
        )
    }
}

@Composable
private fun VariantToggle(variants: List<UITheme>, shown: UITheme, onPick: (UITheme) -> Unit) {
    IconSegments(
        variants.map { IconSegment(it, if (it.isLight) "sun" else "moon", lightHint(it.isLight)) },
        shown,
        onPick,
    )
}

private val TintIcons = listOf("layers", "paintbrush", "cloud")

private val TintHints = listOf("clear" to "Clear", "stained" to "Stained", "biome" to "Biome")

internal fun lightHint(light: Boolean): String =
    if (light) translated("oneconfig.themes.tooltip.light", "Light") else translated("oneconfig.themes.tooltip.dark", "Dark")

@Composable
private fun TintToggle() {
    val segments = TintIcons.mapIndexed { index, icon ->
        val (key, fallback) = TintHints[index]
        IconSegment(index, icon, translated("oneconfig.themes.tooltip.tint.$key", fallback))
    }
    IconSegments(segments, GlassTint.mode) { index ->
        GlassTint.mode = index
        ThemeConfig.selectGlassTint(index)
    }
}

@Composable
internal fun <T> ThemeSelect(items: List<T>, shown: T, label: (T) -> String, onPick: (T) -> Unit) {
    Select(
        shown,
        items,
        { if (it != shown) onPick(it) },
        Modifier.widthIn(max = ThemeSelectMaxWidth),
        label = label,
        size = ControlSize.Compact,
    )
}

@Composable
private fun PaletteAccentRow(palette: PaletteInfo) {
    val choice = PaletteAccent.choice(palette.group)
    val pick: (Int) -> Unit = { value ->
        PaletteAccent.set(palette.group, value)
        ThemeConfig.savePaletteAccents()
        ThemeRegistry.activeTheme?.let(::syncNotificationTheme)
    }
    val circle = LocalTheme.current.shapes.circle
    FlexibleLayout(horizontalSpacing = AccentSwatchGap, verticalSpacing = AccentSwatchGap) {
        palette.accents.forEachIndexed { index, accent ->
            val selected = choice == index || (choice == PaletteAccent.RECOMMENDED && index == palette.defaultAccent)
            AccentSwatch(accent.name, selected, { pick(index) }) {
                Box(Modifier.fillMaxSize().background(accent.color, circle))
            }
        }
        AccentSwatch(translated("oneconfig.themes.tooltip.player_accent", "Use my accent"), choice == PaletteAccent.PLAYER, { pick(PaletteAccent.PLAYER) }) {
            Box(
                Modifier.fillMaxSize().background(LocalTheme.current.componentBackground, circle),
                contentAlignment = Alignment.Center,
            ) {
                Icon("profiles", color = LocalTheme.current.textColor, modifier = Modifier.size(8.dp))
            }
        }
    }
}

@Composable
private fun AccentSwatch(hint: String, selected: Boolean, onPick: () -> Unit, fill: @Composable () -> Unit) {
    val theme = LocalTheme.current
    val circle = theme.shapes.circle
    TooltipBox(hint, placement = TooltipPlacement.AboveCenter) {
        Box(
            Modifier
                .size(AccentSwatchSize)
                .border(if (selected) 2.dp else 1.dp, if (selected) theme.textColor else Color.Transparent, circle)
                .padding(if (selected) 3.dp else 2.dp)
                .oneConfigClickable(
                    hint,
                    onPick,
                    role = A11yRole.RadioButton,
                    enabled = !selected,
                    selected = selected,
                    shape = circle,
                    indication = false,
                ),
        ) {
            fill()
        }
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
    val title = translated("oneconfig.themes.sound_theme.${option.key}", option.title)

    val borderColor by animateAccentColorAsState(
        isSelected,
        if (isSelected) Accent else theme.borderColor,
        Motion.spec(spring<Float>()),
    )
    val borderWidth by Motion.animateDpAsState(
        if (isSelected) 3.dp else 1.dp
    )

    Row(
        modifier = modifier
            .hoverGlow(theme.backgroundShape)
            .background(theme.modCardBackground, theme.backgroundShape)
            .border(borderWidth, borderColor, theme.backgroundShape)
            .oneConfigClickable(
                title,
                onPick,
                role = A11yRole.RadioButton,
                selected = isSelected,
                shape = theme.backgroundShape,
                indication = false,
                sound = null,
            )
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
                title,
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
