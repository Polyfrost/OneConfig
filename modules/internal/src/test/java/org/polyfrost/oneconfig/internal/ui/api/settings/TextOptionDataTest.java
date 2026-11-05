package org.polyfrost.oneconfig.internal.ui.api.settings;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextOptionDataTest {
    @Test
    void readsSecureDefaults() {
        TextOptionData data = new TextOptionData(Properties.simple("key", "Key", null, "", String.class));
        assertFalse(data.getSecure());
        assertEquals('*', data.getSecureMask());
        assertEquals(0, data.getSecureShowLast());
        assertEquals("", data.getSecureShowChars());
    }

    @Test
    void readsSecureMetadata() {
        Property<String> property = Properties.simple("key", "Key", null, "", String.class);
        property.addMetadata("secure", true);
        property.addMetadata("secureMask", '#');
        property.addMetadata("secureShowLast", 4);
        property.addMetadata("secureShowChars", "-");
        TextOptionData data = new TextOptionData(property);

        assertTrue(data.getSecure());
        assertEquals('#', data.getSecureMask());
        assertEquals(4, data.getSecureShowLast());
        assertEquals("-", data.getSecureShowChars());
    }
}
