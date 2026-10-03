package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import java.util.regex.Pattern;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

public enum ChatFormatting {
    BLACK('0'),
    DARK_BLUE('1'),
    DARK_GREEN('2'),
    DARK_AQUA('3'),
    DARK_RED('4'),
    DARK_PURPLE('5'),
    GOLD('6'),
    GRAY('7'),
    DARK_GRAY('8'),
    BLUE('9'),
    GREEN('a'),
    AQUA('b'),
    RED('c'),
    LIGHT_PURPLE('d'),
    YELLOW('e'),
    WHITE('f'),
    OBFUSCATED('k'),
    BOLD('l'),
    STRIKETHROUGH('m'),
    UNDERLINE('n'),
    ITALIC('o'),
    RESET('r');

    public static final char PREFIX_CODE = '§';
    private static final Pattern STRIP_FORMATTING_PATTERN = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private final char code;
    private final String toString;

    private ChatFormatting(final char code) {
        this.code = code;
        this.toString = "§" + String.valueOf(code);
    }

    public String toString() {
        return this.toString;
    }

    @Contract("!null->!null;_->_")
    public static @Nullable String stripFormatting(final @Nullable String input) {
        return input == null ? null : STRIP_FORMATTING_PATTERN.matcher(input).replaceAll("");
    }

    public static @Nullable ChatFormatting getByCode(final char code) {
        char sanitized = Character.toLowerCase(code);

        for(ChatFormatting format : values()) {
            if (format.code == sanitized) {
                return format;
            }
        }

        return null;
    }
}
*///?}
