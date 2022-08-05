package cc.polyfrost.oneconfig.gui.elements.text;

import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.IOUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import org.jetbrains.annotations.NotNull;

import java.awt.geom.Point2D;
import java.util.Arrays;

public class TextInputField extends BasicElement {
    public final int DOWN_LINE = Integer.MIN_VALUE;
    public final int UP_LINE = Integer.MAX_VALUE;
    public final int MOVE_END = Integer.MAX_VALUE - 1;
    public final int MOVE_START = Integer.MIN_VALUE + 1;
    private final int LINE_HEIGHT = 20, MAX_WIDTH;
    private static final String NEW_LINE = "/n";
    protected final String defaultText;
    protected final SVG icon;
    protected final boolean centered, multiLine, password;
    protected String input, renderCache;
    protected Selection selection;
    protected int caretPos;
    protected boolean errored, shown;
    private long vg;
    private Point2D.Float textStart;
    private float x;

    public TextInputField(int width, int height, String defaultText, boolean multiLine, boolean password, boolean centered, SVG icon) {
        super(width, height, false);
        this.multiLine = multiLine;
        this.defaultText = defaultText;
        this.password = password;
        this.input = "";
        this.icon = icon;
        this.centered = centered;
        this.MAX_WIDTH = width - (icon != null ? 32 : 12);
    }

    public TextInputField(int width, int height, String defaultText, boolean multiLine, boolean password, boolean centered) {
        this(width, height, defaultText, multiLine, password, centered, null);
    }

    public TextInputField(int width, int height, String defaultText, boolean multiLine, boolean password) {
        this(width, height, defaultText, multiLine, password, false, null);
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
        if (textStart == null || this.x != x) {
            if (!centered) {
                textStart = new Point2D.Float(icon != null ? x + 32 : x + 12, multiLine ? y + 20 : y + height / 2f + 1);
            }
        }
        this.x = x;

        // DRAW BOX AND ICON
        int colorOutline = errored ? Colors.ERROR_700 : Colors.GRAY_700;
        if (!toggled)
            RenderManager.drawHollowRoundRect(vg, x, y, width - 0.5f, height - 0.5f, colorOutline, 12f, 2f);
        else {
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

        // check the selection
        float selectionLength = 0;
        boolean longSelection = false;
        validateSelection();
        if (selection != null && selection.getText() != null) {
            selectionLength = getTextWidth(selection.getText());
            longSelection = selectionLength > MAX_WIDTH;
        }

        // ACTUAL DRAWING
        float textY = textStart.y;
        int startIndex = 0;
        for (String s : getRenderText()) {
            int endIndex = startIndex + s.length();

            // draw selection           (intellisense doesn't understand, so I have to check it again)
            if (selectionLength != 0 && selection != null) {
                float from = selection.indexStart < endIndex && selection.indexStart >= startIndex ? getTextWidth(s.substring(0, selection.indexStart - startIndex)) : 0;
                float thisLength = selectionLength;
                if (longSelection) {
                    thisLength = getTextWidth(s);
                    selectionLength = selectionLength - thisLength;
                    RenderManager.drawRect(vg, from + textStart.x, textY - 10, thisLength, LINE_HEIGHT, Colors.GRAY_300);
                } else if (selection.indexStart < endIndex && selection.indexStart >= startIndex) {
                    RenderManager.drawRect(vg, from + textStart.x, textY - 10, thisLength, LINE_HEIGHT, Colors.GRAY_300);
                }
            }

            // draw text
            RenderManager.drawText(vg, s, textStart.x, textY, color, 14f, Fonts.REGULAR);

            // draw caret
            if (selection == null && toggled) {
                if (caretPos >= startIndex && caretPos < endIndex) {
                    RenderManager.drawRect(vg, textStart.x + getTextWidth(s.substring(0, caretPos - startIndex)), textY - 10, 1, LINE_HEIGHT, Colors.WHITE);
                }
            }

            // update for next line
            textY += LINE_HEIGHT + 4;
            startIndex += s.length();
        }


    }

    @Override
    public void update(float x, float y) {
        super.update(x, y);
        // todo broken! need a reliable way to distinguish between drag, click, double click, etc.
        /*if (clicked) {
            putCaret(snapToChar(InputUtils.mouseX(), InputUtils.mouseY()));
            toggled = true;
        } else if (pressed) {
            int pos = snapToChar(InputUtils.mouseX(), InputUtils.mouseY());
            if (selection == null) {
                if (pos != -1) {
                    selection = new Selection(pos, 0);
                }
            } else {
                if(pos < selection.indexStart) {
                    selection.indexStart = pos;
                } else {
                    selection.indexEnd = pos;
                }
            }
        }*/

        if (InputUtils.isClicked() && !InputUtils.isAreaHovered(x - 40, y - 20, width + 90, height + 20)) {
            close();
            toggled = false;
        }
    }

    @Override
    public void onClick() {
        //toggled = true;
    }

    private void onDoubleClick() {

    }

    // getting selected coords
    private int snapToChar(float mouseX, float mouseY) {
        String[] lines = getRenderText();
        int lineIndex = getHoveredLineIndex(mouseY);
        if (lineIndex >= lines.length || lineIndex < 0) return -1;
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
                    addChars(caretPos, "    ");
                    return;
                }
                if (isAllowedCharacter(c)) {
                    if (selection != null) {
                        removeSequence(selection.indexStart, selection.indexEnd);
                        selection = null;
                    }
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
        // TODO works but likes to wander around the line
        if (movement == DOWN_LINE || movement == UP_LINE) {
            String lines = getUnformattedRenderText();
            int startOfCurrentLine = lines.substring(0, caretPos).lastIndexOf(NEW_LINE);
            if (startOfCurrentLine == -1) startOfCurrentLine = 0;
            startOfCurrentLine += NEW_LINE.length();
            //System.out.println(input.substring(startOfCurrentLine, caretPos));
            float predictedX = textStart.x + getTextWidth(lines.substring(startOfCurrentLine, caretPos)) + 0.7f;
            float predictedY = textStart.y + lines.substring(0, caretPos).split(NEW_LINE).length * LINE_HEIGHT - (movement == DOWN_LINE ? 0 : LINE_HEIGHT * 2);
            putCaret(snapToChar(predictedX, predictedY) + 1);
            //System.out.println(input.substring(0, snapToChar(predictedX, predictedY)));
            //System.out.println(InputUtils.mouseX() + " " + InputUtils.mouseY());
            //System.out.println("predicted: " + predictedX + " " + predictedY);
            //System.out.println("makes: " + input.substring(0, caretPos));
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

    private void putCaret(int pos) {
        if (pos < 0) caretPos = 0;
        else caretPos = Math.min(pos, input.length());
    }

    // render text
    public String[] getRenderText() {
        System.out.println(Arrays.toString(getUnformattedRenderText().split(NEW_LINE)));
        return getUnformattedRenderText().split(NEW_LINE);
    }

    public String getUnformattedRenderText() {
        if (renderCache == null) {
            final String[] words = input.split(" ");
            final StringBuilder output = new StringBuilder();
            float width = 0;
            for (String word : words) {
                width += getTextWidth(word + " ");
                if (getTextWidth(word) >= MAX_WIDTH) {
                    // TODO skill issue ngl
                }
                if (width >= MAX_WIDTH) {
                    width = 0;
                    output.append(NEW_LINE);
                    width += getTextWidth(word + " ");
                }
                output.append(word).append(" ");

            }
            renderCache = output.toString();
            if (!shown && password) {
                // replace all chars except the new line literal on password fields
                renderCache = renderCache.replaceAll("[^" + NEW_LINE + "]+", "*");
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

    public boolean isErrored() {
        return errored;
    }

    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    private float getTextWidth(String s) {
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


    private void validateSelection() {
        if (selection == null) return;
        // done like this because of debugging and it's so much easier to read
        if (selection.indexStart < 0 || selection.indexEnd < 0) {
            selection = null;
        } else if (selection.indexStart > input.length() || selection.indexEnd > input.length()) {
            selection = null;
        } else if (selection.indexEnd == selection.indexStart) {
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
