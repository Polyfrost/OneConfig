package org.polyfrost.oneconfig.internal.utils;

import io.github.notenoughupdates.moulconfig.Config;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;

@MoulConfig
public interface MoulConfigProcessorAccessor<T extends Config> {

    T oneconfig$getConfig();

}
