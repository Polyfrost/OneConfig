package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.rememberSvgResourcePainter
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
fun Credits() {
    val theme = LocalTheme.current
    val scrollState = rememberScrollState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
            ) {
                rememberSvgResourcePainter(theme.branding.logoPath)?.let { logo ->
                    Image(
                        painter = logo,
                        contentDescription = null,
                        modifier = Modifier.size(474.dp, 54.dp)
                    )
                }
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
                rememberSvgResourcePainter("assets/oneconfig/brand/by-polyfrost.svg")?.let { poly ->
                    Image(
                        painter = poly,
                        contentDescription = "Polyfrost",
                        modifier = Modifier.size(139.dp, 24.dp)
                    )
                }
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
            rememberSvgResourcePainter("assets/oneconfig/brand/atmofrost.svg")?.let { atmofrost ->
                Image(
                    painter = atmofrost,
                    contentDescription = "Atmofrost",
                    modifier = Modifier.padding(bottom = 16.dp).size(100.dp, 41.dp)
                )
            }
        }
        VerticalScrollbar(
            adapter = rememberScrollbarAdapter(scrollState),
            modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight()
        )
    }
}
