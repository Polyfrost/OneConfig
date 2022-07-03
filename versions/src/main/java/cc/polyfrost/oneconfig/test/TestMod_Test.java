package cc.polyfrost.oneconfig.test;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

//#if MC<=11202
@net.minecraftforge.fml.common.Mod(modid = "oneconfig-test-mod", name = "Test Mod", version = "0")
//#else
//$$ @Mod("oneconfig-test-mod")
//#endif
public class TestMod_Test {
    private TestConfig_Test config;

    //#if MC<=11202
    @Mod.EventHandler
    //#endif
    public void init(FMLInitializationEvent event) {
        config = new TestConfig_Test();
    }
}