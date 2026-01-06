package org.polyfrost.oneconfig.internal.utils;

import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;

@MoulConfig
public interface MoulConfigGuiOptionEditorDropdownAccessor {

    String[] oneconfig$values();

    boolean oneconfig$useOrdinal();

    Enum<?>[] oneconfig$constants();

}
