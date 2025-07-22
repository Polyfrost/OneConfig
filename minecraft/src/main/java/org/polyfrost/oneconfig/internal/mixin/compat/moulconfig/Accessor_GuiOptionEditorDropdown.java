package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;

@Pseudo
@MoulConfig
//#if MC==1.8.9||MC>=1.21.4
@RelocatedMixin
//#endif
@Mixin(value = GuiOptionEditorDropdown.class, remap = false)
public interface Accessor_GuiOptionEditorDropdown {

    @Accessor("values")
    public String[] oneconfig$values();

    @Accessor("useOrdinal")
    public boolean oneconfig$useOrdinal();

    @Accessor("constants")
    public Enum<?>[] oneconfig$constants();

}
