package org.polyfrost.oneconfig.internal.mixin.events;

//#if FORGE-LIKE
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.ScreenOpenEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

//#if FORGE
import net.minecraftforge.fml.common.eventhandler.Event;
//#else
//$$ import net.neoforged.bus.api.Event;
//#endif

//#if MC >= 1.20.1
//#if FORGE
//$$ import net.minecraftforge.client.event.ScreenEvent;
//#else
//$$ import net.neoforged.neoforge.client.event.ScreenEvent;
//#endif
//#else
import net.minecraftforge.client.event.GuiOpenEvent;
//#endif

@Mixin(Minecraft.class)
public class Mixin_ScreenOpenEvent_Forge {
    //#if MC >= 1.16.5
    //$$ @ModifyArg(
    //$$     method = "setScreen",
    //$$     at = @At(
    //$$         value = "INVOKE",
                ////#if MC >= 1.20.4
                ////#if FORGE
                ////$$ target = "Lnet/minecraftforge/client/event/ForgeEventFactoryClient;onScreenOpening(Lnet/minecraft/client/gui/screens/Screen;Lnet/minecraft/client/gui/screens/Screen;)Lnet/minecraft/client/gui/screens/Screen;",
                ////#else
                ////$$ target = "Lnet/neoforged/neoforge/client/ClientHooks;clearGuiLayers(Lnet/minecraft/client/Minecraft;)V",
                ////#endif
                ////#else
                //$$ target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z",
                ////#endif
    //$$         remap = false
    //$$     )
    //$$ )
    ////#if MC >= 1.20.4
    ////$$ private Screen screenOpenCallback(Screen oldScreen, Screen newScreen) {
    ////$$     ScreenOpenEvent ourEvent = new ScreenOpenEvent(newScreen);
    ////$$     EventManager.INSTANCE.post(ourEvent);
    ////$$     if (ourEvent.cancelled) {
    ////$$         return oldScreen;
    ////$$     }
    ////$$
    ////$$     return ourEvent.getScreen();
    ////$$ }
    ////#else
    //$$ private Event screenOpenCallback(Event event) {
            //#if MC >= 1.19.2
            //$$ if (!(event instanceof ScreenEvent.Opening)) {
            //$$     return event;
            //$$ }
            //$$
            //$$ Screen screen = ((ScreenEvent.Opening) event).getScreen();
            //#else
            //$$ if (!(event instanceof GuiOpenEvent)) {
            //$$     return event;
            //$$ }
            //$$
            //$$ Screen screen = ((GuiOpenEvent) event).getGui();
            //#endif
    //$$
    //$$     ScreenOpenEvent ourEvent = new ScreenOpenEvent(screen);
    //$$     EventManager.INSTANCE.post(ourEvent);
    //$$     if (ourEvent.cancelled) {
    //#if FORGE
    //$$         event.setCanceled(true);
    //#else
    //$$ ((ScreenEvent.Opening) event).setCanceled(true);
    //#endif
    //$$     }
    //$$
    //$$     return event;
    //$$ }
    ////#endif
    //#else
    @ModifyArg(method = "displayGuiScreen", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/common/eventhandler/EventBus;post(Lnet/minecraftforge/fml/common/eventhandler/Event;)Z", remap = false))
    private Event screenOpenCallback(Event event) {
        if (!(event instanceof GuiOpenEvent)) {
            return event;
        }

        GuiOpenEvent forgeEvent = (GuiOpenEvent) event;
        //#if MC >= 1.12.2
        //$$ GuiScreen screen = forgeEvent.getGui();
        //#else
        GuiScreen screen = forgeEvent.gui;
        //#endif
        ScreenOpenEvent ourEvent = new ScreenOpenEvent(screen);
        EventManager.INSTANCE.post(ourEvent);
        if (ourEvent.cancelled) {
            forgeEvent.setCanceled(true);
        }

        return forgeEvent;
    }
    //#endif
}
//#endif
