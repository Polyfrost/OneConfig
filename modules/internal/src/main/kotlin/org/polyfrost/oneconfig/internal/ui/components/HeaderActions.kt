package org.polyfrost.oneconfig.internal.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.polyfrost.oneconfig.api.config.v1.Property
import org.polyfrost.oneconfig.api.config.v1.ShareCodeResult
import org.polyfrost.oneconfig.api.config.v1.Tree
import org.polyfrost.oneconfig.api.config.v1.applyShareCode
import org.polyfrost.oneconfig.api.config.v1.toShareCode
import org.polyfrost.oneconfig.api.notifications.v1.Notifications
import org.polyfrost.oneconfig.api.ui.v1.components.Confirm
import org.polyfrost.oneconfig.api.ui.v1.components.ControlSize
import org.polyfrost.oneconfig.api.ui.v1.components.MenuAction
import org.polyfrost.oneconfig.api.ui.v1.components.MenuEntry
import org.polyfrost.oneconfig.api.ui.v1.components.MenuSeparator
import org.polyfrost.oneconfig.api.ui.v1.components.MoreButton
import org.polyfrost.oneconfig.internal.ui.api.ConfigData
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.api.ConfigSource
import org.polyfrost.oneconfig.internal.ui.components.settings.bumpResetEpoch
import org.polyfrost.oneconfig.internal.ui.screens.confirmReset
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme
import org.polyfrost.oneconfig.utils.v1.ClipboardHelper

private val PageFilterWidth = 240.dp

@Composable
internal fun PageFilterBar() {
    val theme = LocalTheme.current
    GlobalSearchBar(
        leading = { Icon("filter", color = theme.textColorSecondary, modifier = Modifier.size(15.dp)) },
        modifier = Modifier.width(PageFilterWidth),
        size = ControlSize.Regular,
    )
}

@Composable
internal fun HeaderMoreButton(entries: () -> List<MenuEntry>) {
    var expanded by remember { mutableStateOf(false) }
    val shown = if (expanded) remember(expanded) { entries() } else emptyList()
    MoreButton(
        shown,
        expanded,
        { expanded = it },
        Modifier.size(32.dp),
        contentDescription = translated("oneconfig.header.more", "More actions"),
        size = ControlSize.Regular,
    )
}

private fun shareCodePrefix(id: String): String = "oneconfig:$id:"

private fun editableTree(config: ConfigData): Tree? {
    val tree = ConfigRegistry.findTree(config.id) ?: return null
    if (config.source != ConfigSource.OC || tree.getMetadata<Boolean>("ui_only") == true) return null
    return tree
}

internal fun hasModHeaderMenu(id: String, openOriginal: Runnable?): Boolean {
    if (openOriginal != null) return true
    val config = ConfigRegistry.findById(id) ?: return false
    return editableTree(config) != null
}

internal fun modHeaderMenu(id: String, openOriginal: Runnable?): List<MenuEntry> {
    val config = ConfigRegistry.findById(id) ?: return emptyList()
    val tree = editableTree(config)
    val prefix = shareCodePrefix(id)
    val clipboard = runCatching { ClipboardHelper.getString() }.getOrNull()?.trim()
    return buildList {
        if (tree != null) {
            add(
                MenuAction(
                    translated("oneconfig.header.mod.copy_code", "Copy settings code"),
                    { copySettingsCode(config, tree) },
                    icon = "copy",
                ),
            )
            add(
                MenuAction(
                    translated("oneconfig.header.mod.paste_code", "Paste settings code"),
                    { if (clipboard != null) pasteSettingsCode(config, tree, clipboard) },
                    icon = "clipboard",
                    enabled = clipboard?.startsWith(prefix) == true,
                    disabledReason = translated("oneconfig.header.mod.paste_code.none", "Copy a settings code for this mod first."),
                ),
            )
        }
        if (openOriginal != null) {
            if (isNotEmpty()) add(MenuSeparator)
            add(
                MenuAction(
                    translated("oneconfig.header.mod.original_menu", "Open original menu"),
                    { openOriginal.run() },
                    icon = "external-link",
                ),
            )
        }
        if (tree != null) {
            add(MenuSeparator)
            add(
                MenuAction(
                    translated("oneconfig.mods.menu.reset", "Reset to defaults"),
                    { confirmReset(config, tree) },
                    icon = "refresh",
                    danger = true,
                ),
            )
        }
    }
}

private fun copySettingsCode(config: ConfigData, tree: Tree) {
    val title = config.title.asRenderText()
    runCatching { ClipboardHelper.setString(tree.toShareCode(shareCodePrefix(config.id))) }
        .onSuccess {
            Notifications.success(
                translated("oneconfig.header.mod.copied", "Settings code copied"),
                translated("oneconfig.header.mod.copied.text", "Paste it into %s on another install to use the same settings.", title),
            )
        }
        .onFailure {
            Notifications.error(
                translated("oneconfig.header.mod.copy_failed", "Couldn't copy settings"),
                it.message ?: it.javaClass.simpleName,
            )
        }
}

private fun pasteSettingsCode(config: ConfigData, tree: Tree, code: String) {
    Confirm.ask(
        title = translated("oneconfig.header.mod.paste.title", "Paste settings into %s?", config.title.asRenderText()),
        text = translated(
            "oneconfig.header.mod.paste.text",
            "Settings in the code replace the ones you have now. Anything the code leaves out stays as it is.",
        ),
        confirmLabel = translated("oneconfig.header.mod.paste.confirm", "Paste settings"),
        onConfirm = { applySettingsCode(config, tree, code) },
        icon = "clipboard",
    )
}

private fun applySettingsCode(config: ConfigData, tree: Tree, code: String) {
    val failedTitle = translated("oneconfig.header.mod.paste_failed", "Couldn't paste settings")
    when (val result = tree.applyShareCode(code, shareCodePrefix(config.id))) {
        is ShareCodeResult.Success -> {
            refreshTree(tree)
            Notifications.success(
                translated("oneconfig.header.mod.pasted", "Settings pasted"),
                if (result.applied.size == 1) translated("oneconfig.header.mod.pasted.one", "Applied 1 setting.")
                else translated("oneconfig.header.mod.pasted.text", "Applied %s settings.", result.applied.size),
            )
        }
        ShareCodeResult.WrongPrefix -> Notifications.error(
            failedTitle,
            translated("oneconfig.header.mod.paste.wrong_mod", "That code is for a different mod."),
        )
        is ShareCodeResult.Corrupt -> Notifications.error(
            failedTitle,
            translated("oneconfig.header.mod.paste.corrupt", "That code is damaged or incomplete."),
        )
    }
}

private fun refreshTree(tree: Tree) {
    for (node in tree.map.values) {
        when (node) {
            is Property<*> -> bumpResetEpoch(node)
            is Tree -> refreshTree(node)
        }
    }
}
