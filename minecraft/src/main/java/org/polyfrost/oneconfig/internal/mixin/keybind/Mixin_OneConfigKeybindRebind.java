package org.polyfrost.oneconfig.internal.mixin.keybind;

import net.minecraft.client.KeyMapping;
//? if > 1.8.9 {
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
//?} else {
/*import org.polyfrost.oneconfig.internal.legacy.KeyCodes;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
*///?}
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
//? if >=1.21.10 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
import org.lwjgl.glfw.GLFW;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.polyfrost.oneconfig.internal.ui.keybind.OneConfigKeybindRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//? if > 1.8.9
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;

@Mixin(KeyBindsScreen.class)
public class Mixin_OneConfigKeybindRebind implements OneConfigKeybindRecorder {
    @Shadow public KeyMapping selectedKey;
    //? if > 1.8.9
    @Shadow private KeyBindsList keyBindsList;

    @Unique private final LinkedHashSet<Integer> oneconfig$keys = new LinkedHashSet<>();
    @Unique private final LinkedHashSet<Integer> oneconfig$mouse = new LinkedHashSet<>();
    @Unique private boolean oneconfig$recording = false;
    @Unique private KeyMapping oneconfig$target = null;

    //? if >=1.21.10 {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void oneconfig$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        if (event.key() == GLFW.GLFW_KEY_ESCAPE) {
            oneconfig$recordEscape();
            cir.setReturnValue(true);
            return;
        }
        oneconfig$recordKey(event.key());
        cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    //? if > 1.8.9 {
    private void oneconfig$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
    //?} else
    //private void oneconfig$keyPressed(char keyChar, int keyCode, CallbackInfo ci) {
        if (!oneconfig$isOurs()) return;
        //? if = 1.8.9 {
        /^keyCode = KeyCodes.fromLegacy(keyCode).getValue();
        if (keyCode < 0) {
            ci.cancel();
            return;
        }
        ^///?}
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            oneconfig$recordEscape();
            //$ if > 1.8.9 'cir.setReturnValue(true);' else 'ci.cancel();'
            cir.setReturnValue(true);
            return;
        }
        oneconfig$recordKey(keyCode);
        //$ if > 1.8.9 'cir.setReturnValue(true);' else 'ci.cancel();'
        cir.setReturnValue(true);
    }
    *///?}

    //? if >=1.21.10 {
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void oneconfig$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        oneconfig$recordMouse(event.button());
        cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    //? if > 1.8.9 {
    private void oneconfig$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
    //?} else
    //private void oneconfig$mouseClicked(int mouseX, int mouseY, int button, CallbackInfo ci) {
        if (!oneconfig$isOurs()) return;
        oneconfig$recordMouse(button);
        //$ if > 1.8.9 'cir.setReturnValue(true);' else 'ci.cancel();'
        cir.setReturnValue(true);
    }
    *///?}

    //? if >=26.1 {
    @Inject(method = "extractRenderState", at = @At("HEAD"))
    private void oneconfig$poll(CallbackInfo ci) {
        oneconfig$pollInputs();
    }
    //?} else {
    /*@Inject(method = "render", at = @At("HEAD"))
    //? if > 1.8.9 {
    private void oneconfig$poll(CallbackInfo ci) {
    //?} else
    //private void oneconfig$poll(int mouseX, int mouseY, float tickDelta, CallbackInfo ci) {
        oneconfig$pollInputs();
    }
    *///?}

    @Unique
    private void oneconfig$pollInputs() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge != null) bridge.setActiveRebind(oneconfig$target != null ? oneconfig$target : this.selectedKey);
        if (!oneconfig$recording) return;
        //? if > 1.8.9 {
        long window = Platform.compatibility().windowHandle();
        for (int k : oneconfig$keys) if (GLFW.glfwGetKey(window, k) != GLFW.GLFW_RELEASE) return;
        for (int b : oneconfig$mouse) if (GLFW.glfwGetMouseButton(window, b) != GLFW.GLFW_RELEASE) return;
        //?} else {
        /*for (int k : oneconfig$keys) if (Keyboard.isKeyDown(KeyCodes.toLegacy(k))) return;
        for (int b : oneconfig$mouse) if (Mouse.isButtonDown(b)) return;
        *///?}
        oneconfig$commit();
    }

    @Override
    public boolean oneconfig$isOurs() {
        if (oneconfig$target != null) return true;
        KeyMapping sel = this.selectedKey;
        if (sel == null) return false;
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        return bridge != null && bridge.bindFor(sel) != null;
    }

    @Unique
    private boolean oneconfig$begin() {
        if (oneconfig$target == null) {
            KeyMapping sel = this.selectedKey;
            MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
            if (sel == null || bridge == null || bridge.bindFor(sel) == null) return false;
            oneconfig$target = sel;
        }
        oneconfig$recording = true;
        return true;
    }

    @Override
    public void oneconfig$recordKey(int keyCode) {
        if (!oneconfig$begin()) return;
        oneconfig$keys.add(keyCode);
        oneconfig$preview();
    }

    @Unique
    private void oneconfig$recordMouse(int button) {
        if (!oneconfig$begin()) return;
        oneconfig$mouse.add(button);
        oneconfig$preview();
    }

    @Override
    public void oneconfig$recordEscape() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        KeyMapping target = oneconfig$target != null ? oneconfig$target : this.selectedKey;
        if (bridge != null && target != null) {
            bridge.clearPreview();
            bridge.menuUnbind(target);
        }
        oneconfig$reset();
        oneconfig$finishSelection();
    }

    @Unique
    private void oneconfig$preview() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge == null || oneconfig$target == null) return;
        bridge.setPreview(oneconfig$target, oneconfig$toArray(oneconfig$keys), oneconfig$toArray(oneconfig$mouse));
        //? if > 1.8.9
        if (this.keyBindsList != null) this.keyBindsList.resetMappingAndUpdateButtons();
    }

    @Unique
    private void oneconfig$commit() {
        KeyMapping sel = oneconfig$target;
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
        //? if > 1.8.9
        if (this.keyBindsList != null) this.keyBindsList.resetMappingAndUpdateButtons();
    }

    @Unique
    private void oneconfig$reset() {
        oneconfig$recording = false;
        oneconfig$keys.clear();
        oneconfig$mouse.clear();
        oneconfig$target = null;
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
