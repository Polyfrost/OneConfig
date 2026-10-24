package org.polyfrost.oneconfig.api.ui.v1.item

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import net.minecraft.world.item.ItemStack
import org.polyfrost.oneconfig.internal.ui.components.item.ItemCatalog
import org.polyfrost.oneconfig.internal.ui.components.item.ItemIcon
import org.polyfrost.oneconfig.internal.ui.components.item.MinecraftItemCatalogService
import org.polyfrost.oneconfig.internal.ui.components.item.rememberItemIconHandle

/** [stack] drawn the way the item selector draws an item, so stacks such as potion variants keep their look */
@Composable
fun ItemStackIcon(
    stack: ItemStack,
    modifier: Modifier = Modifier,
    framed: Boolean = true,
    alpha: Float = 1f,
) {
    val icon = rememberItemIconHandle(Unit) { forHud ->
        (ItemCatalog.platformService() as? MinecraftItemCatalogService)?.openStack(stack, forHud)
    }
    SideEffect { icon?.update(stack) }
    ItemIcon(icon, modifier, framed, alpha)
}
