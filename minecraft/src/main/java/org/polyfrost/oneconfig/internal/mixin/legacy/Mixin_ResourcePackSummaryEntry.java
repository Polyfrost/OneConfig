package org.polyfrost.oneconfig.internal.mixin.legacy;

//? if = 1.8.9 {
/*import java.io.IOException;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.render.texture.DynamicTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.ornithemc.osl.resource.loader.impl.adapter.ResourcePackSummaryEntry", remap = false)
public class Mixin_ResourcePackSummaryEntry {
	@ModifyExpressionValue(
		method = "<init>",
		at = @At(
			value = "FIELD",
			target = "Lnet/minecraft/client/render/texture/TextureUtil;MISSING_TEXTURE:Lnet/minecraft/client/render/texture/DynamicTexture;",
			remap = true
		),
		remap = false
	)
	private DynamicTexture oneconfig$useDefaultPackIcon(DynamicTexture missingTexture) {
		try {
			return new DynamicTexture(Minecraft.getInstance().getResourcePacks().defaultPack.getIcon());
		} catch (IOException e) {
			throw new IllegalStateException("Unable to load the default resource pack icon", e);
		}
	}
}
*///?}
