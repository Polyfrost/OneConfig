package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.compose.foundation.layout.Arrangement
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.Column
import org.polyfrost.compose.foundation.layout.Row
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.foundation.layout.padding
import org.polyfrost.compose.foundation.rememberScrollState
import org.polyfrost.compose.foundation.verticalScroll
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.platform.LocalDensity
import org.polyfrost.compose.ui.text.font.FontWeight
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.poly.GUTTER
import org.polyfrost.oneconfig.internal.ui.poly.PolyWindow
import org.polyfrost.oneconfig.internal.ui.poly.SvgImage
import org.polyfrost.oneconfig.internal.ui.poly.brandTint
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val oneConfigVersion: String? by lazy {
    runCatching {
        ModInfo.loadedMods.firstOrNull { it.id == "oneconfigv1" }?.version
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

private data class CreditSection(val title: String, val entries: List<String>)

private val creditSections = listOf(
    CreditSection(
        "People",
        listOf(
            "Wyvest - OG Team - CEO",
            "Lynith - CTO",
            "Qewer - Designer",
            "Mo2men - Designer",
            "Caledonian - Designer",
            "nextdaydelivery - OG Team - OneConfig Founding Engineer",
            "Lunasa - PolyCompose Founding Engineer",
            "Mona - CompatEngine Founding Engineer",
            "Deftu - OneConfig Major Engineer",
            "ThinkSeal - Major Tester",
            "Zetvue - Sound Design",
            "rachel - Lead Cosmetics Designer (OneClient)",
            "Pauline - OG Team - OneConfig Utilities",
            "xtrm - OG Team - OneConfig Utilities",
            "MoonTidez - OG Team - Designer (v0)",
            "DeDiamondPro - OG Team - OneConfig Config Backend, GUI Frontend, HUD",
        )
    ),
    CreditSection(
        "Special Thanks",
        listOf(
            "Scherso and FireStorm for saving us",
            "ImToggle for his contributions to OneConfig/Polyfrost mods",
            "Sk1er for his lasting impact on Minecraft modding",
            "The Polyfrost team for their support and contributions",
            "All our open source contributors and testers for their help in making modding accessible to everyone",
        )
    ),
)

@Composable
fun CreditsWindow(onClose: () -> Unit) {
    PolyWindow(onClose) {
        Box(Modifier.fillMaxSize().padding(top = 19.dp, bottom = 19.dp)) {
            Credits()
        }
    }
}

@Composable
fun Credits() {
    val theme = LocalTheme.current
    val pxPerDp = LocalDensity.current.density
    val scrollState = rememberScrollState()

    ScrollbarBox(scrollState, GUTTER.dp, Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(horizontal = GUTTER.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                val logoTint = remember(theme.branding.logoPath, Accent) { brandTint(theme.branding.logoPath, Accent) }
                SvgImage(theme.branding.logoPath, 474f, 54f, pxPerDp, recolor = logoTint)
                oneConfigVersion?.let { version ->
                    Text(
                        "v$version",
                        color = theme.textColorSecondary,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Text("by ", color = theme.textColorSecondary, fontSize = 13.sp)
                SvgImage("assets/oneconfig/brand/by-polyfrost.svg", 139f, 24f, pxPerDp)
            }
            creditSections.forEach { section ->
                Text(
                    section.title,
                    color = theme.textColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    section.entries.forEach { entry ->
                        Text(" - $entry", color = theme.textColorSecondary, fontSize = 13.sp)
                    }
                }
            }
            Text(
                "Legal and Corporate:",
                color = theme.textColorSecondary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
            )
            SvgImage("assets/oneconfig/brand/atmofrost.svg", 100f, 41f, pxPerDp, Modifier.padding(bottom = 16.dp))
        }
    }
}
