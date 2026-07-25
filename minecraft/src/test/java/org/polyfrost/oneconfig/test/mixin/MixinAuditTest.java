/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2026 Polyfrost.
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

package org.polyfrost.oneconfig.test.mixin;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.polyfrost.oneconfig.test.mixin.MixinAuditErrorCollector.Failure;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.MixinService;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MixinAuditTest {

    private static final String MOD_ID = "oneconfigv1";

    private static final String OWN_CONFIG_PREFIX = "mixins.oneconfigv1";

    private static final String CLASSLOAD_PHASE = "<classload>";

    private enum LoadResult {
        TRANSFORMED,
        ABSENT,
        ERRORED
    }

    @BeforeAll
    static void setUpEnvironment() {
        MixinAuditErrorCollector.reset();

        String collectorName = MixinAuditErrorCollector.class.getName();
        Mixins.registerErrorHandlerClass(collectorName);

        Class<?> resolved;
        try {
            resolved = classProvider().findClass(collectorName, false);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Mixin's class provider cannot see " + collectorName
                    + ", the error handler would never record anything", e);
        }
        assertSame(MixinAuditErrorCollector.class, resolved,
                "Error handler resolved to a different class than the test uses, failures would go unrecorded");

        try {
            SharedConstants.tryDetectVersion();
            Bootstrap.bootStrap();
        } catch (Throwable t) {
            System.out.println("[mixin-audit] Minecraft bootstrap did not complete, continuing anyway:");
            t.printStackTrace(System.out);
        }
    }

    @Test
    void auditMixins() throws Exception {
        MixinEnvironment environment = MixinEnvironment.getCurrentEnvironment();

        assertInstanceOf(IMixinTransformer.class, environment.getActiveTransformer(),
                "No active mixin transformer - Fabric Loader did not boot properly");

        IClassProvider provider = classProvider();
        MixinTargetScanner.Result declared =
                MixinTargetScanner.scan(MOD_ID, MixinAuditTest.class.getClassLoader(), provider);

        System.out.println("[mixin-audit] configs           : " + String.join(", ", declared.configNames()));
        System.out.println("[mixin-audit] mixins declared   : " + declared.mixinClasses().size());
        System.out.println("[mixin-audit] mixins missing    : " + declared.missingMixinClasses().size());
        System.out.println("[mixin-audit] mixins untargeted : " + declared.untargetedMixins().size());
        System.out.println("[mixin-audit] targets declared  : " + declared.targets().size());
        for (String untargeted : declared.untargetedMixins()) {
            System.out.println("[mixin-audit]   untargeted: " + untargeted);
        }

        assertFalse(declared.mixinClasses().isEmpty(), "OneConfig's mixin configs declared no mixins at all");
        assertFalse(declared.targets().isEmpty(), "OneConfig's mixins declared no targets at all");

        TreeSet<String> absent = new TreeSet<>();
        int transformed = 0;
        for (String target : declared.targets()) {
            switch (load(target, provider)) {
                case TRANSFORMED -> transformed++;
                case ABSENT -> absent.add(target);
                case ERRORED -> { /* already recorded under CLASSLOAD_PHASE */ }
            }
        }

        System.out.println("[mixin-audit] transformed       : " + transformed);
        System.out.println("[mixin-audit] skipped (absent)  : " + absent.size());
        for (String target : absent) {
            System.out.println("[mixin-audit]   - " + target);
        }

        for (String missing : declared.missingMixinClasses()) {
            MixinAuditErrorCollector.record("PREPARE", OWN_CONFIG_PREFIX, missing, null,
                    new ClassNotFoundException("Declared mixin class is not on the classpath"));
        }
        List<Failure> own = new ArrayList<>();
        int foreign = 0;
        for (Failure failure : MixinAuditErrorCollector.failures()) {
            if (failure.configName() != null && failure.configName().startsWith(OWN_CONFIG_PREFIX)) {
                own.add(failure);
            } else {
                foreign++;
            }
        }

        List<Failure> fatal = own.stream().filter(f -> !CLASSLOAD_PHASE.equals(f.phase())).toList();
        for (Failure failure : own) {
            if (CLASSLOAD_PHASE.equals(failure.phase())) {
                System.out.println("[mixin-audit] non-fatal class-load problem:\n" + failure.render());
            }
        }
        if (fatal.isEmpty()) {
            assertTrue(transformed > 0, "No declared target could be loaded");
            return;
        }

        StringBuilder report = new StringBuilder();
        report.append(fatal.size()).append(" OneConfig mixin(s) failed on this version:\n\n");
        int index = 1;
        for (Failure failure : fatal) {
            report.append('[').append(index++).append("]\n").append(failure.render()).append('\n');
        }
        if (foreign > 0) {
            report.append("(").append(foreign)
                    .append(" further failure(s) from third-party mixin configs were ignored.)\n");
        }
        Assertions.fail(report.toString());
    }

    private static IClassProvider classProvider() {
        return MixinService.getService().getClassProvider();
    }

    private static LoadResult load(String binaryName, IClassProvider provider) {
        try {
            provider.findClass(binaryName, false);
            return LoadResult.TRANSFORMED;
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            return LoadResult.ABSENT;
        } catch (Throwable t) {
            MixinAuditErrorCollector.record(CLASSLOAD_PHASE, OWN_CONFIG_PREFIX, "<unknown>", binaryName, t);
            return LoadResult.ERRORED;
        }
    }
}
