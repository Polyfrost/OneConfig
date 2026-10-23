package org.polyfrost.oneconfig.internal.ui.components.settings

import androidx.compose.runtime.Composable
import org.polyfrost.oneconfig.internal.ui.api.settings.ItemListOptionData
import org.polyfrost.oneconfig.internal.ui.components.item.ItemIcon
import org.polyfrost.oneconfig.internal.ui.components.item.ItemStrings
import org.polyfrost.oneconfig.internal.ui.components.item.ItemTileIcon
import org.polyfrost.oneconfig.internal.ui.components.item.rememberItemCatalog

@Composable
fun ItemListOption(data: ItemListOptionData) = TileListOption(
    data,
    rememberItemCatalog(data.prop),
    ItemTileIcon,
    titleKey = ItemStrings.Title,
    searchPlaceholderKey = ItemStrings.Search,
    emptyKey = ItemStrings.Empty,
    unavailableKey = ItemStrings.Unavailable,
    rowIcon = { id, _, modifier -> ItemIcon(id, modifier) },
)
