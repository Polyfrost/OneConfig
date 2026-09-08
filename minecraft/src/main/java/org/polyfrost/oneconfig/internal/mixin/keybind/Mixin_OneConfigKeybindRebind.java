package org.polyfrost.oneconfig.internal.mixin.keybind;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
//? if >= 1.21.10
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
//? if >=1.21.10 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}
//? if < 26.3
import org.lwjgl.glfw.GLFW;
//? if >= 26.3
//import org.lwjgl.sdl.SDLMouse;
import org.polyfrost.oneconfig.api.platform.v1.Platform;
import org.polyfrost.oneconfig.api.ui.v1.keybind.internal.MinecraftKeybindBridgeImpl;
import org.polyfrost.oneconfig.internal.ui.keybind.OneConfigKeybindRecorder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;

@Mixin(KeyBindsScreen.class)
public class Mixin_OneConfigKeybindRebind implements OneConfigKeybindRecorder {
    @Shadow public KeyMapping selectedKey;
    @Shadow private KeyBindsList keyBindsList;

    @Unique private final LinkedHashSet<Integer> oneconfig$keys = new LinkedHashSet<>();
    @Unique private final LinkedHashSet<Integer> oneconfig$mouse = new LinkedHashSet<>();
    @Unique private boolean oneconfig$recording = false;
    @Unique private KeyMapping oneconfig$target = null;

    //? if >=1.21.10 {
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void oneconfig$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        if (event.key() == InputConstants.KEY_ESCAPE) {
            oneconfig$recordEscape();
            cir.setReturnValue(true);
            return;
        }
        oneconfig$recordKey(event.key());
        cir.setReturnValue(true);
    }
    //?} else {
    /*@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void oneconfig$keyPressed(int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        if (keyCode == InputConstants.KEY_ESCAPE) {
            oneconfig$recordEscape();
            cir.setReturnValue(true);
            return;
        }
        oneconfig$recordKey(keyCode);
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
    private void oneconfig$mouseClicked(double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> cir) {
        if (!oneconfig$isOurs()) return;
        oneconfig$recordMouse(button);
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
    private void oneconfig$poll(CallbackInfo ci) {
        oneconfig$pollInputs();
    }
    *///?}

    @Unique
    private void oneconfig$pollInputs() {
        MinecraftKeybindBridgeImpl bridge = MinecraftKeybindBridgeImpl.instance();
        if (bridge != null) bridge.setActiveRebind(oneconfig$target != null ? oneconfig$target : this.selectedKey);
        if (!oneconfig$recording) return;
        for (int k : oneconfig$keys) {
            //? if sdl_keycodes {
            /*if (InputConstants.isKeyDown(k)) return;
            *///?} else {
            //~ if < 1.21.10 'Minecraft.getInstance().getWindow()' -> 'Platform.compatibility().windowHandle()'
            if (InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), k)) return;
            //?}
        }
        //? if >= 26.3 {
        /*int buttons = SDLMouse.SDL_GetMouseState(null, null);
        for (int b : oneconfig$mouse) {
            if (b > 0 && b <= Integer.SIZE && (buttons & (1 << (b - 1))) != 0) return;
        }
        *///?} else {
        long window = Platform.compatibility().windowHandle();
        for (int b : oneconfig$mouse) if (GLFW.glfwGetMouseButton(window, b) != InputConstants.RELEASE) return;
        //?}
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
        //? if sdl_keycodes
        //if (keyCode <= 0) return;
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
