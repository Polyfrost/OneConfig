package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.LoaderPlatform;
//#if MC>=11600
//$$ import net.minecraftforge.fml.ModList;
//#endif
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

public class LoaderPlatformImpl implements LoaderPlatform {
    @Override
    public boolean isModLoaded(String id) {
        //#if MC>=11600
        //$$ return ModList.get().isLoaded(id);
        //#else
        return Loader.isModLoaded(id);
        //#endif
    }

    @Override
    public boolean hasActiveModContainer() {
        return Loader.instance().activeModContainer() != null;
    }

    @Override
    public ActiveMod getActiveModContainer() {
        ModContainer container = Loader.instance().activeModContainer();
        if (container == null) return null;
        //#if MC==11202
        return new ActiveMod(container.getName(), container.getModId(), container.getVersion());
        //#else
        //$$ return new ActiveMod(container.getModInfo().getDisplayName(), container.getModId(), container.getModInfo().getVersion().getQualifier());
        //#endif
    }
}
