package io.polyfrost.oneconfig.gui.elements;

import io.polyfrost.oneconfig.config.OneConfigConfig;
import io.polyfrost.oneconfig.lwjgl.RenderManager;
import io.polyfrost.oneconfig.lwjgl.font.Fonts;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

public class TextInputField extends BasicElement {

    protected final String defaultText;
    protected String input;
    protected final boolean mulitLine;
    protected boolean password;

    public TextInputField(int width, int height, String defaultText, boolean multiLine, boolean password) {
        super(width, height, false);
        this.mulitLine = multiLine;
        this.defaultText = defaultText;
        this.password = password;
        this.input = defaultText;
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
        RenderManager.drawRectangle(vg, x, y, width, height, OneConfigConfig.GRAY_700);
        RenderManager.drawRectangle(vg, x + 2, y + 2, width - 2, height - 4, OneConfigConfig.GRAY_900);
        super.update(x, y);
        int color = toggled ? OneConfigConfig.WHITE : OneConfigConfig.WHITE_60;
        float width = RenderManager.getTextWidth(vg, input, 14f);

        if(toggled) {
            RenderManager.drawLine(vg, x + width + 12, (float) y + 7, x + width + 13, (float) y + height - 7, 1, OneConfigConfig.WHITE);
        }

        if(input.equals("")){
            RenderManager.drawString(vg, defaultText, x + 12, y + 17, color, 14f, Fonts.INTER_REGULAR);
        }
        RenderManager.drawString(vg, input, x + 12, y + 17, color, 14f, Fonts.INTER_REGULAR);

    }

    public void keyTyped(char c, int key) {
        if (toggled) {
            if(GuiScreen.isCtrlKeyDown()) {
                if(key == Keyboard.KEY_BACK) {
                    try {
                        input = input.substring(0, input.lastIndexOf(" "));
                    } catch (Exception e) {
                        input = "";
                    }
                }
                return;
            }
            if (key == Keyboard.KEY_BACK) {
                if (input.length() > 0) {
                    input = input.substring(0, input.length() - 1);
                    return;
                }
            }
            if(key == Keyboard.KEY_TAB) {
                input += "    ";
                return;
            }

            if(key == Keyboard.KEY_RETURN) {
                toggled = false;
            }

            if(key == Keyboard.KEY_LCONTROL || key == Keyboard.KEY_RCONTROL || key == Keyboard.KEY_LMENU || key == Keyboard.KEY_RMENU || key == Keyboard.KEY_LMETA || key == Keyboard.KEY_RMETA || key == Keyboard.KEY_LSHIFT || key == Keyboard.KEY_RSHIFT || key == Keyboard.KEY_RETURN || key == Keyboard.KEY_CAPITAL || key == 221) {
                return;
            }
            input += c;
        }
    }
}
