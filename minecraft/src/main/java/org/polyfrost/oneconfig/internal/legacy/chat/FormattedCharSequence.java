package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*@FunctionalInterface
public interface FormattedCharSequence {
    FormattedCharSequence EMPTY = (output) -> true;

    boolean accept(final FormattedCharSink output);

    static FormattedCharSequence forward(final String plainText, final Style style) {
        return plainText.isEmpty() ? EMPTY : (output) -> StringDecomposer.iterate(plainText, style, output);
    }
}
*///?}
