package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.nanovg.NanoVG;

import static org.lwjgl.nanovg.NanoVG.nvgResetScissor;
import static org.lwjgl.nanovg.NanoVG.nvgScissor;

public class TextInputField extends BasicElement {

    protected final String defaultText;
    protected String input;
    protected final boolean multiLine;
    protected boolean password;

    protected int caretPos;
    protected int x, y;
    protected long vg;
    protected int deltaX;
    protected int prevCaret = 0;

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

    @Override
    public void draw(long vg, int x, int y) {
        this.x = x;
        this.y = y;
        this.vg = vg;
        RenderManager.drawHollowRoundRect(vg, x, y, width, height, OneConfigConfig.GRAY_700, 12f, 2f);
        super.update(x, y);
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
        if (clicked) {
            NanoVG.nvgGlobalAlpha(vg, 0.3f);
            int point = (Mouse.getX() - x) - deltaX;
            if (Mouse.getX() - x - 12 < 0) {
                RenderManager.drawRect(vg, x + offset + width, y + 2, -width, height - 4, OneConfigConfig.BLUE_600);
            } else if (!(Mouse.getX() - x > RenderManager.getTextWidth(vg, input, 14f) + 12)) {
                RenderManager.drawRect(vg, x + offset + width, y + 2, point, height - 4, OneConfigConfig.BLUE_600);
            } else {
                RenderManager.drawRect(vg, x + offset + width, y + 2, RenderManager.getTextWidth(vg, input, 14f) - width, height - 4, OneConfigConfig.BLUE_600);
            }
            //System.out.println(offset + width);
            //System.out.println(point);
            prevCaret = calculatePos((int) (offset + width));
            NanoVG.nvgGlobalAlpha(vg, 1f);
        }
        try {
            //System.out.println("prevCaret: " + prevCaret + " caretPos: " + caretPos);
            //RenderManager.drawRect(vg, (int) x + RenderManager.getTextWidth(vg, input.substring(0, caretPos), 14f), y, (int) RenderManager.getTextWidth(vg, input.substring(caretPos, prevCaret), 14f), height, OneConfigConfig.GRAY_300);
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (toggled) {
            RenderManager.drawLine(vg, x + width + 12, (float) y + 7, x + width + 12, (float) y + height - 7, 1, OneConfigConfig.WHITE);
        }


        if (input.equals("")) {
            RenderManager.drawString(vg, defaultText, x + 12, y + 17, color, 14f, Fonts.INTER_REGULAR);
        }

        if (!password) {
            RenderManager.drawString(vg, input, x + offset, y + 17, color, 14f, Fonts.INTER_REGULAR);
        } else {
            RenderManager.drawString(vg, s.toString(), x + offset, y + 17, color, 14f, Fonts.INTER_REGULAR);
        }
        nvgResetScissor(vg);
    }

    public void keyTyped(char c, int key) {
        if (toggled) {
            if (GuiScreen.isCtrlKeyDown()) {
                if (key == Keyboard.KEY_BACK) {
                    try {
                        input = input.substring(0, input.lastIndexOf(" "));
                        caretPos = input.length();
                    } catch (Exception e) {
                        input = "";
                        caretPos = 0;
                    }
                }
                return;
            }
            if (key == Keyboard.KEY_BACK) {
                if (input.length() > 0) {
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
                return;
            }
            if (key == Keyboard.KEY_LEFT) {
                caretPos--;
                if (caretPos < 0) {
                    caretPos = 0;
                }
                return;
            }


            if (key == Keyboard.KEY_RETURN) {
                toggled = false;
            }

            if (key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL || key == Keyboard.KEY_LMENU || key == Keyboard.KEY_RMENU || key == Keyboard.KEY_LMETA || key == Keyboard.KEY_RMETA || key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_CAPITAL || key == 221) {
                return;
            }
            input = addCharAtPoint(caretPos, c);
            caretPos++;
        }
    }

    private @NotNull String addCharAtPoint(int index, char c) {
        return input.substring(0, index) + c + input.substring(index);
    }

    @Override
    public void onClick() {
        deltaX = Mouse.getX() - x;
        toggled = true;
        caretPos = calculatePos(Mouse.getX());
    }

    private int calculatePos(int pos) {
        String s1 = "";
        int i;
        for (char c : input.toCharArray()) {
            if (pos - x - 12 < 0) {
                return 0;
            }
            if (pos - x - 12 > RenderManager.getTextWidth(vg, input, 14f)) {
                deltaX = (int) RenderManager.getTextWidth(vg, input, 14f) + 12;
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
