package org.polyfrost.oneconfig.test

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.minecraft.client.gui.screens.TitleScreen
import org.polyfrost.oneconfig.api.event.v1.EventManager
import org.polyfrost.oneconfig.api.event.v1.events.TickEvent
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.internal.ui.compose.ComposeScreen
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute

object ComposeUiCheck_Test {
    private var ticks = 0
    private var opened = false

    private val poppins = FontFamily(
        Font("assets/oneconfig/fonts/Poppins/Poppins-Regular.ttf", FontWeight.Normal),
        Font("assets/oneconfig/fonts/Poppins/Poppins-SemiBold.ttf", FontWeight.SemiBold),
    )

    fun initialize() {
        if (System.getProperty("oneconfig.test.composeui") == null) return
        EventManager.register(TickEvent.End::class.java, Runnable {
            if (opened || Platform.screen().current<Any?>() !is TitleScreen || ++ticks < 60) return@Runnable
            opened = true
            when (System.getProperty("oneconfig.test.composeui")) {
                "shell" -> Platform.screen().display(OneConfigUIScreen.forRoute(testRoute()))
                "hud" -> HudManager.openEditor()
                else -> Platform.screen().display(object : ComposeScreen() {
                    @Composable
                    override fun compose() = Sample()
                })
            }
            println("[CUI] opened")
            ComposeUiHarness_Test.start()
        })
    }

    private fun testRoute(): Any? {
        val r = System.getProperty("oneconfig.test.composeui.route") ?: return null
        if (r.startsWith("mod:")) return ModConfigRoute(r.removePrefix("mod:"))
        return Class.forName("org.polyfrost.oneconfig.internal.ui.navigation.graph.${r}Graph").getField("INSTANCE").get(null)
    }

    @Composable
    private fun Sample() {
        var text by remember { mutableStateOf("Edit me") }
        Box(Modifier.fillMaxSize().background(Color(0xFFD0D4DC)).padding(24.dp)) {
            Column(
                Modifier.width(420.dp).shadow(24.dp, RoundedCornerShape(16.dp), spotColor = Color.Cyan).clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1E2128)).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BasicText("compose-ui on PolyRenderer", style = TextStyle(color = Color.White, fontSize = 20.sp, fontFamily = poppins, fontWeight = FontWeight.SemiBold))
                BasicText(
                    "A longer paragraph that should wrap across several lines inside the card, using the Poppins font family resolved through PlatformText.",
                    style = TextStyle(color = Color(0xFFB0B6C0), fontSize = 13.sp, fontFamily = poppins),
                )
                BasicText("Default font, ellipsized after one line because this text keeps going and going", maxLines = 1, overflow = TextOverflow.Ellipsis, style = TextStyle(color = Color.White, fontSize = 13.sp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(Modifier.size(40.dp).background(Color(0xFF2F7DF6), CircleShape))
                    Box(Modifier.size(80.dp, 40.dp).background(Brush.horizontalGradient(listOf(Color(0xFFFF4080), Color(0xFF40A0FF))), RoundedCornerShape(8.dp)))
                    Box(Modifier.size(80.dp, 40.dp).border(2.dp, Color(0xFF40FF90), RoundedCornerShape(8.dp)))
                }
                BasicTextField(text, { text = it }, Modifier.fillMaxWidth().background(Color(0xFF15171C), RoundedCornerShape(6.dp)).padding(8.dp),
                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp, fontFamily = poppins))
                LazyColumn(Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(8.dp)), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(30) { i ->
                        Box(Modifier.fillMaxWidth().height(24.dp).background(Color(0xFF2A2E37), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            BasicText("Row $i", style = TextStyle(color = Color.White, fontSize = 12.sp, fontFamily = poppins))
                        }
                    }
                }
            }
        }
    }
}
