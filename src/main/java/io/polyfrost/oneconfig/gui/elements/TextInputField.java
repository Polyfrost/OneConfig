package io.polyfrost.oneconfig.gui.elements;

import com.google.common.base.Strings;
import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import net.minecraft.client.gui.GuiScreen;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.input.Keyboard;

import static org.lwjgl.nanovg.NanoVG.*;

public class TextInputField extends BasicElement {

    protected final String defaultText;
    protected String input;
    protected final boolean multiLine;
    protected boolean password;

    protected int caretPos;

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
        RenderManager.drawHollowRoundRect(vg, x, y, width, height, OneConfigConfig.GRAY_700, 12f, 2f);
        super.update(x, y);
        int color = toggled ? OneConfigConfig.WHITE : OneConfigConfig.WHITE_60;
        float width;
        StringBuilder s = new StringBuilder();
        int offset = 12;
        if(!password) {
            width = RenderManager.getTextWidth(vg, input.substring(0, caretPos), 14f);
        } else {
            for(int i = 0; i < input.length(); i++) {
                s.append("*");
            }
            width = RenderManager.getTextWidth(vg, s.substring(0, caretPos), 14f);
        }

        if(toggled) {
            RenderManager.drawLine(vg, x + width + 12, (float) y + 7, x + width + 13, (float) y + height - 7, 1, OneConfigConfig.WHITE);
        }

        if(input.equals("")){
            RenderManager.drawString(vg, defaultText, x + 12, y + 17, color, 14f, Fonts.INTER_REGULAR);
        }
        nvgScissor(vg, x, y, this.width, height);
        if(!password) {
            RenderManager.drawString(vg, input, x + offset, y + 17, color, 14f, Fonts.INTER_REGULAR);
            nvgResetScissor(vg);
        } else {

            RenderManager.drawString(vg, s.toString(), x + offset, y + 17, color, 14f, Fonts.INTER_REGULAR);
        }
    }

    public void keyTyped(char c, int key) {
        if (toggled) {
            if(GuiScreen.isCtrlKeyDown()) {
                if(key == Keyboard.KEY_BACK) {
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
                    if(caretPos == input.length()) {
                        input = input.substring(0, input.length() - 1);
                    } else {
                        input = input.substring(0, caretPos - 1) + input.substring(caretPos);
                    }
                    caretPos--;
                }
                return;
            }
            if(key == Keyboard.KEY_TAB) {
                input += "    ";
                caretPos += 4;
                return;
            }

            if (key == Keyboard.KEY_RIGHT) {
                caretPos++;
                if(caretPos > input.length()) {
                    caretPos = input.length();
                }
                return;
            }
            if (key == Keyboard.KEY_LEFT) {
                caretPos--;
                if(caretPos < 0) {
                    caretPos = 0;
                }
                return;
            }



            if(key == Keyboard.KEY_RETURN) {
                toggled = false;
            }

            if(key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL || key == Keyboard.KEY_LMENU || key == Keyboard.KEY_RMENU || key == Keyboard.KEY_LMETA || key == Keyboard.KEY_RMETA || key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_CAPITAL || key == 221) {
                return;
            }
            input = addCharAtPoint(caretPos, c);
            caretPos++;
        }
    }

    private @NotNull String addCharAtPoint(int index, char c) {
        return input.substring(0, index) + c + input.substring(index);
    }
}
