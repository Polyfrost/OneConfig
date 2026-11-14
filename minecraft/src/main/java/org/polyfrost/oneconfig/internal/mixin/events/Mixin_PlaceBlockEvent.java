package org.polyfrost.oneconfig.internal.mixin.events;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import org.polyfrost.oneconfig.api.event.v1.EventManager;
import org.polyfrost.oneconfig.api.event.v1.events.PlaceBlockEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//? if > 1.8.9 {
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
//?} else {
/*import net.minecraft.core.Direction;
import net.minecraft.entity.living.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.item.ItemStack;
*///?}

@Mixin(BlockItem.class)
public class Mixin_PlaceBlockEvent {

    //? if > 1.8.9 {
    @Inject(method = "placeBlock", at = @At("RETURN"))
    private void ocfg$onPlaceBlock(BlockPlaceContext context, BlockState placementState, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        Player player = context.getPlayer();
        if (player == null || player != Minecraft.getInstance().player) return;
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        EventManager.INSTANCE.post(new PlaceBlockEvent(player, level, pos, level.getBlockState(pos)));
    }
    //?} else {
    /*@Inject(
            method = "useOn",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/World;playSound(DDDLjava/lang/String;FF)V",
                    shift = At.Shift.AFTER
            )
    )
    private void ocfg$onPlaceBlock(ItemStack stack, PlayerEntity player, World world, BlockPos pos, Direction direction, float hitX, float hitY, float hitZ, CallbackInfoReturnable<Boolean> cir) {
        if (!world.isClient || player != Minecraft.getInstance().player) return;
        EventManager.INSTANCE.post(new PlaceBlockEvent(player, world, pos, world.getBlockState(pos)));
    }
    *///?}

}
