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

package org.polyfrost.oneconfig.internal.mixin;

import net.minecraft.crash.CrashReport;
import org.polyfrost.oneconfig.api.ui.v1.TinyFD;
import org.polyfrost.oneconfig.api.ui.v1.UIManager;
import org.polyfrost.oneconfig.utils.v1.MavenUpdateChecker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReport.class)
public abstract class CrashReportMixin {
    @Inject(method = "<init>", at = @At("RETURN"))
    private void ocfg$checkApiIssues(String desc, Throwable cause, CallbackInfo ci) {
        if (cause instanceof LinkageError) {
            String message = cause.getMessage();
            if (message != null && message.contains("org.polyfrost.oneconfig")) {
                ocfg$apiDeath(true);
                return;
            }
            for (StackTraceElement e : cause.getStackTrace()) {
                if (e.getClassName().contains("org.polyfrost.oneconfig")) {
                    ocfg$apiDeath(true);
                    return;
                }
            }
            // asm: still show the window in this case: it's a possible error.
            ocfg$apiDeath(false);
        }
    }

    @Unique
    private static void ocfg$apiDeath(boolean certain) {
        if (!MavenUpdateChecker.oneconfig().hasUpdate()) return;
        TinyFD tinyfd = UIManager.INSTANCE.getTinyFD();
        String title = certain ? "OneConfig API Error" : "OneConfig API Error (Possibly)";
        String message = "OneConfig has detected an crash that is potentially caused by an outdated version of OneConfig.\nYou can probably fix this by updating OneConfig by pressing OK, and restarting your game.";
        boolean upd = tinyfd.showMessageBox(title, message, TinyFD.OK_CANCEL_DIALOG, TinyFD.WARNING_ICON, true);
        if (upd) {
            // todo shit yeah
        }
    }
}
