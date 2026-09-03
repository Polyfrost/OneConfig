package org.polyfrost.oneconfig.internal.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.OneConfigConfig
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.LocalUiOversample
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.localizedString
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.sound.UiSoundEvent
import org.polyfrost.oneconfig.internal.ui.sound.UiSounds
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.MinecraftDark
import org.polyfrost.oneconfig.internal.ui.themes.MinecraftLight
import org.polyfrost.oneconfig.internal.ui.themes.PolyGlassDark
import org.polyfrost.oneconfig.internal.ui.themes.PolyGlassLight
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import kotlin.math.roundToInt

private enum class OnboardingPage { LOOK_AND_FEEL, SOUND, DONE }

private fun themeFor(light: Boolean, minecraft: Boolean): UITheme = when {
    minecraft && light -> MinecraftLight
    minecraft -> MinecraftDark
    light -> PolyGlassLight
    else -> PolyGlassDark
}

@Composable
fun OnboardingFlow(onFinish: () -> Unit) {
    val pages = remember { listOf(OnboardingPage.LOOK_AND_FEEL, OnboardingPage.SOUND, OnboardingPage.DONE) }
    var page by remember { mutableIntStateOf(0) }
    val active = ThemeRegistry.activeTheme
    var lightTheme by remember { mutableStateOf(active?.previewImage?.endsWith("-light") == true) }
    var minecraftStyle by remember { mutableStateOf(active?.previewImage?.startsWith("minecraft") == true) }
    var uiVolume by remember { mutableStateOf(OneConfigConfig.uiSoundVolume) }
    var ambienceVolume by remember { mutableStateOf(OneConfigConfig.uiAmbienceVolume) }

    var themeApplied by remember { mutableStateOf(false) }
    LaunchedEffect(lightTheme, minecraftStyle) {
        if (themeApplied) ThemeRegistry.activate(themeFor(lightTheme, minecraftStyle))
        themeApplied = true
    }

    val finish = {
        OneConfigConfig.completeOnboarding(uiVolume, ambienceVolume)
        onFinish()
    }
    val advance: () -> Unit = { if (page == pages.size - 1) finish() else page++ }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = minOf(maxWidth.value / DESIGN_WIDTH, maxHeight.value / DESIGN_HEIGHT) *
            guiScaleFactor() * UI_SCALE * GUI_DENSITY_TRIM
        CompositionLocalProvider(
            LocalUiOversample provides (LocalUiOversample.current * scale.coerceAtLeast(1f)),
            LocalPanelWidth provides PANEL_WIDTH,
            LocalPanelHeight provides PANEL_HEIGHT,
        ) {
            Box(
                Modifier
                    .align(Alignment.Center)
                    .requiredSize(DESIGN_WIDTH.dp, DESIGN_HEIGHT.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        transformOrigin = TransformOrigin.Center
                    },
            ) {
                Box(
                    Modifier
                        .offset(((DESIGN_WIDTH - PANEL_WIDTH) / 2f).dp, ((DESIGN_HEIGHT - PANEL_HEIGHT) / 2f).dp)
                        .size(PANEL_WIDTH.dp, PANEL_HEIGHT.dp)
                        .shadow(
                            elevation = 29.dp,
                            shape = PANEL_SHAPE,
                            ambientColor = ShadowColor,
                            spotColor = ShadowColor,
                        )
                        .clip(PANEL_SHAPE)
                        .background(PageBackground.copy(alpha = 0.9f))
                        .border(BorderWidth, LocalTheme.current.borderColor, PANEL_SHAPE),
                ) {
                    when (pages[page]) {
                        OnboardingPage.LOOK_AND_FEEL -> LookAndFeelPage(
                            lightTheme, { lightTheme = it },
                            minecraftStyle, { minecraftStyle = it },
                        )

                        OnboardingPage.SOUND -> SoundPage(
                            uiVolume,
                            {
                                uiVolume = it
                                OneConfigConfig.uiSoundVolume = it
                            },
                            ambienceVolume,
                            {
                                ambienceVolume = it
                                OneConfigConfig.uiAmbienceVolume = it
                                UiSounds.refreshAmbience()
                            },
                        )

                        OnboardingPage.DONE -> DonePage()
                    }
                    BottomNavigation(
                        page = page,
                        pageCount = pages.size,
                        onSkip = finish,
                        onBack = { page-- },
                        onNext = advance,
                    )
                }
            }
        }
    }
}

@Composable
private fun LookAndFeelPage(
    light: Boolean,
    onLight: (Boolean) -> Unit,
    minecraft: Boolean,
    onMinecraft: (Boolean) -> Unit,
) {
    Header(
        translate("oneconfig.onboarding.look_and_feel.kicker", "Let's configure the"),
        translate("oneconfig.onboarding.look_and_feel.title", "Look & Feel"),
    )
    val colorsHeight = LABEL_HEIGHT + 32f
    val styleHeight = LABEL_HEIGHT + 155f
    val total = colorsHeight + SECTION_GAP + styleHeight
    var y = CONTENT_TOP + ((CONTENT_BOTTOM - CONTENT_TOP) - total) / 2f
    SectionLabel(translate("oneconfig.onboarding.look_and_feel.colors", "UI Colors"), y)
    Row(Modifier.offset(SECTION_X.dp, (y + LABEL_HEIGHT).dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        ChoiceButton(translate("oneconfig.onboarding.look_and_feel.dark", "Dark"), "moon", !light, CHOICE_WIDTH) {
            onLight(false)
        }
        ChoiceButton(translate("oneconfig.onboarding.look_and_feel.light", "Light"), "sun", light, CHOICE_WIDTH) {
            onLight(true)
        }
    }
    y += colorsHeight + SECTION_GAP
    SectionLabel(translate("oneconfig.onboarding.look_and_feel.style", "UI Style"), y)
    Row(Modifier.offset(SECTION_X.dp, (y + LABEL_HEIGHT).dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        StyleCard("PolyGlass", !minecraft, rounded = true) { onMinecraft(false) }
        StyleCard("Minecraft", minecraft, rounded = false) { onMinecraft(true) }
    }
}

@Composable
private fun SoundPage(
    uiVolume: Float,
    onUiVolume: (Float) -> Unit,
    ambienceVolume: Float,
    onAmbienceVolume: (Float) -> Unit,
) {
    Header(
        translate("oneconfig.onboarding.sound.kicker", "One last thing:"),
        translate("oneconfig.onboarding.sound.title", "Sound"),
    )
    OnboardingText(
        translate(
            "oneconfig.onboarding.sound.description",
            "OneConfig plays its own interface sounds.\nSet them to taste, or drag them to zero for silence.",
        ),
        15,
        Modifier.offset(215.dp, 137.dp).width(450.dp),
        TextPrimary,
        FontWeight.Light,
    )
    val sectionHeight = LABEL_HEIGHT + 32f
    val total = sectionHeight * 2f + SECTION_GAP
    val top = CONTENT_TOP + 60f
    var y = top + ((CONTENT_BOTTOM - top) - total) / 2f
    VolumeSection(translate("oneconfig.onboarding.sound.ui_volume", "UI Sounds"), y, uiVolume, onUiVolume)
    y += sectionHeight + SECTION_GAP
    VolumeSection(
        translate("oneconfig.onboarding.sound.ambience_volume", "Ambience"),
        y,
        ambienceVolume,
        onAmbienceVolume,
    )
}

@Composable
private fun VolumeSection(label: String, y: Float, value: Float, onValue: (Float) -> Unit) {
    SectionLabel(label, y)
    Row(Modifier.offset(SECTION_X.dp, (y + LABEL_HEIGHT).dp), verticalAlignment = Alignment.CenterVertically) {
        VolumeSlider(value, onValue)
        Spacer(Modifier.width(18.dp))
        Box(
            Modifier.width(64.dp).height(26.dp).clip(ppShape(6.dp)).background(ChoiceBackground)
                .border(1.dp, PanelBorderBrush, ppShape(6.dp)),
            contentAlignment = Alignment.CenterStart,
        ) { OnboardingText("${(value * 100f).roundToInt()}%", 12, Modifier.padding(start = 8.dp)) }
    }
}

@Composable
private fun VolumeSlider(value: Float, onValue: (Float) -> Unit) {
    val thumbSize = 13.dp
    var trackWidthPx by remember { mutableStateOf(0f) }
    val progress by animateFloatAsState(value.coerceIn(0f, 1f), animationSpec = spring())
    Box(
        Modifier
            .width(SLIDER_WIDTH.dp)
            .height(13.dp)
            .onSizeChanged { trackWidthPx = it.width.toFloat() }
            .pointerInput(Unit) {
                val thumbPx = thumbSize.toPx()
                var lastValue = Float.NaN
                var lastTickAt = 0L
                fun update(x: Float) {
                    val usableWidth = (trackWidthPx - thumbPx).coerceAtLeast(1f)
                    val fraction = ((x - thumbPx / 2f) / usableWidth).coerceIn(0f, 1f)
                    val stepped = (fraction * VOLUME_STEPS).roundToInt() / VOLUME_STEPS.toFloat()
                    if (stepped != lastValue) {
                        lastValue = stepped
                        val now = System.currentTimeMillis()
                        if (now - lastTickAt >= 70L) {
                            lastTickAt = now
                            UiSounds.play(UiSoundEvent.SLIDER_TICK)
                        }
                    }
                    onValue(stepped)
                }
                awaitEachGesture {
                    val down = awaitFirstDown()
                    update(down.position.x)
                    down.consume()
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break
                        update(change.position.x)
                        change.consume()
                    } while (change.pressed)
                }
            },
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .width(SLIDER_WIDTH.dp)
                .height(7.dp)
                .clip(ppShape(4.dp))
                .background(ChoiceBackground)
                .border(1.dp, PanelBorderBrush, ppShape(4.dp)),
        ) {
            Box(Modifier.fillMaxWidth(progress).height(7.dp).background(Accent))
        }
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset((progress * (trackWidthPx - thumbSize.toPx())).roundToInt(), 0) }
                .size(thumbSize)
                .clip(ppShape(7.dp))
                .background(TextPrimary),
        )
    }
}

@Composable
private fun DonePage() {
    OnboardingIcon("check-circle", TextPrimary, Modifier.offset(374.75.dp, 157.dp).size(130.5.dp))
    OnboardingText(
        translate("oneconfig.onboarding.done.title", "All Done!"),
        32,
        Modifier.offset(0.dp, 311.dp).width(PANEL_WIDTH.dp),
    )
    OnboardingText(
        translateFormatted(
            "oneconfig.onboarding.done.description",
            "Press '%s' at any time to open OneConfig and change any of this again.",
            OneConfigConfig.oneConfigKeybind.displayName(),
        ),
        15,
        Modifier.offset(225.dp, 382.dp).width(430.dp),
        TextPrimary,
        FontWeight.Light,
    )
}

@Composable
private fun Header(kicker: String, title: String) {
    val width = LocalPanelWidth.current
    OnboardingText(kicker, 15, Modifier.offset(0.dp, 35.dp).width(width.dp), TextPrimary, FontWeight.Normal)
    OnboardingText(title, 32, Modifier.offset(0.dp, 66.dp).width(width.dp), TextPrimary, FontWeight.Normal)
}

@Composable
private fun SectionLabel(label: String, y: Float) {
    OnboardingText(
        label,
        15,
        Modifier.offset(SECTION_X.dp, y.dp).width(CHOICE_WIDTH.dp),
        TextPrimary,
        FontWeight.Normal,
        TextAlign.Start,
    )
}

@Composable
private fun ChoiceButton(
    label: String,
    icon: String,
    selected: Boolean,
    width: Float,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    onClick: () -> Unit,
) {
    val contentColor = if (primary) Color.White else TextPrimary
    val interactionSource = rememberInteractionSource()
    Row(
        modifier
            .width(width.dp)
            .height(32.dp)
            .clip(ButtonShape)
            .background(
                when {
                    primary -> Accent
                    selected -> Accent.asSelectedBackground
                    else -> ChoiceBackground
                },
            )
            .border(BorderWidth, if (selected || primary) SolidColor(Accent) else PanelBorderBrush, ButtonShape)
            .onClick(interactionSource, onClick),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OnboardingIcon(icon, contentColor, Modifier.size(17.dp))
        OnboardingText(label, 14, color = contentColor, weight = FontWeight.Medium)
    }
}

@Composable
private fun StyleCard(label: String, selected: Boolean, rounded: Boolean, onClick: () -> Unit) {
    val interactionSource = rememberInteractionSource()
    Box(
        Modifier.size(CHOICE_WIDTH.dp, 155.dp).clip(ButtonShape)
            .background(if (selected) Accent.asSelectedBackground else ChoiceBackground)
            .border(BorderWidth, if (selected) SolidColor(Accent) else PanelBorderBrush, ButtonShape)
            .onClick(interactionSource, onClick),
    ) {
        UiPreview(Modifier.offset(13.dp, 12.dp), rounded)
        OnboardingText(
            label,
            14,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 9.dp),
            TextPrimary,
            FontWeight.Medium,
        )
    }
}

@Composable
private fun UiPreview(modifier: Modifier, rounded: Boolean) {
    val shape = if (rounded) RoundedCornerShape(8.dp) else RectangleShape
    Row(modifier.size(172.dp, 108.dp).clip(shape).border(1.dp, Color(0x1AFFFFFF), shape)) {
        Column(
            Modifier.width(44.dp).height(108.dp).background(Color(0xB3151C22)).padding(8.dp, 7.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.size(29.dp, 7.dp).background(Accent))
            repeat(3) {
                Box(
                    Modifier.width(if (it == 0) 20.dp else 29.dp).height(4.dp)
                        .background(if (it == 0) TextSecondary else TextPrimary),
                )
            }
        }
        Column(Modifier.width(128.dp).height(108.dp).background(Color(0xF211171C)).padding(8.dp, 7.dp)) {
            Row {
                Box(Modifier.width(43.dp).height(7.dp).background(TextPrimary))
                Spacer(Modifier.width(61.dp))
                Box(Modifier.size(7.dp).background(TextPrimary))
            }
            Spacer(Modifier.height(8.dp))
            repeat(3) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) {
                        Box(
                            Modifier.size(34.dp, 23.dp).background(Color(0xFF1A2229))
                                .padding(top = 17.dp).background(Accent),
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
            }
        }
    }
}

@Composable
private fun BottomNavigation(
    page: Int,
    pageCount: Int,
    onSkip: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val panelWidth = LocalPanelWidth.current
    val buttonY = LocalPanelHeight.current - NAV_BOTTOM_INSET
    val nextX = panelWidth - 26f - NAV_BUTTON_WIDTH
    if (page == 0) {
        ChoiceButton(
            translate("oneconfig.onboarding.skip", "Skip"),
            "close",
            false,
            NAV_BUTTON_WIDTH,
            Modifier.offset(26.dp, buttonY.dp),
            onClick = onSkip,
        )
    } else {
        ChoiceButton(
            translate("oneconfig.onboarding.back", "Back"),
            "left-arrow",
            false,
            NAV_BUTTON_WIDTH,
            Modifier.offset(26.dp, buttonY.dp),
            onClick = onBack,
        )
    }
    ChoiceButton(
        if (page == pageCount - 1) translate("oneconfig.onboarding.finish", "Finish")
        else translate("oneconfig.onboarding.next", "Next"),
        "right-arrow",
        false,
        NAV_BUTTON_WIDTH,
        Modifier.offset(nextX.dp, buttonY.dp),
        primary = true,
        onClick = onNext,
    )
    Row(
        Modifier.offset(((panelWidth - (pageCount * 17f - 5f)) / 2f).dp, (buttonY + 10f).dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { index ->
            Box(
                Modifier.size(if (index == page) 12.dp else 10.dp)
                    .clip(ppShape(8.dp))
                    .background(if (index == page) Color(0x80EBF2FF) else Color(0x73232D32))
                    .border(1.dp, if (index == page) Color(0xCCFFFFFF) else Color(0x66FFFFFF), ppShape(8.dp)),
            )
        }
    }
}

@Composable
private fun OnboardingText(
    text: String,
    size: Int,
    modifier: Modifier = Modifier,
    color: Color = TextPrimary,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Center,
) = Text(text, modifier, color = color, fontSize = size.sp, fontWeight = weight, textAlign = align)

@Composable
private fun OnboardingIcon(name: String, color: Color, modifier: Modifier) = Icon(name, color, modifier)

private fun translate(key: String, fallback: String): String = localizedString(key, fallback)

private fun translateFormatted(key: String, fallback: String, vararg args: Any): String =
    if (Platform.i18n().hasTranslation(key)) Platform.i18n().translateString(key, *args)
    else String.format(fallback, *args)

private fun guiScaleFactor(): Float = runCatching {
    (Platform.compatibility().options().guiScale / REFERENCE_GUI_SCALE).coerceIn(0.01f, 1f)
}.getOrDefault(1f)

private const val DESIGN_WIDTH = 1920f
private const val DESIGN_HEIGHT = 1080f
private const val UI_SCALE = DESIGN_WIDTH / 1240f
private const val REFERENCE_GUI_SCALE = 2f
private const val GUI_DENSITY_TRIM = 0.88f
private const val PANEL_WIDTH = 880f
private const val PANEL_HEIGHT = 660f

private const val CONTENT_TOP = 140f
private const val CONTENT_BOTTOM = 557f
private const val SECTION_GAP = 24f
private const val LABEL_HEIGHT = 32f
private const val SECTION_X = 232f
private const val CHOICE_WIDTH = 198f
private const val SLIDER_WIDTH = 332f
private const val NAV_BOTTOM_INSET = 56f
private const val NAV_BUTTON_WIDTH = 100f
private const val VOLUME_STEPS = 20

private val LocalPanelWidth = compositionLocalOf { PANEL_WIDTH }
private val LocalPanelHeight = compositionLocalOf { PANEL_HEIGHT }

private val isMinecraftTheme: Boolean
    @Composable
    @ReadOnlyComposable
    get() = LocalTheme.current.previewImage.startsWith("minecraft")

@Composable
@ReadOnlyComposable
private fun ppShape(radius: Dp): Shape =
    if (isMinecraftTheme) RectangleShape else RoundedCornerShape(radius)

private val PANEL_SHAPE: Shape
    @Composable
    @ReadOnlyComposable
    get() = ppShape(9.dp)

private val ButtonShape: Shape
    @Composable
    @ReadOnlyComposable
    get() = ppShape(9.dp)

private val BorderWidth = 1.5.dp
private const val PanelBorderAngleDeg = 20.0

private val PageBackground: Color
    @Composable get() = LocalTheme.current.pageBackground
private val ShadowColor = Color(0x26000000)

private val ChoiceBackground: Color
    @Composable get() = LocalTheme.current.componentBackground.copy(alpha = 0.5f)
private val TextPrimary: Color
    @Composable get() = LocalTheme.current.textColor
private val TextSecondary: Color
    @Composable get() = LocalTheme.current.textColorSecondary
private val Color.asSelectedBackground: Color get() = copy(alpha = 0.22f)

private val PanelBorderBrush: Brush = object : ShaderBrush() {
    override fun createShader(size: Size): Shader {
        val radians = Math.toRadians(PanelBorderAngleDeg)
        val ux = kotlin.math.cos(radians).toFloat()
        val uy = kotlin.math.sin(radians).toFloat()
        val len = size.width * ux + size.height * uy
        return LinearGradientShader(
            from = Offset.Zero,
            to = Offset(ux * len, uy * len),
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.15f),
                Color.White.copy(alpha = 0.5f),
            ),
            colorStops = listOf(0f, 0.5f, 1f),
        )
    }
}
