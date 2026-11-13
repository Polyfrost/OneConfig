package org.polyfrost.oneconfig.internal.ui.themes

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp

private val MinecraftIconOverrides = mapOf(
    "cog" to "/assets/oneconfig/ico/minecraft/cog.svg",
    "alert-circle" to "/assets/oneconfig/ico/minecraft/alert-circle.svg",
    "align" to "/assets/oneconfig/ico/minecraft/align.svg",
    "align-center" to "/assets/oneconfig/ico/minecraft/align-center.svg",
    "align-left" to "/assets/oneconfig/ico/minecraft/align-left.svg",
    "align-right" to "/assets/oneconfig/ico/minecraft/align-right.svg",
    "bell" to "/assets/oneconfig/ico/minecraft/bell.svg",
    "bold" to "/assets/oneconfig/ico/minecraft/bold.svg",
    "capitalized" to "/assets/oneconfig/ico/minecraft/capitalized.svg",
    "check-circle" to "/assets/oneconfig/ico/minecraft/check-circle.svg",
    "clipboard" to "/assets/oneconfig/ico/minecraft/clipboard.svg",
    "close" to "/assets/oneconfig/ico/minecraft/close.svg",
    "cloud" to "/assets/oneconfig/ico/minecraft/cloud.svg",
    "combat" to "/assets/oneconfig/ico/minecraft/combat.svg",
    "console" to "/assets/oneconfig/ico/minecraft/console.svg",
    "copy" to "/assets/oneconfig/ico/minecraft/copy.svg",
    "copy-plus" to "/assets/oneconfig/ico/minecraft/copy-plus.svg",
    "dots-grid" to "/assets/oneconfig/ico/minecraft/dots-grid.svg",
    "dots-vertical" to "/assets/oneconfig/ico/minecraft/dots-vertical.svg",
    "down" to "/assets/oneconfig/ico/minecraft/down.svg",
    "external-link" to "/assets/oneconfig/ico/minecraft/external-link.svg",
    "eye" to "/assets/oneconfig/ico/minecraft/eye.svg",
    "eye-off" to "/assets/oneconfig/ico/minecraft/eye-off.svg",
    "hud" to "/assets/oneconfig/ico/minecraft/hud.svg",
    "hypixel" to "/assets/oneconfig/ico/minecraft/hypixel.svg",
    "info" to "/assets/oneconfig/ico/minecraft/info.svg",
    "italic" to "/assets/oneconfig/ico/minecraft/italic.svg",
    "keyboard" to "/assets/oneconfig/ico/minecraft/keyboard.svg",
    "group" to "/assets/oneconfig/ico/minecraft/group.svg",
    "ungroup" to "/assets/oneconfig/ico/minecraft/ungroup.svg",
    "layers" to "/assets/oneconfig/ico/minecraft/layers.svg",
    "left-arrow" to "/assets/oneconfig/ico/minecraft/left-arrow.svg",
    "lightning-01" to "/assets/oneconfig/ico/minecraft/lightning-01.svg",
    "lock" to "/assets/oneconfig/ico/minecraft/lock.svg",
    "lowercase" to "/assets/oneconfig/ico/minecraft/lowercase.svg",
    "maximise" to "/assets/oneconfig/ico/minecraft/maximise.svg",
    "minus" to "/assets/oneconfig/ico/minecraft/minus.svg",
    "moon" to "/assets/oneconfig/ico/minecraft/moon.svg",
    "move" to "/assets/oneconfig/ico/minecraft/move.svg",
    "paintbrush" to "/assets/oneconfig/ico/minecraft/paintbrush.svg",
    "plus" to "/assets/oneconfig/ico/minecraft/plus.svg",
    "preferences" to "/assets/oneconfig/ico/minecraft/preferences.svg",
    "profiles" to "/assets/oneconfig/ico/minecraft/profiles.svg",
    "qol" to "/assets/oneconfig/ico/minecraft/qol.svg",
    "refresh" to "/assets/oneconfig/ico/minecraft/refresh.svg",
    "right-arrow" to "/assets/oneconfig/ico/minecraft/right-arrow.svg",
    "search" to "/assets/oneconfig/ico/minecraft/search.svg",
    "scissors" to "/assets/oneconfig/ico/minecraft/scissors.svg",
    "select-all" to "/assets/oneconfig/ico/minecraft/select-all.svg",
    "settings" to "/assets/oneconfig/ico/minecraft/settings.svg",
    "shuffle" to "/assets/oneconfig/ico/minecraft/shuffle.svg",
    "spanner" to "/assets/oneconfig/ico/minecraft/spanner.svg",
    "star" to "/assets/oneconfig/ico/minecraft/star.svg",
    "star-filled" to "/assets/oneconfig/ico/minecraft/star-filled.svg",
    "strikethrough" to "/assets/oneconfig/ico/minecraft/strikethrough.svg",
    "sun" to "/assets/oneconfig/ico/minecraft/sun.svg",
    "text" to "/assets/oneconfig/ico/minecraft/text.svg",
    "text-input" to "/assets/oneconfig/ico/minecraft/text-input.svg",
    "tick" to "/assets/oneconfig/ico/minecraft/tick.svg",
    "trash" to "/assets/oneconfig/ico/minecraft/trash.svg",
    "underline" to "/assets/oneconfig/ico/minecraft/underline.svg",
    "unlock" to "/assets/oneconfig/ico/minecraft/unlock.svg",
    "up" to "/assets/oneconfig/ico/minecraft/up.svg",
    "uppercase" to "/assets/oneconfig/ico/minecraft/uppercase.svg",
    "x-circle" to "/assets/oneconfig/ico/minecraft/x-circle.svg",
)

val PolyfrostedDark = UITheme(
    "polyfrosted-dark", "Polyfrosted Dark",

    Color(0xC411171C),
    Color(0xB3151C22),
    Color(0xB2232D32),
    Color(0x59232D32),
    Color(0xFF1A2229),
    Color(0xFF1A2229),

    Color(0x1AFFFFFF),
    Color(0xFFD5DBFF),
    Color(0xFF757883),
    Color(0xFFFFFFFF),

    Color(0xFF000000),
    Color(0xFFFFFFFF),
    Color(0xFFFFD700),

    true,

    RoundedCornerShape(Radii.LG),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    RoundedCornerShape(Radii.XS),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    CircleShape,

    UIBranding("assets/oneconfig/brand/oneconfig.svg"),
    UITypography(
        family = FontFamily(
            Font("assets/oneconfig/fonts/Poppins/Poppins-Black.ttf", FontWeight.Black),
            Font("assets/oneconfig/fonts/Poppins/Poppins-ExtraBold.ttf", FontWeight.ExtraBold),
            Font("assets/oneconfig/fonts/Poppins/Poppins-ExtraLight.ttf", FontWeight.ExtraLight),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Regular.ttf", FontWeight.Normal),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Light.ttf", FontWeight.Light),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Medium.ttf", FontWeight.Medium),
            Font("assets/oneconfig/fonts/Poppins/Poppins-SemiBold.ttf", FontWeight.SemiBold),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Thin.ttf", FontWeight.Thin)
        )
    )
)

val PolyfrostedLight = UITheme(
    "polyfrosted-light", "Polyfrosted Light",

    Color(0xB0D0D7F3),
    Color(0xB2D0D7F3),
    Color(0x59EBF2FF),
    Color(0x59EBF2FF),
    Color(0xB2EBF2FF),
    Color(0xFFD0D7F3),

    Color(0x4DA3A3A3),
    Color.Black,
    Color(0xFF4E5661),
    Color(0xFFD5DBFF),

    Color(0xFF000000),
    Color(0xFF1A2229),
    Color(0xFFB57D00),

    true,

    RoundedCornerShape(Radii.LG),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    RoundedCornerShape(Radii.XS),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    CircleShape,

    UIBranding("assets/oneconfig/brand/oneconfig-light.svg"),
    UITypography(
        family = FontFamily(
            Font("assets/oneconfig/fonts/Poppins/Poppins-Black.ttf", FontWeight.Black),
            Font("assets/oneconfig/fonts/Poppins/Poppins-ExtraBold.ttf", FontWeight.ExtraBold),
            Font("assets/oneconfig/fonts/Poppins/Poppins-ExtraLight.ttf", FontWeight.ExtraLight),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Regular.ttf", FontWeight.Normal),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Light.ttf", FontWeight.Light),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Medium.ttf", FontWeight.Medium),
            Font("assets/oneconfig/fonts/Poppins/Poppins-SemiBold.ttf", FontWeight.SemiBold),
            Font("assets/oneconfig/fonts/Poppins/Poppins-Thin.ttf", FontWeight.Thin)
        )
    )
)
val MinecraftDark = UITheme(
    "minecraft-dark", "Minecraft Dark",

    Color(0xFF11171C),
    Color(0xFF11171C),
    Color(0xB2232D32),
    Color(0x59232D32),
    Color(0xFF1A2229),
    Color(0xFF1A2229),

    Color(0x1AFFFFFF),
    Color(0xFFD5DBFF),
    Color(0xFF757883),
    Color(0xFFFFFFFF),

    Color(0xFF000000),
    Color(0xFFFFFFFF),
    Color(0xFFFFD700),

    false,

    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),

    UIBranding("assets/oneconfig/brand/oneconfig-mc.svg"),
    UITypography(
        family = FontFamily(
            Font("assets/oneconfig/fonts/minecraft/Minecraft-Bold.otf", FontWeight.Bold),
            Font("assets/oneconfig/fonts/minecraft/Minecraft-Regular.otf", FontWeight.Normal)
        )
    ),
    MinecraftIconOverrides
).withShapes(RoundedCornerShape(0.dp), RoundedCornerShape(0.dp)).withMinecraftStyle()
val MinecraftLight = UITheme(
    "minecraft-light", "Minecraft Light",

    Color(0xFFE8EDFF),
    Color(0xFFE8EDFF),
    Color(0x59EBF2FF),
    Color(0x59EBF2FF),
    Color(0xB2EBF2FF),
    Color(0xFFD0D7F3),

    Color(0x4DA3A3A3),
    Color.Black,
    Color(0xFF4E5661),
    Color(0xFFD5DBFF),

    Color(0xFF000000),
    Color(0xFF1A2229),
    Color(0xFFB57D00),

    false,

    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),
    RoundedCornerShape(0.dp),

    UIBranding("assets/oneconfig/brand/oneconfig-mc-light.svg"),
    UITypography(
        family = FontFamily(
            Font("assets/oneconfig/fonts/minecraft/Minecraft-Bold.otf", FontWeight.Bold),
            Font("assets/oneconfig/fonts/minecraft/Minecraft-Regular.otf", FontWeight.Normal)
        )
    ),
    MinecraftIconOverrides
).withShapes(RoundedCornerShape(0.dp), RoundedCornerShape(0.dp)).withMinecraftStyle()

val PolyGlassDark = UITheme(
    "polyglass-dark", "PolyGlass Dark",

    Color(0x8C16181D),
    Color(0x99121419),
    Color(0x7322252C),
    Color(0x6620232A),
    Color(0x7322252C),
    Color(0xCC16181D),

    Color(0x14FFFFFF),
    Color(0xFFFFFFFF),
    Color(0xB3FFFFFF),
    Color(0xFFFFFFFF),

    Color(0xFF000000),
    Color(0xFFFFFFFF),
    Color(0xFFFFD60A),

    true,

    RoundedCornerShape(Radii.LG),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    RoundedCornerShape(Radii.XS),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    CircleShape,

    UIBranding("assets/oneconfig/brand/oneconfig-white.svg"),
    PolyfrostedDark.typography,
).withoutGlow().withInsetModFooter().withTranslucentSelection().withLiquidGlass().withCursorEffects()

val PolyGlassLight = UITheme(
    "polyglass-light", "PolyGlass Light",

    Color(0xA6FAFBFF),
    Color(0xB8F2F4F9),
    Color(0x66FFFFFF),
    Color(0x57FFFFFF),
    Color(0x66FFFFFF),
    Color(0xD1F6F8FC),

    Color(0x8CFFFFFF),
    Color(0xFF0E1118),
    Color(0x9E0E1118),
    Color(0xFFFFFFFF),

    Color(0xFF1A2440),
    Color(0xFFFFFFFF),
    Color(0xFFC98A00),

    true,

    RoundedCornerShape(Radii.LG),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    RoundedCornerShape(Radii.XS),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    CircleShape,

    UIBranding("assets/oneconfig/brand/oneconfig-light.svg"),
    PolyfrostedLight.typography,
).withoutGlow().withInsetModFooter().withTranslucentSelection().withLiquidGlass().withCursorEffects()

private fun paletteAccents(vararg entries: Pair<String, Long>): List<PaletteAccentOption> =
    entries.map { (name, argb) -> PaletteAccentOption(name, Color(argb)) }

private fun catppuccinAccents(
    rosewater: Long, flamingo: Long, pink: Long, mauve: Long, red: Long, maroon: Long, peach: Long,
    yellow: Long, green: Long, teal: Long, sky: Long, sapphire: Long, blue: Long, lavender: Long,
): List<PaletteAccentOption> = paletteAccents(
    "Rosewater" to rosewater,
    "Flamingo" to flamingo,
    "Pink" to pink,
    "Mauve" to mauve,
    "Red" to red,
    "Maroon" to maroon,
    "Peach" to peach,
    "Yellow" to yellow,
    "Green" to green,
    "Teal" to teal,
    "Sky" to sky,
    "Sapphire" to sapphire,
    "Blue" to blue,
    "Lavender" to lavender,
)

private const val CATPPUCCIN_DEFAULT_ACCENT = 3

private const val CATPPUCCIN_GROUP = "Catppuccin"

private const val NORD_GROUP = "Nord"

private fun paletteTheme(
    id: String,
    name: String,
    page: Color,
    sidebar: Color,
    chip: Color,
    modCard: Color,
    component: Color,
    popup: Color,
    border: Color,
    text: Color,
    textSecondary: Color,
    accentText: Color,
    shadow: Color,
    thumb: Color,
    favorite: Color,
    track: Color,
    light: Boolean,
    info: PaletteInfo,
): UITheme = UITheme(
    id, name,

    page,
    sidebar,
    chip,
    modCard,
    component,
    popup,

    border,
    text,
    textSecondary,
    accentText,

    shadow,
    thumb,
    favorite,

    true,

    RoundedCornerShape(Radii.LG),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    RoundedCornerShape(Radii.XS),
    RoundedCornerShape(Radii.SM),
    RoundedCornerShape(Radii.MD),
    CircleShape,

    UIBranding(if (light) "assets/oneconfig/brand/oneconfig-light.svg" else "assets/oneconfig/brand/oneconfig.svg"),
    PolyfrostedDark.typography,
).withControlTrackColor(track).withPalette(info)

val CatppuccinLatte = paletteTheme(
    "catppuccin-latte", "Catppuccin Latte",
    page = Color(0xB0EFF1F5),
    sidebar = Color(0xB2E6E9EF),
    chip = Color(0x80CCD0DA),
    modCard = Color(0x80DCE0E8),
    component = Color(0xFFDCE0E8),
    popup = Color(0xFFE6E9EF),
    border = Color(0x99BCC0CC),
    text = Color(0xFF4C4F69),
    textSecondary = Color(0xFF6C6F85),
    accentText = Color(0xFFEFF1F5),
    shadow = Color(0xFF000000),
    thumb = Color(0xFF4C4F69),
    favorite = Color(0xFFDF8E1D),
    track = Color(0xFFACB0BE),
    light = true,
    info = PaletteInfo(
        CATPPUCCIN_GROUP, "Latte",
        catppuccinAccents(
            0xFFDC8A78, 0xFFDD7878, 0xFFEA76CB, 0xFF8839EF, 0xFFD20F39, 0xFFE64553, 0xFFFE640B,
            0xFFDF8E1D, 0xFF40A02B, 0xFF179299, 0xFF04A5E5, 0xFF209FB5, 0xFF1E66F5, 0xFF7287FD,
        ),
        CATPPUCCIN_DEFAULT_ACCENT,
    ),
)

val CatppuccinFrappe = paletteTheme(
    "catppuccin-frappe", "Catppuccin Frapp\u00e9",
    page = Color(0xC4303446),
    sidebar = Color(0xB3292C3C),
    chip = Color(0xB2414559),
    modCard = Color(0x59414559),
    component = Color(0xFF414559),
    popup = Color(0xFF292C3C),
    border = Color(0x8051576D),
    text = Color(0xFFC6D0F5),
    textSecondary = Color(0xFF838BA7),
    accentText = Color(0xFF232634),
    shadow = Color(0xFF232634),
    thumb = Color(0xFFC6D0F5),
    favorite = Color(0xFFE5C890),
    track = Color(0xFF626880),
    light = false,
    info = PaletteInfo(
        CATPPUCCIN_GROUP, "Frapp\u00e9",
        catppuccinAccents(
            0xFFF2D5CF, 0xFFEEBEBE, 0xFFF4B8E4, 0xFFCA9EE6, 0xFFE78284, 0xFFEA999C, 0xFFEF9F76,
            0xFFE5C890, 0xFFA6D189, 0xFF81C8BE, 0xFF99D1DB, 0xFF85C1DC, 0xFF8CAAEE, 0xFFBABBF1,
        ),
        CATPPUCCIN_DEFAULT_ACCENT,
    ),
)

val CatppuccinMacchiato = paletteTheme(
    "catppuccin-macchiato", "Catppuccin Macchiato",
    page = Color(0xC424273A),
    sidebar = Color(0xB31E2030),
    chip = Color(0xB2363A4F),
    modCard = Color(0x59363A4F),
    component = Color(0xFF363A4F),
    popup = Color(0xFF1E2030),
    border = Color(0x80494D64),
    text = Color(0xFFCAD3F5),
    textSecondary = Color(0xFF8087A2),
    accentText = Color(0xFF181926),
    shadow = Color(0xFF181926),
    thumb = Color(0xFFCAD3F5),
    favorite = Color(0xFFEED49F),
    track = Color(0xFF5B6078),
    light = false,
    info = PaletteInfo(
        CATPPUCCIN_GROUP, "Macchiato",
        catppuccinAccents(
            0xFFF4DBD6, 0xFFF0C6C6, 0xFFF5BDE6, 0xFFC6A0F6, 0xFFED8796, 0xFFEE99A0, 0xFFF5A97F,
            0xFFEED49F, 0xFFA6DA95, 0xFF8BD5CA, 0xFF91D7E3, 0xFF7DC4E4, 0xFF8AADF4, 0xFFB7BDF8,
        ),
        CATPPUCCIN_DEFAULT_ACCENT,
    ),
)

val CatppuccinMocha = paletteTheme(
    "catppuccin-mocha", "Catppuccin Mocha",
    page = Color(0xC41E1E2E),
    sidebar = Color(0xB3181825),
    chip = Color(0xB2313244),
    modCard = Color(0x59313244),
    component = Color(0xFF313244),
    popup = Color(0xFF181825),
    border = Color(0x8045475A),
    text = Color(0xFFCDD6F4),
    textSecondary = Color(0xFF7F849C),
    accentText = Color(0xFF11111B),
    shadow = Color(0xFF11111B),
    thumb = Color(0xFFCDD6F4),
    favorite = Color(0xFFF9E2AF),
    track = Color(0xFF585B70),
    light = false,
    info = PaletteInfo(
        CATPPUCCIN_GROUP, "Mocha",
        catppuccinAccents(
            0xFFF5E0DC, 0xFFF2CDCD, 0xFFF5C2E7, 0xFFCBA6F7, 0xFFF38BA8, 0xFFEBA0AC, 0xFFFAB387,
            0xFFF9E2AF, 0xFFA6E3A1, 0xFF94E2D5, 0xFF89DCEB, 0xFF74C7EC, 0xFF89B4FA, 0xFFB4BEFE,
        ),
        CATPPUCCIN_DEFAULT_ACCENT,
    ),
)

private val NordAccents = paletteAccents(
    "Frost Teal" to 0xFF8FBCBB,
    "Frost Cyan" to 0xFF88C0D0,
    "Frost Glacier" to 0xFF81A1C1,
    "Frost Blue" to 0xFF5E81AC,
    "Aurora Red" to 0xFFBF616A,
    "Aurora Orange" to 0xFFD08770,
    "Aurora Yellow" to 0xFFEBCB8B,
    "Aurora Green" to 0xFFA3BE8C,
    "Aurora Purple" to 0xFFB48EAD,
)

val NordDark = paletteTheme(
    "nord", "Nord",
    page = Color(0xC42E3440),
    sidebar = Color(0xB33B4252),
    chip = Color(0xB2434C5E),
    modCard = Color(0x59434C5E),
    component = Color(0xFF434C5E),
    popup = Color(0xFF3B4252),
    border = Color(0x804C566A),
    text = Color(0xFFECEFF4),
    textSecondary = Color(0x99D8DEE9),
    accentText = Color(0xFF2E3440),
    shadow = Color(0xFF000000),
    thumb = Color(0xFFECEFF4),
    favorite = Color(0xFFEBCB8B),
    track = Color(0xFF4C566A),
    light = false,
    info = PaletteInfo(NORD_GROUP, "Nord", NordAccents, 1),
)

val NordLight = paletteTheme(
    "nord-light", "Nord Light",
    page = Color(0xB0ECEFF4),
    sidebar = Color(0xB2E5E9F0),
    chip = Color(0x80D8DEE9),
    modCard = Color(0x80E5E9F0),
    component = Color(0xFFD8DEE9),
    popup = Color(0xFFE5E9F0),
    border = Color(0x99D8DEE9),
    text = Color(0xFF2E3440),
    textSecondary = Color(0xFF4C566A),
    accentText = Color(0xFFECEFF4),
    shadow = Color(0xFF000000),
    thumb = Color(0xFF2E3440),
    favorite = Color(0xFFD08770),
    track = Color(0x664C566A),
    light = true,
    info = PaletteInfo(NORD_GROUP, "Nord Light", NordAccents, 3),
)
