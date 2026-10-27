package org.polyfrost.oneconfig.internal.ui.api.settings;

import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.api.config.v1.Properties;
import org.polyfrost.oneconfig.api.config.v1.Property;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TextOptionDataTest {
    private static TextOptionData secure() {
        Property<String> property = Properties.simple("key", "Key", null, "", String.class);
        property.addMetadata("secure", true);
        return new TextOptionData(property);
    }

    @Test
    void masksEveryCharacterByDefault() {
        assertEquals("*******************", secure().mask("abcd-efgh-ijkl-mnop"));
        assertEquals("", secure().mask(""));
    }

    @Test
    void showsTheLastCharactersAndListedOnes() {
        TextOptionData data = secure();
        data.getProp().addMetadata("secureShowLast", 4);
        assertEquals("***************mnop", data.mask("abcd-efgh-ijkl-mnop"));
        assertEquals("ab", data.mask("ab"));

        data.getProp().addMetadata("secureShowChars", "-");
        data.getProp().addMetadata("secureMask", '\u2022');
        assertEquals("\u2022\u2022\u2022\u2022-\u2022\u2022\u2022\u2022-\u2022\u2022\u2022\u2022-mnop", data.mask("abcd-efgh-ijkl-mnop"));
    }
}
