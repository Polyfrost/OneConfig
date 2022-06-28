package cc.polyfrost.oneconfig.hud;

import cc.polyfrost.oneconfig.config.annotations.Dropdown;
import cc.polyfrost.oneconfig.config.annotations.Switch;
import cc.polyfrost.oneconfig.config.annotations.Text;

import java.util.Collections;
import java.util.List;

public abstract class SingleTextHud extends TextHud {
    public SingleTextHud(String title, boolean enabled) {
        this(title, enabled, 0, 0);
    }

    public SingleTextHud(String title, boolean enabled, int x, int y) {
        super(enabled, x, y);
        this.title = title;
    }

    /**
     * This function is called every tick
     *
     * @return The new text
     */
    protected abstract String getText();

    /**
     * This function is called every frame
     *
     * @return The new text, null to use the cached value
     */
    protected String getTextFrequent() {
        return null;
    }

    /**
     * This function is called every tick in the move GUI
     *
     * @return The new text
     */
    protected String getExampleText() {
        return getText();
    }

    /**
     * This function is called every frame in the move GUI
     *
     * @return The new text, null to use the cached value
     */
    protected String getExampleTextFrequent() {
        return getTextFrequent();
    }

    @Override
    protected List<String> getLines() {
        return Collections.singletonList(getCompleteText(getText()));
    }

    @Override
    protected List<String> getLinesFrequent() {
        String text = getTextFrequent();
        if (text == null) return null;
        return Collections.singletonList(getCompleteText(text));
    }

    @Override
    protected List<String> getExampleLines() {
        return Collections.singletonList(getCompleteText(getExampleText()));
    }

    @Override
    protected List<String> getExampleLinesFrequent() {
        String text = getExampleTextFrequent();
        if (text == null) return null;
        return Collections.singletonList(getCompleteText(text));
    }

    protected final String getCompleteText(String text) {
        boolean showTitle = !title.trim().isEmpty();
        StringBuilder builder = new StringBuilder();
        if (brackets) {
            builder.append("[");
        }

        if (showTitle && titleLocation == 0) {
            builder.append(title).append(": ");
        }

        builder.append(text);

        if (showTitle && titleLocation == 1) {
            builder.append(" ").append(title);
        }

        if (brackets) {
            builder.append("]");
        }
        return builder.toString();
    }


    @Switch(
            name = "Brackets"
    )
    public boolean brackets = false;

    @Text(
            name = "Title"
    )
    public String title;

    @Dropdown(
            name = "Title Location",
            options = {"Left", "Right"}
    )
    public int titleLocation = 0;
}