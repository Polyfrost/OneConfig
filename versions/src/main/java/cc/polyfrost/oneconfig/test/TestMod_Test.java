package cc.polyfrost.oneconfig.test;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@net.minecraftforge.fml.common.Mod(modid = "oneconfig-test-mod", name = "Test Mod", version = "0")
public class TestMod_Test {
    private TestConfig_Test config;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        config = new TestConfig_Test();
    }
}