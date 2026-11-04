package org.polyfrost.oneconfig.internal.ui.components.settings;

import java.util.Collections;
import androidx.compose.ui.text.AnnotatedString;
import androidx.compose.ui.text.input.OffsetMapping;
import androidx.compose.ui.text.input.TransformedText;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SecureTransformationTest {
    private static final String GRINNING = "😀";
    private static final String TEARS_OF_JOY = "😂";
    private static final String SQUARED_SA = "🈂";

    private static TransformedText mask(String text, char mask, int showLast, String showChars) {
        return new SecureTransformation(mask, showLast, showChars).filter(new AnnotatedString(text, Collections.emptyList()));
    }

    private static void assertMapping(TransformedText masked, int[] toMasked, int[] toRaw) {
        OffsetMapping mapping = masked.getOffsetMapping();
        for (int i = 0; i < toMasked.length; i++) {
            assertEquals(toMasked[i], mapping.originalToTransformed(i), "original offset " + i);
        }
        for (int i = 0; i < toRaw.length; i++) {
            assertEquals(toRaw[i], mapping.transformedToOriginal(i), "masked offset " + i);
        }
    }

    @Test
    void masksEveryCharacterByDefault() {
        TransformedText masked = mask("abcd", '*', 0, "");
        assertEquals("****", masked.getText().getText());
        assertMapping(masked, new int[]{0, 1, 2, 3, 4}, new int[]{0, 1, 2, 3, 4});
        assertEquals("", mask("", '*', 0, "").getText().getText());
    }

    @Test
    void showsTheLastCharactersAndListedOnes() {
        assertEquals("***************mnop", mask("abcd-efgh-ijkl-mnop", '*', 4, "").getText().getText());
        assertEquals("ab", mask("ab", '*', 4, "").getText().getText());
        assertEquals("••••-••••-••••-mnop", mask("abcd-efgh-ijkl-mnop", '•', 4, "-").getText().getText());
    }

    @Test
    void matchesListedCharactersAsWholeCodePoints() {
        TransformedText masked = mask(TEARS_OF_JOY + GRINNING, '*', 0, GRINNING + SQUARED_SA);
        assertEquals("*" + GRINNING, masked.getText().getText());
        assertMapping(masked, new int[]{0, 0, 1, 2, 3}, new int[]{0, 2, 3, 4});
    }

    @Test
    void countsShowLastInWholeCodePoints() {
        TransformedText masked = mask("ab" + GRINNING, '*', 1, "");
        assertEquals("**" + GRINNING, masked.getText().getText());
        assertMapping(masked, new int[]{0, 1, 2, 3, 4}, new int[]{0, 1, 2, 3, 4});

        TransformedText hiddenEmoji = mask(GRINNING + "b", '*', 1, "");
        assertEquals("*b", hiddenEmoji.getText().getText());
        assertMapping(hiddenEmoji, new int[]{0, 0, 1, 2}, new int[]{0, 2, 3});
    }

    @Test
    void masksLineBreaksUnlessListed() {
        assertEquals("*****", mask("ab\ncd", '*', 0, "").getText().getText());
        assertEquals("**\n**", mask("ab\ncd", '*', 0, "\n").getText().getText());
    }
}
