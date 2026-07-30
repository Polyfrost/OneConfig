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
    "close" to "/assets/oneconfig/ico/minecraft/close.svg",
    "cloud" to "/assets/oneconfig/ico/minecraft/cloud.svg",
    "combat" to "/assets/oneconfig/ico/minecraft/combat.svg",
    "console" to "/assets/oneconfig/ico/minecraft/console.svg",
    "dots-grid" to "/assets/oneconfig/ico/minecraft/dots-grid.svg",
    "dots-vertical" to "/assets/oneconfig/ico/minecraft/dots-vertical.svg",
    "down" to "/assets/oneconfig/ico/minecraft/down.svg",
    "eye" to "/assets/oneconfig/ico/minecraft/eye.svg",
    "eye-off" to "/assets/oneconfig/ico/minecraft/eye-off.svg",
    "hud" to "/assets/oneconfig/ico/minecraft/hud.svg",
    "hypixel" to "/assets/oneconfig/ico/minecraft/hypixel.svg",
    "info" to "/assets/oneconfig/ico/minecraft/info.svg",
    "italic" to "/assets/oneconfig/ico/minecraft/italic.svg",
    "keyboard" to "/assets/oneconfig/ico/minecraft/keyboard.svg",
    "layers" to "/assets/oneconfig/ico/minecraft/layers.svg",
    "left-arrow" to "/assets/oneconfig/ico/minecraft/left-arrow.svg",
    "lightning-01" to "/assets/oneconfig/ico/minecraft/lightning-01.svg",
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
    "settings" to "/assets/oneconfig/ico/minecraft/settings.svg",
    "shuffle" to "/assets/oneconfig/ico/minecraft/shuffle.svg",
    "spanner" to "/assets/oneconfig/ico/minecraft/spanner.svg",
    "star" to "/assets/oneconfig/ico/minecraft/star.svg",
    "strikethrough" to "/assets/oneconfig/ico/minecraft/strikethrough.svg",
    "sun" to "/assets/oneconfig/ico/minecraft/sun.svg",
    "text" to "/assets/oneconfig/ico/minecraft/text.svg",
    "text-input" to "/assets/oneconfig/ico/minecraft/text-input.svg",
    "tick" to "/assets/oneconfig/ico/minecraft/tick.svg",
    "trash" to "/assets/oneconfig/ico/minecraft/trash.svg",
    "underline" to "/assets/oneconfig/ico/minecraft/underline.svg",
    "up" to "/assets/oneconfig/ico/minecraft/up.svg",
    "uppercase" to "/assets/oneconfig/ico/minecraft/uppercase.svg",
    "x-circle" to "/assets/oneconfig/ico/minecraft/x-circle.svg",
)

val PolyGlassDark = UITheme(
    "polyglass-dark", "PolyGlass Dark",

    Color(0xC411171C),
    Color(0xB3151C22),
    Color(0xB2232D32),
    Color(0xFF74777F),
    Color(0x59232D32),
    Color(0xFF1A2229),
    Color(0xFF1A2229),

    Color(0x1AFFFFFF),
    Color(0xFFD5DBFF),
    Color(0xFF757883),
    Color(0xFFFFFFFF),

    Color(0xFF000000),
    Color(0xFFFFFFFF),

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

val PolyGlassLight = UITheme(
    "polyglass-light", "PolyGlass Light",

    Color(0xB0D0D7F3),
    Color(0xB2D0D7F3),
    Color(0x59EBF2FF),
    Color(0xFF74777F),
    Color(0x59EBF2FF),
    Color(0xB2EBF2FF),
    Color(0xFFD0D7F3),

    Color(0x4DA3A3A3),
    Color.Black,
    Color(0xFF4E5661),
    Color(0xFFD5DBFF),

    Color(0xFF000000),
    Color(0xFF1A2229),

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
    Color(0xFF74777F),
    Color(0x59232D32),
    Color(0xFF1A2229),
    Color(0xFF1A2229),

    Color(0x1AFFFFFF),
    Color(0xFFD5DBFF),
    Color(0xFF757883),
    Color(0xFFFFFFFF),

    Color(0xFF000000),
    Color(0xFFFFFFFF),

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
)
val MinecraftLight = UITheme(
    "minecraft-light", "Minecraft Light",

    Color(0xFFE8EDFF),
    Color(0xFFE8EDFF),
    Color(0x59EBF2FF),
    Color(0xFF74777F),
    Color(0x59EBF2FF),
    Color(0xB2EBF2FF),
    Color(0xFFD0D7F3),

    Color(0x4DA3A3A3),
    Color.Black,
    Color(0xFF4E5661),
    Color(0xFFD5DBFF),

    Color(0xFF000000),
    Color(0xFF1A2229),

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
)
