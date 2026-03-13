package org.polyfrost.oneconfig.internal.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.internal.ui.themes.UIBranding
import org.polyfrost.oneconfig.internal.ui.themes.UITheme
import org.polyfrost.oneconfig.internal.ui.themes.UITypography

private val Theme = UITheme(
    Color(0xF211171C),
    Color(0xB3151C22),
    Color(0xB2232D32),
    Color(0x59232D32),
    Color(0xFF1A2229),

    Color(0x1AFFFFFF),
    Color(0xFFD5DBFF),
    Color(0xFF757883),

    RoundedCornerShape(16.dp),
    RoundedCornerShape(7.dp),
    RoundedCornerShape(12.dp),
    RoundedCornerShape(3.dp),

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

@Composable
fun OneConfigInterface(
    windowWidth: Float,
    windowHeight: Float,
    initialRoute: Any = ModsGraph,
    shellBackdrop: DrawScope.(Offset) -> Unit = {}
) {
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
            LocalTheme provides Theme,
            LocalLifecycleOwner provides Lifecycle,
            LocalViewModelStoreOwner provides OCViewModelStoreOwner,
        ) {
            Shell(windowWidth, windowHeight, shellBackdrop)
        }
    }
}
