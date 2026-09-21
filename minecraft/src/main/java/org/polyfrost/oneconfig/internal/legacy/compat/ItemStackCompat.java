package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import net.minecraft.item.ItemStack;

public interface ItemStackCompat {
    default boolean isEmpty() {
        ItemStack stack = (ItemStack) (Object) this;
        return stack.getItem() == null || stack.count <= 0;
    }

    default boolean isBarVisible() {
        return ((ItemStack) (Object) this).isDamaged();
    }

    default int getBarWidth() {
        ItemStack stack = (ItemStack) (Object) this;
        return (int) Math.round(13.0 - stack.getDamage() * 13.0 / stack.getMaxDamage());
    }

    default int getBarColor() {
        ItemStack stack = (ItemStack) (Object) this;
        int health = (int) Math.round(255.0 - stack.getDamage() * 255.0 / stack.getMaxDamage());
        return (255 - health) << 16 | health << 8;
    }
}
*///?}
