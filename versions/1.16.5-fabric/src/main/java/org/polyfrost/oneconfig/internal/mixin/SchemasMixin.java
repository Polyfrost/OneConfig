package org.polyfrost.oneconfig.internal.mixin;

import com.mojang.datafixers.DataFixer;
import com.mojang.datafixers.DataFixerBuilder;
import net.minecraft.datafixer.Schemas;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.Executor;

//#if MC<=11904
/**
 * Modified from LazyDFU under the MIT licence.
 * Source: <a href="https://github.com/astei/lazydfu/blob/master/LICENSE">here</a>
 */
@Mixin(Schemas.class)
public abstract class SchemasMixin {

    @Redirect(method = "create", at = @At(value = "NEW", target = "com/mojang/datafixers/DataFixerBuilder"))
    private static DataFixerBuilder create$replaceBuilder(int dataVersion) {
        return new DataFixerBuilder(dataVersion) {
            @Override
            public DataFixer build(Executor executor) {
                return super.build(f -> {});
            }
        };
    }
}
//#endif