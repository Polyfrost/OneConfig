import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.hud.screens.HudDesignStudio
import org.polyfrost.oneconfig.internal.ui.Theme
import org.polyfrost.oneconfig.internal.ui.shell.Lifecycle
import org.polyfrost.oneconfig.internal.ui.shell.OCViewModelStoreOwner
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

fun main() {
    singleWindowApplication(
        title = "One Config UI Test",
        state = WindowState(width = 1700.dp, height = 900.dp)
    ) {
        Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource("assets/oneconfig/images/background.png"),
            contentDescription = "Background Image",
            contentScale = ContentScale.FillBounds
        )
        CompositionLocalProvider(
            LocalTheme provides Theme,
            LocalLifecycleOwner provides Lifecycle,
            LocalViewModelStoreOwner provides OCViewModelStoreOwner,
        ) {
            HudDesignStudio()
        }
    }
}
