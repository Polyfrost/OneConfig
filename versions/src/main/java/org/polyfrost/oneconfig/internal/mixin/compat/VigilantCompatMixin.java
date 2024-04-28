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

package org.polyfrost.oneconfig.internal.mixin.compat;

import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.CallablePropertyValue;
import gg.essential.vigilance.data.PropertyAttributesExt;
import gg.essential.vigilance.data.PropertyCollector;
import gg.essential.vigilance.data.PropertyData;
import gg.essential.vigilance.data.PropertyType;
import gg.essential.vigilance.data.SortingBehavior;
import kotlin.jvm.internal.DefaultConstructorMarker;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.visualize.Visualizer;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

import static org.polyfrost.oneconfig.api.config.v1.Property.prop;

// todo rewrite
@Mixin(value = Vigilant.class, remap = false)
@Pseudo
public abstract class VigilantCompatMixin {
    @Dynamic("OneConfig VCAL Processor")
    @Inject(method = "<init>(Ljava/io/File;Ljava/lang/String;Lgg/essential/vigilance/data/PropertyCollector;Lgg/essential/vigilance/data/SortingBehavior;ILkotlin/jvm/internal/DefaultConstructorMarker;)V", at = @At("TAIL"), remap = false)
    public void ocfg$compat$vigilance(File file, String title, PropertyCollector collector, SortingBehavior par4, int par5, DefaultConstructorMarker par6, CallbackInfo ci) {
        Tree b = new Tree(file.getName(), title, null, null);
        for (PropertyData data : collector.getProperties()) {
            PropertyAttributesExt attrs = data.getAttributesExt();
            String sub = attrs.getSubcategory();
            Tree bt = new Tree(attrs.getName(), attrs.getName(), attrs.getDescription(), null);
            bt.put(
                    prop("category", attrs.getCategory()),
                    prop("subcategory", sub.isEmpty() ? "General" : sub)
            );
            if (data.getDataType() == PropertyType.BUTTON) {
                Runnable r = () -> ((CallablePropertyValue) data.getValue()).invoke(data.getInstance());
                bt.put(
                        prop("visualizer", Visualizer.ButtonVisualizer.class),
                        prop("runnable", r),
                        prop("text", attrs.getPlaceholder())
                );
                b.put(bt);
                continue;
            }
            switch (data.getDataType()) {
                case TEXT:
                case PARAGRAPH:
                    bt.put(
                            prop("placeholder", attrs.getPlaceholder()),
                            prop("display", Visualizer.TextVisualizer.class)
                    );
                    break;
                case NUMBER:
                case DECIMAL_SLIDER:
                case PERCENT_SLIDER:
                case SLIDER:
                    float min = attrs.getMin();
                    float max = attrs.getMax();
                    if (min == 0f) {
                        min = attrs.getMinF();
                    }
                    if (max == 0f) {
                        max = attrs.getMaxF();
                    }
                    bt.put(
                            prop("min", min),
                            prop("max", max),
                            prop("visualizer", Visualizer.SliderVisualizer.class)
                    );
                    break;
                case SWITCH:
                case CHECKBOX: // todo
                    bt.put(prop("visualizer", Visualizer.SwitchVisualizer.class));
                    break;
                case SELECTOR:
                    bt.put(
                            prop("options", attrs.getOptions()),
                            prop("visualizer", Visualizer.DropdownVisualizer.class)
                    );
                    break;
                case COLOR:
                    bt.put(prop("visualizer", Visualizer.ColorVisualizer.class));
                    // todo
                    break;
                default:
                    System.err.println("[OneConfig VCAL] Unknown type: " + data.getDataType());
                    break;
            }
            b.put(bt);
        }
//        ConfigManager.INSTANCE.supplyMetadata(file.getName(), b, false);
    }

}