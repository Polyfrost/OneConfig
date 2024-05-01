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
import cc.polyfrost.oneconfig.gui.elements.config.ConfigButton;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigCheckbox;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigColorElement;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigDropdown;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigDualOption;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigNumber;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigSlider;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigSwitch;
import cc.polyfrost.oneconfig.gui.elements.config.ConfigTextBox;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.DummyProperty;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.api.config.v1.visualize.Visualizer;
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

@Mixin(value = Config.class, remap = false)
@Pseudo
public abstract class OneConfigV0CompatMixin {
    @Shadow
    @Final
    public transient HashMap<String, BasicOption> optionNames;

    @Shadow
    @Final
    protected transient String configFile;

    @Shadow
    @Final
    public transient Mod mod;

    @Shadow
    @Final
    private transient Logger logger;

    @Inject(method = "initialize", at = @At("RETURN"))
    @Dynamic("OneConfig V0 Compat")
    private void ocfg$compat$v0(CallbackInfo ci) {
        try {
            Tree t = Tree.tree(configFile);
            t.setTitle(mod.name);
            for (Map.Entry<String, BasicOption> entry : optionNames.entrySet()) {
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
                    prop = DummyProperty.dummy(id);
                    visualizer = Visualizer.ButtonVisualizer.class;
                    prop.addMetadata("runnable", opt.get());
                } else {
                    prop = Property.prop(id, opt.name, opt.get(), opt.getField().getType());
                    if (opt instanceof ConfigSwitch || opt instanceof ConfigCheckbox) {
                        visualizer = Visualizer.SwitchVisualizer.class;
                    } else if (opt instanceof ConfigSlider) {
                        visualizer = Visualizer.SliderVisualizer.class;
                    } else if (opt instanceof ConfigDropdown) {
                        visualizer = Visualizer.DropdownVisualizer.class;
                    } else if (opt instanceof ConfigDualOption) {
                        visualizer = Visualizer.RadioVisualizer.class;
                    } else if (opt instanceof ConfigTextBox) {
                        visualizer = Visualizer.TextVisualizer.class;
                    } else if (opt instanceof ConfigColorElement) {
                        visualizer = Visualizer.ColorVisualizer.class;
                    } else if (opt instanceof ConfigNumber) {
                        visualizer = Visualizer.NumberVisualizer.class;
                    } else continue;
                }

                prop.description = opt.description;
                prop.addMetadata("visualizer", visualizer);
                prop.addDisplayCondition(opt::isHidden);
                MethodHandle setter = MHUtils.getMethodHandle(opt, "set", void.class, Object.class).getOrThrow();
                prop.addCallback(v -> MHUtils.invokeCatching(setter, v));
                target.put(prop);
            }
            ConfigManager.active().register(t);
        } catch (Exception e) {
            logger.error("[V1] Failed to perform compatibility with OneConfig V0!", e);
        }
    }
}
//#endif
