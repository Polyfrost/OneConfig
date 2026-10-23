package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.internal.ui.api.settings.EffectListOptionData
import org.polyfrost.oneconfig.internal.ui.components.effect.EffectIcon
import org.polyfrost.oneconfig.internal.ui.components.effect.EffectStrings
import org.polyfrost.oneconfig.internal.ui.components.effect.EffectTileIcon
import org.polyfrost.oneconfig.internal.ui.components.effect.rememberEffectCatalog

@Composable
fun EffectListOption(data: EffectListOptionData) = TileListOption(
    data,
    rememberEffectCatalog(data.prop),
    EffectTileIcon,
    titleKey = EffectStrings.Title,
    searchPlaceholderKey = EffectStrings.Search,
    emptyKey = EffectStrings.Empty,
    unavailableKey = EffectStrings.Unavailable,
    rowIcon = { id, _, modifier -> EffectIcon(id, modifier) },
)
