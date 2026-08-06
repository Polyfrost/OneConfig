package org.polyfrost.oneconfig.internal.mixin.skia;

import androidx.compose.ui.platform.GlobalSnapshotManager;
import androidx.compose.ui.scene.BaseComposeScene;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(BaseComposeScene.class)
public class Mixin_FixComposeRaceCondition {
    @WrapOperation(remap = false, method = "<init>", at = @At(value = "INVOKE", target = "Landroidx/compose/ui/platform/GlobalSnapshotManager;ensureStarted()V"))
    void impl$redirectEnsureStartup(GlobalSnapshotManager instance, Operation<Void> original) {
        // This is done to prevent the startup of the GlobalSnapshotManager, which causes
        // issues when the Compose scene is not built in the AWT thread.
    }
}