package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import java.util.Objects;

public class Style extends net.minecraft.text.Style {
    public static final Style EMPTY = new Style(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
    );

    private final Style parent;
    private final ChatFormatting color;
    private final Integer rgbColor;
    private final Boolean bold;
    private final Boolean italic;
    private final Boolean underlined;
    private final Boolean strikethrough;
    private final Boolean obfuscated;
    private final String insertion;

    public Style() {
        this(null, null, null, null, null, null, null, null, null);
    }

    private Style(
            final Style parent,
            final ChatFormatting color,
            final Integer rgbColor,
            final Boolean bold,
            final Boolean italic,
            final Boolean underlined,
            final Boolean strikethrough,
            final Boolean obfuscated,
            final String insertion
    ) {
        this.parent = parent;
        this.color = color;
        this.rgbColor = rgbColor;
        this.bold = bold;
        this.italic = italic;
        this.underlined = underlined;
        this.strikethrough = strikethrough;
        this.obfuscated = obfuscated;
        this.insertion = insertion;
    }

    public Style applyTo(final Style base) {
        if (this == EMPTY) {
            return base;
        }

        return new Style(
                base.parent,
                this.color != null ? this.color : base.color,
                this.rgbColor != null ? this.rgbColor : base.rgbColor,
                this.bold != null ? this.bold : base.bold,
                this.italic != null ? this.italic : base.italic,
                this.underlined != null ? this.underlined : base.underlined,
                this.strikethrough != null ? this.strikethrough : base.strikethrough,
                this.obfuscated != null ? this.obfuscated : base.obfuscated,
                this.insertion != null ? this.insertion : base.insertion
        );
    }

    public Style applyFormats(final ChatFormatting... formats) {
        Style result = this;

        for (final ChatFormatting format : formats) {
            result = result.applyFormat(format);
        }

        return result;
    }

    public Style applyFormat(final ChatFormatting format) {
        if (format == ChatFormatting.RESET) {
            return EMPTY;
        }

        if (isColor(format)) {
            return new Style(
                    this.parent,
                    format,
                    getColor(format),
                    this.bold,
                    this.italic,
                    this.underlined,
                    this.strikethrough,
                    this.obfuscated,
                    this.insertion
            );
        }

        if (format == ChatFormatting.BOLD) {
            return this.withBold(true);
        }

        if (format == ChatFormatting.ITALIC) {
            return this.withItalic(true);
        }

        if (format == ChatFormatting.UNDERLINE) {
            return this.withUnderlined(true);
        }

        if (format == ChatFormatting.STRIKETHROUGH) {
            return this.withStrikethrough(true);
        }

        if (format == ChatFormatting.OBFUSCATED) {
            return this.withObfuscated(true);
        }

        return this;
    }

    public Style applyLegacyFormat(final ChatFormatting format) {
        ChatFormatting color = this.color;
        Integer rgbColor = this.rgbColor;
        Boolean bold = this.bold;
        Boolean italic = this.italic;
        Boolean underlined = this.underlined;
        Boolean strikethrough = this.strikethrough;
        Boolean obfuscated = this.obfuscated;

        switch (format) {
            case OBFUSCATED:
                obfuscated = true;
                break;
            case BOLD:
                bold = true;
                break;
            case STRIKETHROUGH:
                strikethrough = true;
                break;
            case UNDERLINE:
                underlined = true;
                break;
            case ITALIC:
                italic = true;
                break;
            case RESET:
                return EMPTY;
            default:
                obfuscated = false;
                bold = false;
                italic = false;
                underlined = false;
                strikethrough = false;
                color = format;
                rgbColor = getColor(format);
        }

        return new Style(
                this.parent,
                color,
                rgbColor,
                bold,
                italic,
                underlined,
                strikethrough,
                obfuscated,
                this.insertion
        );
    }

    public Style withColor(final int color) {
        return new Style(
                this.parent,
                null,
                color,
                this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.insertion
        );
    }

    public Style withColor(final ChatFormatting color) {
        return new Style(
                this.parent,
                color,
                getColor(color),
                this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.insertion
        );
    }

    public Style withBold(final Boolean bold) {
        return new Style(this.parent, this.color, this.rgbColor, bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, this.insertion);
    }

    public Style withItalic(final Boolean italic) {
        return new Style(this.parent, this.color, this.rgbColor, this.bold, italic, this.underlined, this.strikethrough, this.obfuscated, this.insertion);
    }

    public Style withUnderlined(final Boolean underlined) {
        return new Style(this.parent, this.color, this.rgbColor, this.bold, this.italic, underlined, this.strikethrough, this.obfuscated, this.insertion);
    }

    public Style withStrikethrough(final Boolean strikethrough) {
        return new Style(this.parent, this.color, this.rgbColor, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated, this.insertion);
    }

    public Style withObfuscated(final Boolean obfuscated) {
        return new Style(this.parent, this.color, this.rgbColor, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated, this.insertion);
    }

    public Style withInsertion(final String insertion) {
        return new Style(this.parent, this.color, this.rgbColor, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated, insertion);
    }

    public Style withoutShadow() {
        return this;
    }

    @Override
    public Style setParent(final net.minecraft.text.Style parent) {
        return new Style(
                parent instanceof Style ? (Style) parent : fromLegacy(parent),
                this.color,
                this.rgbColor,
                this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.insertion
        );
    }

    @Override
    public Style setColor(final net.minecraft.text.Formatting color) {
        return this.withColor(fromLegacy(color));
    }

    @Override
    public Style setBold(final Boolean bold) {
        return this.withBold(bold);
    }

    @Override
    public Style setItalic(final Boolean italic) {
        return this.withItalic(italic);
    }

    @Override
    public Style setUnderlined(final Boolean underlined) {
        return this.withUnderlined(underlined);
    }

    @Override
    public Style setStrikethrough(final Boolean strikethrough) {
        return this.withStrikethrough(strikethrough);
    }

    @Override
    public Style setObfuscated(final Boolean obfuscated) {
        return this.withObfuscated(obfuscated);
    }

    @Override
    public Style setInsertion(final String insertion) {
        return this.withInsertion(insertion);
    }

    @Override
    public Style copy() {
        return this;
    }

    @Override
    public Style deepCopy() {
        return this;
    }

    @Override
    public String asString() {
        StringBuilder builder = new StringBuilder();

        net.minecraft.text.Formatting color = this.getColor();
        if (color != null) builder.append(color);

        if (this.isBold()) builder.append(net.minecraft.text.Formatting.BOLD);
        if (this.isItalic()) builder.append(net.minecraft.text.Formatting.ITALIC);
        if (this.isUnderlined()) builder.append(net.minecraft.text.Formatting.UNDERLINE);
        if (this.isStrikethrough()) builder.append(net.minecraft.text.Formatting.STRIKETHROUGH);
        if (this.isObfuscated()) builder.append(net.minecraft.text.Formatting.OBFUSCATED);

        return builder.toString();
    }

    @Override
    public boolean isEmpty() {
        return this.parent == null
                && this.color == null
                && this.rgbColor == null
                && this.bold == null
                && this.italic == null
                && this.underlined == null
                && this.strikethrough == null
                && this.obfuscated == null
                && this.insertion == null;
    }

    @Override
    public net.minecraft.text.Formatting getColor() {
        if (this.color != null) {
            return toLegacy(this.color);
        }

        return this.parent == null ? null : this.parent.getColor();
    }

    public Integer getRgbColor() {
        if (this.rgbColor != null) {
            return this.rgbColor;
        }

        return this.parent == null ? null : this.parent.getRgbColor();
    }

    @Override
    public boolean isBold() {
        if (this.bold != null) {
            return this.bold;
        }

        return this.parent != null && this.parent.isBold();
    }

    @Override
    public boolean isItalic() {
        if (this.italic != null) {
            return this.italic;
        }

        return this.parent != null && this.parent.isItalic();
    }

    @Override
    public boolean isUnderlined() {
        if (this.underlined != null) {
            return this.underlined;
        }

        return this.parent != null && this.parent.isUnderlined();
    }

    @Override
    public boolean isStrikethrough() {
        if (this.strikethrough != null) {
            return this.strikethrough;
        }

        return this.parent != null && this.parent.isStrikethrough();
    }

    @Override
    public boolean isObfuscated() {
        if (this.obfuscated != null) {
            return this.obfuscated;
        }

        return this.parent != null && this.parent.isObfuscated();
    }

    @Override
    public String getInsertion() {
        if (this.insertion != null) {
            return this.insertion;
        }

        return this.parent == null ? null : this.parent.getInsertion();
    }

    public static Style fromLegacy(final net.minecraft.text.Style style) {
        if (style == null) {
            return EMPTY;
        }

        if (style instanceof Style) {
            return (Style) style;
        }

        return EMPTY
                .withColor(fromLegacy(style.getColor()))
                .withBold(style.isBold() ? true : null)
                .withItalic(style.isItalic() ? true : null)
                .withUnderlined(style.isUnderlined() ? true : null)
                .withStrikethrough(style.isStrikethrough() ? true : null)
                .withObfuscated(style.isObfuscated() ? true : null)
                .withInsertion(style.getInsertion());
    }

    private static boolean isColor(final ChatFormatting formatting) {
        return formatting != null && formatting.ordinal() <= ChatFormatting.WHITE.ordinal();
    }

    private static Integer getColor(final ChatFormatting formatting) {
        if (formatting == null) {
            return null;
        }

        return switch (formatting) {
            case BLACK -> 0x000000;
            case DARK_BLUE -> 0x0000AA;
            case DARK_GREEN -> 0x00AA00;
            case DARK_AQUA -> 0x00AAAA;
            case DARK_RED -> 0xAA0000;
            case DARK_PURPLE -> 0xAA00AA;
            case GOLD -> 0xFFAA00;
            case GRAY -> 0xAAAAAA;
            case DARK_GRAY -> 0x555555;
            case BLUE -> 0x5555FF;
            case GREEN -> 0x55FF55;
            case AQUA -> 0x55FFFF;
            case RED -> 0xFF5555;
            case LIGHT_PURPLE -> 0xFF55FF;
            case YELLOW -> 0xFFFF55;
            case WHITE -> 0xFFFFFF;
            default -> null;
        };
    }

    private static net.minecraft.text.Formatting toLegacy(final ChatFormatting formatting) {
        return formatting == null ? null : net.minecraft.text.Formatting.valueOf(formatting.name());
    }

    private static ChatFormatting fromLegacy(final net.minecraft.text.Formatting formatting) {
        return formatting == null ? null : ChatFormatting.valueOf(formatting.name());
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof Style)) {
            return false;
        }

        final Style other = (Style) obj;
        return Objects.equals(this.parent, other.parent)
                && this.color == other.color
                && Objects.equals(this.rgbColor, other.rgbColor)
                && Objects.equals(this.bold, other.bold)
                && Objects.equals(this.italic, other.italic)
                && Objects.equals(this.underlined, other.underlined)
                && Objects.equals(this.strikethrough, other.strikethrough)
                && Objects.equals(this.obfuscated, other.obfuscated)
                && Objects.equals(this.insertion, other.insertion);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                this.parent,
                this.color,
                this.rgbColor,
                this.bold,
                this.italic,
                this.underlined,
                this.strikethrough,
                this.obfuscated,
                this.insertion
        );
    }
}
*///?}
