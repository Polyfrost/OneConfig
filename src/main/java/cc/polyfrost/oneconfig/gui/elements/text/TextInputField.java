package cc.polyfrost.oneconfig.gui.elements.text;

import cc.polyfrost.oneconfig.gui.animations.EaseInOutQuad;
import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.IOUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.Point2D;

public class TextInputField extends BasicElement {
    public static final int DOWN_LINE = Integer.MIN_VALUE;
    public static final int UP_LINE = Integer.MAX_VALUE;
    public static final int MOVE_END = Integer.MAX_VALUE - 1;
    public static final int MOVE_START = Integer.MIN_VALUE + 1;
    private static final int LINE_HEIGHT = 20;
    /**
     * <a href="https://https://www.cl.cam.ac.uk/~mgk25/ucs/examples/UTF-8-test.txt">UTF-8 Stress Test</a> Character 2.3.1 (used because it's never going to be used in theory)
     */
    private static final char NEW_LINE = '\uD7FF';
    private final int maxWidth;
    private final int maxLines;
    protected final String defaultText;
    protected final SVG icon;
    protected final boolean centered;
    protected final boolean password;
    protected boolean mouseWasDown;
    protected boolean dragging;
    protected boolean errored;
    protected boolean shown;
    protected EaseInOutQuad animation;
    protected String input = "";
    protected String renderCache;
    protected Selection selection;
    protected int caretPos;
    protected int requestedLines = 1;
    private long vg;
    protected long clickTime;
    protected long clickTime2;
    private Point2D.Float textStart;
    private float x;
    private float y;

    public TextInputField(int width, int maxHeight, String defaultText, boolean password, boolean centered, SVG icon) {
        super(width, maxHeight, false);
        if (maxHeight < LINE_HEIGHT + 12) {
            maxLines = 1;
            System.err.println("Minimum height for a text box is 32px, setting to 32px");
            height = 32;
        } else {
            maxLines = maxHeight / LINE_HEIGHT;
        }
        this.animation = new EaseInOutQuad(200, 0, maxLines * LINE_HEIGHT, false);
        this.defaultText = defaultText;
        this.password = password;
        this.icon = icon;
        this.centered = centered;
        this.maxWidth = width - (icon != null ? 32 : 12) - (password ? 24 : 0) - (centered ? 12 : 0);
    }

    public TextInputField(int width, int maxHeight, boolean centered) {
        this(width, maxHeight, "", false, centered, null);
    }

    public static boolean isAllowedCharacter(char character) {
        if (character == UKeyboard.KEY_LCONTROL || character == UKeyboard.KEY_RCONTROL || character == UKeyboard.KEY_LMENU || character == UKeyboard.KEY_RMENU || character == UKeyboard.KEY_LMETA || character == UKeyboard.KEY_RMETA || character == UKeyboard.KEY_LSHIFT || character == UKeyboard.KEY_RSHIFT || character == UKeyboard.KEY_ENTER || character == UKeyboard.KEY_CAPITAL || character == 221 || character == UKeyboard.KEY_HOME)
            return false;
        if (!Character.isDefined(character)) return false;
        if (UKeyboard.isCtrlKeyDown() || UKeyboard.isAltKeyDown()) return false;
        return character != 167 && character >= ' ' && character != 127;
    }

    @Override
    public void draw(long vg, float x, float y) {
        this.vg = vg;
        if (textStart == null || this.x != x || this.y != y) {
            textStart = new Point2D.Float(icon != null ? x + 36 : x + 12, maxLines != 1 ? y + 20 : y + height / 2f + 1);
        }
        this.x = x;
        this.y = y;

        // box animations stuff
        if (requestedLines < 1) requestedLines = 1;
        if (animation.get(200) < requestedLines * LINE_HEIGHT) {
            animation = new EaseInOutQuad(200, height, requestedLines * LINE_HEIGHT, false);
        } else if (animation.get(200) > requestedLines * LINE_HEIGHT) {
            animation = new EaseInOutQuad(200, height, requestedLines * LINE_HEIGHT, false);
        }

        // DRAW BOX AND ICON
        height = animation.get() + 12;
        int colorOutline = errored ? Colors.ERROR_700 : Colors.GRAY_700;
        if (!toggled) {
            RenderManager.drawHollowRoundRect(vg, x, y, width - 0.5f, height - 0.5f, colorOutline, 12f, 2f);
        } else {
            RenderManager.setAlpha(vg, 0.15f);
            RenderManager.drawRoundedRect(vg, x - 4, y - 4, width + 8, height + 8, errored ? Colors.ERROR_600 : Colors.PRIMARY_600, 16);
            RenderManager.setAlpha(vg, 1f);
            RenderManager.drawHollowRoundRect(vg, x, y, width - 0.5f, height - 0.5f, errored ? Colors.ERROR_600 : Colors.PRIMARY_600, 12f, 2f);
        }
        update(x, y);
        int color = toggled ? Colors.WHITE : Colors.WHITE_60;
        if (icon != null) {
            RenderManager.drawSvg(vg, icon, x + 12, y + height / 2f - 12f, 24, 24, color);
        }
        if (password) {
            final SVG icon = shown ? SVGs.EYE_OFF : SVGs.EYE;
            boolean hovered = InputUtils.isAreaHovered(x + maxWidth + 18, y + height / 2f - 9f, 18, 18);
            int eyeColor = hovered ? Colors.WHITE : Colors.WHITE_80;
            if (hovered && InputUtils.isClicked()) {
                shown = !shown;
                invalidateAll();
            }
            if (hovered && Platform.getMousePlatform().isButtonDown(0)) RenderManager.setAlpha(vg, 0.5f);
            RenderManager.drawSvg(vg, icon, x + maxWidth + 20, y + height / 2f - 9f, 18, 18, eyeColor);
        }

        if (input.equals("") && toggled) {
            // draw empty stuff
            if (defaultText != null && !defaultText.isEmpty()) {
                if (centered) {
                    textStart.x = x + (width / 2f) - (getTextWidth(defaultText) / 2f);
                }
                RenderManager.drawText(vg, defaultText, textStart.x, textStart.y, Colors.WHITE_60, 14f, Fonts.REGULAR);
            }
            if (toggled) {
                RenderManager.drawRect(vg, textStart.x, textStart.y - 10, 1, LINE_HEIGHT, Colors.WHITE);
            }
            return;
        }

        // check the selection
        validateSelection();

        // ACTUAL DRAWING
        float textY = textStart.y;
        int startIndex = 0;
        boolean caretDone = false;
        for (final String line : getRenderText()) {
            if (centered) {
                textStart.x = x + (width / 2f) - (getTextWidth(line) / 2f);
            }
            final int endIndex = startIndex + line.length();

            // draw selection
            if (selection != null) {
                final float from = selection.indexStart < endIndex && selection.indexStart >= startIndex ? getTextWidth(line.substring(0, selection.indexStart - startIndex)) : 0;
                float to = selection.indexEnd < endIndex && selection.indexEnd >= startIndex ? getTextWidth(line.substring(0, selection.indexEnd - startIndex)) : getTextWidth(line);
                if(selection.indexEnd < startIndex) to = 0;
                if(from != 0 && to != 0) {
                    RenderManager.drawRect(vg, textStart.x + from, textY - 10, to - from, LINE_HEIGHT, Colors.GRAY_300);
                }

                //}
                //RenderManager.drawRect(vg, textStart.x + from, textY - 10, to - from, LINE_HEIGHT, Colors.GRAY_300);
            }

            // draw text
            RenderManager.drawText(vg, line, textStart.x, textY, color, 14f, Fonts.REGULAR);

            // draw caret
            if (selection == null && toggled && !caretDone) {
                // check if caret is applicable for this line
                if ((caretPos >= startIndex && caretPos <= endIndex)) {
                    RenderManager.drawRect(vg, textStart.x + getTextWidth(line.substring(0, caretPos - startIndex)), textY - 10, 1, LINE_HEIGHT, Colors.WHITE);
                    caretDone = true;
                }
            }

            // update for next line
            textY += LINE_HEIGHT + 4;
            startIndex += line.length();
        }
    }

    @Override
    public void update(float x, float y) {
        super.update(x, y);
        boolean isMouseDown = Platform.getMousePlatform().isButtonDown(0);
        boolean hovered = Platform.getMousePlatform().isButtonDown(0) && this.hovered;
        boolean doubleClick = false;
        // double click
        if (clicked && System.currentTimeMillis() - clickTime < 200 && System.currentTimeMillis() - clickTime2 < 200) {
            putCaret(snapToChar(InputUtils.mouseX(), InputUtils.mouseY()));
            int last = input.lastIndexOf(" ", caretPos);
            int next = input.indexOf(" ", caretPos);
            selection = new Selection(last == -1 ? 0 : last + 1, next == -1 ? input.length() : next);
            doubleClick = true;
            toggled = true;
        }
        // drag logic stuff
        if (hovered && isMouseDown && !mouseWasDown) {
            if (selection != null) selection = null;
            clickTime = System.currentTimeMillis();
            dragging = true;
        }
        // single click
        if (clicked && System.currentTimeMillis() - clickTime < 200 && !doubleClick) {
            if (!toggled) toggled = true;
            putCaret(snapToChar(InputUtils.mouseX(), InputUtils.mouseY()));
            clickTime2 = System.currentTimeMillis();
        }
        // drag
        //if (dragging && System.currentTimeMillis() - clickTime > 100 && !doubleClick) {
        // TODO dragging
            /*int pos = snapToChar(InputUtils.mouseX(), InputUtils.mouseY());
            if (pos > input.length() || pos < 0) return;
            if (selection == null) {
                putCaret(pos);
                selection = new Selection(caretPos, 0);
            } else {
                if (caretPos > pos) {
                    selection.indexStart = pos;
                } else {
                    selection.indexEnd = pos;
                }
            }*/
        //}
        // reset on outside click
        if (InputUtils.isClicked() && !InputUtils.isAreaHovered(x - 40, y - 20, width + 90, height + 20)) {
            close();
            toggled = false;
        }

        // reset
        mouseWasDown = Platform.getMousePlatform().isButtonDown(0);
        if (dragging && InputUtils.isClicked(true)) {
            dragging = false;
        }
    }

    // getting selected coords
    private int snapToChar(float mouseX, float mouseY) {
        // TODO fix on centered box
        String[] lines = getRenderText();
        int lineIndex = getHoveredLineIndex(mouseY);
        if (lineIndex >= lines.length) {
            return input.length();
        } else if (lineIndex < 0) {
            return 0;
        }
        int index = 0;
        for (int i = 0; i < lineIndex; i++) {
            index += lines[i].length();
        }
        String text = lines[lineIndex];
        if (mouseX < textStart.x) {
            return index;
        }
        if (mouseX > getTextWidth(text) + textStart.x) {
            return index + text.length();
        }
        StringBuilder stringBuilder = new StringBuilder();
        for (char c : text.toCharArray()) {
            stringBuilder.append(c);
            float width = textStart.x + getTextWidth(stringBuilder.toString());
            if (width >= mouseX) {
                return index + stringBuilder.length() - 1;
            }
        }
        return -1;
    }

    private int getHoveredLineIndex(float mouseY) {
        return (int) ((mouseY - textStart.y) / LINE_HEIGHT);
    }

    public void keyTyped(char c, int key) {
        if (c == NEW_LINE) {
            throw new IllegalArgumentException("funny one mate");
        }
        if (key == UKeyboard.KEY_ESCAPE) {
            // TODO: fix this at a higher level
            this.toggled = false;
            return;
        }
        try {
            if (toggled) {
                // COPYING
                if (UKeyboard.isKeyComboCtrlC(key)) {
                    if (selection != null && selection.getText() != null) {
                        IOUtils.copyStringToClipboard(selection.getText());
                    }
                    return;
                }
                if (UKeyboard.isKeyComboCtrlV(key) || key == 0xD2) {
                    if (requestedLines > maxLines) return;
                    String clip = IOUtils.getStringFromClipboard();
                    if (clip != null) {
                        addChars(caretPos, clip);
                    }
                    return;
                }
                if (UKeyboard.isKeyComboCtrlX(key)) {
                    if (selection != null && selection.getText() != null) {
                        IOUtils.copyStringToClipboard(selection.getText());
                        removeSequence(selection.indexStart, selection.indexEnd);
                    }
                    return;
                }
                if (UKeyboard.isKeyComboCtrlA(key)) {
                    selection = new Selection(0, input.length());
                    return;
                }

                // CONTROL NAVIGATION
                if (UKeyboard.isCtrlKeyDown()) {
                    int target;
                    if (key == UKeyboard.KEY_BACKSPACE) {
                        removeSequence(getInput(false).trim().lastIndexOf(" "), caretPos);
                    } else if (key == UKeyboard.KEY_LEFT) {
                        target = getInput(false).lastIndexOf(" ");
                        if (target != -1) {
                            moveCaret(target - caretPos, UKeyboard.isShiftKeyDown());
                        } else {
                            moveCaret(-caretPos, UKeyboard.isShiftKeyDown());
                        }
                    } else if (key == UKeyboard.KEY_RIGHT) {
                        target = input.indexOf(" ", caretPos + 1);
                        if (target != -1) {
                            moveCaret(target - caretPos, UKeyboard.isShiftKeyDown());
                        } else {
                            moveCaret(input.length() - caretPos, UKeyboard.isShiftKeyDown());
                        }

                    }
                    return;
                }

                // NAVIGATION
                if (key == UKeyboard.KEY_LEFT) {
                    moveCaret(-1, UKeyboard.isShiftKeyDown());
                    return;
                }
                if (key == UKeyboard.KEY_RIGHT) {
                    moveCaret(1, UKeyboard.isShiftKeyDown());
                    return;
                }
                if (key == UKeyboard.KEY_UP || key == 201) { // 201 = page up
                    moveCaret(UP_LINE, UKeyboard.isShiftKeyDown());
                    return;
                }
                if (key == UKeyboard.KEY_DOWN || key == 209) {     // 209 = page down
                    moveCaret(DOWN_LINE, UKeyboard.isShiftKeyDown());
                    return;
                }
                if (key == UKeyboard.KEY_ENTER) {
                    if (requestedLines == maxLines) return;
                    addChars(caretPos, NEW_LINE);
                    return;
                }

                // DELETING
                if (key == UKeyboard.KEY_DELETE) {
                    clear();
                    return;
                }
                if (key == UKeyboard.KEY_END) {
                    close();
                    return;
                }

                // BACKSPACE
                if (key == UKeyboard.KEY_BACKSPACE) {
                    if (selection != null) {
                        removeSequence(selection.indexStart, selection.indexEnd);
                        selection = null;
                    }
                    removeChars(caretPos, 1);
                    return;
                }

                // ADDING
                if (key == UKeyboard.KEY_TAB) {
                    if (requestedLines > maxLines) return;
                    addChars(caretPos, "    ");
                    return;
                }
                if (isAllowedCharacter(c)) {
                    if (selection != null) {
                        removeSequence(selection.indexStart, selection.indexEnd);
                        selection = null;
                    }
                    if (requestedLines > maxLines) return;
                    addChars(caretPos, c);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // add and removers
    protected void addChars(int index, char c) {
        if (index < 0 || index > input.length()) {
            return;
        }
        input = input.substring(0, index) + c + input.substring(index);
        moveCaret(1);
        invalidateAll();
    }

    public void addChars(int index, @NotNull String s) {
        if (index < 0 || index > input.length()) {
            return;
        }
        input = input.substring(0, index) + s + input.substring(index);
        moveCaret(s.length());
        invalidateAll();
    }

    public void removeChars(int index, int amount) {
        if (index - amount >= 0 && index >= 0) {
            input = input.substring(0, index - amount) + input.substring(index);
            moveCaret(-amount);
            invalidateAll();
        }
    }

    /**
     * Removes all characters INSIDE the given range.
     */
    protected void removeSequence(int from, int to) {
        if (to == -1) {
            clear();
        }
        if ((from < 0) || (to > input.length()) || (from > to)) return;
        input = input.substring(0, from) + input.substring(to);
        moveCaret(from - to);
        invalidateAll();
    }

    // caret movement
    public void moveCaret(int movement, boolean select) {
        if (!select && selection != null) {
            selection = null;
        }
        if (movement == DOWN_LINE || movement == UP_LINE) {
            final String[] lines = getRenderText();
            int curr = 0;
            int i = 0;
            for (final String line : lines) {
                if (curr + line.length() > caretPos) {
                    break;
                }
                curr += line.length() + 1;
                i++;
            }
            if (i < 0) return;
            final float currentWidth = getTextWidth(lines[i].substring(0, caretPos - curr));
            i = (movement == DOWN_LINE) ? i + 1 : i - 1;
            if (i < 0) {
                moveCaret(-caretPos, select);
                return;
            }
            if (i >= lines.length) {
                moveCaret(input.length() - caretPos, select);
                return;
            }
            final StringBuilder builder = new StringBuilder();
            for (final char c : lines[i].toCharArray()) {
                builder.append(c);
                final float width = getTextWidth(builder.toString());
                if (width > currentWidth) {
                    if (movement == UP_LINE) {
                        moveCaret(builder.length() - 1 - caretPos, select);
                    } else {
                        moveCaret(lines[i - 1].length() - caretPos + builder.length(), select);
                    }
                    return;
                }
            }
        } else if (movement == MOVE_END) {
            if (select) {
                selection = new Selection(caretPos, input.length());
            }
            caretPos = input.length();
        } else if (movement == MOVE_START) {
            if (select) {
                selection = new Selection(0, caretPos);
            }
            caretPos = 0;
        } else {
            if (movement + caretPos > input.length()) {
                caretPos = input.length();
            } else if (movement + caretPos < 0) {
                caretPos = 0;
            } else {
                if (select) {
                    if (selection == null) {
                        if (movement > 0) {
                            selection = new Selection(caretPos, caretPos + movement);
                        } else {
                            selection = new Selection(caretPos + movement, caretPos);
                        }
                    } else selection.append(movement);
                }
                caretPos += movement;
            }
        }
    }

    public void moveCaret(int movement) {
        moveCaret(movement, false);
    }

    public void putCaret(int pos) {
        if (pos < 0) caretPos = 0;
        else caretPos = Math.min(pos, input.length());
    }

    // render text
    public String[] getRenderText() {
        return getUnformattedRenderText().split(String.valueOf(NEW_LINE));
    }

    public String getUnformattedRenderText() {
        if (renderCache == null) {
            if (maxLines == 1) {
                renderCache = input;
                if (!shown && password) {
                    StringBuilder s1 = new StringBuilder();
                    while (s1.length() < input.length()) {
                        s1.append('*');
                    }
                    renderCache = s1.toString();
                }
                return renderCache;
            }
            final String[] words = input.split(" ");
            final StringBuilder output = new StringBuilder();
            float width = 0;
            int lineAmount = 1;
            for (String word : words) {
                width += getTextWidth(word + " ");
                if (getTextWidth(word) >= maxWidth) {
                    for (char c : word.toCharArray()) {
                        output.append(c);
                        width += getTextWidth(String.valueOf(c));
                        if (width >= maxWidth) {
                            output.append(NEW_LINE);
                            width = 0;
                            lineAmount++;
                        }
                    }
                } else if (width >= maxWidth) {
                    width = 0;
                    output.append(NEW_LINE);
                    width += getTextWidth(word + " ");
                    lineAmount++;
                    if (lineAmount >= maxLines) {
                        requestedLines = maxLines;
                        break;
                    }
                }
                requestedLines = lineAmount == 0 ? 1 : lineAmount;
                output.append(word).append(" ");

            }
            renderCache = output.toString();
            if (!shown && password) {
                StringBuilder s1 = new StringBuilder();
                for (char c : renderCache.trim().toCharArray()) {
                    if (c == NEW_LINE) s1.append(NEW_LINE);
                    else s1.append('*');
                }
                renderCache = s1.toString();
            }
        }
        return renderCache;
    }

    /**
     * Invalidate the entire render cache, meaning the new lines have to be recalculated.
     */
    private void invalidateAll() {
        renderCache = null;
    }

    public void clear() {
        input = "";
        selection = null;
        caretPos = 0;
        invalidateAll();
    }

    public void close() {
        if (selection != null) {
            selection = null;
        }
        toggled = false;
    }


    // basic getters and setters

    /**
     * Get the input of the text field.
     *
     * @param after MODE flag: if true, will return the string AFTER the caret. else, it will return the string BEFORE the caret.
     */
    public String getInput(boolean after) {
        if (after) return input.substring(caretPos);
        else return input.substring(0, caretPos);
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        caretPos = input.length();
        this.input = input;
        invalidateAll();
    }

    public void setShowState(boolean state) {
        if (!password) return;
        this.shown = state;
    }

    public boolean getShowState() {
        if (!password) return false;
        return this.shown;
    }

    public boolean isErrored() {
        return errored;
    }

    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    private float getTextWidth(@NotNull String s) {
        if (!shown && password) {
            StringBuilder s1 = new StringBuilder();
            while (s1.length() < s.length()) {
                s1.append('*');
            }
            return RenderManager.getTextWidth(vg, s1.toString(), 14.0f, Fonts.REGULAR);
        } else {
            return RenderManager.getTextWidth(vg, s, 14.0f, Fonts.REGULAR);
        }
    }


    // selection
    private void validateSelection() {
        if (selection == null) return;
        // done like this because of debugging and it's so much easier to read
        if (selection.indexStart < 0 || selection.indexEnd < 0) {
            selection = null;
        } else if (selection.indexStart > input.length() || selection.indexEnd > input.length()) {
            selection = null;
        } else if (selection.indexEnd == selection.indexStart && !dragging) {
            selection = null;
        }
    }

    public class Selection {
        public int indexStart, indexEnd;

        public Selection(int from, int to) {
            set(from, to);
        }

        public void append(int amount) {
            if (amount > 0) {
                indexEnd += amount;
            } else {
                indexStart += amount;
            }
        }

        public void set(int indexStart, int indexEnd) {
            this.indexStart = indexStart;
            this.indexEnd = indexEnd;
        }

        public String getText() {
            if ((indexStart < 0) || (indexEnd > input.length()) || (indexStart > indexEnd)) return null;
            return input.substring(indexStart, indexEnd);
        }
    }
}
