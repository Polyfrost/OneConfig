package org.polyfrost.oneconfig.internal.ui.pages

import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.config.v1.ConfigManager
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.internal.ConfigVisualizer
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.Notifications
import org.polyfrost.oneconfig.internal.ui.OneConfigUI
import org.polyfrost.polyui.component.Component
import org.polyfrost.polyui.component.Drawable
import org.polyfrost.polyui.component.extensions.*
import org.polyfrost.polyui.component.impl.*
import org.polyfrost.polyui.data.PolyImage
import org.polyfrost.polyui.event.Event
import org.polyfrost.polyui.unit.Align
import org.polyfrost.polyui.unit.Vec2
import java.nio.file.StandardOpenOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

internal enum class TreeSource {
    CONFIG, // Comes from OneConfig's config manager
    COMMAND, // Comes from another mod's command, which we assume opens it's config UI
    COMPAT, // Comes from the native compat layer, will open the mod config ui, known config libs get registered as config
}

private val LOGGER = LogManager.getLogger("OneConfig/UI/ModsPage")

internal fun ModsPage(trees: Map<TreeSource, Set<Tree>>): Drawable {
    if (trees.isEmpty()) {
        return Group(
            Text("oneconfig.mods.none", fontSize = 24f).setFont { medium },
            Text("oneconfig.mods.none.desc", fontSize = 14f),
            size = Vec2(1130f, 635f),
            alignment = Align(main = Align.Content.Start, cross = Align.Content.Center, pad = Vec2(18f, 18f), mode = Align.Mode.Vertical, wrap = Align.Wrap.NEVER),
        ).named("oneconfig.mods")
    }

    // todo add categories
    val modOrderFile = ConfigManager.internal().folder.resolve("mods_ordering")
    val order: List<String>? = if (modOrderFile.exists()) {
        modOrderFile.readText().split(',')
    } else null

    val sb = StringBuilder()
    return Group(
        children = trees.flatMap { (source, treeSet) ->
            treeSet.mapNotNull { tree ->
                if (tree.getMetadata<Any?>("hidden") != null) return@mapNotNull null
                if (tree.title == null || tree.id == null) {
                    val metadataKeys = tree.metadata?.keys?.joinToString(", ", prefix = "[", postfix = "]") ?: "[]"
                    val treeSnapshot = runCatching { tree.toString() }.getOrElse { "<toString failed: ${it::class.simpleName}>" }
                    LOGGER.warn(
                        "Skipping tree with missing info (source={}, id={}, title={}, metadataKeys={}, childCount={}, type={}, snapshot={})",
                        source,
                        tree.id ?: "<null>",
                        tree.title ?: "<null>",
                        metadataKeys,
                        tree.map.size,
                        tree::class.java.name,
                        treeSnapshot,
                    )
                    return@mapNotNull null
                }
                ModCard(source, tree).events {
                    Event.Lifetime.Removed then {
                        sb.append(tree.id).append(',')
                        false
                    }
                }
            }
        }.reorder(order),
        visibleSize = Vec2(1130f, 635f),
        alignment = Align(line = Align.Line.Start, pad = Vec2(18f, 18f)),
    ).makeRearrangeableGrid().named("oneconfig.mods").events {
        Event.Lifetime.Removed then {
            ConfigManager.internal().folder.resolve("mods_ordering").writeText(sb.toString(), Charsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
        }
    }
}

private fun List<Component>.reorder(order: List<String>?): Array<Component> {
    if (order == null || order.isEmpty()) return this.toTypedArray()
    val map = this.associateBy { it.name } as MutableMap
    try {
        val out = order.mapNotNull { map.remove(it) }
        return (out + map.values).toTypedArray()
    } catch (e: Exception) {
        LOGGER.error("Failed to reorder mod list, the ordering file may be corrupted. Reverting to default ordering.", e)
        return this.toTypedArray()
    }
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
            Text(tree.title ?: tree.id, fontSize = 16f).setFont { medium },
            radii = modBoxBotRad,
            alignment = barAlign,
            size = Vec2(256f, 36f),
        ).also { it.acceptsInput = true }.setPalette { brand.fg },
        alignment = modBoxAlign,
    ).onClick { _ ->
        when (source) {
            TreeSource.CONFIG -> OneConfigUI.openPage(ConfigVisualizer.INSTANCE.get(tree))
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
    }.named(tree.id)
}

private fun ModCardImage(tree: Tree): Drawable {
    val banner = tree.getMetadata<PolyImage>("banner")
    if (banner != null) {
        return Image(banner).onInit {
            size = size.coerceAtMost(Vec2(256f, 104f))
        }
    }

    val configuredIcon = tree.getMetadata<PolyImage>("icon")
    if (configuredIcon != null) {
        return Image(configuredIcon).onInit {
            size = size.coerceAtMost(Vec2(64f, 64f))
        }
    }

    // Otherwise, just return text
    return try {
        Text(tree.title ?: tree.id, fontSize = 18f).setFont { semiBold }
    } catch (e: Exception) {
        LOGGER.error("Failed to load name of a mod card somehow! If you see this, contact us to get a reward!", e)
        // Shouldn't happen ever, might as well add it just in case
        Image(defaultModImage).onInit {
            size = size.coerceAtMost(Vec2(64f, 64f))
        }
    }
}
