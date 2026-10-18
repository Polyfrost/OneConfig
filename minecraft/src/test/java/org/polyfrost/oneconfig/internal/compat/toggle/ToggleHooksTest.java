package org.polyfrost.oneconfig.internal.compat.toggle;

import org.junit.jupiter.api.Test;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToggleHooksTest {

    @Test
    void everyListedMixinExists() {
        for (String mixin : ToggleHooks.mixins()) {
            String path = "org/polyfrost/oneconfig/internal/mixin/" + mixin.replace('.', '/') + ".class";
            assertNotNull(ToggleHooks.class.getClassLoader().getResource(path), mixin);
        }
    }

    @Test
    void hooksThatMatchOrFindNothingFit() {
        assertTrue(ToggleHooks.fits(Target.class.getName(), Fitting.class.getName()));
        assertTrue(ToggleHooks.fits(Target.class.getName(), FindsNothing.class.getName()));
    }

    @Test
    void hooksOfTheWrongShapeDoNotFit() {
        assertFalse(ToggleHooks.fits(Target.class.getName(), InstanceOntoStatic.class.getName()));
        assertFalse(ToggleHooks.fits(Target.class.getName(), WrongReturnType.class.getName()));
        assertFalse(ToggleHooks.fits(Target.class.getName(), ReturnsNothing.class.getName()));
    }

    @Test
    void oneBadHookTakesItsWholeMixinDown() {
        assertFalse(ToggleHooks.fits(Target.class.getName(), OneOfTwoFits.class.getName()));
    }

    @SuppressWarnings("unused")
    static class Target {
        static boolean isEnabled() {
            return true;
        }

        void tick() {
        }
    }

    @SuppressWarnings("unused")
    static class Fitting {
        @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true, require = 0)
        private static void enabled(CallbackInfoReturnable<Boolean> cir) {
        }

        @Inject(method = {"tick", "update"}, at = @At("HEAD"), cancellable = true, require = 0)
        private void tick(CallbackInfo ci) {
        }
    }

    @SuppressWarnings("unused")
    static class FindsNothing {
        @Inject(method = "renamed", at = @At("HEAD"), cancellable = true, require = 0)
        private void renamed(CallbackInfo ci) {
        }
    }

    @SuppressWarnings("unused")
    static class InstanceOntoStatic {
        @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true, require = 0)
        private void enabled(CallbackInfoReturnable<Boolean> cir) {
        }
    }

    @SuppressWarnings("unused")
    static class WrongReturnType {
        @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true, require = 0)
        private static void enabled(CallbackInfoReturnable<Float> cir) {
        }
    }

    @SuppressWarnings("unused")
    static class ReturnsNothing {
        @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
        private void tick(CallbackInfoReturnable<Boolean> cir) {
        }
    }

    @SuppressWarnings("unused")
    static class OneOfTwoFits {
        @Inject(method = "tick", at = @At("HEAD"), cancellable = true, require = 0)
        private void tick(CallbackInfo ci) {
        }

        @Inject(method = "isEnabled", at = @At("HEAD"), cancellable = true, require = 0)
        private void enabled(CallbackInfoReturnable<Boolean> cir) {
        }
    }
}
