package org.polyfrost.oneconfig.internal.utils;

//? moul_compat {
import io.github.notenoughupdates.moulconfig.Config;
import org.polyfrost.oneconfig.relocator.annotations.MoulConfig;

@MoulConfig
public interface MoulConfigProcessorAccessor<T extends Config> {

    T oneconfig$getConfig();

}
//? }