package org.polyfrost.oneconfig.internal.mixin.compat.moulconfig;

import io.github.notenoughupdates.moulconfig.gui.editors.GuiOptionEditorDropdown;
import org.polyfrost.oneconfig.internal.utils.MoulConfigGuiOptionEditorDropdownAccessor;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;
import org.polyfrost.oneconfig.relocator.annotations.RelocatedMixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

@Pseudo
@MoulConfig
//#if MC==1.8.9||MC>=1.21.4
@RelocatedMixin
//#endif
@Mixin(value = GuiOptionEditorDropdown.class, remap = false)
public class Mixin_GuiOptionEditorDropdown implements MoulConfigGuiOptionEditorDropdownAccessor {
    @Shadow
    private boolean useOrdinal;
    @Shadow
    private Enum<?>[] constants;

    @Unique
    private String[] oneconfig$resolvedValues;

    // MoulConfig 3.x uses String[] for this field, 4.x uses List<StructuredText>.
    // Reflection avoids the @Shadow descriptor mismatch that breaks mixin application.
    public String[] oneconfig$values() {
        if (oneconfig$resolvedValues != null) return oneconfig$resolvedValues;
        try {
            Field f = this.getClass().getDeclaredField("values");
            f.setAccessible(true);
            Object val = f.get(this);
            if (val instanceof String[]) {
                oneconfig$resolvedValues = (String[]) val;
            } else if (val instanceof List<?>) {
                List<?> list = (List<?>) val;
                String[] result = new String[list.size()];
                for (int i = 0; i < list.size(); i++) {
                    Object item = list.get(i);
                    if (item instanceof String) {
                        result[i] = (String) item;
                    } else {
                        try {
                            Method getText = item.getClass().getMethod("getText");
                            result[i] = (String) getText.invoke(item);
                        } catch (Exception e) {
                            result[i] = item.toString();
                        }
                    }
                }
                oneconfig$resolvedValues = result;
            } else {
                oneconfig$resolvedValues = new String[0];
            }
        } catch (Exception e) {
            oneconfig$resolvedValues = new String[0];
        }
        return oneconfig$resolvedValues;
    }

    public boolean oneconfig$useOrdinal() {
        return this.useOrdinal;
    }

    public Enum<?>[] oneconfig$constants() {
        return this.constants;
    }

}
