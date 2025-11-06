package org.polyfrost.oneconfig.internal.mixin.events;

//#if FABRIC
//$$ import net.minecraft.client.Minecraft;
//$$ import org.polyfrost.oneconfig.api.event.v1.EventManager;
//$$ import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
//$$ import org.spongepowered.asm.mixin.Mixin;
//$$ import org.spongepowered.asm.mixin.injection.At;
//$$ import org.spongepowered.asm.mixin.injection.Inject;
//$$ import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
//$$
//#if MC >= 1.16.5
//$$ import net.minecraft.client.gui.screens.Screen;
//#else
//$$ import net.minecraft.client.gui.GuiScreen;
//#endif
//$$
//$$ @Mixin(Minecraft.class)
//$$ public class Mixin_ScreenOpenEvent_Fabric {
//$$     @Inject(
                //#if MC >= 1.16.5
                //$$ method = "setScreen",
                //#else
                //$$ method = "displayGuiScreen",
                //#endif
//$$             at = @At(
//$$                     value = "INVOKE",
//$$                     shift = At.Shift.BY,
                    //#if MC >= 1.16.5
                    //$$ target = "Lnet/minecraft/client/player/LocalPlayer;respawn()V",
                    //$$ by = 2
                    //#elseif MC >= 1.12.2
                    //$$ target = "Lnet/minecraft/client/gui/GuiGameOver;<init>(Lnet/minecraft/util/text/ITextComponent;)V",
                    //$$ by = 3
                    //#else
                    //$$ target = "Lnet/minecraft/client/gui/GuiGameOver;<init>()V",
                    //$$ by = 3
                    //#endif
//$$             ),
//$$             cancellable = true
//$$     )
//$$     private void screenOpenCallback(
            //#if MC >= 1.16.5
            //$$ Screen screen,
            //#else
            //$$ GuiScreen screen,
            //#endif
//$$         CallbackInfo ci
//$$     ) {
//$$         ScreenOpenEvent event = new ScreenOpenEvent(screen);
//$$         EventManager.INSTANCE.post(event);
//$$         if (event.cancelled) {
//$$             ci.cancel();
//$$         }
//$$     }
//$$ }
//#endif
