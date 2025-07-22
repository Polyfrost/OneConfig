package org.polyfrost.oneconfig.internal.ui.pages

import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.Notifications
import org.polyfrost.oneconfig.internal.ui.OneConfigUI
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2

internal enum class TreeSource {
    CONFIG, // Comes from OneConfig's config manager
    COMMAND, // Comes from another mod's command, which we assume opens it's config UI
    COMPAT, // Comes from the native compat layer, will open the mod config ui, known config libs get registered as config
}

internal fun ModsPage(trees: Map<TreeSource, Set<Tree>>): Drawable {
    if (trees.isEmpty()) {
        return Group(
            Text("oneconfig.mods.none", fontSize = 24f).setFont { medium },
            Text("oneconfig.mods.none.desc", fontSize = 14f),
            size = Vec2(1130f, 635f),
            alignment = Align(main = Align.Content.Center, pad = Vec2(18f, 18f), mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER),
        ).namedId("EmptyModsPage")
    }

    // todo add categories
    return Group(
        children = trees.flatMap { (source, treeSet) ->
            treeSet.filter {
                it.getMetadata<Any?>("hidden") == null
            }.map { tree ->
                ModCard(source, tree)
            }
        }.toTypedArray(),
        visibleSize = Vec2(1130f, 635f),
        alignment = Align(line = Align.Line.Start, pad = Vec2(18f, 18f)),
    ).makeRearrangeableGrid().namedId("ModsPage")
}

private fun ModCard(
    source: TreeSource,
    tree: Tree
): Drawable {
    return Group(
        Block(
            ModCardImage(tree),
            radii = modBoxTopRad,
            alignment = imageAlign,
            size = Vec2(256f, 104f),
        ).withBorder(1f) { page.border5 }.withHoverStates(),
        Block(
            Text(tree.title, fontSize = 16f).setFont { medium },
            radii = modBoxBotRad,
            alignment = barAlign,
            size = Vec2(256f, 36f),
        ).also { it.acceptsInput = true }.setPalette { brand.fg },
        alignment = modBoxAlign,
    ).onClick { _ ->
        when (source) {
            TreeSource.CONFIG -> OneConfigUI.openPage(ConfigVisualizer.INSTANCE.get(tree), tree.title)
            TreeSource.COMMAND -> Platform.compatibility().executeTreeAction(tree.id)
            TreeSource.COMPAT -> tree.getMetadata<() -> Unit>("on_click")?.invoke() ?: Unit
        }
    }.onRightClick { _ ->
        if (source == TreeSource.CONFIG) {
            PopupMenu(Text("Restore Defaults").setDestructivePalette().withHoverStates().onClick { _ ->
                val backup = ConfigManager.backup().get(tree.id)
                if (backup == null) {
                    Notifications.enqueue(
                        Notifications.Type.Error,
                        "Backup Failure",
                        "Couldn't find the backup for ${tree.id}. You can fix this by manually deleting the config file and restarting your game, which will reset your config! Click here to do so."
                    ).onClick { _ ->
                        ConfigManager.active().delete(tree.id)
                    }
                }
                tree.overwrite(backup, false)
                polyUI.unfocus()
                false
            }, polyUI = polyUI)
        }
    }.namedId("ModCard")
}

private fun ModCardImage(tree: Tree): Drawable {
    val configuredIcon = tree.getMetadata<PolyImage>("icon")
    if (configuredIcon != null) {
        return Image(configuredIcon).onInit {
            size = size.coerceAtMost(Vec2(64f, 64f))
        }
    }

    // Otherwise, just return text
    return try {
        Text(tree.title, fontSize = 18f).setFont { semiBold }
    } catch (e: Exception) {
        // Shouldn't happen ever, might as well add it just in case
        Image(defaultModImage).onInit {
            size = size.coerceAtMost(Vec2(64f, 64f))
        }
    }
}
