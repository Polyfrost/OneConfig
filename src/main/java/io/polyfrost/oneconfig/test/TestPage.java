package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.config.annotations.Option;
import io.polyfrost.oneconfig.config.data.OptionType;

public class TestPage {
    @Option(
            name = "Other test switch",
            description = "Best description",
            subcategory = "Test",
            type = OptionType.SWITCH
    )
    public static boolean switchTest;
}
