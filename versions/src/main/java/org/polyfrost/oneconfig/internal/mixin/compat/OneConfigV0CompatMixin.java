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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.config.v1.ConfigManager;
import org.polyfrost.oneconfig.api.config.v1.Property;
import org.polyfrost.oneconfig.api.config.v1.Tree;
import org.polyfrost.oneconfig.utils.v1.MHUtils;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;

@Mixin(value = Config.class, remap = false)
@Pseudo
public abstract class OneConfigV0CompatMixin {
    @Unique
    private static final Logger OCFG$LOGGER = LogManager.getLogger("OneConfig/Compat");

    @Shadow
    @Final
    public HashMap<String, BasicOption> optionNames;

    @Shadow
    @Final
    protected transient String configFile;

    @Shadow
    @Final
    public transient Mod mod;

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
                    target = t.getChild(path[i]);
                }
                BasicOption opt = entry.getValue();
                Property<?> prop = Property.prop(path[path.length - 1], opt.get(), opt.getField().getType());
                prop.setTitle(opt.name);
                prop.description = opt.description;
                prop.addDisplayCondition(opt::isHidden);
                MethodHandle setter = MHUtils.getMethodHandle(opt, "set", void.class, Object.class).getOrThrow();
                prop.addCallback(v -> MHUtils.invokeCatching(setter, v));
                target.put(prop);
            }
            ConfigManager.active().register(t);
        } catch (Exception e) {
            OCFG$LOGGER.error("Failed to perform compatibility with OneConfig V0 on {}", mod, e);
        }
    }
}
//#endif
