package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
//? if >=1.21.10 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import org.lwjgl.glfw.GLFW;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;

@Mixin(KeyBindsScreen.class)
public class Mixin_OneConfigKeybindRebind {
    @Shadow public KeyMapping selectedKey;
    @Shadow private KeyBindsList keyBindsList;

    @Unique private final LinkedHashSet<Integer> oneconfig$keys = new LinkedHashSet<>();
    @Unique private final LinkedHashSet<Integer> oneconfig$mouse = new LinkedHashSet<>();
    @Unique private boolean oneconfig$recording = false;

    //? if >=1.21.10 {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void oneconfig$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            MinecraftKeybindBridgeImpl.instance().clearPreview();
            MinecraftKeybindBridgeImpl.instance().menuUnbind(this.selectedKey);
            oneconfig$reset();
            oneconfig$finishSelection();
            cir.setReturnValue(true);
            return;
        }
        oneconfig$recording = true;
        oneconfig$keys.add(event.key());
        oneconfig$preview();
        cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void oneconfig$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            MinecraftKeybindBridgeImpl.instance().clearPreview();
            MinecraftKeybindBridgeImpl.instance().menuUnbind(this.selectedKey);
            oneconfig$reset();
            oneconfig$finishSelection();
            cir.setReturnValue(true);
            return;
        }
        oneconfig$recording = true;
        oneconfig$keys.add(keyCode);
        oneconfig$preview();
        cir.setReturnValue(true);
    }
    *///?}

    //? if >=1.21.10 {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void oneconfig$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        oneconfig$recording = true;
        oneconfig$mouse.add(event.button());
        oneconfig$preview();
        cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void oneconfig$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        oneconfig$recording = true;
        oneconfig$mouse.add(button);
        oneconfig$preview();
        cir.setReturnValue(true);
    }
    *///?}

    //? if >=1.21.10 {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void oneconfig$poll(CallbackInfo ci) {
        oneconfig$pollInputs();
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    private void oneconfig$poll(CallbackInfo ci) {
        oneconfig$pollInputs();
    }
    *///?}

    @Unique
    private void oneconfig$pollInputs() {
        if (!oneconfig$recording) return;
        long window = Platform.compatibility().windowHandle();
        for (int k : oneconfig$keys) if (GLFW.glfwGetKey(window, k) != GLFW.GLFW_RELEASE) return;
        for (int b : oneconfig$mouse) if (GLFW.glfwGetMouseButton(window, b) != GLFW.GLFW_RELEASE) return;
        oneconfig$commit();
    }

    @Unique
    private boolean oneconfig$isOurs() {
        KeyMapping sel = this.selectedKey;
        if (sel == null) return false;
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        return bridge != null && bridge.bindFor(sel) != null;
    }

    @Unique
    private void oneconfig$preview() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null || this.selectedKey == null) return;
        bridge.setPreview(this.selectedKey, oneconfig$toArray(oneconfig$keys), oneconfig$toArray(oneconfig$mouse));
        this.keyBindsList.resetMappingAndUpdateButtons();
    }

    @Unique
    private void oneconfig$commit() {
        KeyMapping sel = this.selectedKey;
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (sel != null && bridge != null && (!oneconfig$keys.isEmpty() || !oneconfig$mouse.isEmpty())) {
            bridge.menuRebind(sel, oneconfig$toArray(oneconfig$keys), oneconfig$toArray(oneconfig$mouse), true);
        }
        if (bridge != null) bridge.clearPreview();
        oneconfig$reset();
        oneconfig$finishSelection();
    }

    @Unique
    private void oneconfig$finishSelection() {
        this.selectedKey = null;
        this.keyBindsList.resetMappingAndUpdateButtons();
    }

    @Unique
    private void oneconfig$reset() {
        oneconfig$recording = false;
        oneconfig$keys.clear();
        oneconfig$mouse.clear();
    }

    @Unique
    private static int[] oneconfig$toArray(LinkedHashSet<Integer> set) {
        if (set.isEmpty()) return null;
        int[] out = new int[set.size()];
        int i = 0;
        for (int v : set) out[i++] = v;
        return out;
    }
}
