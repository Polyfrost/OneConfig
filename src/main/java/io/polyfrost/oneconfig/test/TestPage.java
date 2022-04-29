package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.config.annotations.Option;
import io.polyfrost.oneconfig.config.data.OptionType;

public class TestPage {
    @Option(
            name = "Text field 1x",
            subcategory = "Test",
            type = OptionType.TEXT
    )
    public static String testDescription;

    @Option(
            name = "Text field 1x",
            subcategory = "Test",
            type = OptionType.TEXT
    )
    public static String testDescription2;

    @Option(
            name = "Text field 2x",
            subcategory = "Test",
            type = OptionType.TEXT,
            size = 2
    )
    public static String testDescription3;

    @Option(
            name = "Secure text field",
            subcategory = "Test",
            type = OptionType.TEXT,
            secure = true
    )
    public static String testDescription4;

    @Option(
            name = "Text box",
            subcategory = "Test",
            type = OptionType.TEXT,
            multiLine = true
    )
    public static String testDescription5;
}
