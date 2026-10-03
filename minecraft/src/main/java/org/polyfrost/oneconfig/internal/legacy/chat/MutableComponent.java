package org.polyfrost.oneconfig.internal.legacy.chat;

//? if = 1.8.9 {
/*import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import net.ornithemc.osl.text.impl.Locale;
import org.jspecify.annotations.Nullable;

public final class MutableComponent implements Component {
    private final ComponentContents contents;
    private final List<Component> siblings;
    private Style style;
    private FormattedCharSequence visualOrderText = FormattedCharSequence.EMPTY;
    @Nullable
    private Locale decomposedWith;

    MutableComponent(final ComponentContents contents, final List<Component> siblings, final Style style) {
        this.contents = contents;
        this.siblings = siblings;
        this.style = style;
    }

    public static MutableComponent create(final ComponentContents contents) {
        return new MutableComponent(contents, Lists.newArrayList(), new Style());
    }

    @Override
    public ComponentContents getContents() {
        return this.contents;
    }

    @Override
    public List getSiblings() {
        return this.siblings;
    }

    public MutableComponent setStyle(final Style style) {
        this.style = style;
        return this;
    }

    @Override
    public MutableComponent setStyle(final net.minecraft.text.Style style) {
        this.style = Style.fromLegacy(style);
        return this;
    }

    @Override
    public Style getStyle() {
        return this.style;
    }

    public MutableComponent append(final String text) {
        return text.isEmpty() ? this : this.append(Component.literal(text));
    }

    public MutableComponent append(final Component component) {
        this.siblings.add(component);
        return this;
    }

    @Override
    public MutableComponent append(final net.minecraft.text.Text text) {
        if (text instanceof Component component) {
            return this.append(component);
        }

        return this.append(Component.literal(text.getString()).setStyle(text.getStyle()));
    }

    public MutableComponent withStyle(final Style patch) {
        this.setStyle(patch.applyTo(this.getStyle()));
        return this;
    }

    public MutableComponent withStyle(final ChatFormatting format) {
        this.setStyle(this.getStyle().applyFormat(format));
        return this;
    }

    @Override
    public FormattedCharSequence getVisualOrderText() {
        Locale currentLocale = Locale.find();
        if (this.decomposedWith != currentLocale) {
            this.visualOrderText = FormattedCharSequence.forward(this.getString(), this.getStyle());
            this.decomposedWith = currentLocale;
        }

        return this.visualOrderText;
    }

    @Override
    public String getContent() {
        StringBuilder builder = new StringBuilder();
        this.contents.visit(contents -> {
            builder.append(contents);
            return Optional.empty();
        });
        return builder.toString();
    }

    @Override
    public String getFormattedString() {
        StringBuilder builder = new StringBuilder();
        this.visit((style, contents) -> {
            if (!contents.isEmpty()) {
                builder.append(style.asString()).append(contents);
                if (!style.isEmpty()) builder.append(ChatFormatting.RESET);
            }
            return Optional.empty();
        }, Style.EMPTY);
        return builder.toString();
    }

    @Override
    public Iterator<net.minecraft.text.Text> iterator() {
        return Iterators.concat(
                Iterators.singletonIterator((net.minecraft.text.Text) this),
                Iterators.concat(Iterators.transform(
                        (Iterator<net.minecraft.text.Text>) (Iterator<?>) this.siblings.iterator(),
                        net.minecraft.text.Text::iterator
                ))
        );
    }

    public boolean equals(final Object o) {
        return this == o
                ? true
                : o instanceof MutableComponent that && this.contents.equals(that.contents) && this.style.equals(that.style) && this.siblings.equals(that.siblings);
    }

    public int hashCode() {
        int result = 1;
        result = 31 * result + this.contents.hashCode();
        result = 31 * result + this.style.hashCode();
        return 31 * result + this.siblings.hashCode();
    }

    public String toString() {
        StringBuilder result = new StringBuilder(this.contents.toString());
        boolean hasStyle = !this.style.isEmpty();
        boolean hasSiblings = !this.siblings.isEmpty();
        if (hasStyle || hasSiblings) {
            result.append('[');
            if (hasStyle) {
                result.append("style=");
                result.append(this.style);
            }

            if (hasStyle && hasSiblings) {
                result.append(", ");
            }

            if (hasSiblings) {
                result.append("siblings=");
                result.append(this.siblings);
            }

            result.append(']');
        }

        return result.toString();
    }
}
*///?}
