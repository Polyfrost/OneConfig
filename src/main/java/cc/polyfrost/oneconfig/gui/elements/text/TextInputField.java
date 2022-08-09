/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.gui.elements.text;

import cc.polyfrost.oneconfig.gui.elements.BasicElement;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.RenderManager;
import cc.polyfrost.oneconfig.renderer.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.renderer.scissor.Scissor;
import cc.polyfrost.oneconfig.renderer.scissor.ScissorManager;
import cc.polyfrost.oneconfig.utils.IOUtils;
import cc.polyfrost.oneconfig.utils.InputUtils;
import cc.polyfrost.oneconfig.utils.MathUtils;
import cc.polyfrost.oneconfig.utils.TextUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Objects;

public class TextInputField extends BasicElement {

    protected final String defaultText;
    protected final boolean multiLine;
    protected String input, selectedText;
    protected boolean password;

    protected int caretPos;
    protected float x, y;
    protected float start, end;
    protected int startLine, endLine;
    protected long vg;
    protected int prevCaret = 0;
    protected boolean isDoubleClick = false;
    protected boolean onlyNums = false;
    protected boolean errored = false;
    protected boolean centered = false;
    protected SVG icon;
    protected ArrayList<String> wrappedText = null;
    private long clickTimeD1;
    private int lines = 1;

    public TextInputField(int width, int height, String defaultText, boolean multiLine, boolean password, SVG icon) {
        super(width, height, false);
        this.multiLine = multiLine;
        this.defaultText = defaultText;
        this.password = password;
        this.input = "";
        this.icon = icon;
    }

    public TextInputField(int width, int height, String defaultText, boolean multiLine, boolean password) {
        this(width, height, defaultText, multiLine, password, null);
    }

    public TextInputField(int width, int height, boolean centered, String defaultText) {
        this(width, height, defaultText, false, false, null);
        this.centered = centered;
    }

    public static boolean isAllowedCharacter(char character) {
        return character != 167 && character >= ' ' && character != 127;
    }

    public void onlyAcceptNumbers(boolean state) {
        onlyNums = state;
    }

    public String getInput() {
        return input;
    }

    public void setInput(String input) {
        this.input = input;
    }

    public boolean getPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public void setCentered(boolean centered) {
        this.centered = centered;
    }

    public boolean isErrored() {
        return errored;
    }

    public void setErrored(boolean errored) {
        this.errored = errored;
    }

    @Override
    public void draw(long vg, float x, float y) {
        this.x = x;
        this.y = y;
        this.vg = vg;
        try {
            int colorOutline = errored ? Colors.ERROR_700 : Colors.GRAY_700;
            if (!toggled)
                RenderManager.drawHollowRoundRect(vg, x, y, width - 0.5f, height - 0.5f, colorOutline, 12f, 2f);
            else {
                RenderManager.setAlpha(vg, 0.15f);
                RenderManager.drawRoundedRect(vg, x - 4, y - 4, width + 8, height + 8, errored ? Colors.ERROR_600 : Colors.PRIMARY_600, 16);
                RenderManager.setAlpha(vg, 1f);
                RenderManager.drawHollowRoundRect(vg, x, y, width - 0.5f, height - 0.5f, errored ? Colors.ERROR_600 : Colors.PRIMARY_600, 12f, 2f);
            }
            Scissor scissor = ScissorManager.scissor(vg, x, y, width, height);
            super.update(x, y);
            if (Platform.getMousePlatform().isButtonDown(0) && !InputUtils.isAreaHovered(x - 40, y - 20, width + 90, height + 20)) {
                onClose();
                toggled = false;
            }
            int color = toggled ? Colors.WHITE : Colors.WHITE_60;
            if (!toggled) caretPos = input.length();
            if (caretPos > input.length()) caretPos = input.length();
            if (prevCaret > input.length()) prevCaret = input.length();
            if (caretPos < 0) caretPos = 0;
            if (prevCaret < 0) prevCaret = 0;
            if (icon != null) {
                RenderManager.drawSvg(vg, icon, x + 12, y + height / 2f - 12f, 24, 24, color);
                x += 32;
                this.x = x;
            }
            float width;
            StringBuilder s = new StringBuilder();
            if (multiLine) {
                wrappedText = TextUtils.wrapText(vg, input, this.width - 24, 14f, Fonts.REGULAR);
                lines = wrappedText.size();
                if (!toggled) caretPos = wrappedText.get(wrappedText.size() - 1).length();
                int caretLine = (int) MathUtils.clamp(getCaretLine(caretPos), 0, wrappedText.size() - 1);
                width = RenderManager.getTextWidth(vg, wrappedText.get(caretLine).substring(0, getLineCaret(caretPos, caretLine)), 14f, Fonts.REGULAR);
            } else if (!password) {
                width = RenderManager.getTextWidth(vg, input.substring(0, caretPos), 14f, Fonts.REGULAR);
            } else {
                for (int i = 0; i < input.length(); i++) {
                    s.append("*");
                }
                width = RenderManager.getTextWidth(vg, s.substring(0, caretPos), 14f, Fonts.REGULAR);
            }
            if (hovered) {
                int state = Platform.getMousePlatform().getButtonState(0); //todo does this work
                if (state == 1) {
                    if (multiLine) {
                        int caretLine = Math.max(0, Math.min(wrappedText.size() - 1, (int) Math.floor((InputUtils.mouseY() - y - 10) / 24f)));
                        caretPos = calculatePos(InputUtils.mouseX(), wrappedText.get(caretLine));
                    } else prevCaret = calculatePos(InputUtils.mouseX(), input);
                    if (System.currentTimeMillis() - clickTimeD1 < 300) {
                        onDoubleClick();
                        isDoubleClick = true;
                    }
                    clickTimeD1 = System.currentTimeMillis();
                } else {
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
            float halfTextWidth = this.getTextWidth(vg, input) / 2f;
            if (start != 0f && end != 0f && toggled) {
                if (!multiLine) {
                    RenderManager.drawRect(vg, start, y + height / 2f - 10, end, 20, Colors.GRAY_300);
                } else if (startLine == endLine) {
                    RenderManager.drawRect(vg, start, y + 10 + 24 * startLine, end, 20, Colors.GRAY_300);
                } else {
                    RenderManager.drawRect(vg, start, y + 10 + 24 * startLine, this.width - 24, 20, Colors.GRAY_300);
                    for (int i = startLine + 1; i < endLine; i++) {
                        RenderManager.drawRect(vg, x + 12, y + 10 + 24 * i, this.width - 24, 20, Colors.GRAY_300);
                    }
                    RenderManager.drawRect(vg, x + 12, y + 10 + 24 * endLine, end, 20, Colors.GRAY_300);
                }
            }
            if (hovered) {
                if (Platform.getMousePlatform().isButtonDown(0) && !isDoubleClick) {
                    if (multiLine) {
                        int caretLine = Math.max(0, Math.min(wrappedText.size() - 1, (int) Math.floor((InputUtils.mouseY() - y - 10) / 24f)));
                        caretPos = calculatePos(InputUtils.mouseX(), wrappedText.get(caretLine));
                        for (int i = 0; i < caretLine; i++) caretPos += wrappedText.get(i).length();
                    } else caretPos = calculatePos(InputUtils.mouseX(), input);
                    if (caretPos > prevCaret) {
                        if (!centered) start = x + 12 + this.getTextWidth(vg, input.substring(0, prevCaret));
                        else
                            start = x + this.width / 2f - halfTextWidth + this.getTextWidth(vg, input.substring(0, prevCaret));
                        end = this.getTextWidth(vg, input.substring(prevCaret, caretPos));
                        selectedText = input.substring(prevCaret, caretPos);
                    } else {
                        if (!centered) start = x + 12 + this.getTextWidth(vg, input.substring(0, prevCaret));
                        else
                            start = x + this.width / 2f - halfTextWidth + this.getTextWidth(vg, input.substring(0, prevCaret));
                        end = -this.getTextWidth(vg, input.substring(caretPos, prevCaret));
                        selectedText = input.substring(caretPos, prevCaret);
                    }
                }
            }

            if(disabled) RenderManager.setAlpha(vg, 0.5f);
            if (toggled) {
                if (multiLine) {
                    float lineY = y + 20 + getCaretLine(caretPos) * 24;
                    RenderManager.drawLine(vg, x + width + 12, lineY - 10, x + width + 12, lineY + 10, 1, Colors.WHITE);
                } else if (!centered) {
                    RenderManager.drawLine(vg, x + width + 12, y + height / 2f - 10, x + width + 12, y + height / 2f + 10, 1, Colors.WHITE);
                } else {
                    RenderManager.drawLine(vg, x + this.width / 2f - halfTextWidth + width, y + height / 2f - 10, x + this.width / 2f - halfTextWidth + width, y + height / 2f + 10, 1, Colors.WHITE);
                }
            }


            if (input.equals("")) {
                if (multiLine) {
                    RenderManager.drawText(vg, defaultText, x + 12, y + 16, color, 14f, Fonts.REGULAR);
                } else if (!centered) {
                    RenderManager.drawText(vg, defaultText, x + 12, y + height / 2f + 1, color, 14f, Fonts.REGULAR);
                } else {
                    RenderManager.drawText(vg, defaultText, x + this.width / 2f - halfTextWidth, y + height / 2f + 1, color, 14f, Fonts.REGULAR);
                }
            }

            if (!password) {
                if (multiLine) {
                    float textY = y + 20;
                    for (String line : wrappedText) {
                        RenderManager.drawText(vg, line, x + 12, textY, color, 14f, Fonts.REGULAR);
                        textY += 24;
                    }
                } else if (!centered) {
                    RenderManager.drawText(vg, input, x + 12, y + height / 2f + 1, color, 14f, Fonts.REGULAR);
                } else {
                    RenderManager.drawText(vg, input, x + this.width / 2f - halfTextWidth, y + height / 2f + 1, color, 14f, Fonts.REGULAR);
                }
            } else {
                RenderManager.drawText(vg, s.toString(), x + 12, y + height / 2f + 1, color, 14f, Fonts.REGULAR);
            }
            RenderManager.setAlpha(vg, 1f);
            ScissorManager.resetScissor(vg, scissor);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void keyTyped(char c, int key) {
        try {
            if (toggled) {
                if (UKeyboard.isKeyComboCtrlC(key)) {
                    if (selectedText != null && start != 0f && end != 0f) {
                        IOUtils.copyStringToClipboard(selectedText);
                    }
                    return;
                }
                if (UKeyboard.isKeyComboCtrlV(key) || key == 0xD2) { // TODO: is this the same in LWJGL 3?
                    try {
                        String clip = IOUtils.getStringFromClipboard();
                        input = input.substring(0, caretPos) + clip + input.substring(caretPos);
                        caretPos = caretPos + Objects.requireNonNull(clip).length();
                        return;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (key == UKeyboard.KEY_DELETE) {
                    input = "";
                }


                if (UKeyboard.isCtrlKeyDown()) {
                    if (key == UKeyboard.KEY_BACKSPACE && !UKeyboard.isKeyComboCtrlX(key)) {
                        try {
                            input = input.substring(0, input.lastIndexOf(" "));
                            caretPos = input.length();
                        } catch (Exception e) {
                            input = "";
                            caretPos = 0;
                        }
                        return;
                    }
                    if (UKeyboard.isKeyComboCtrlA(key)) {
                        prevCaret = 0;
                        caretPos = input.length();
                        start = !centered ? x + 12 : x + this.width / 2f - this.getTextWidth(vg, input) / 2f;
                        selectedText = input;
                        if (!multiLine) end = this.getTextWidth(vg, input);
                        if (multiLine) {
                            end = this.getTextWidth(vg, wrappedText.get(wrappedText.size() - 1));
                            startLine = 0;
                            endLine = wrappedText.size() - 1;
                        }
                        return;
                    }
                    if (UKeyboard.isKeyComboCtrlX(key)) {
                        if (selectedText != null && start != 0f && end != 0f) {
                            IOUtils.copyStringToClipboard(selectedText);
                            key = UKeyboard.KEY_BACKSPACE;
                        } else return;
                    }
                    if (key == UKeyboard.KEY_LEFT) {
                        caretPos = input.substring(0, caretPos).lastIndexOf(' ') + 1;
                    }
                    if (key == UKeyboard.KEY_RIGHT) {
                        caretPos = input.indexOf(' ', caretPos);
                        if (caretPos == -1) caretPos = input.length();
                    }

                }
                if (key == UKeyboard.KEY_BACKSPACE) {
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
                if (key == UKeyboard.KEY_TAB) {
                    if (onlyNums) return;
                    input += "    ";
                    caretPos += 4;
                    return;
                }

                if (key == UKeyboard.KEY_RIGHT) {
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
                if (key == UKeyboard.KEY_LEFT) {
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
                if (key == UKeyboard.KEY_UP || key == 201) { // 201 = page up
                    caretPos = 0;
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                    return;
                }
                if (key == UKeyboard.KEY_DOWN || key == 209) {     // 209 = page down
                    caretPos = input.length();
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                    return;
                }


                if (key == UKeyboard.KEY_ENTER) {
                    onClose();
                    toggled = false;
                    if (start != 0f && end != 0f) {
                        start = 0f;
                        end = 0f;
                    }
                }
                if (key == UKeyboard.KEY_END) {
                    onClose();
                    toggled = false;
                }


                if (key == UKeyboard.KEY_LCONTROL || key == UKeyboard.KEY_RCONTROL || key == UKeyboard.KEY_LMENU || key == UKeyboard.KEY_RMENU || key == UKeyboard.KEY_LMETA || key == UKeyboard.KEY_RMETA || key == UKeyboard.KEY_LSHIFT || key == UKeyboard.KEY_RSHIFT || key == UKeyboard.KEY_ENTER || key == UKeyboard.KEY_CAPITAL || key == 221 || key == UKeyboard.KEY_HOME) {
                    return;
                }
                if (onlyNums) {
                    if (!Character.isDigit(c) && key != 52) return;
                }
                if (!Character.isDefined(key)) return;
                if (!Character.isDefined(c)) return;
                if (UKeyboard.isCtrlKeyDown()) return;
                if (isAllowedCharacter(c)) {
                    if (selectedText != null) {
                        if (caretPos > prevCaret) caretPos = prevCaret;
                        if (selectedText.equals(input)) {
                            input = "";
                        }
                        selectedText = null;
                    }
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
        toggled = true;
    }

    private void onDoubleClick() {
        prevCaret = input.substring(0, caretPos).lastIndexOf(' ') + 1;
        caretPos = input.indexOf(' ', caretPos);
        if (caretPos == -1) caretPos = input.length();
        selectedText = input.substring(prevCaret, caretPos);
        if (multiLine) {
            int caretLine = Math.max(0, Math.min(wrappedText.size() - 1, (int) Math.floor((InputUtils.mouseY() - y - 10) / 24f)));
            startLine = caretLine;
            endLine = caretLine;
            start = x + 12 + this.getTextWidth(vg, wrappedText.get(caretLine).substring(0, getLineCaret(prevCaret, startLine)));
            end = this.getTextWidth(vg, wrappedText.get(caretLine).substring(getLineCaret(prevCaret, startLine), getLineCaret(caretPos, startLine)));
        } else {
            start = x + 12 + this.getTextWidth(vg, input.substring(0, prevCaret));
            end = this.getTextWidth(vg, input.substring(prevCaret, caretPos));
        }
    }

    private int calculatePos(float pos, String string) {
        if (centered) pos -= 12;
        String s1 = "";
        int i;
        for (char c : string.toCharArray()) {
            if (pos - x - 12 < 0) {
                return 0;
            }
            if (pos - x - 12 > this.getTextWidth(vg, string)) {
                return string.length();
            }
            s1 += c;
            i = (int) this.getTextWidth(vg, s1);
            if (i >= pos - x - 16) {
                return s1.length();
            }
        }
        return 0;
    }

    public void onClose() {
    }

    private float getTextWidth(long vg, String s) {
        if (password) {
            StringBuilder s1 = new StringBuilder();
            while (s1.length() < s.length()) {
                s1.append('*');
            }
            return RenderManager.getTextWidth(vg, s1.toString(), 14.0f, Fonts.REGULAR);
        } else {
            return RenderManager.getTextWidth(vg, s, 14.0f, Fonts.REGULAR);
        }
    }

    private int getCaretLine(int caret) {
        int pos = 0;
        for (int i = 0; i < wrappedText.size(); i++) {
            pos += wrappedText.get(i).length();
            if (pos < caret - 1) continue;
            return i;
        }
        return 0;
    }

    private float getCaretX(int caret) {
        int pos = 0;
        for (String text : wrappedText) {
            float length = RenderManager.getTextWidth(vg, text, 14.0f, Fonts.REGULAR);
            if (pos + length < caret) {
                pos += length;
                continue;
            }
            return RenderManager.getTextWidth(vg, text.substring(0, caret - pos), 14.0f, Fonts.REGULAR);
        }
        return 0;
    }

    private int getLineCaret(int caret, int line) {
        int pos = 0;
        for (String text : wrappedText) {
            int length = text.length();
            if (pos + length < caret - 1) {
                pos += length;
                continue;
            }
            return caret - pos;
        }
        return 0;
    }

    public int getLines() {
        return lines;
    }
}
