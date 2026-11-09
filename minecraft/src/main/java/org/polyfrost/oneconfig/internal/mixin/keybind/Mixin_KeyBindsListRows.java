package org.polyfrost.oneconfig.internal.mixin.keybind;

//? if = 1.8.9 {
/*import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import java.util.Arrays;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyBindsList.class)
public class Mixin_KeyBindsListRows {
    @Shadow
    @Final
    @Mutable
    private EntryListWidget.Entry[] entries;

    @ModifyExpressionValue(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/util/Set;size()I"))
    private int oneconfig$headerBound(int categories) {
        return Minecraft.getInstance().options.keyMappings.length;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void oneconfig$trimRows(CallbackInfo ci) {
        entries = Arrays.stream(entries).filter(Objects::nonNull).toArray(EntryListWidget.Entry[]::new);
    }
}
*///?}
