package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import net.minecraft.resource.Identifier;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import net.kyori.adventure.text.format.TextDecoration;
import org.jspecify.annotations.Nullable;

public interface Component extends Message, FormattedText, Text {
    Style getStyle();

    ComponentContents getContents();

    @Override
    default String getString() {
        return FormattedText.super.getString();
    }

    default net.kyori.adventure.text.Component asComponent() {
        net.kyori.adventure.text.Component result = net.kyori.adventure.text.Component.empty();
        for (Component part : this.toFlatList()) {
            Style style = part.getStyle();
            net.kyori.adventure.text.format.Style.Builder adventureStyle = net.kyori.adventure.text.format.Style.style();
            Integer color = style.getRgbColor();
            if (color != null) {
                adventureStyle.color(net.kyori.adventure.text.format.TextColor.color(color));
            }
            adventureStyle.decoration(TextDecoration.BOLD, style.isBold());
            adventureStyle.decoration(TextDecoration.ITALIC, style.isItalic());
            adventureStyle.decoration(TextDecoration.UNDERLINED, style.isUnderlined());
            adventureStyle.decoration(TextDecoration.STRIKETHROUGH, style.isStrikethrough());
            adventureStyle.decoration(TextDecoration.OBFUSCATED, style.isObfuscated());
            if (style.getInsertion() != null) {
                adventureStyle.insertion(style.getInsertion());
            }
            result = result.append(net.kyori.adventure.text.Component.text(part.getString(), adventureStyle.build()));
        }
        return result;
    }

    default String getString(final int limit) {
        StringBuilder builder = new StringBuilder();
        this.visit(contents -> {
            int remaining = limit - builder.length();
            if (remaining <= 0) {
                return STOP_ITERATION;
            } else {
                builder.append(contents.length() <= remaining ? contents : contents.substring(0, remaining));
                return Optional.empty();
            }
        });
        return builder.toString();
    }

    @Override
    List getSiblings();

    @Nullable
    default String tryCollapseToString() {
        return this.getContents() instanceof PlainTextContents text && this.getSiblings().isEmpty() && this.getStyle().isEmpty() ? text.text() : null;
    }

    default MutableComponent plainCopy() {
        return MutableComponent.create(this.getContents());
    }

    default MutableComponent copy() {
        return new MutableComponent(this.getContents(), new ArrayList(this.getSiblings()), this.getStyle());
    }

    FormattedCharSequence getVisualOrderText();

    @Override
    default <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
        Style selfStyle = this.getStyle().deepCopy().setParent(parentStyle);
        Optional<T> selfResult = this.getContents().visit(output, selfStyle);
        if (selfResult.isPresent()) {
            return selfResult;
        } else {
            for (Object sibling : this.getSiblings()) {
                Optional<T> result = ((Component) sibling).visit(output, selfStyle);
                if (result.isPresent()) {
                    return result;
                }
            }

            return Optional.empty();
        }
    }

    @Override
    default <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
        Optional<T> selfResult = this.getContents().visit(output);
        if (selfResult.isPresent()) {
            return selfResult;
        } else {
            for (Object sibling : this.getSiblings()) {
                Optional<T> result = ((Component) sibling).visit(output);
                if (result.isPresent()) {
                    return result;
                }
            }

            return Optional.empty();
        }
    }

    default List<Component> toFlatList() {
        return this.toFlatList(new Style());
    }

    default List<Component> toFlatList(final Style rootStyle) {
        List<Component> result = Lists.<Component>newArrayList();
        this.visit((style, contents) -> {
            if (!contents.isEmpty()) {
                result.add(literal(contents).withStyle(style));
            }

            return Optional.empty();
        }, rootStyle);
        return result;
    }

    default boolean contains(final Component other) {
        if (this.equals(other)) {
            return true;
        } else {
            List<Component> flat = this.toFlatList();
            List<Component> otherFlat = other.toFlatList(this.getStyle());
            return Collections.indexOfSubList(flat, otherFlat) != -1;
        }
    }

    static Component nullToEmpty(@Nullable final String text) {
        return text != null ? literal(text) : empty();
    }

    static MutableComponent literal(final String text) {
        return MutableComponent.create(PlainTextContents.create(text));
    }

    static MutableComponent fromLegacy(final String text) {
        if (text.indexOf(ChatFormatting.PREFIX_CODE) < 0) {
            return literal(text);
        }

        MutableComponent result = empty();
        Style style = Style.EMPTY;
        int start = 0;

        for (int i = 0; i < text.length() - 1; i++) {
            if (text.charAt(i) != ChatFormatting.PREFIX_CODE) {
                continue;
            }

            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting == null) {
                continue;
            }

            if (start < i) {
                result.append(literal(text.substring(start, i)).setStyle(style));
            }

            if (formatting == ChatFormatting.RESET) {
                style = Style.EMPTY;
            } else if (formatting.ordinal() <= ChatFormatting.WHITE.ordinal()) {
                // Legacy color codes reset decorations before applying the new color.
                style = Style.EMPTY.applyFormat(formatting);
            } else {
                style = style.applyFormat(formatting);
            }

            i++;
            start = i + 1;
        }

        if (start < text.length()) {
            result.append(literal(text.substring(start)).setStyle(style));
        }

        return result;
    }

    static MutableComponent translatable(final String key) {
        return MutableComponent.create(new TranslatableContents(key, null, TranslatableContents.NO_ARGS));
    }

    static MutableComponent translatable(final String key, final Object... args) {
        return MutableComponent.create(new TranslatableContents(key, null, args));
    }

    static MutableComponent translatableEscape(final String key, final Object... args) {
        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (!TranslatableContents.isAllowedPrimitiveArgument(arg) && !(arg instanceof Component)) {
                args[i] = String.valueOf(arg);
            }
        }

        return translatable(key, args);
    }

    static MutableComponent translatableWithFallback(final String key, @Nullable final String fallback) {
        return MutableComponent.create(new TranslatableContents(key, fallback, TranslatableContents.NO_ARGS));
    }

    static MutableComponent translatableWithFallback(final String key, @Nullable final String fallback, final Object... args) {
        return MutableComponent.create(new TranslatableContents(key, fallback, args));
    }

    static MutableComponent empty() {
        return MutableComponent.create(PlainTextContents.EMPTY);
    }

    static Component translationArg(final Date date) {
        return literal(date.toString());
    }

    static Component translationArg(final Message message) {
        return (Component)(message instanceof Component component ? component : literal(message.getString()));
    }

    static Component translationArg(final UUID uuid) {
        return literal(uuid.toString());
    }

    static Component translationArg(final Identifier id) {
        return literal(id.toString());
    }

    static Component translationArg(final ChunkPos chunkPos) {
        return literal(chunkPos.toString());
    }

    static Component translationArg(final URI uri) {
        return literal(uri.toString());
    }
}
*///?}
