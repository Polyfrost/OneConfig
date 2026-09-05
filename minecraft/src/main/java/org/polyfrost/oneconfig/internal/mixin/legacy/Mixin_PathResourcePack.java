package org.polyfrost.oneconfig.internal.mixin.legacy;

//? if = 1.8.9 {
/*import java.io.IOException;
import java.io.InputStream;
import java.nio.file.OpenOption;
import java.nio.file.Path;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.ornithemc.osl.resource.loader.impl.resource.pack.PathResourcePack;
import org.polyfrost.oneconfig.internal.legacy.ornithe.DeferredResource;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PathResourcePack.class, remap = false)
public abstract class Mixin_PathResourcePack {
	@WrapOperation(
		method = "getResource(Ljava/lang/String;)Ljava/io/InputStream;",
		at = @At(
			value = "INVOKE",
			target = "Ljava/nio/file/Files;newInputStream(Ljava/nio/file/Path;[Ljava/nio/file/OpenOption;)Ljava/io/InputStream;"
		)
	)
	private InputStream oneconfig$deferInputStream(Path path, OpenOption[] options, Operation<InputStream> original) throws IOException {
		return DeferredResource.fileInputStream(path);
	}
}
*///?}
