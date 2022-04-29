package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import io.polyfrost.oneconfig.utils.InputUtils;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.ChatAllowedCharacters;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

import static org.lwjgl.nanovg.NanoVG.nvgResetScissor;
import static org.lwjgl.nanovg.NanoVG.nvgScissor;

public class TextInputField extends BasicElement {

    protected final String defaultText;
    protected String input, selectedText;
    protected final boolean multiLine;
    protected boolean password;

    protected int caretPos;
    protected int x, y;
    protected float start, end;
    private long clickTimeD1;
    protected long vg;
    protected int prevCaret = 0;
    protected boolean isDoubleClick = false;

    public TextInputField(int width, int height, String defaultText, boolean multiLine, boolean password) {
        super(width, height, false);
        this.multiLine = multiLine;
        this.defaultText = defaultText;
        this.password = password;
        this.input = "";
    }

    public void setInput(String input) {
        this.input = input;
    }

    public String getInput() {
        return input;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public boolean getPassword() {
        return password;
    }

    @Override
    public void draw(long vg, int x, int y) {
        this.x = x;
        this.y = y;
        this.vg = vg;
        try {
            RenderManager.drawHollowRoundRect(vg, x, y, width, height, OneConfigConfig.GRAY_700, 12f, 2f);
            super.update(x, y);
            if (Mouse.isButtonDown(0) && !InputUtils.isAreaHovered(x - 40, y - 20, width + 90, height + 20)) {
                toggled = false;
            }
            int color = toggled ? OneConfigConfig.WHITE : OneConfigConfig.WHITE_60;
            float width;
            StringBuilder s = new StringBuilder();
            int offset = 12;
            if (!password) {
                width = RenderManager.getTextWidth(vg, input.substring(0, caretPos), 14f);
            } else {
                for (int i = 0; i < input.length(); i++) {
                    s.append("*");
                }
                width = RenderManager.getTextWidth(vg, s.substring(0, caretPos), 14f);
            }
            nvgScissor(vg, x, y, this.width, height);


            while (Mouse.next()) {
                if (Mouse.getEventButtonState()) {
                    if (Mouse.getEventButton() == 0) {
                        prevCaret = calculatePos(Mouse.getEventX());
                        if (System.currentTimeMillis() - clickTimeD1 < 300) {
                            onDoubleClick();
                            isDoubleClick = true;
                        }
                        clickTimeD1 = System.currentTimeMillis();
                    }
                } else {
                    if (Mouse.getEventButton() == 0) {
                        long clickTimeU = System.currentTimeMillis();
                        if (clickTimeU - clickTimeD1 < 200) {
                            if (!isDoubleClick) {
                                start = 0;
                                end = 0;
                            }
                            prevCaret = caretPos;
                            isDoubleClick = false;
                        }

                    }
                }
            }
            if (start != 0f && end != 0f && toggled) {
                RenderManager.drawRect(vg, start, y + 10, end, 20, OneConfigConfig.GRAY_300);
            }

            if (Mouse.isButtonDown(0) && !isDoubleClick) {
                caretPos = calculatePos(Mouse.getX());

                if (caretPos > prevCaret) {
                    start = x + offset + RenderManager.getTextWidth(vg, input.substring(0, prevCaret), 14f);
                    end = RenderManager.getTextWidth(vg, input.substring(prevCaret, caretPos), 14f);
                    selectedText = input.substring(prevCaret, caretPos);
                } else {
                    start = x + offset + RenderManager.getTextWidth(vg, input.substring(0, prevCaret), 14f);
                    end = -RenderManager.getTextWidth(vg, input.substring(caretPos, prevCaret), 14f);
                    selectedText = input.substring(caretPos, prevCaret);
                }


            }


            if (toggled) {
                RenderManager.drawLine(vg, x + width + 12, (float) y + height / 2f - 10, x + width + 12, (float) y + height / 2f + 10, 1, OneConfigConfig.WHITE);
            }


            if (input.equals("")) {
                RenderManager.drawString(vg, defaultText, x + 12, y + height / 2f + 1, color, 14f, Fonts.INTER_REGULAR);
            }

            if (!password) {
                RenderManager.drawString(vg, input, x + offset, y + height / 2f + 1, color, 14f, Fonts.INTER_REGULAR);
            } else {
                RenderManager.drawString(vg, s.toString(), x + offset, y + height / 2f + 1, color, 14f, Fonts.INTER_REGULAR);
            }
            nvgResetScissor(vg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void keyTyped(char c, int key) {
        try {
            if (toggled) {
                if (GuiScreen.isKeyComboCtrlC(key)) {
                    if (selectedText != null && start != 0f && end != 0f) {
                        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selectedText), null);
                    }
                    return;
                }
                if (GuiScreen.isKeyComboCtrlV(key) || key == Keyboard.KEY_INSERT) {
                    try {
                        String clip = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null).getTransferData(DataFlavor.stringFlavor).toString();
                        input = input.substring(0, caretPos) + clip + input.substring(caretPos);
                        caretPos = caretPos + clip.length();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if(key == Keyboard.KEY_DELETE) {
                    input = "";
                }


                if (GuiScreen.isCtrlKeyDown()) {
                    if (key == Keyboard.KEY_BACK && !GuiScreen.isKeyComboCtrlX(key)) {
                        try {
                            input = input.substring(0, input.lastIndexOf(" "));
                            caretPos = input.length();
                        } catch (Exception e) {
                            input = "";
                            caretPos = 0;
                        }
                        return;
                    }
                    if (GuiScreen.isKeyComboCtrlA(key)) {
                        prevCaret = 0;
                        caretPos = input.length();
                        start = x + 12;
                        end = RenderManager.getTextWidth(vg, input, 14f);
                        return;
                    }
                    if (GuiScreen.isKeyComboCtrlX(key)) {
                        if (selectedText != null && start != 0f && end != 0f) {
                            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(selectedText), null);
                            key = Keyboard.KEY_BACK;
                        } else return;
                    }
                    if (key == Keyboard.KEY_LEFT) {
                        caretPos = input.substring(0, caretPos).lastIndexOf(' ') + 1;
                    }
                    if (key == Keyboard.KEY_RIGHT) {
                        caretPos = input.indexOf(' ', caretPos);
                        if (caretPos == -1) caretPos = input.length();
                    }

                }
                if (key == Keyboard.KEY_BACK) {
                    if (input.length() > 0) {
                        if (start != 0f && end != 0f) {
                            start = 0f;
                            end = 0f;
                            if (caretPos > prevCaret) {
                                input = input.substring(0, prevCaret) + input.substring(caretPos);
                                caretPos = prevCaret;
                            }
                            if (caretPos < prevCaret) {
                                input = input.substring(0, caretPos) + input.substring(prevCaret);
                            }
                            return;
                        }
                        if (caretPos == input.length()) {
                            input = input.substring(0, input.length() - 1);
                        } else {
                            input = input.substring(0, caretPos - 1) + input.substring(caretPos);
                        }
                        caretPos--;
                    }
                    return;
                }
                if (key == Keyboard.KEY_TAB) {
                    input += "    ";
                    caretPos += 4;
                    return;
                }

                if (key == Keyboard.KEY_RIGHT) {
                    caretPos++;
                    if (caretPos > input.length()) {
                        caretPos = input.length();
                    }
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                    return;
                }
                if (key == Keyboard.KEY_LEFT) {
                    caretPos--;
                    if (caretPos < 0) {
                        caretPos = 0;
                    }
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                    return;
                }
                if (key == Keyboard.KEY_UP || key == 201) { // 201 = page up
                    caretPos = 0;
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                    return;
                }
                if (key == Keyboard.KEY_DOWN || key == 209) {     // 209 = page down
                    caretPos = input.length();
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                    return;
                }


                if (key == Keyboard.KEY_RETURN) {
                    toggled = false;
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                }
                if(key == Keyboard.KEY_END) {
                    toggled = false;
                }


                if (key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL || key == Keyboard.KEY_LMENU || key == Keyboard.KEY_RMENU || key == Keyboard.KEY_LMETA || key == Keyboard.KEY_RMETA || key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_CAPITAL || key == 221 || key == Keyboard.KEY_HOME) {
                    return;
                }
                if (!Character.isDefined(key)) return;
                if (!Character.isDefined(c)) return;
                if(GuiScreen.isCtrlKeyDown()) return;
                if(ChatAllowedCharacters.isAllowedCharacter(c)) {
                    input = addCharAtPoint(caretPos, c);
                    caretPos++;
                }
                if (start != 0f && end != 0f) {
                    start = 0f;
                    end = 0f;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private @NotNull String addCharAtPoint(int index, char c) {
        return input.substring(0, index) + c + input.substring(index);
    }

    @Override
    public void onClick() {
        caretPos = calculatePos(Mouse.getX());
        toggled = true;

    }

    private void onDoubleClick() {
        prevCaret = input.substring(0,caretPos).lastIndexOf(' ') + 1;
        caretPos = input.indexOf(' ', caretPos);
        if(caretPos == -1) caretPos = input.length();
        selectedText = input.substring(prevCaret, caretPos);
        start = x + 12 + RenderManager.getTextWidth(vg, input.substring(0, prevCaret), 14f);
        end = RenderManager.getTextWidth(vg, input.substring(prevCaret, caretPos), 14f);
    }

    private int calculatePos(int pos) {
        String s1 = "";
        int i;
        for (char c : input.toCharArray()) {
            if (pos - x - 12 < 0) {
                return 0;
            }
            if (pos - x - 12 > RenderManager.getTextWidth(vg, input, 14f)) {
                return input.length();
            }
            s1 += c;
            i = (int) RenderManager.getTextWidth(vg, s1, 14f);
            if (i >= pos - x - 16) {
                return s1.length();
            }
        }
        return 0;
    }
}
