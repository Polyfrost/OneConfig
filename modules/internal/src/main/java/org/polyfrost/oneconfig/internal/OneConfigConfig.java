package org.polyfrost.oneconfig.internal;

import org.polyfrost.oneconfig.api.config.v1.Config;
import org.polyfrost.oneconfig.api.config.v1.annotations.Switch;

public class OneConfigConfig extends Config {
    @Switch(title = "Test")
    public static boolean test = false;

    public OneConfigConfig() {
        super("oneconfig.json", "assets/oneconfig/brand/oneconfig.svg", "OneConfig", Category.QOL);
    }
}
