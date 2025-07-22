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

package org.polyfrost.oneconfig.internal;

import kotlin.Unit;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.polyfrost.oneconfig.internal.generated.RelocatedMixins;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class OneConfigMixinInit implements IMixinConfigPlugin {

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        List<String> mixins = new ArrayList<>();


        RelocatedMixins.INSTANCE.register(e -> {
            mixins.add(e);
            return Unit.INSTANCE;
        });

        //#if MC>=1.19.2
        //$$mixins.add("Mixin_MinecraftClientResourceStuff");
        //$$mixins.add("compat.yacl.Mixin_YetAnotherConfigLib_Builder");
        //#endif

        //#if MC>=1.20.4
        //$$mixins.add("compat.rconfig.Mixin_Configurations");
        //#endif

        //#if MC>1.16
        //$$mixins.add("compat.modmenu.Mixin_ModMenu");
        //#endif

        //#if FORGE
        mixins.add("events.Mixin_ChatReceiveEvent_Forge");
        //#if MC < 1.13
        mixins.add("compat.Mixin_LegacyOneConfigCarryover");
        mixins.add("compat.Mixin_OverwriteLegacyCommand");
        mixins.add("compat.Mixin_OverwriteLegacyKeyBind");
        mixins.add("fixes.Mixin_ASMModParser_IgnoreForgeJava9Spam");
        mixins.add("fixes.Mixin_JarDiscoverer_IgnoreForgeJava9Spam");
        mixins.add("hidpi.Mixin_FixLoadingScreenHiDPI");
        //#endif
        //#else
        //$$ mixins.add("fabric.Mixin_LoadShaderInvoker_Fabric");
        //$$ mixins.add("fabric.Mixin_ChatReceiveEvent_Fabric");
        //#endif

        //#if MC >= 1.16
        //$$ mixins.add("events.Mixin_ModernWindowFocusEvent");
        //$$ mixins.add("command.Mixin_ModernArgumentTypesAccessor");
        //$$ // mixins.add("command.Mixin_ModernArgumentTypeEntryAccessor");
        //#if MC < 1.19
        //$$ mixins.add("fixes.Mixin_LazyDataFixerUpper");
        //$$ mixins.add("events.Mixin_ChatSendEvent");
        //#endif
        //#else
        mixins.add("events.Mixin_KeyInputEvent_Screen");
        mixins.add("hidpi.Mixin_EnableHiDPI");
        mixins.add("hidpi.Mixin_FixDisplaySizeHiDPI");
        mixins.add("hidpi.Mixin_FixMousePositionHiDPI_Screen");
        mixins.add("hidpi.Mixin_FixMousePositionHiDPI");
        //#if MC <= 1.8.9
        mixins.add("Mixin_SoundHandlerAccessor");
        //#endif
        //#endif

        {
            Logger logger = LogManager.getLogger(OneConfigMixinInit.class);
            logger.info("Loaded {} non-common Mixins", mixins.size());

            for (int i = 0; i < mixins.size(); i += 5) {
                int end = Math.min(i + 5, mixins.size());
                List<String> batch = mixins.subList(i, end);
                logger.info("Loaded Mixins: {}", String.join(", ", batch));
            }
        }

        return mixins;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

}
