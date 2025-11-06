package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.util.MovingObjectPosition;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class Mixin_PlayerInteractEvent_RightAction {
    @Shadow public MovingObjectPosition objectMouseOver;
    @Shadow public EntityPlayerSP thePlayer;

    @Inject(
            method = "rightClickMouse",
            at = @At(
                    value = "INVOKE",
                    //#if MC >= 1.16.5
                    //#if FABRIC
                    //$$ target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"
                    //#else
                    //#if MC >= 1.19.4
                    //$$ target = "Lnet/minecraftforge/client/ForgeHooksClient;onClickInput(ILnet/minecraft/client/KeyMapping;Lnet/minecraft/world/InteractionHand;)Lnet/minecraftforge/client/event/InputEvent$InteractionKeyMappingTriggered;",
                    //#elseif MC >= 1.18.2
                    //$$ target = "Lnet/minecraftforge/client/ForgeHooksClient;onClickInput(ILnet/minecraft/client/KeyMapping;Lnet/minecraft/world/InteractionHand;)Lnet/minecraftforge/client/event/InputEvent$ClickInputEvent;",
                    //#elseif MC >= 1.17.1
                    //$$ target = "Lnet/minecraftforge/client/ForgeHooksClient;onClickInput(ILnet/minecraft/client/KeyMapping;Lnet/minecraft/world/InteractionHand;)Lnet/minecraftforge/client/event/InputEvent$ClickInputEvent;",
                    //#else
                    //$$ target = "Lnet/minecraftforge/client/ForgeHooksClient;onClickInput(ILnet/minecraft/client/settings/KeyBinding;Lnet/minecraft/util/Hand;)Lnet/minecraftforge/client/event/InputEvent$ClickInputEvent;",
                    //#endif
                    //$$ remap = false
                    //#endif
                    //#elseif MC >= 1.12.2
                    //$$ target = "Lnet/minecraft/client/entity/EntityPlayerSP;getHeldItem(Lnet/minecraft/util/EnumHand;)Lnet/minecraft/item/ItemStack;"
                    //#else
                    target = "Lnet/minecraft/entity/player/InventoryPlayer;getCurrentItem()Lnet/minecraft/item/ItemStack;"
                    //#endif
            ),
            cancellable = true
    )
    private void onPlayerInteractCallback(CallbackInfo ci) {
        MovingObjectPosition rayCastedObject = this.objectMouseOver;
        PlayerInteractEvent.Type type = PlayerInteractEvent.Type.AIR;
        if (rayCastedObject != null) {
            MovingObjectPosition.MovingObjectType typeOfHit =
                    //#if MC >= 1.16.5
                    //$$ rayCastedObject.getType();
                    //#else
                    rayCastedObject.typeOfHit;
                    //#endif
            switch (typeOfHit) {
                case BLOCK:
                    type = PlayerInteractEvent.Type.BLOCK;
                    break;
                case ENTITY:
                    type = PlayerInteractEvent.Type.ENTITY;
                    break;
            }
        }

        PlayerInteractEvent event = new PlayerInteractEvent(this.thePlayer, PlayerInteractEvent.Action.RIGHT, type);
        EventManager.INSTANCE.post(event);
        if (event.cancelled) {
            ci.cancel();
        }
    }
}
