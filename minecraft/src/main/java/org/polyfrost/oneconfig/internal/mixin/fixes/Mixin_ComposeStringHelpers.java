package org.polyfrost.oneconfig.internal.mixin.fixes;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.text.BreakIterator;

@Mixin(targets = "androidx.compose.foundation.text.StringHelpers_skikoKt", remap = false)
public class Mixin_ComposeStringHelpers {
    @Shadow
    private static boolean isWhitespace(int codePoint) {
        throw new AssertionError();
    }

    private static BreakIterator oneconfig$chars(String text) {
        BreakIterator it = BreakIterator.getCharacterInstance();
        it.setText(text);
        return it;
    }

    @Overwrite
    public static int findPrecedingBreak(String text, int index) {
        return oneconfig$chars(text).preceding(index);
    }

    @Overwrite
    public static int findFollowingBreak(String text, int index) {
        return oneconfig$chars(text).following(index);
    }

    @Overwrite
    private static boolean canBeEmojiOrPictographic(String text) {
        return text.codePoints().anyMatch(cp -> Character.isEmoji(cp) || Character.isExtendedPictographic(cp) || cp == 0xFE0F);
    }

    @Overwrite
    public static int findNextNonWhitespaceSymbolsSubsequenceStartOffset(int offset, String text) {
        BreakIterator it = oneconfig$chars(text);
        int next = it.next();
        while (next < offset && next != BreakIterator.DONE) next = it.next();
        int current = next;
        next = it.next();
        while (next != BreakIterator.DONE && next < text.length()) {
            if (isWhitespace(text.codePointAt(current)) && !isWhitespace(text.codePointAt(next))) return next;
            current = next;
            next = it.next();
        }
        return current;
    }

    @Overwrite
    public static int midpointPositionWithUnicodeSymbols(String text) {
        int half = (int) text.codePoints().count() / 2;
        BreakIterator it = oneconfig$chars(text);
        int pos = 0;
        for (int i = 0; i <= half; i++) pos = it.next();
        return pos;
    }
}
