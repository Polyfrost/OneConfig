package org.polyfrost.oneconfig.internal.legacy.compat;

//? if = 1.8.9 {
/*import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.polyfrost.oneconfig.internal.legacy.KeyCodes;

public interface KeyBindingCompat {
    default String saveString() {
        return KeyCodes.fromLegacy(((KeyMapping) (Object) this).getKeyCode()).getName();
    }

    default InputConstants.Key getDefaultKey() {
        return KeyCodes.fromLegacy(((KeyMapping) (Object) this).getDefaultKeyCode());
    }

    default void setKey(InputConstants.Key key) {
        ((KeyMapping) (Object) this).setKeyCode(KeyCodes.toLegacy(key));
    }

}
*///?}
