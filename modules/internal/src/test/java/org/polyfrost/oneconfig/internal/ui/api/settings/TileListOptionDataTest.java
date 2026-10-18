package org.polyfrost.oneconfig.internal.ui.api.settings;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TileListOptionDataTest {
    @Test
    void readsListMetadataAndKeepsIdsAsGiven() {
        Property<String[]> property = Properties.simple(
            "swatches",
            "Swatches",
            null,
            new String[]{" test:red ", "", "test:red", "blue"},
            String[].class
        );
        property.addMetadata("maxEntries", 3);
        property.addMetadata("reorderable", false);
        TileListOptionData data = new TileListOptionData(property);

        assertEquals(List.of("test:red", "blue"), data.ids());
        assertEquals(3, data.getMaxEntries());
        assertEquals(false, data.getReorderable());

        data.setIds(List.of("test:green", "test:green", "test:blue"));
        assertArrayEquals(new String[]{"test:green", "test:blue"}, property.get());
    }
}
