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

package cc.polyfrost.oneconfig.gui.elements.config;

import cc.polyfrost.oneconfig.config.annotations.HypixelKey;
import cc.polyfrost.oneconfig.config.annotations.Text;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.animations.Animation;
import cc.polyfrost.oneconfig.gui.animations.DummyAnimation;
import cc.polyfrost.oneconfig.gui.elements.IFocusable;
import cc.polyfrost.oneconfig.gui.elements.text.TextInputField;
import cc.polyfrost.oneconfig.internal.assets.Colors;
import cc.polyfrost.oneconfig.internal.assets.SVGs;
import cc.polyfrost.oneconfig.internal.config.HypixelKeys;
import cc.polyfrost.oneconfig.internal.utils.DescriptionRenderer;
import cc.polyfrost.oneconfig.libs.universal.UResolution;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.renderer.NanoVGHelper;
import cc.polyfrost.oneconfig.renderer.asset.SVG;
import cc.polyfrost.oneconfig.renderer.font.Fonts;
import cc.polyfrost.oneconfig.utils.InputHandler;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;

import java.lang.reflect.Field;

public class ConfigTextBox extends BasicOption implements IFocusable {
    private final boolean secure;
    private final boolean multiLine;
    private final boolean hypixelKey;
    public final TextInputField textField;
    private Animation descriptionAnimation = new DummyAnimation(0f);
    private float hoverTime = 0f;


    public ConfigTextBox(Field field, Object parent, String name, String description, String category, String subcategory, int size, String placeholder, boolean secure, boolean multiLine, boolean apiKey) {
        super(field, parent, name, description, category, subcategory, size);
        this.secure = secure || apiKey;
        this.multiLine = multiLine;
        this.hypixelKey = apiKey;
        this.textField = new TextInputField(size == 1 ? 256 : 640, multiLine ? 64 : 32, placeholder, multiLine, secure || apiKey);
    }

    public static ConfigTextBox create(Field field, Object parent) {
        Text text = field.getAnnotation(Text.class);
        boolean hypixelKey = field.isAnnotationPresent(HypixelKey.class);
        return new ConfigTextBox(field, parent, text.name(), text.description(), text.category(), text.subcategory(), text.secure() || hypixelKey || text.multiline() ? 2 : text.size(), text.placeholder(), text.secure(), text.multiline(), hypixelKey);
    }

    @Override
    public void draw(long vg, int x, int y, InputHandler inputHandler) {
        final NanoVGHelper nanoVGHelper = NanoVGHelper.INSTANCE;

        if (!isEnabled()) nanoVGHelper.setAlpha(vg, 0.5f);
        textField.disable(!isEnabled());
        nanoVGHelper.drawText(vg, name, x, y + 16, nameColor, 14, Fonts.MEDIUM);

        try {
            String value = (String) get();
            textField.setInput(value == null ? "" : value);
        } catch (IllegalAccessException ignored) {
        }

        if (multiLine && textField.getLines() > 2) textField.setHeight(64 + 24 * (textField.getLines() - 2));
        else if (multiLine) textField.setHeight(64);
        textField.draw(vg, x + (size == 1 ? 224 : 352), y, inputHandler);

        if (hypixelKey) {
            final SVG icon = SVGs.ARROWS_CLOCKWISE_BOLD;
            boolean hovered = inputHandler.isAreaHovered(x + 947, y + 7, 18, 18) && isEnabled();
            int color = hovered ? Colors.WHITE : Colors.WHITE_80;
            if (hovered && inputHandler.isClicked()) HypixelKeys.INSTANCE.syncKeys();
            if (hovered && Platform.getMousePlatform().isButtonDown(0)) nanoVGHelper.setAlpha(vg, 0.5f);
            nanoVGHelper.drawSvg(vg, icon, x + 967 - 18 - 4, y + 7, 18, 18, color);
        }

        if (secure) {
            final SVG icon = textField.getPassword() ? SVGs.EYE_OFF : SVGs.EYE;
            boolean hovered = inputHandler.isAreaHovered(x + 967, y + 7, 18, 18) && isEnabled();
            int color = hovered ? Colors.WHITE : Colors.WHITE_80;
            if (hovered && inputHandler.isClicked()) textField.setPassword(!textField.getPassword());
            if (hovered && Platform.getMousePlatform().isButtonDown(0)) nanoVGHelper.setAlpha(vg, 0.5f);
            nanoVGHelper.drawSvg(vg, icon, x + 967, y + 7, 18, 18, color);
        }
        nanoVGHelper.setAlpha(vg, 1f);
    }

    @Override
    public void keyTyped(char key, int keyCode) {
        if (!isEnabled()) return;
        textField.keyTyped(key, keyCode);
        try {
            set(textField.getInput());
        } catch (IllegalAccessException ignored) {
        }
    }

    @Override
    public int getHeight() {
        return multiLine ? textField.getHeight() : 32;
    }

    @Override
    protected boolean shouldDrawDescription() {
        return super.shouldDrawDescription() && !textField.isToggled();
    }

    public void drawSyncDescription(long vg, int x, int y, InputHandler inputHandler) {
        if (inputHandler.isAreaHovered(x + 947, y + 7, 18, 18)) hoverTime += GuiUtils.getDeltaTime();
        else hoverTime = 0;
        NanoVGHelper.INSTANCE.translate(vg, 967 - 18 - 4, 0);
        DescriptionRenderer.drawDescription(vg, x, y, "Sync other API keys marked in OneConfig to the same value.", null, () -> descriptionAnimation, (a) -> descriptionAnimation = a, hoverTime > 350, (UResolution.getWindowWidth() / 2f < inputHandler.mouseX()) ? DescriptionRenderer.DescriptionPosition.RIGHT : DescriptionRenderer.DescriptionPosition.LEFT, inputHandler);
        NanoVGHelper.INSTANCE.translate(vg, -967 + 18 + 4, 0);
    }
  
    @Override
    public boolean hasFocus() {
        return textField.isToggled();
    }
}
