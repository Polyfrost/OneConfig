package org.polyfrost.oneconfig.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.compose.rememberNavController
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.LocalNavController
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.shell.Shell
import org.polyfrost.oneconfig.internal.ui.themes.Theme
import org.polyfrost.oneconfig.internal.ui.themes.ThemeRegistry
import org.polyfrost.oneconfig.internal.ui.themes.UIBranding
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.UITypography

val PolyGlassDark = UITheme(
    "polyglass-dark", "PolyGlass Dark",

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

    true,

    RoundedCornerShape(16.dp),
    RoundedCornerShape(7.dp),
    RoundedCornerShape(12.dp),
    RoundedCornerShape(3.dp),
    RoundedCornerShape(6.dp),
    RoundedCornerShape(12.dp),
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
    Color(0x59EBF2FF),
    Color(0xB2EBF2FF),
    Color(0xFFD0D7F3),

    Color(0x4DA3A3A3),
    Color.Black,
    Color(0xFF78818D),
    Color(0xFFD5DBFF),

    Color(0xFF000000),
    Color(0xFF1A2229),

    true,

    RoundedCornerShape(16.dp),
    RoundedCornerShape(7.dp),
    RoundedCornerShape(12.dp),
    RoundedCornerShape(3.dp),
    RoundedCornerShape(6.dp),
    RoundedCornerShape(12.dp),
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
    )
)
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
    Color(0xFF78818D),
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
    )
)

@Composable
fun OneConfigInterface(
    windowWidth: Float,
    windowHeight: Float,
    initialRoute: Any = ModsGraph,
    shellBackdrop: DrawScope.(Offset) -> Unit = {}
) {
    ThemeRegistry.init()

    LocalNavController.current = rememberNavController()

    LaunchedEffect(initialRoute) {
        if (initialRoute != ModsGraph) {
            LocalNavController.wrapper.navigate(initialRoute)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CompositionLocalProvider(
            LocalLifecycleOwner provides Lifecycle,
            LocalViewModelStoreOwner provides OCViewModelStoreOwner,
        ) {
            Theme {
                Shell(windowWidth, windowHeight, shellBackdrop)
            }
        }
    }
}
