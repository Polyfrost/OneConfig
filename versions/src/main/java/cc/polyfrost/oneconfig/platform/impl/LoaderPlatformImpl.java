package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.LoaderPlatform;
//#if MC>=11600
    //#if FORGE==1
    //$$ import net.minecraftforge.fml.ModList;
    //#else
    //$$ import net.fabricmc.loader.api.FabricLoader;
    //#endif
//#endif
//#if FORGE==1
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
//#endif

public class LoaderPlatformImpl implements LoaderPlatform {
    @Override
    public boolean isModLoaded(String id) {
        //#if MC>=11600
            //#if FORGE==1
            //$$ return ModList.get().isLoaded(id);
            //#else
            //$$ return FabricLoader.getInstance().isModLoaded(id);
            //#endif
        //#else
        return Loader.isModLoaded(id);
        //#endif
    }

    @Override
    public boolean hasActiveModContainer() {
        //#if FORGE==1
        return Loader.instance().activeModContainer() != null;
        //#else
        //$$ return false;
        //#endif
    }

    @Override
    public ActiveMod getActiveModContainer() {
        //#if FORGE==1
        ModContainer container = Loader.instance().activeModContainer();
        if (container == null) return null;
            //#if MC==11202
            return new ActiveMod(container.getName(), container.getModId(), container.getVersion());
            //#else
            //$$ return new ActiveMod(container.getModInfo().getDisplayName(), container.getModId(), container.getModInfo().getVersion().getQualifier());
            //#endif
        //#else
        //$$ return null;
        //#endif
    }
}
