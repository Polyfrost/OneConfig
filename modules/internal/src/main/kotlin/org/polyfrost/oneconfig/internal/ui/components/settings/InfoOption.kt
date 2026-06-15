package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.polyfrost.oneconfig.internal.ui.api.settings.InfoOptionData
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.Accent
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val SuccessColor = Color(0xFF239A60)
private val WarningColor = Color(0xFFFFAB1D)
private val ErrorColor = Color(0xFFFF4444)

@Composable
fun InfoOption(data: InfoOptionData) {
    val theme = LocalTheme.current
    val (iconName, iconColor) = when (data.type) {
        InfoOptionData.Type.Info -> "info" to Accent
        InfoOptionData.Type.Success -> "check-circle" to SuccessColor
        InfoOptionData.Type.Warning -> "alert-circle" to WarningColor
        InfoOptionData.Type.Error -> "x-circle" to ErrorColor
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(iconName, color = iconColor, modifier = Modifier.size(24.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                data.heading,
                modifier = Modifier.fillMaxWidth(),
                color = theme.textColor,
                fontSize = 16.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.Medium,
            )
            data.body?.let {
                Text(
                    it,
                    modifier = Modifier.fillMaxWidth(),
                    color = theme.textColorSecondary,
                    fontSize = 13.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
