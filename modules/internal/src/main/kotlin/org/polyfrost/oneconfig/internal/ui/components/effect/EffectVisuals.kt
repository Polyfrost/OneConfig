package org.polyfrost.oneconfig.internal.ui.components.effect

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.picker.PickerTileIcon
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
    val bitmap = remember(id, revision) { EffectCatalog.icon(id)?.toComposeImageBitmap() }
    if (bitmap == null) {
        Icon(
            placeholder,
            modifier = modifier.aspectRatio(1f).padding(2.dp),
            color = LocalTheme.current.textColorSecondary.copy(alpha = alpha),
        )
        return
    }
    Image(bitmap, contentDescription = null, modifier = modifier.aspectRatio(1f), alpha = alpha, filterQuality = FilterQuality.None)
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
