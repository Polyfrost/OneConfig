package org.polyfrost.oneconfig.internal.ui.components.effect

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import org.polyfrost.compose.foundation.Image
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.size
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.compose.ui.draw.alpha
import org.polyfrost.compose.ui.graphics.FilterQuality
import org.polyfrost.compose.ui.graphics.copy
import org.polyfrost.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileIcon
import org.polyfrost.oneconfig.internal.ui.components.sizeDp
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

/** The catalog deduplicated and sorted by display name so grid positions never jump */
@Composable
fun rememberEffectCatalog(key: Any? = Unit): List<EffectDescriptor> {
    val revision = EffectCatalog.revision
    return remember(key, revision) {
        EffectCatalog.effects()
            .distinctBy(EffectDescriptor::id)
            .sortedWith { first, second ->
                first.displayName.compareTo(second.displayName, ignoreCase = true)
                    .takeIf { it != 0 } ?: first.id.compareTo(second.id)
            }
    }
}

/** The icon for [id] showing [placeholder] if the effect is unknown */
@Composable
fun EffectIcon(
    id: String,
    modifier: Modifier = Modifier,
    alpha: Float = 1f,
    placeholder: String = "eye-off",
) {
    val revision = EffectCatalog.revision
    val bitmap = remember(id, revision) { EffectCatalog.icon(id) }
    if (bitmap == null) {
        val secondary = LocalTheme.current.textColorSecondary
        Box(modifier, contentAlignment = Alignment.Center) {
            Icon(
                placeholder,
                modifier = Modifier.size((modifier.sizeDp(18f) - 4f).coerceAtLeast(0f).dp),
                color = secondary.copy(alpha = secondary.alphaF * alpha),
            )
        }
        return
    }
    Image(bitmap, null, modifier.alpha(alpha), filterQuality = FilterQuality.None)
}

internal val EffectTileIcon: PickerTileIcon<EffectDescriptor> = { effect, modifier, alpha ->
    EffectIcon(effect.id, modifier, alpha)
}

/** Default translation keys where every composable taking one accepts an override */
object EffectStrings {
    const val Title = "oneconfig.effectlist.title"
    const val Search = "oneconfig.effectlist.search"
    const val Empty = "oneconfig.effectlist.empty"
    const val Unavailable = "oneconfig.effectlist.unavailable"
}
