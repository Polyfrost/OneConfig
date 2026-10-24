package org.polyfrost.oneconfig.api.hud.v1;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.serialize.ObjectSerializer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HudAlignmentLegacyTest {
    @Test
    void oldPolyAlignValuesLoadAsAnchors() {
        Map<String, Object> saved = new HashMap<>();
        saved.put("class", "org.polyfrost.compose.layout.PolyAlign");
        saved.put("value", "BottomRight");
        assertEquals(HudAnchor.BottomRight, ObjectSerializer.INSTANCE.deserialize(saved));
    }
}
