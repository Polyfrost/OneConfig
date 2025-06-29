package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@Mixin(targets = {
        "io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown",
        "at.hannibal2.skyhanni.deps.moulconfig.gui.editors.GuiOptionEditorDropdown"
}, remap = false)
public interface Accessor_GuiOptionEditorDropdown {

    @Accessor("values")
    public String[] oneconfig$values();

    @Accessor("useOrdinal")
    public boolean oneconfig$useOrdinal();

    @Accessor("constants")
    public Enum<?>[] oneconfig$constants();

}
