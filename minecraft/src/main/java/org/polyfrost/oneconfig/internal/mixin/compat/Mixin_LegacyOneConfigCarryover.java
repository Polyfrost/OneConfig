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

//#if MC<=11202 && FORGE

import cc.polyfrost.oneconfig.config.Config;
import cc.polyfrost.oneconfig.config.data.Mod;
import cc.polyfrost.oneconfig.config.elements.BasicOption;
import cc.polyfrost.oneconfig.gui.elements.config.*;
import dev.deftu.omnicore.api.loader.OmniLoader;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.*;
import org.polyfrost.oneconfig.internal.DynamicPolyImage;
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.polyfrost.polyui.data.PolyImage;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mixin(value = Config.class, remap = false)
@Pseudo
public abstract class Mixin_LegacyOneConfigCarryover {

    @Shadow
    @Final
    public transient HashMap<String, BasicOption> optionNames;
    @Shadow
    @Final
    public transient Mod mod;
    @Shadow
    @Final
    protected transient String configFile;
    @Shadow
    @Final
    private transient Logger logger;

    @Dynamic("OneConfig V0 Compat")
    @SuppressWarnings("DataFlowIssue")
    @Inject(method = "initialize", at = @At("RETURN"))
    private void compat$v0(CallbackInfo ci) {
        try {
            Tree t = Tree.tree(configFile);
            t.setTitle(mod.name);

            String iconPath = mod.modIcon;
            if (iconPath != null) {
                InputStream stream = OmniLoader.getResourceOrNull(OmniLoader.getActiveMod().getId(), iconPath);
                logger.debug("[V1] Found icon stream: {}", stream);
                if (stream != null) {
                    t.addMetadata("icon", new DynamicPolyImage(iconPath, stream));
                } else {
                    t.addMetadata("icon", PolyImage.of(iconPath));
                }
            }

            for (Map.Entry<String, BasicOption> entry : optionNames.entrySet()) {
                try {
                    String[] path = entry.getKey().split("\\.");
                    Tree target = t;
                    for (int i = 0; i < path.length - 1; i++) {
                        target = t.getOrPutChild(path[i]);
                    }

                    BasicOption opt = entry.getValue();
                    if (opt.getField() == null) continue;
                    String id = path[path.length - 1];
                    Property<?> prop;
                    Class<? extends Visualizer> visualizer;
                    if (opt instanceof ConfigButton) {
                        prop = Properties.dummy(id);
                        visualizer = Visualizer.ButtonVisualizer.class;
                        prop.addMetadata("runnable", opt.get());
                    } else {
                        Object parent = MHUtils.getFieldInHierarchy(BasicOption.class, opt, "parent").getOrThrow();
                        prop = Properties.field(opt.name, opt.description, opt.getField(), parent);
                        if (opt instanceof ConfigSwitch || opt instanceof ConfigCheckbox) {
                            visualizer = Visualizer.SwitchVisualizer.class;
                        } else if (opt instanceof ConfigSlider) {
                            visualizer = Visualizer.SliderVisualizer.class;

                            // todo: step

                            float min = oneconfig$getOptionMetadataField(opt, "min");
                            float max = oneconfig$getOptionMetadataField(opt, "max");
                            prop.addMetadata("min", min);
                            prop.addMetadata("max", max);
                        } else if (opt instanceof ConfigDropdown) {
                            visualizer = Visualizer.DropdownVisualizer.class;

                            List<String> options = oneconfig$getOptionMetadataField(opt, "options");
                            prop.addMetadata("options", options.toArray(new String[0]));
                        } else if (opt instanceof ConfigDualOption) {
                            visualizer = Visualizer.RadioVisualizer.class;

                            String left = oneconfig$getOptionMetadataField(opt, "left");
                            String right = oneconfig$getOptionMetadataField(opt, "right");

                            List<String> options = new ArrayList<>();
                            options.add(left);
                            options.add(right);
                            prop.addMetadata("options", options);
                        } else if (opt instanceof ConfigTextBox) {
                            visualizer = Visualizer.TextVisualizer.class;
                            // todo: placeholder, validate
                        } else if (opt instanceof ConfigColorElement) {
                            visualizer = Visualizer.ColorVisualizer.class;

                            // todo: Needs special compatibility to convert from OneColor to PolyColor
                        } else if (opt instanceof ConfigNumber) {
                            visualizer = Visualizer.NumberVisualizer.class;
                            // todo: unit, min, max, placeholder
                        } else continue;
                    }

                    prop.description = opt.description;
                    prop.addMetadata("visualizer", visualizer);
                    prop.addDisplayCondition(() -> {
                        if (opt.isHidden()) return Property.Display.HIDDEN;
                        else return Property.Display.SHOWN;
                    });

                    target.put(prop);
                } catch (Exception e) {
                    logger.error("[V1] Failed to add option {} to OneConfig V1!", entry.getKey(), e);
                }
            }

            ConfigManager.active().register(t);
            logger.info("[V1] Successfully moved {} options to OneConfig V1!", optionNames.size());
            if (Boolean.getBoolean("oneconfig.carryover.debug")) {
                logger.info("[V1] Carryover tree: {}\n{}", t, t.contentToString());
            }
        } catch (Exception e) {
            logger.error("[V1] Failed to perform compatibility with OneConfig V0!", e);
        }
    }

    @Unique
    @SuppressWarnings("unchecked")
    private <T> T oneconfig$getOptionMetadataField(BasicOption option, String name) {
        try {
            Field field = option.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return (T) field.get(option);
        } catch (NoSuchFieldException e) {
            logger.error("[V1] Failed to get field {} from option {}!", name, option.name, e);
        } catch (IllegalAccessException e) {
            logger.error("[V1] Failed to access field {} from option {}!", name, option.name, e);
        }

        return null;
    }

}
//#endif
