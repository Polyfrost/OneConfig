package org.polyfrost.oneconfig.internal.ui.search;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchMatchesTest {
    @Test
    void fuzzyMatchesCamelCaseWords() {
        assertTrue(DefaultSearchProviderKt.searchMatches("PolyNametag", "nametags", 2));
        assertTrue(DefaultSearchProviderKt.searchMatches("PolyNametag", "nametag", 2));
        assertTrue(DefaultSearchProviderKt.searchMatches("My PolyNametag Settings", "polynametags", 2));
        assertFalse(DefaultSearchProviderKt.searchMatches("PolyNametag", "sprint", 2));
        assertTrue(DefaultSearchProviderKt.searchMatches("FPSCounter", "countr", 2));
        assertFalse(DefaultSearchProviderKt.searchMatches("PolyNametag", "play", 2));
        assertFalse(DefaultSearchProviderKt.searchMatches("FPSCounter", "fly", 2));
    }

    @Test
    void shortQueriesOnlyMatchSubstrings() {
        assertFalse(DefaultSearchProviderKt.searchMatches("Hud", "x", 2));
        assertFalse(DefaultSearchProviderKt.searchMatches("Show FPS", "xy", 2));
        assertFalse(DefaultSearchProviderKt.searchMatches("Hud", "a", 10));
        assertTrue(DefaultSearchProviderKt.searchMatches("Hud", "h", 2));
        assertTrue(DefaultSearchProviderKt.searchMatches("Search Typo Tolerance", "tolerence", 2));
    }
}
