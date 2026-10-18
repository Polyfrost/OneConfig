package org.polyfrost.oneconfig.internal.ui.components.effect;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.internal.ui.api.settings.EffectListOptionData;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class EffectIdTest {
    @Test
    void namespacesBareIdsAndKeepsModdedOnes() {
        assertEquals("minecraft:speed", EffectCatalogServiceKt.normalizeEffectId(" Speed "));
        assertEquals("minecraft:speed", EffectCatalogServiceKt.normalizeEffectId("minecraft:speed"));
        assertEquals("example:rage", EffectCatalogServiceKt.normalizeEffectId("example:rage"));
        assertEquals("", EffectCatalogServiceKt.normalizeEffectId("  "));
    }

    @Test
    void normalizesBlankDuplicateAndBareIdsWithoutReordering() {
        assertEquals(
            List.of("minecraft:speed", "example:rage"),
            EffectCatalogServiceKt.normalizeEffectIds(List.of("speed", "", "minecraft:speed", "example:rage"))
        );
    }

    @Test
    void optionDataNamespacesStoredIds() {
        Property<String[]> property = Properties.simple(
            "effects",
            "Effects",
            null,
            new String[]{"speed", "minecraft:speed", "example:rage"},
            String[].class
        );
        EffectListOptionData data = new EffectListOptionData(property);

        assertEquals(List.of("minecraft:speed", "example:rage"), data.ids());

        data.setIds(List.of("night_vision", "minecraft:haste"));
        assertArrayEquals(new String[]{"minecraft:night_vision", "minecraft:haste"}, property.get());
    }
}
