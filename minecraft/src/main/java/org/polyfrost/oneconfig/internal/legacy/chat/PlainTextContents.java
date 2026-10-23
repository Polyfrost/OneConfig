package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import java.util.Optional;

public interface PlainTextContents extends ComponentContents {
    PlainTextContents EMPTY = new PlainTextContents() {
        public String toString() {
            return "empty";
        }

        @Override
        public String text() {
            return "";
        }
    };

    static PlainTextContents create(String text) {
        return text.isEmpty() ? EMPTY : new LiteralContents(text);
    }

    String text();

    record LiteralContents(String text) implements PlainTextContents {
        @Override
        public <T> Optional<T> visit(FormattedText.ContentConsumer<T> contentConsumer) {
            return contentConsumer.accept(this.text);
        }

        @Override
        public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> styledContentConsumer, Style style) {
            return styledContentConsumer.accept(style, this.text);
        }

        public String toString() {
            return "literal{" + this.text + "}";
        }
    }
}
*///?}
