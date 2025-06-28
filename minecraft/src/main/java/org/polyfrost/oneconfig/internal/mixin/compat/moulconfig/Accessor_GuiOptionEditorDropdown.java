package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(value = GuiOptionEditorDropdown.class, remap = false)
public interface Accessor_GuiOptionEditorDropdown {

    @Accessor("values")
    public String[] oneconfig$values();
    @Accessor("useOrdinal")
    public boolean oneconfig$useOrdinal();
    @Accessor("constants")
    public Enum<?>[] oneconfig$constants();

}
