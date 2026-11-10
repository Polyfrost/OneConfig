package org.polyfrost.oneconfig.internal.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.UUID
import org.polyfrost.oneconfig.api.platform.v1.ModInfo
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.api.ui.v1.components.PlayerHead
import org.polyfrost.oneconfig.internal.ui.api.TranslationCredits
import org.polyfrost.oneconfig.internal.ui.components.Icon
import org.polyfrost.oneconfig.internal.ui.components.ScrollbarBox
import org.polyfrost.oneconfig.internal.ui.components.Text
import org.polyfrost.oneconfig.internal.ui.components.accentGradientFill
import org.polyfrost.oneconfig.internal.ui.components.hoverGlow
import org.polyfrost.oneconfig.internal.ui.components.rememberAccentBrandTint
import org.polyfrost.oneconfig.internal.ui.components.rememberSvgResourcePainter
import org.polyfrost.oneconfig.internal.ui.components.translated
import org.polyfrost.oneconfig.internal.ui.search.searchMatches
import org.polyfrost.oneconfig.internal.ui.shell.PageGutter
import org.polyfrost.oneconfig.internal.ui.shell.pageSearchQuery
import org.polyfrost.oneconfig.internal.ui.themes.LocalTheme

private val oneConfigVersion: String? by lazy {
    runCatching {
        ModInfo.loadedMods.firstOrNull { it.id == "oneconfig" }?.version
    }.getOrNull()?.trim()?.takeIf { it.isNotEmpty() }
}

private class Person(val name: String, val role: String, uuid: String?) {
    val id: UUID = uuid?.let { UUID(java.lang.Long.parseUnsignedLong(it.substring(0, 16), 16), java.lang.Long.parseUnsignedLong(it.substring(16), 16)) }
        ?: UUID.nameUUIDFromBytes("OfflinePlayer:$name".toByteArray(Charsets.UTF_8))
}

private class Thanks(val icon: String, val title: String, val detail: String)

private val people = listOf(
    Person("Wyvest", "CEO", "a53314040e77440e8bef24c071dac1ae"),
    Person("Lynith", "CTO", "f247be7c5b8241c69148793ded77e71f"),
    Person("nextdaydelivery", "OneConfig founding engineer", "19d91bd9a3c848eaab9db226d3cddc5f"),
    Person("Deftu", "OneConfig major engineer", "63677969c51746fbb60fa130b3d9beb9"),
    Person("Lunasa", "PolyCompose founding engineer", "37184fafe63e4a2bb47fc4df1459dc0b"),
    Person("Mona", "CompatEngine founding engineer", "a33e494beebe44718619a654e20a379f"),
    Person("DeDiamondPro", "Config backend, GUI frontend, HUD", "0b4d470ff2fb487493341eaef8ba4804"),
    Person("rachel", "Lead cosmetics designer, OneClient", "7602c0f2930a472abd0ae70d53cba33a"),
    Person("Qewer", "Designer", "4a06b0431c7e488093719559e14cece7"),
    Person("Caledonian", "Designer", "e5de6a33a9e04a4e8a493201d74875a6"),
    Person("Mo2men", "Designer", "bdad0d588e35422d938ae1a4c0d5d026"),
    Person("Zetvue", "Sound design", "9a23f6435e334101a5f2d63f248b49c2"),
    Person("Pauline", "OneConfig utilities", "7bfb3d3f098a412db57653bf3ed8f8df"),
    Person("xtrm", "OneConfig utilities", "16f96760bd544605915abbeaea748c33"),
    Person("MoonTidez", "Designer, v0", "acbfaeb2bcfe49bcb605ac1ebe16c2d1"),
    Person("ThinkSeal", "Major tester", "94418374e58a497cbcba29d34727eca4"),
)

private val thankedPeople = listOf(
    Person("Scherso", "For saving us", "466d70e6fe944960ac17597948f437b3"),
    Person("FireStorm", "For saving us", "5298863fbba54350819bdbe83e7d6714"),
    Person("ImToggle", "Contributions to OneConfig and Polyfrost mods", "c8bf4768af4448cba25901e42fb7bc79"),
    Person("Sk1er", "Lasting impact on Minecraft modding", "1a5c41c8a9524ddfa19c66f6bb3707a1"),
)

private fun translationCredit(): String {
    val i18n = Platform.i18n()
    val loaded = ModInfo.loadedMods
    val mods = (TranslationCredits.namespaces - "oneconfig")
        .map { namespace -> loaded.firstOrNull { it.id == namespace }?.name ?: namespace }
        .distinct()
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
    val credit = if (mods.isEmpty()) {
        i18n.translateString("oneconfig.credits.translations")
    } else {
        i18n.translateString("oneconfig.credits.translations.mods", mods.joinToString(", "))
    }
    val packs = TranslationCredits.packs
        .filter { pack -> loaded.any { it.id == pack.modId } }
        .map { i18n.translateString("oneconfig.credits.translations.pack", it.name, it.author) }
    if (packs.isEmpty()) return credit
    return i18n.translateString("oneconfig.credits.translations.packs", credit, packs.joinToString(", "))
}

private fun specialThanks() = listOf(
    Thanks("profiles", "The Polyfrost team", "For their support and contributions"),
    Thanks("text", "Translators", translationCredit()),
    Thanks("star", "Contributors and testers", "For helping make modding accessible to everyone"),
)

@Composable
fun Credits() {
    val scrollState = rememberScrollState()
    val query = pageSearchQuery().trim().lowercase()
    val thanks = remember { specialThanks() }
    val shownPeople = remember(query) { if (query.isEmpty()) people else people.filter { searchMatches("${it.name} ${it.role}", query) } }
    val shownThankedPeople = remember(query) { if (query.isEmpty()) thankedPeople else thankedPeople.filter { searchMatches("${it.name} ${it.role}", query) } }
    val shownThanks = remember(query) { if (query.isEmpty()) thanks else thanks.filter { searchMatches("${it.title} ${it.detail}", query) } }

    ScrollbarBox(scrollState, PageGutter, Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(start = PageGutter, end = PageGutter, bottom = PageGutter),
        ) {
            if (query.isEmpty()) Masthead()
            if (shownPeople.isNotEmpty()) {
                GroupHeading(translated("oneconfig.credits.people", "People"))
                PeopleGrid(shownPeople)
            }
            if (shownThankedPeople.isNotEmpty() || shownThanks.isNotEmpty()) {
                GroupHeading(translated("oneconfig.credits.thanks", "Special thanks"))
                if (shownThankedPeople.isNotEmpty()) PeopleGrid(shownThankedPeople)
                if (shownThanks.isNotEmpty()) {
                    Column(
                        Modifier.padding(top = if (shownThankedPeople.isEmpty()) 0.dp else 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        shownThanks.forEach { ThanksCard(it) }
                    }
                }
            }
            if (shownPeople.isEmpty() && shownThankedPeople.isEmpty() && shownThanks.isEmpty()) {
                Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) {
                    Text(translated("oneconfig.credits.nomatch", "No credits match \"%s\"", query), color = LocalTheme.current.textColorSecondary, fontSize = 12.sp)
                }
            }
            if (query.isEmpty()) Legal()
        }
    }
}

@Composable
private fun Masthead() {
    val theme = LocalTheme.current
    Column(Modifier.padding(top = 8.dp, bottom = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rememberSvgResourcePainter(theme.logoPath)?.let { logo ->
            Image(
                painter = logo,
                contentDescription = null,
                colorFilter = rememberAccentBrandTint(theme.logoPath),
                modifier = Modifier.size(263.dp, 30.dp).accentGradientFill(),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("by", color = theme.textColorSecondary, fontSize = 12.sp)
            rememberSvgResourcePainter("assets/oneconfig/brand/by-polyfrost.svg")?.let { poly ->
                Image(painter = poly, contentDescription = "Polyfrost", modifier = Modifier.size(104.dp, 18.dp))
            }
            oneConfigVersion?.let { version ->
                Text(
                    "v$version",
                    color = theme.textColorSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 4.dp)
                        .background(theme.chipBackground, theme.buttonShape)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                )
            }
        }
    }
}

@Composable
private fun GroupHeading(title: String) {
    Text(
        title.uppercase(),
        color = LocalTheme.current.textColor.copy(0.8f),
        fontSize = 11.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier.padding(top = 24.dp, bottom = 10.dp, start = 2.dp),
    )
}

@Composable
private fun PeopleGrid(people: List<Person>) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val fit = (maxWidth / 240.dp).toInt().coerceIn(1, 4)
        val columns = (fit downTo (fit - 1).coerceAtLeast(1)).firstOrNull { people.size % it == 0 } ?: fit
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            people.chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { PersonCard(it, Modifier.weight(1f)) }
                    repeat(columns - row.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun PersonCard(person: Person, modifier: Modifier) {
    val theme = LocalTheme.current
    Row(
        modifier
            .height(72.dp)
            .hoverGlow(theme.modCardShape)
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, theme.borderColor, theme.modCardShape)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PlayerHead(person.id, 40.dp, Modifier, theme.buttonShape)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(person.name, color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(person.role, color = theme.textColorSecondary, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ThanksCard(thanks: Thanks) {
    val theme = LocalTheme.current
    Row(
        Modifier.fillMaxWidth()
            .heightIn(min = 64.dp)
            .hoverGlow(theme.modCardShape)
            .background(theme.modCardBackground, theme.modCardShape)
            .border(1.dp, theme.borderColor, theme.modCardShape)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(theme.chipBackground, theme.buttonShape), contentAlignment = Alignment.Center) {
            Icon(thanks.icon, color = theme.textColor, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(thanks.title, color = theme.textColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(thanks.detail, color = theme.textColorSecondary, fontSize = 12.sp, lineHeight = 18.sp)
        }
    }
}

@Composable
private fun Legal() {
    val theme = LocalTheme.current
    Row(
        Modifier.fillMaxWidth().padding(top = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(translated("oneconfig.credits.legal", "Legal and corporate"), color = theme.textColorSecondary, fontSize = 11.sp)
        rememberSvgResourcePainter("assets/oneconfig/brand/atmofrost.svg")?.let { atmofrost ->
            Image(painter = atmofrost, contentDescription = "Atmofrost", modifier = Modifier.size(68.dp, 28.dp))
        }
    }
}
