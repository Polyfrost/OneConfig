package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import java.util.Locale;

public class TranslatableFormatException extends IllegalArgumentException {
    public TranslatableFormatException(final TranslatableContents component, final String message) {
        super(String.format(Locale.ROOT, "Error parsing: %s: %s", component, message));
    }

    public TranslatableFormatException(final TranslatableContents component, final int index) {
        super(String.format(Locale.ROOT, "Invalid index %d requested for %s", index, component));
    }

    public TranslatableFormatException(final TranslatableContents component, final Throwable t) {
        super(String.format(Locale.ROOT, "Error while parsing: %s", component), t);
    }
}
*///?}
