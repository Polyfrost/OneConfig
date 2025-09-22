package org.polyfrost.oneconfig.api.platform.v1.internal;

import dev.deftu.omnicore.api.loader.ModInfo;
import dev.deftu.omnicore.api.loader.OmniLoader;
import org.polyfrost.oneconfig.api.platform.v1.CompatibilityPlatform;

import java.util.HashSet;
import java.util.Set;

//#if FORGE && MC <= 1.12.2
import dev.deftu.omnicore.api.client.OmniClient;
import net.minecraftforge.client.ClientCommandHandler;
//#endif

public class CompatibilityPlatformImpl implements CompatibilityPlatform {

    public Set<ModInfo> getValidTrees() {
        //#if MC >= 1.16.5 || FABRIC
        //$$ // Unneeded in 1.16.5+ / Legacy Fabric
        //$$ return new HashSet<>();
        //#else
        // Returns a set of mod trees which can be displayed as items in the config menu
        Set<ModInfo> result = new HashSet<>();
        Set<ModInfo> mods = OmniLoader.getMods();
        Set<String> commands = ClientCommandHandler.instance.getCommands().keySet();

        for (ModInfo mod : mods) {
            if (!commands.contains(mod.getId())) {
                continue;
            }

            result.add(mod);
        }

        return result;
        //#endif
    }

    public void executeTreeAction(String action) {
        //#if MC >= 1.16.5 || FABRIC
        //$$ // Unneeded in 1.16.5+ / Legacy Fabric
        //#else
        // Executes the command for the given mod
        ClientCommandHandler.instance.executeCommand(OmniClient.getPlayer(), action);
        //#endif
    }

}
