package cc.polyfrost.oneconfig.test;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.InitializationEvent;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;

//#if MC<=11202
@net.minecraftforge.fml.common.Mod(modid = "oneconfig-test-mod", name = "Test Mod", version = "0")
//#endif
public class TestMod_Test {
    private TestConfig_Test config;
    public TestMod_Test() {
        EventManager.INSTANCE.register(this);
    }

    @Subscribe
    public void init(InitializationEvent e) {
        config = new TestConfig_Test();
    }
}