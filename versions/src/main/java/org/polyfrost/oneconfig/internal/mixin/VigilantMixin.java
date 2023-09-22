/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
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

package org.polyfrost.oneconfig.internal.mixin;

import gg.essential.vigilance.Vigilant;
import gg.essential.vigilance.data.CallablePropertyValue;
import gg.essential.vigilance.data.PropertyAttributesExt;
import gg.essential.vigilance.data.PropertyCollector;
import gg.essential.vigilance.data.PropertyData;
import gg.essential.vigilance.data.PropertyType;
import gg.essential.vigilance.data.SortingBehavior;
import kotlin.jvm.internal.DefaultConstructorMarker;
import org.polyfrost.oneconfig.api.config.Config;
import org.polyfrost.oneconfig.api.config.Property;
import org.polyfrost.oneconfig.api.config.Tree;
import org.polyfrost.oneconfig.api.config.data.Category;
import org.polyfrost.oneconfig.api.config.visualize.Visualizer;
import org.polyfrost.oneconfig.internal.config.ConfigManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;

import static org.polyfrost.oneconfig.api.config.Property.prop;

@Mixin(Vigilant.class)
@Pseudo
public abstract class VigilantMixin {
    @Final
    @Shadow
    private PropertyCollector propertyCollector;

    @Unique
    private Tree oneconfig$cfg;

    @Inject(method = "<init>(Ljava/io/File;Ljava/lang/String;Lgg/essential/vigilance/data/PropertyCollector;Lgg/essential/vigilance/data/SortingBehavior;ILkotlin/jvm/internal/DefaultConstructorMarker;)V", at = @At("TAIL"))
    public void oneconfig$compat(File par1, String par2, PropertyCollector par3, SortingBehavior par4, int par5, DefaultConstructorMarker par6, CallbackInfo ci) {
        Tree.Builder b = Tree.tree(par1.getName());
        for(PropertyData data : par3.getProperties()) {
            PropertyAttributesExt attrs = data.getAttributesExt();
            String cat = attrs.getCategory();
            String sub = attrs.getSubcategory();
            String name = attrs.getName();
            if(data.getDataType() == PropertyType.BUTTON) {
                Runnable r = () -> ((CallablePropertyValue) data.getValue()).invoke(data.getInstance());
                Property<?> p = prop(name, r);
                p.addMetadata("text", attrs.getPlaceholder());
                p.addMetadata("description", attrs.getDescription());
                p.addMetadata("title", name);
                p.addMetadata("category", cat);
                p.addMetadata("subcategory", sub.isEmpty() ? "General" : sub);
                b.put(p);
                continue;
            }
            Property<?> p = prop(name, data.getValue().getValue(data.getInstance()));
            p.addMetadata("description", attrs.getDescription());
            p.addMetadata("title", name);
            p.addMetadata("category", cat);
            p.addMetadata("subcategory", sub.isEmpty() ? "General" : sub);
            switch (data.getDataType()) {
                case TEXT:
                case PARAGRAPH:
                    p.addMetadata("placeholder", attrs.getPlaceholder());
                    p.addMetadata("display", Visualizer.TextVisualizer.class);
                    break;
                case NUMBER:
                case DECIMAL_SLIDER:
                case PERCENT_SLIDER:
                case SLIDER:
                    p.addMetadata("display", Visualizer.SliderVisualizer.class);
                    float min = attrs.getMin();
                    float max = attrs.getMax();
                    if(min == 0f) {
                        min = attrs.getMinF();
                    }
                    if(max == 0f) {
                        max = attrs.getMaxF();
                    }
                    p.addMetadata("min", min);
                    p.addMetadata("max", max);
                    break;
                case SWITCH:
                case CHECKBOX: // todo?
                    p.addMetadata("display", Visualizer.SwitchVisualizer.class);
                    break;
                case SELECTOR:
                    p.addMetadata("options", attrs.getOptions());
                    p.addMetadata("display", Visualizer.DropdownVisualizer.class);
                    break;
                case COLOR:
                    p.addMetadata("display", Visualizer.ColorVisualizer.class);
                    break;
                default:
                    System.out.println("[OneConfig VCAL] Unknown type: " + data.getDataType());
                    break;
            }
            p.addCallback((v) -> data.getValue().setValue(v, data.getInstance()));
            b.put(p);
        }
        oneconfig$cfg = b.build();
        ConfigManager.INSTANCE.registerConfig(new Config(par1.getName(), par2, Category.OTHER));
    }

    @Inject(method = "readData", at = @At("TAIL"))
    public void oneconfig$read(CallbackInfo ci) {
        for(PropertyData data : propertyCollector.getProperties()) {
            if(data.getDataType() == PropertyType.BUTTON) continue;
            Property<?> p = oneconfig$cfg.get(data.getAttributesExt().getName());
            if(p == null) continue;
            p.setAs(data.getValue().getValue(data.getInstance()));
        }
    }

}