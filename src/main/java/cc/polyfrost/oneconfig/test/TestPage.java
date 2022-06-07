package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.config.annotations.Switch;

public class TestPage {

    @Switch(
            name = "Epic Test Switch"
    )
    boolean test = false;
}
