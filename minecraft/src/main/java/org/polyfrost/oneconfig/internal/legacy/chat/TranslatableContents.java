package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.ornithemc.osl.text.impl.Locale;
import org.jspecify.annotations.Nullable;

public class TranslatableContents implements ComponentContents {
    public static final Object[] NO_ARGS = new Object[0];
    private static final FormattedText TEXT_PERCENT = FormattedText.of("%");
    private static final FormattedText TEXT_NULL = FormattedText.of("null");
    private final String key;
    @Nullable
    private final String fallback;
    private final Object[] args;
    @Nullable
    private Locale decomposedWith;
    private List<FormattedText> decomposedParts = ImmutableList.of();
    private static final Pattern FORMAT_PATTERN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");

    public static boolean isAllowedPrimitiveArgument(@Nullable final Object object) {
        return object instanceof Number || object instanceof Boolean || object instanceof String;
    }

    public TranslatableContents(final String key, @Nullable final String fallback, final Object[] args) {
        this.key = key;
        this.fallback = fallback;
        this.args = args;
    }

    private void decompose() {
        Locale currentLocale = Locale.find();
        if (currentLocale != this.decomposedWith) {
            this.decomposedWith = currentLocale;
            String translated = currentLocale.get(this.key);
            String format;
            if (translated == null || translated.equals(this.key)) {
                format = this.fallback != null ? this.fallback : this.key;
            } else {
                format = translated;
            }

            try {
                Builder<FormattedText> parts = ImmutableList.builder();
                this.decomposeTemplate(format, parts::add);
                this.decomposedParts = parts.build();
            } catch (TranslatableFormatException var4) {
                this.decomposedParts = ImmutableList.of(FormattedText.of(format));
            }
        }
    }

    private void decomposeTemplate(final String template, final Consumer<FormattedText> decomposedParts) {
        Matcher matcher = FORMAT_PATTERN.matcher(template);

        try {
            int replacementIndex = 0;
            int current = 0;

            while (matcher.find(current)) {
                int start = matcher.start();
                int end = matcher.end();
                if (start > current) {
                    String prefix = template.substring(current, start);
                    if (prefix.indexOf(37) != -1) {
                        throw new IllegalArgumentException();
                    }

                    decomposedParts.accept(FormattedText.of(prefix));
                }

                String formatType = matcher.group(2);
                String formatString = template.substring(start, end);
                if ("%".equals(formatType) && "%%".equals(formatString)) {
                    decomposedParts.accept(TEXT_PERCENT);
                } else {
                    if (!"s".equals(formatType)) {
                        throw new TranslatableFormatException(this, "Unsupported format: '" + formatString + "'");
                    }

                    String possiblePositionIndex = matcher.group(1);
                    int index = possiblePositionIndex != null ? Integer.parseInt(possiblePositionIndex) - 1 : replacementIndex++;
                    decomposedParts.accept(this.getArgument(index));
                }

                current = end;
            }

            if (current < template.length()) {
                String tail = template.substring(current);
                if (tail.indexOf(37) != -1) {
                    throw new IllegalArgumentException();
                }

                decomposedParts.accept(FormattedText.of(tail));
            }
        } catch (IllegalArgumentException var12) {
            throw new TranslatableFormatException(this, var12);
        }
    }

    private FormattedText getArgument(final int index) {
        if (index >= 0 && index < this.args.length) {
            Object arg = this.args[index];
            if (arg instanceof Component componentArg) {
                return componentArg;
            } else {
                return arg == null ? TEXT_NULL : FormattedText.of(arg.toString());
            }
        } else {
            throw new TranslatableFormatException(this, index);
        }
    }

    @Override
    public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style currentStyle) {
        this.decompose();

        for (FormattedText part : this.decomposedParts) {
            Optional<T> result = part.visit(output, currentStyle);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    @Override
    public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
        this.decompose();

        for (FormattedText part : this.decomposedParts) {
            Optional<T> result = part.visit(output);
            if (result.isPresent()) {
                return result;
            }
        }

        return Optional.empty();
    }

    public boolean equals(final Object o) {
        return this == o
                ? true
                : o instanceof TranslatableContents that
                  && Objects.equals(this.key, that.key)
                  && Objects.equals(this.fallback, that.fallback)
                  && Arrays.equals(this.args, that.args);
    }

    public int hashCode() {
        int result = Objects.hashCode(this.key);
        result = 31 * result + Objects.hashCode(this.fallback);
        return 31 * result + Arrays.hashCode(this.args);
    }

    public String toString() {
        return "translation{key='"
                + this.key
                + "'"
                + (this.fallback != null ? ", fallback='" + this.fallback + "'" : "")
                + ", args="
                + Arrays.toString(this.args)
                + "}";
    }

    public String getKey() {
        return this.key;
    }
}
*///?}
