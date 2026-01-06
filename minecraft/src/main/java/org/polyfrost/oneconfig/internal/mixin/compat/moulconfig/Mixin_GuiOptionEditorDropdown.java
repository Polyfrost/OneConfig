package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown;
import org.polyfrost.oneconfig.internal.utils.MoulConfigGuiOptionEditorDropdownAccessor;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

@Pseudo
@MoulConfig
//#if MC==1.8.9||MC>=1.21.4
@RelocatedMixin
//#endif
@Mixin(value = GuiOptionEditorDropdown.class, remap = false)
public class Mixin_GuiOptionEditorDropdown implements MoulConfigGuiOptionEditorDropdownAccessor {

    @Shadow
    private String[] values;
    @Shadow
    private boolean useOrdinal;
    @Shadow
    private Enum<?>[] constants;

    public String[] oneconfig$values() {
        return this.values;
    }

    public boolean oneconfig$useOrdinal() {
        return this.useOrdinal;
    }

    public Enum<?>[] oneconfig$constants() {
        return this.constants;
    }

}
