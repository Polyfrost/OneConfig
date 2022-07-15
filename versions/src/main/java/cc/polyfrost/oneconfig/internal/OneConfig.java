package cc.polyfrost.oneconfig.internal;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.ShutdownEvent;
import cc.polyfrost.oneconfig.internal.command.OneConfigCommand;
import cc.polyfrost.oneconfig.internal.config.OneConfigConfig;
import cc.polyfrost.oneconfig.internal.config.Preferences;
import cc.polyfrost.oneconfig.internal.config.core.ConfigCore;
import cc.polyfrost.oneconfig.internal.config.core.KeyBindHandler;
import cc.polyfrost.oneconfig.internal.gui.BlurHandler;
import cc.polyfrost.oneconfig.internal.hud.HudCore;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.utils.commands.CommandManager;
import cc.polyfrost.oneconfig.utils.gui.GuiUtils;
import cc.polyfrost.oneconfig.utils.hypixel.HypixelUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

/**
 * The main class of OneConfig.
 */
//#if MC<=11202
@net.minecraftforge.fml.common.Mod(modid = "@ID@", name = "@NAME@", version = "@VER@")
//#else
//#if FORGE==1
//$$ @net.minecraftforge.fml.common.Mod("@ID@")
//#endif
//#endif
public class OneConfig {

    public OneConfig() {
        EventManager.INSTANCE.register(this);
    }

    public static final Logger LOGGER = LogManager.getLogger("@NAME@");
    private static boolean initialized = false;

    /**
     * Called after mods are loaded.
     * <p><b>SHOULD NOT BE CALLED!</b></p>
     */
    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static void init() {
        if (initialized) return;
        if (OneConfigConfig.getInstance() == null) {
            OneConfigConfig.getInstance();
        }
        if (Preferences.getInstance() == null) {
            Preferences.getInstance();
        }
        GuiUtils.getDeltaTime(); // called to make sure static initializer is called
        try {
            EventManager.INSTANCE.register(BlurHandler.INSTANCE);
        } catch (Exception e) {
            e.printStackTrace();
        }
        CommandManager.INSTANCE.registerCommand(OneConfigCommand.class);
        EventManager.INSTANCE.register(new HudCore());
        HypixelUtils.INSTANCE.initialize();
        EventManager.INSTANCE.register(KeyBindHandler.INSTANCE);
        ConfigCore.sortMods();

        initialized = true;
    }

    @Subscribe
    private void onShutdown(ShutdownEvent event) {
        ConfigCore.saveAll();
    }
}
