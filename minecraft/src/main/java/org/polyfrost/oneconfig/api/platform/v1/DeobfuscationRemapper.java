package org.polyfrost.oneconfig.api.platform.v1;

//#if FORGE-LIKE
//#if MC <= 1.12.2
//$ import net.minecraftforge.fml.common.asm.transformers.deobf.FMLDeobfuscatingRemapper;
//#else
//#if MC >= 1.17.1
//#if NEOFORGE
//$ import net.neoforged.fml.util.ObfuscationReflectionHelper;
//#else
//$ import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
//#endif
//#else
//$ import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
//#endif
//#if MC < 1.20.6
//$ import cpw.mods.modlauncher.api.INameMappingService;
//#endif
//#endif
//#else
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.MappingResolver;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
//#endif

import org.objectweb.asm.commons.Remapper;

public class DeobfuscationRemapper extends Remapper {

    public static final DeobfuscationRemapper INSTANCE = new DeobfuscationRemapper();

    //#if FABRIC
    private static final MappingResolver RESOLVER = FabricLoader.getInstance().getMappingResolver();
    private static final String INTERMEDIARY = "intermediary";
    private static final Pattern CLASS_PATTERN = Pattern.compile("Lnet\\/minecraft\\/([^;]+);");
    //#endif

    public String map(String typeName) {
        //#if FORGE-LIKE
        //#if MC <= 1.12.2
        //$ return FMLDeobfuscatingRemapper.INSTANCE.map(typeName);
        //#else
        //#if MC >= 1.20.6
        //$ throw new UnsupportedOperationException("Remapping of class names is not yet supported in 1.20.6+"); // TODO
        //#else
        //$ return ObfuscationReflectionHelper.remapName(INameMappingService.Domain.CLASS, typeName);
        //#endif
        //#endif
        //#else
        return RESOLVER.mapClassName(INTERMEDIARY, typeName);
        //#endif
    }

    @Override
    public String mapMethodName(String owner, String name, String desc) {
        //#if FORGE-LIKE
        //#if MC <= 1.12.2
        //$ return FMLDeobfuscatingRemapper.INSTANCE.mapMethodName(owner, name, desc);
        //#else
        //#if MC >= 1.20.6
        //$ throw new UnsupportedOperationException("Remapping of class names is not yet supported in 1.20.6+"); // TODO
        //#else
        //$ return ObfuscationReflectionHelper.remapName(INameMappingService.Domain.METHOD, name);
        //#endif
        //#endif
        //#else
        return RESOLVER.mapMethodName(INTERMEDIARY, owner, name, desc);
        //#endif
    }

    @Override
    public String mapFieldName(String owner, String name, String desc) {
        //#if FORGE-LIKE
        //#if MC <= 1.12.2
        //$ return FMLDeobfuscatingRemapper.INSTANCE.mapFieldName(owner, name, desc);
        //#else
        //#if MC >= 1.20.6
        //$ throw new UnsupportedOperationException("Remapping of class names is not yet supported in 1.20.6+"); // TODO
        //#else
        //$ return ObfuscationReflectionHelper.remapName(INameMappingService.Domain.FIELD, name);
        //#endif
        //#endif
        //#else
        return RESOLVER.mapFieldName(INTERMEDIARY, owner, name, desc);
        //#endif
    }

    @Override
    public String mapSignature(String signature, boolean typeSignature) {
        //#if FORGE-LIKE
        //#if MC <= 1.12.2
        //$ return FMLDeobfuscatingRemapper.INSTANCE.mapSignature(signature, typeSignature);
        //#else
        //$ return super.mapSignature(signature, typeSignature);
        //#endif
        //#else
        StringBuffer buffer = new StringBuffer();

        Matcher matcher = CLASS_PATTERN.matcher(signature);
        while (matcher.find()) {
            matcher.appendReplacement(buffer, "L" + RESOLVER.mapClassName(INTERMEDIARY, matcher.group(1)) + ";");
        }

        return matcher.appendTail(buffer).toString();
        //#endif
    }

}
