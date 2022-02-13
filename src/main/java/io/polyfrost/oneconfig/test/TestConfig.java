package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.config.annotations.Category;
import io.polyfrost.oneconfig.config.annotations.Switch;
import io.polyfrost.oneconfig.config.annotations.TextField;
import io.polyfrost.oneconfig.config.data.ModData;
import io.polyfrost.oneconfig.config.data.ModType;
import io.polyfrost.oneconfig.config.interfaces.Config;

import java.io.File;

public class TestConfig extends Config {

    @Switch(name = "Cool Switch")
    public static boolean toggle = false;

    @Category(name = "Cool Category")
    public static class category {
        @TextField(name = "Cool text field")
        public static String text = "Very cool text";
    }

    public TestConfig() {
        super(new ModData("hacks", ModType.QOL, "ShadyDev", "1.0"), new File("./config/hacksConfig.json"));
    }
}
