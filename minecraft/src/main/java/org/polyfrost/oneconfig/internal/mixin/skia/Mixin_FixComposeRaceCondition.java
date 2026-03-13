package org.polyfrost.oneconfig.internal.mixin.skia;

import androidx.compose.ui.platform.GlobalSnapshotManager;
import androidx.compose.ui.scene.BaseComposeScene;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BaseComposeScene.class)
public class Mixin_FixComposeRaceCondition {
    @Redirect(remap = false, method = "<init>", at = @At(value = "INVOKE", target = "Landroidx/compose/ui/platform/GlobalSnapshotManager;ensureStarted()V"))
    void impl$redirectEnsureStartup(GlobalSnapshotManager instance) {
        // This is done to prevent the startup of the GlobalSnapshotManager, which causes
        // issues when the Compose scene is not built in the AWT thread.
    }
}