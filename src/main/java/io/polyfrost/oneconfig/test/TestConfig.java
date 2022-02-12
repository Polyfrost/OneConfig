package io.polyfrost.oneconfig.test;

import io.polyfrost.oneconfig.annotations.Category;
import io.polyfrost.oneconfig.annotations.Switch;
import io.polyfrost.oneconfig.annotations.TextField;
import io.polyfrost.oneconfig.interfaces.Config;

import java.io.File;

public class TestConfig extends Config {

    @Switch(name = "Cool Switch")
    public static boolean toggle = false;

    @Category(name = "Cool Category")
    public static class category {
        @TextField(name = "Cool text field")
        public static String text = "e";
    }

    public TestConfig() {
        super(new File("./config/testConfig.json"));
    }
}
