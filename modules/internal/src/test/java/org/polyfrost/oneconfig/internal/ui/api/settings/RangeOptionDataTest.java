package org.polyfrost.oneconfig.internal.ui.api.settings;

import java.util.ArrayList;
import java.util.List;
import kotlin.Pair;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class RangeOptionDataTest {
    @Test
    void readClampsAPairStoredOutsideTheBounds() {
        Property<int[]> property = bounded(Properties.simple("range", "Range", null, new int[]{-5, 500}, int[].class));
        RangeOptionData data = new RangeOptionData(property, RangeOptionData.Style.Spinner);

        assertEquals(new Pair<>(0f, 100f), data.read());
    }

    @Test
    void rangesClampValuesStoredOutsideTheBounds() {
        Property<int[]> property = bounded(Properties.simple("ranges", "Ranges", null, new int[]{-5, 10, 50, 500}, int[].class));
        RangeListOptionData data = new RangeListOptionData(property, RangeOptionData.Style.Spinner);

        assertEquals(List.of(new Pair<>(0f, 10f), new Pair<>(50f, 100f)), data.ranges());
    }

    @Test
    void singleRangesAndRangeListsRoundWholeNumbersTheSameWay() {
        Property<int[]> single = bounded(Properties.simple("range", "Range", null, new int[]{0, 0}, int[].class));
        new RangeOptionData(single, RangeOptionData.Style.Spinner).write(10.7f, 20.5f);

        Property<int[]> list = bounded(Properties.simple("ranges", "Ranges", null, new int[0], int[].class));
        new RangeListOptionData(list, RangeOptionData.Style.Spinner).setRanges(List.of(new Pair<>(10.7f, 20.5f)));

        assertArrayEquals(new int[]{11, 21}, single.get());
        assertArrayEquals(new int[]{11, 21}, list.get());
    }

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void boxedAndLongStorageRoundsRatherThanTruncates() {
        Property<List<Integer>> boxed = bounded((Property) Properties.simple("boxed", "Boxed", null, new ArrayList<>(List.of(0, 0)), List.class));
        new RangeOptionData(boxed, RangeOptionData.Style.Spinner).write(10.7f, 20.2f);

        Property<long[]> longs = bounded(Properties.simple("longs", "Longs", null, new long[]{0, 0}, long[].class));
        new RangeOptionData(longs, RangeOptionData.Style.Spinner).write(10.7f, 20.2f);

        assertEquals(List.of(11, 20), boxed.get());
        assertArrayEquals(new long[]{11, 20}, longs.get());
    }

    private static <T> Property<T> bounded(Property<T> property) {
        property.addMetadata("min", 0f);
        property.addMetadata("max", 100f);
        return property;
    }
}
