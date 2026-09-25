package org.polyfrost.oneconfig.internal.ui.hud.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.hud.v1.GlobalHudSettings
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.LegacyHudMarker
import org.polyfrost.oneconfig.internal.ui.components.IconButton
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.onClick
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.components.settings.SwitchControl
import org.polyfrost.oneconfig.internal.ui.hud.GlobalHudSettingsBridge
import org.polyfrost.oneconfig.internal.ui.hud.components.Dropdown
import org.polyfrost.oneconfig.internal.ui.hud.components.NumberSpinner
import org.polyfrost.oneconfig.internal.ui.hud.screens.sections.Section
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/**
 * The "Global HUD Settings" panel of the design studio
 *
 * Hides every HUD at once and forces a font, text weight and background style onto all of them,
 * writing through [GlobalHudSettingsBridge]
 */
@Composable
fun GlobalHudSettingsPanel(
    modifier: Modifier = Modifier,
    onClose: () -> Unit,
) {
    val theme = LocalTheme.current
    Box(
        modifier = Modifier
            .width(360.dp)
            .then(modifier)
            .background(theme.popupBackground, theme.backgroundShape)
            .border(1.dp, theme.borderColor, theme.backgroundShape),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Global HUD", color = theme.textColor, fontSize = 18.sp)
                IconButton("close") { onClose() }
            }
            Text(
                "Applies to all HUDs at once.",
                color = theme.textColorSecondary,
                fontSize = 12.sp,
            )

            Section("Visibility") {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SwitchRow(GlobalHudSettings.enabled, "HUD overlay") {
                        GlobalHudSettingsBridge.setEnabled(it)
                    }
                    Text(
                        "Hides every HUD outside the editor.",
                        color = theme.textColorSecondary,
                        fontSize = 12.sp,
                    )
                }
            }

            Section("Text") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Replaces each HUD's own font and weight.",
                        color = theme.textColorSecondary,
                        fontSize = 12.sp,
                    )
                    SwitchRow(GlobalHudSettings.overrideFont, "One font for all") {
                        GlobalHudSettingsBridge.setFontOverride(it)
                    }
                    if (GlobalHudSettings.overrideFont) {
                        Dropdown(
                            "Font",
                            GlobalHudSettings.font,
                            { GlobalHudSettingsBridge.setFont(it) }
                        )
                    }
                    SwitchRow(GlobalHudSettings.overrideTextWeight, "One weight for all") {
                        GlobalHudSettingsBridge.setTextWeightOverride(it)
                    }
                    if (GlobalHudSettings.overrideTextWeight) {
                        Dropdown(
                            "Weight",
                            GlobalHudSettings.textWeight,
                            { GlobalHudSettingsBridge.setTextWeight(it) }
                        )
                    }
                }
            }

            Section("Background") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Replaces each HUD's background and corner radius.",
                        color = theme.textColorSecondary,
                        fontSize = 12.sp,
                    )
                    SwitchRow(GlobalHudSettings.overrideShowBackground, "Override visibility") {
                        GlobalHudSettingsBridge.setShowBackgroundOverride(it)
                    }
                    if (GlobalHudSettings.overrideShowBackground) {
                        SwitchRow(GlobalHudSettings.showBackground, "Show backgrounds") {
                            GlobalHudSettingsBridge.setShowBackground(it)
                        }
                    }
                    SwitchRow(GlobalHudSettings.overrideBackgroundRadius, "Override corner radius") {
                        GlobalHudSettingsBridge.setBackgroundRadiusOverride(it)
                    }
                    if (GlobalHudSettings.overrideBackgroundRadius) {
                        NumberSpinner(
                            "Radius", "px",
                            GlobalHudSettings.backgroundRadius,
                            { GlobalHudSettingsBridge.setBackgroundRadius(it) },
                            0f, 32f, 1f, width = 140.dp,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ApplyOverridesButton()
                Text(
                    "Bakes the overrides into every HUD's own settings.",
                    color = theme.textColorSecondary,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun SwitchRow(checked: Boolean, label: String, onCheckedChange: (Boolean) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SwitchControl(checked, onCheckedChange)
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
    }
}

@Composable
private fun ApplyOverridesButton() {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        if (isHovered) Accent.copy(alpha = 0.75f) else Accent,
    )

    Box(
        modifier = Modifier
            .pointerHoverIcon(PointerIcon.Hand)
            .background(bgColor, theme.buttonShape)
            .onClick(interactionSource) { applyOverridesToAllHuds() }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("Apply to all HUDs", color = theme.accentTextColor, fontSize = 13.sp)
    }
}

/** Bakes the active global overrides into every HUD's own stored settings, placed or not yet */
private fun applyOverridesToAllHuds() {
    val targets = LinkedHashSet<Hud>()
    targets.addAll(HudManager.activeInstances)
    targets.addAll(HudManager.providers())
    val applyFont = GlobalHudSettings.overrideFont
    val font = GlobalHudSettings.font
    val applyWeight = GlobalHudSettings.overrideTextWeight
    val weight = GlobalHudSettings.textWeight
    val applyBackground = GlobalHudSettings.overrideShowBackground
    val showBackground = GlobalHudSettings.showBackground
    val applyRadius = GlobalHudSettings.overrideBackgroundRadius
    val radius = GlobalHudSettings.backgroundRadius
    if (!applyFont && !applyWeight && !applyBackground && !applyRadius) return
    Snapshot.withMutableSnapshot {
        for (hud in targets) {
            // legacy HUDs draw their own look, so there is nothing to bake values into
            if (hud is LegacyHudMarker) continue
            if (applyFont) hud.font = font
            if (applyWeight) hud.textWeight = weight
            if (applyBackground) hud.showBackground = showBackground
            if (applyRadius) hud.bgRadius = radius
        }
    }
}

/** Stands in for a per-HUD control that [GlobalHudSettings] currently overrides for every HUD */
@Composable
internal fun GloballyManagedHint(label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = LocalTheme.current.textColor, fontSize = 14.sp)
        Text(
            "Set globally",
            color = LocalTheme.current.textColorSecondary,
            fontSize = 12.sp,
        )
    }
}
