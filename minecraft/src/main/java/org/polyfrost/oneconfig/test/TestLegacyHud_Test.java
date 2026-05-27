/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

/*
//~ gui_graphics
package org.polyfrost.oneconfig.test;

import kotlin.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;
import org.polyfrost.oneconfig.api.hud.v1.LegacyHud;

public class TestLegacyHud_Test extends LegacyHud {
    private static final float W = 20f;
    private static final float H = 20f;

    private ItemStack stack;

    public TestLegacyHud_Test() {
        super("test-legacy-hud", "Item HUD", Category.Companion.getINFO());
    }

    @Override
    public float getWidth() {
        return W;
    }

    @Override
    public float getHeight() {
        return H;
    }

    @Override
    public boolean update() {
        return false;
    }

    @Override
    public void render(@NotNull OmniRenderingContext mcCtx) {
        GuiGraphics graphics = mcCtx.graphics();
        if (graphics == null) return;

        if (stack == null) stack = new ItemStack(Items.DIAMOND_SWORD);

        graphics.fill(0, 0, (int) W, (int) H, 0x80000000);

        graphics.renderItem(stack, 2, 2);
    }

    @NotNull
    @Override
    public Pair<Float, Float> defaultPosition() {
        return new Pair<>(10f, 120f);
    }
}
*/