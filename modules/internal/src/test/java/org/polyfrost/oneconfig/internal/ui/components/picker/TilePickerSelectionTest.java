package org.polyfrost.oneconfig.internal.ui.components.picker;

import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TilePickerSelectionTest {
    private record Tag(String getId, String getDisplayName) implements PickerEntry {}

    private final List<Tag> tags = List.of(
        new Tag("example:red_tag", "Crimson Tag"),
        new Tag("example:blue_tag", "Azure Tag"),
        new Tag("other:green_tag", "Leaf Tag")
    );

    @Test
    void searchesAnyEntryTypeByNameAndIdTerms() {
        assertEquals(List.of(tags.get(1)), TilePickerKt.filterEntries(tags, "azure"));
        assertEquals(List.of(tags.get(2)), TilePickerKt.filterEntries(tags, "other tag"));
        assertEquals(tags, TilePickerKt.filterEntries(tags, "  "));
    }

    @Test
    void togglesAndHonorsSingleAndMultipleLimits() {
        assertEquals(List.of("example:blue_tag"), TilePickerKt.toggleEntry(List.of("example:red_tag"), "example:blue_tag", 1));
        assertEquals(List.of(), TilePickerKt.toggleEntry(List.of("example:red_tag"), "example:red_tag", 1));
        assertEquals(
            List.of("example:red_tag"),
            TilePickerKt.toggleEntry(List.of("example:red_tag", "example:blue_tag"), "example:blue_tag", 2)
        );
        assertEquals(
            List.of("example:red_tag", "example:blue_tag"),
            TilePickerKt.toggleEntry(List.of("example:red_tag", "example:blue_tag"), "other:green_tag", 2)
        );
        assertEquals(List.of(), TilePickerKt.toggleEntry(List.of("example:red_tag", "example:red_tag"), "example:red_tag", 0));
    }

    @Test
    void keepsSelectedEntriesClickableWhenFull() {
        List<String> full = List.of("example:red_tag", "example:blue_tag");
        assertTrue(TilePickerKt.canSelect(full, "example:red_tag", 2));
        assertFalse(TilePickerKt.canSelect(full, "other:green_tag", 2));
        assertTrue(TilePickerKt.canSelect(full, "other:green_tag", 0));
    }
}
