package org.polyfrost.oneconfig.internal.ui.hud.screens.sections

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudGroup
import org.polyfrost.oneconfig.api.hud.v1.HudGroups
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.ui.v1.components.TooltipBox
import org.polyfrost.oneconfig.api.ui.v1.components.oneConfigClickable
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.localizedLabel
import org.polyfrost.oneconfig.internal.ui.components.rememberInteractionSource
import org.polyfrost.oneconfig.internal.ui.hud.screens.hudGroupMembers
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** The Settings tab of [group], which lists what is inside it and has what its HUDs share, with [onOpen] stepping into one */
@Composable
fun GroupSettings(group: HudGroup, query: String, onOpen: (Set<Hud>) -> Unit) {
    // the group reads the HUD list, which compose cannot see change
    HudManager.instancesRevision.intValue
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PresetActions(group, query)
        CardSection("HUDs", query) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                for (sub in group.subGroups) {
                    GroupRow(sub.name ?: hudGroupMembers(sub), "group") { onOpen(sub.huds.toSet()) }
                }
                for (hud in group.directHuds) {
                    GroupRow(localizedLabel(hud.title) ?: hud.title, "right-arrow") { onOpen(setOf(hud)) }
                }
            }
        }
        SharedSettings(group.huds, query)
    }
}

@Composable
private fun PresetActions(group: HudGroup, query: String) {
    val preset = HudGroups.presetOf(group) ?: return
    if (preset.actions.isEmpty() || HudGroups.presetMembers(group, preset).size < 2) return
    val locked = group.locked
    val names = preset.actions.map { localizedLabel(it.name) ?: it.name }
    CardSection(hudPanelText("arrange", "Arrange"), query, *names.toTypedArray()) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            for ((action, name) in preset.actions.zip(names)) {
                GroupRow(name, "align", enabled = !locked, description = action.description) { HudGroups.runPresetAction(group, action) }
            }
        }
    }
}

@Composable
internal fun GroupRow(text: String, icon: String, enabled: Boolean = true, description: String? = null, onClick: () -> Unit) {
    val theme = LocalTheme.current
    val interactionSource = rememberInteractionSource()
    val isHovered by interactionSource.collectIsHoveredAsState()
    val background by animateColorAsState(theme.textColor.copy(alpha = if (isHovered && enabled) 0.10f else 0f))
    TooltipBox(description) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (enabled) 1f else 0.5f)
                .background(background, theme.buttonShape)
                .oneConfigClickable(text, onClick, enabled = enabled, shape = theme.buttonShape, indication = false, interactionSource = interactionSource)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(icon, color = theme.textColorSecondary, modifier = Modifier.size(14.dp))
            Text(text, color = theme.textColor, fontSize = 13.sp, modifier = Modifier.weight(1f))
        }
    }
}
