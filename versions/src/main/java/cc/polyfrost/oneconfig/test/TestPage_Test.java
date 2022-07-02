package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.Switch;

public class TestPage_Test {

    @Switch(
            name = "Epic Test Switch"
    )
    boolean test = false;
}
