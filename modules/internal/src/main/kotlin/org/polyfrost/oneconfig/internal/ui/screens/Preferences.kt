package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import org.polyfrost.compose.foundation.layout.Box
import org.polyfrost.compose.foundation.layout.fillMaxSize
import org.polyfrost.compose.ui.Alignment
import org.polyfrost.compose.ui.Modifier
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

const val PREFERENCES_ID = "oneconfig.json"

@Composable
fun Preferences(
    initialCategory: String? = null,
    searchQuery: String = "",
    onCategory: ((from: String?, to: String) -> Unit)? = null,
) {
    val revision = ConfigRegistry.revision
    val tree = ConfigRegistry.findTree(PREFERENCES_ID)

    if (tree == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No preferences available.", color = LocalTheme.current.textColorSecondary)
        }
        return
    }

    key(revision, tree) {
        ConfigScreen(tree, initialCategory, pageKey = PREFERENCES_ID, searchQuery = searchQuery, onCategory = onCategory)
    }
}
