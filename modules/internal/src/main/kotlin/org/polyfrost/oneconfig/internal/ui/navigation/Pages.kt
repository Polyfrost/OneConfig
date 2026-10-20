package org.polyfrost.oneconfig.internal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import java.util.Locale
import kotlinx.serialization.Serializable
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.page.ModConfigTarget
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPage
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPages
import org.polyfrost.oneconfig.api.ui.v1.page.PageGroup
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.internal.ui.components.resolve
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ChangeLogGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.CreditsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.KeybindsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModConfigRoute
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ModsGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.PreferencesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ProfilesGraph
import org.polyfrost.oneconfig.internal.ui.navigation.graph.ThemesGraph
import org.polyfrost.oneconfig.internal.ui.screens.Changelog
import org.polyfrost.oneconfig.internal.ui.screens.Credits
import org.polyfrost.oneconfig.internal.ui.screens.Keybinds
import org.polyfrost.oneconfig.internal.ui.screens.Mods
import org.polyfrost.oneconfig.internal.ui.screens.Preferences
import org.polyfrost.oneconfig.internal.ui.screens.Profiles
import org.polyfrost.oneconfig.internal.ui.screens.Themes
import org.polyfrost.oneconfig.internal.ui.shell.LocalShell

@Serializable
data class PageRoute(val pageId: String)

internal class BuiltinPage(
    key: String,
    englishTitle: String,
    icon: String,
    group: PageGroup,
    override val order: Int,
    val route: Any,
    private val content: @Composable () -> Unit,
) : OneConfigPage(
    "oneconfig:$key",
    PageText.translatable("oneconfig.page.$key.title", englishTitle),
    icon,
    group,
) {
    override val searchPlaceholder: PageText =
        PageText.translatable("oneconfig.page.$key.search", "Search $englishTitle...")

    @Composable
    override fun Content() = content()
}

object BuiltinPages {
    internal val all: List<BuiltinPage> = listOf(
        BuiltinPage("mods", "Mods", "settings", PageGroup.MODS_AND_OPTIONS, 100, ModsGraph) { Mods() },
        BuiltinPage("profiles", "Profiles", "profiles", PageGroup.MODS_AND_OPTIONS, 200, ProfilesGraph) { Profiles() },
        BuiltinPage("keybinds", "Keybinds", "keyboard", PageGroup.MODS_AND_OPTIONS, 300, KeybindsGraph) { Keybinds() },
        BuiltinPage("themes", "Themes", "paintbrush", PageGroup.PERSONALIZATION, 100, ThemesGraph) { Themes() },
        BuiltinPage("preferences", "Preferences", "cog", PageGroup.PERSONALIZATION, 200, PreferencesGraph) { Preferences() },
        BuiltinPage("changelog", "Changelog", "refresh", PageGroup.ONECONFIG, 100, ChangeLogGraph) { Changelog() },
        BuiltinPage("credits", "Credits", "star", PageGroup.ONECONFIG, 200, CreditsGraph) { Credits() },
    )

    @Volatile
    private var registered = false

    @JvmStatic
    fun register() {
        if (registered) return
        synchronized(this) {
            if (registered) return
            for (page in all) {
                val existing = OneConfigPages.byId(page.id)
                if (existing === page) continue
                if (existing != null) {
                    LOGGER.warn("Replacing page {} from {} with the built-in OneConfig page because the oneconfig namespace is reserved", page.id, existing.javaClass.name)
                    OneConfigPages.unregister(page.id)
                }
                OneConfigPages.register(page)
            }
            registered = true
        }
    }
}

private val LOGGER = LogManager.getLogger("OneConfig/UI")

fun registeredPages(): List<OneConfigPage> {
    BuiltinPages.register()
    return OneConfigPages.all()
}

fun pageRoute(page: OneConfigPage): Any = (page as? BuiltinPage)?.route ?: PageRoute(page.id)

fun resolveOpenTarget(target: Any?): Any? = when (target) {
    is String -> {
        BuiltinPages.register()
        OneConfigPages.byId(target)?.let(::pageRoute) ?: run {
            LOGGER.error("Cannot open unknown OneConfig page {}, opening the mods page instead", target)
            ModsGraph
        }
    }
    is OneConfigPage -> {
        BuiltinPages.register()
        if (OneConfigPages.byId(target.id) === target) pageRoute(target) else {
            LOGGER.error("Cannot open OneConfig page {} because it is not registered, opening the mods page instead", target.id)
            ModsGraph
        }
    }
    is ModConfigTarget -> ModConfigRoute(target.modId, target.category)
    else -> target
}

fun NavGraphBuilder.pageDestinations() {
    composable<PageRoute> { entry ->
        val id = entry.toRoute<PageRoute>().pageId
        val page = OneConfigPages.byId(id)
        if (page != null) {
            page.Content()
            return@composable
        }
        val shell = LocalShell.current
        LaunchedEffect(id) {
            LOGGER.warn("OneConfig page {} is not registered, opening the mods page instead", id)
            if (shell.navigation.currentRoute == PageRoute(id)) shell.navigation.navigate(ModsGraph)
        }
    }
}

fun NavBackStackEntry?.isOn(route: Any): Boolean {
    val entry = this ?: return false
    val destination = entry.destination
    if (route is PageRoute) {
        return destination.hasRoute(PageRoute::class) && runCatching { entry.toRoute<PageRoute>().pageId }.getOrNull() == route.pageId
    }
    return destination.hierarchy.any { it.hasRoute(route::class) }
}

fun NavBackStackEntry?.currentPage(): OneConfigPage? {
    val entry = this ?: return null
    if (entry.destination.hasRoute(PageRoute::class)) {
        val id = runCatching { entry.toRoute<PageRoute>().pageId }.getOrNull() ?: return null
        return OneConfigPages.byId(id)
    }
    return registeredPages().firstOrNull { it is BuiltinPage && entry.isOn(it.route) }
}

class SidebarEntry(val icon: String, val label: String, val route: Any)

class SidebarSection(val title: String, val entries: List<SidebarEntry>)

@Suppress("DEPRECATION")
fun sidebarSections(): List<SidebarSection> {
    val pages = registeredPages()
    val groups = pages.map { it.group }.distinct()
    return NavigationGroups.map { group ->
        SidebarSection(
            groups.firstOrNull { it.title.fallback == group.id }?.title?.resolve() ?: group.id,
            group.routes.map { def ->
                val page = pages.firstOrNull { pageRoute(it) == def.route }
                val label = page?.title?.resolve()
                    ?: def.id.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                SidebarEntry(def.icon, label, def.route)
            },
        )
    }
}

fun NavBackStackEntry?.currentPageTitle(): String? =
    currentPage()?.title?.resolve()
        ?: sidebarSections().asSequence().flatMap { it.entries }.firstOrNull { isOn(it.route) }?.label

fun searchPlaceholder(entry: NavBackStackEntry?): String = searchPlaceholder(entry.currentPage(), entry.currentPageTitle())

fun searchPlaceholder(page: OneConfigPage?, title: String?): String {
    page?.searchPlaceholder?.let { return it.resolve() }
    if (title == null) return translated("oneconfig.header.search", "Search...")
    return translated("oneconfig.header.search.page", "Search %s...", title)
}
