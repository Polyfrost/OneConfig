package org.polyfrost.oneconfig.internal.ui.navigation

import androidx.compose.runtime.Composable
import org.apache.logging.log4j.LogManager
import org.polyfrost.oneconfig.api.ui.v1.page.ModConfigTarget as PublicModConfigTarget
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPage
import org.polyfrost.oneconfig.api.ui.v1.page.OneConfigPages
import org.polyfrost.oneconfig.api.ui.v1.page.PageGroup
import org.polyfrost.oneconfig.api.ui.v1.page.PageText
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.components.resolve
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.shell.LocalPageScope
import org.polyfrost.oneconfig.internal.ui.shell.ModConfigTarget
import org.polyfrost.oneconfig.internal.ui.shell.Page
import org.polyfrost.oneconfig.internal.ui.shell.PolyPage
import org.polyfrost.oneconfig.internal.ui.shell.ShellNav

internal class BuiltinPage(
    key: String,
    englishTitle: String,
    icon: String,
    group: PageGroup,
    override val order: Int,
    val route: PolyPage,
) : OneConfigPage(
    "oneconfig:$key",
    PageText.translatable("oneconfig.page.$key.title", englishTitle),
    icon,
    group,
) {
    override val searchPlaceholder: PageText =
        PageText.translatable("oneconfig.page.$key.search", "Search $englishTitle...")

    @Composable
    override fun Content() {
        val scope = LocalPageScope.current ?: return
        Page(route, null, scope.host, scope.query, scope.nav)
    }
}

object BuiltinPages {
    internal val all: List<BuiltinPage> = listOf(
        BuiltinPage("mods", "Mods", "settings", PageGroup.MODS_AND_OPTIONS, 100, PolyPage.Mods),
        BuiltinPage("profiles", "Profiles", "profiles", PageGroup.MODS_AND_OPTIONS, 200, PolyPage.Profiles),
        BuiltinPage("keybinds", "Keybinds", "keyboard", PageGroup.MODS_AND_OPTIONS, 300, PolyPage.Keybinds),
        BuiltinPage("themes", "Themes", "paintbrush", PageGroup.PERSONALIZATION, 100, PolyPage.Themes),
        BuiltinPage("preferences", "Preferences", "cog", PageGroup.PERSONALIZATION, 200, PolyPage.Preferences),
        BuiltinPage("changelog", "Changelog", "refresh", PageGroup.ONECONFIG, 100, PolyPage.Changelog),
        BuiltinPage("credits", "Credits", "star", PageGroup.ONECONFIG, 200, PolyPage.Credits),
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

fun pageTarget(page: OneConfigPage): Pair<PolyPage, Any?> =
    (page as? BuiltinPage)?.let { it.route to null } ?: (PolyPage.Page to page.id)

fun resolveOpenTarget(target: Any?): Pair<PolyPage, Any?>? = when (target) {
    is String -> {
        BuiltinPages.register()
        OneConfigPages.byId(target)?.let(::pageTarget) ?: run {
            LOGGER.error("Cannot open unknown OneConfig page {}, opening the mods page instead", target)
            PolyPage.Mods to null
        }
    }
    is OneConfigPage -> {
        BuiltinPages.register()
        if (OneConfigPages.byId(target.id) === target) pageTarget(target) else {
            LOGGER.error("Cannot open OneConfig page {} because it is not registered, opening the mods page instead", target.id)
            PolyPage.Mods to null
        }
    }
    is PublicModConfigTarget -> ConfigRegistry.findById(target.modId)
        ?.let { PolyPage.ModConfig to ModConfigTarget(it, target.category) }
        ?: (PolyPage.Mods to null)
    else -> null
}

fun ShellNav.isOn(route: PolyPage, arg: Any? = null): Boolean =
    current.hierarchy == route && (route != PolyPage.Page || this.arg == arg)

fun pageFor(page: PolyPage, arg: Any? = null): OneConfigPage? =
    if (page == PolyPage.Page) (arg as? String)?.let(OneConfigPages::byId)
    else registeredPages().firstOrNull { it is BuiltinPage && it.route == page.hierarchy }

fun ShellNav.currentPage(): OneConfigPage? = pageFor(current, arg)

fun ShellNav.currentPageTitle(): String? = currentPage()?.title?.resolve()

class SidebarEntry(val icon: String, val label: String, val route: PolyPage, val arg: Any? = null)

class SidebarSection(val title: String, val entries: List<SidebarEntry>)

fun sidebarSections(): List<SidebarSection> = registeredPages()
    .filter { it.showInSidebar }
    .groupBy { it.group }
    .map { (group, pages) ->
        SidebarSection(group.title.resolve(), pages.map {
            val (route, arg) = pageTarget(it)
            SidebarEntry(it.icon, it.title.resolve(), route, arg)
        })
    }

fun searchPlaceholder(page: PolyPage, arg: Any? = null): String {
    val target = pageFor(page, arg)
    return searchPlaceholder(target, target?.title?.resolve())
}

fun searchPlaceholder(page: OneConfigPage?, title: String?): String {
    page?.searchPlaceholder?.let { return it.resolve() }
    if (title == null) return translated("oneconfig.header.search", "Search...")
    return translated("oneconfig.header.search.page", "Search %s...", title)
}
