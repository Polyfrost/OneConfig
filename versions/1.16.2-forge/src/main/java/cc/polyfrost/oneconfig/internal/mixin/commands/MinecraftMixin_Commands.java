//#if FORGE==1
/* This file contains an adaptation of code from Fabric API (FabricMC/fabric)
 * Project found at <https://github.com/FabricMC/fabric/tree/1.18.2/>
 * For the avoidance of doubt, this file is still licensed under the terms
 * of OneConfig's Licensing.
 *
 *                 LICENSE NOTICE FOR ADAPTED CODE
 *
 * Copyright (c) 2016, 2017, 2018, 2019 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Significant changes (as required by the Apache 2.0):
 * - Removed Fabric "help" command.
 * - Removed unnecessary Fabric event for command registration.
 * - Renamed some classes that use Mixin to use MCP mappings instead.
 * - Refactored to use MCP mappings.
 *
 * As per the terms of the Apache 2.0 License, a copy of the License
 * is found at `src/main/resources/licenses/Fabric-License.txt`.
 */
package cc.polyfrost.oneconfig.internal.mixin.commands;

import cc.polyfrost.oneconfig.utils.commands.ClientCommandInternals;
import net.minecraft.client.GameConfiguration;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin_Commands {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onConstruct(GameConfiguration gameConfig, CallbackInfo info) {
        ClientCommandInternals.finalizeInit();
    }
}
//#endif