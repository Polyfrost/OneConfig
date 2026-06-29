package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import java.util.Optional;

public interface FormattedText {

    private static Style applyTo(final Style style, final Style parentStyle) {
        return style.deepCopy().setParent(parentStyle);
    }

    Optional<Unit> STOP_ITERATION = Optional.of(Unit.INSTANCE);

    <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output);

    <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle);

    static FormattedText of(final String text) {
        return new FormattedText() {
            @Override
            public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
                return output.accept(text);
            }

            @Override
            public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
                return output.accept(parentStyle, text);
            }
        };
    }

    static FormattedText of(final String text, final Style style) {
        return new FormattedText() {
            @Override
            public <T> Optional<T> visit(final FormattedText.ContentConsumer<T> output) {
                return output.accept(text);
            }

            @Override
            public <T> Optional<T> visit(final FormattedText.StyledContentConsumer<T> output, final Style parentStyle) {
                return output.accept(applyTo(style, parentStyle), text);
            }
        };
    }

    default String getString() {
        StringBuilder builder = new StringBuilder();
        this.visit(contents -> {
            builder.append(contents);
            return Optional.empty();
        });
        return builder.toString();
    }

    interface ContentConsumer<T> {
        Optional<T> accept(final String contents);
    }

    interface StyledContentConsumer<T> {
        Optional<T> accept(final Style style, final String contents);
    }
}
*///?}
