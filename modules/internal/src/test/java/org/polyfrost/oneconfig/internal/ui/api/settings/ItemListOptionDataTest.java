package org.polyfrost.oneconfig.internal.ui.api.settings;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ItemListOptionDataTest {
    @Test
    void normalizesAndPreservesArrayStorage() {
        Property<String[]> property = Properties.simple(
            "items",
            "Items",
            null,
            new String[]{" minecraft:diamond ", "", "minecraft:diamond", "example:missing"},
            String[].class
        );
        ItemListOptionData data = new ItemListOptionData(property);

        assertEquals(List.of("minecraft:diamond", "example:missing"), data.ids());

        data.setIds(List.of("minecraft:apple", "minecraft:ender_pearl"));
        assertArrayEquals(new String[]{"minecraft:apple", "minecraft:ender_pearl"}, property.get());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void normalizesAndPreservesListStorage() {
        Property<List<String>> property = (Property) Properties.simple(
            "items",
            "Items",
            null,
            new ArrayList<>(List.of("minecraft:apple")),
            List.class
        );
        ItemListOptionData data = new ItemListOptionData(property);

        data.setIds(List.of(" minecraft:diamond ", "minecraft:diamond", "example:missing"));

        assertInstanceOf(List.class, property.get());
        assertEquals(List.of("minecraft:diamond", "example:missing"), property.get());
    }

    @Test
    void preservesAConcreteListFieldType() throws NoSuchFieldException {
        ConcreteListHolder holder = new ConcreteListHolder();
        Property<?> property = Properties.field(
            null,
            null,
            ConcreteListHolder.class.getDeclaredField("items"),
            holder
        );
        ItemListOptionData data = new ItemListOptionData(property);

        data.setIds(List.of("minecraft:diamond", "minecraft:apple"));

        assertInstanceOf(LinkedList.class, holder.items);
        assertEquals(List.of("minecraft:diamond", "minecraft:apple"), holder.items);
    }

    private static final class ConcreteListHolder {
        private LinkedList<String> items = new LinkedList<>(List.of("minecraft:stone"));
    }
}
