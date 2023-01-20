/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.KeyBind;
import cc.polyfrost.oneconfig.config.core.OneKeyBind;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.OneConfigGui;
import cc.polyfrost.oneconfig.gui.elements.BasicButton;
import cc.polyfrost.oneconfig.gui.elements.IFocusable;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.libs.universal.UKeyboard;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.color.ColorPalette;

import java.lang.reflect.Field;

public class ConfigKeyBind extends BasicOption implements IFocusable {
    private final BasicButton button;
    private boolean clicked = false;

    public ConfigKeyBind(Field field, Object parent, String name, String description, String category, String subcategory, int size) {
        super(field, parent, name, description, category, subcategory, size);
        button = new BasicButton(256, 32, "", SVGs.KEYSTROKE, null, BasicButton.ALIGNMENT_JUSTIFIED, ColorPalette.SECONDARY);
        button.setToggleable(true);
    }

    public static ConfigKeyBind create(Field field, Object parent) {
        KeyBind keyBind = field.getAnnotation(KeyBind.class);
        return new ConfigKeyBind(field, parent, keyBind.name(), keyBind.description(), keyBind.category(), keyBind.subcategory(), keyBind.size());
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;
        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);
        nanoVGHelper.drawText(vg, name, x, y + 17, nameColor, 14f, Fonts.MEDIUM);
        OneKeyBind keyBind = getKeyBind();
        String text = keyBind.getDisplay();
        button.disable(!isEnabled());
        if (button.isToggled()) {
            if (text.equals("")) text = "Recording... (ESC to clear)";
            if (!clicked) {
                keyBind.clearKeys();
                setKeyBind(keyBind);
                clicked = true;
            } else if (keyBind.getSize() == 0 || keyBind.isActive()) {
                OneConfigGui.INSTANCE.allowClose = false;
            } else {
                button.setToggled(false);
                clicked = false;
                OneConfigGui.INSTANCE.allowClose = true;
            }
        } else if (text.equals("")) text = "None";
        button.setText(text);
        button.draw(vg, x + (size == 1 ? 224 : 736), y, inputHandler);
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (!button.isToggled()) return;
        OneKeyBind keyBind = getKeyBind();
        if (keyCode == UKeyboard.KEY_ESCAPE) {
            keyBind.clearKeys();
            button.setToggled(false);
            OneConfigGui.INSTANCE.allowClose = true;
            clicked = false;
        } else keyBind.addKey(keyCode);
        setKeyBind(keyBind);
    }

    private OneKeyBind getKeyBind() {
        OneKeyBind keyBind = new OneKeyBind();
        try {
            field.setAccessible(true);
            keyBind = (OneKeyBind) get();
        } catch (IllegalAccessException ignored) {
        }
        return keyBind;
    }

    private void setKeyBind(OneKeyBind keyBind) {
        try {
            set(keyBind);
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public int getHeight() {
        return 32;
    }

    @Override
    public boolean hasFocus() {
        return clicked;
    }
}
