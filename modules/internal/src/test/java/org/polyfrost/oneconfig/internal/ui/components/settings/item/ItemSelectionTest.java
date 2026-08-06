package org.polyfrost.oneconfig.internal.ui.components.settings.item;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemSelectionTest {
    private final List<ItemDescriptor> items = List.of(
        new ItemDescriptor("minecraft:diamond_sword", "Diamond Sword"),
        new ItemDescriptor("minecraft:golden_apple", "Golden Apple"),
        new ItemDescriptor("example:blue_gem", "Azure Crystal")
    );

    @Test
    void normalizesBlankAndDuplicateIdsWithoutReordering() {
        assertEquals(
            List.of("minecraft:diamond", "example:gem"),
            ItemCatalogServiceKt.normalizeItemIds(List.of(" minecraft:diamond ", "", "minecraft:diamond", "example:gem"))
        );
    }

    @Test
    void searchesLocalizedNamesAndRegistryIdsByAllTerms() {
        assertEquals(
            List.of("minecraft:diamond_sword"),
            ItemCatalogServiceKt.filterItems(items, "diamond sword").stream().map(ItemDescriptor::getId).toList()
        );
        assertEquals(
            List.of("example:blue_gem"),
            ItemCatalogServiceKt.filterItems(items, "example gem").stream().map(ItemDescriptor::getId).toList()
        );
    }

    @Test
    void togglesAndHonorsSingleAndMultipleLimits() {
        assertEquals(
            List.of("minecraft:golden_apple"),
            ItemCatalogServiceKt.toggleItem(List.of("minecraft:diamond_sword"), "minecraft:golden_apple", 1)
        );
        assertEquals(
            List.of("minecraft:diamond_sword", "minecraft:golden_apple"),
            ItemCatalogServiceKt.toggleItem(
                List.of("minecraft:diamond_sword", "minecraft:golden_apple"),
                "example:blue_gem",
                2
            )
        );
        assertEquals(
            List.of(),
            ItemCatalogServiceKt.toggleItem(List.of("minecraft:diamond_sword"), "minecraft:diamond_sword", 0)
        );
    }
}
