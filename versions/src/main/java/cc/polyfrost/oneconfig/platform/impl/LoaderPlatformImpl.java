package cc.polyfrost.oneconfig.platform.impl;

import cc.polyfrost.oneconfig.platform.LoaderPlatform;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;

@SuppressWarnings("unused")
public class LoaderPlatformImpl implements LoaderPlatform {
    @Override
    public boolean isModLoaded(String id) {
        return Loader.isModLoaded(id);
    }

    @Override
    public boolean hasActiveModContainer() {
        return Loader.instance().activeModContainer() != null;
    }

    @Override
    public ActiveMod getActiveModContainer() {
        ModContainer container = Loader.instance().activeModContainer();
        if (container == null)
            return null;
        return new ActiveMod(container.getName(), container.getModId(), container.getVersion());
    }
}
