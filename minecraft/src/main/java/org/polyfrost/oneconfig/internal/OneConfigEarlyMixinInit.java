package org.polyfrost.oneconfig.internal;

import com.bawnorton.mixinsquared.MixinSquaredBootstrap;
import com.bawnorton.mixinsquared.adjuster.MixinAnnotationAdjusterRegistrar;
import com.bawnorton.mixinsquared.api.MixinAnnotationAdjuster;
import com.bawnorton.mixinsquared.api.MixinCanceller;
import com.bawnorton.mixinsquared.canceller.MixinCancellerRegistrar;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public class OneConfigEarlyMixinInit implements IMixinConfigPlugin {
    //? neoforge {
    /*private static final ServiceLoader<MixinCanceller> CANCELLERS = ServiceLoader.load(MixinCanceller.class);
    private static final ServiceLoader<MixinAnnotationAdjuster> ANNOTATION_ADJUSTERS = ServiceLoader.load(MixinAnnotationAdjuster.class);
    *///? }


    public static void load() {
        //? neoforge {
        /*CANCELLERS.forEach(MixinCancellerRegistrar::register);
        ANNOTATION_ADJUSTERS.forEach(MixinAnnotationAdjusterRegistrar::register);
        *///? } else {
        net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypointContainers("mixinsquared", MixinCanceller.class).forEach(container -> {
            String id = container.getProvider().getMetadata().getId();
            try {
                MixinCanceller canceller = container.getEntrypoint();
                MixinCancellerRegistrar.register(canceller);
            } catch (Throwable e) {
                System.err.printf("Mod %s provides a broken MixinCanceller implementation:\n", id);
                e.printStackTrace(System.err);
            }
        });
        net.fabricmc.loader.api.FabricLoader.getInstance().getEntrypointContainers("mixinsquared-adjuster", MixinAnnotationAdjuster.class).forEach(container -> {
            String id = container.getProvider().getMetadata().getId();
            try {
                MixinAnnotationAdjuster annotationAdjuster = container.getEntrypoint();
                MixinAnnotationAdjusterRegistrar.register(annotationAdjuster);
            } catch (Throwable e) {
                System.err.printf("Mod %s provides a broken MixinAnnotationAdjuster implementation:\n", id);
                e.printStackTrace(System.err);
            }
        });
        //? }
    }

    @Override
    public void onLoad(String mixinPackage) {
        MixinSquaredBootstrap.init();
        load();
        MixinExtrasBootstrap.init();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        MixinSquaredBootstrap.reOrderExtensions();
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
