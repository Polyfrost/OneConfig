package org.polyfrost.oneconfig.internal.mixin.render;

//? if >= 1.21.11 {
import com.mojang.blaze3d.vertex.BufferBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BufferBuilder.class)
public interface BufferBuilderAccessor {
    @Accessor("vertexPointer")
    long oneconfig$getVertexPointer();

    @Accessor("vertices")
    int oneconfig$getVertices();

    @Accessor("vertexSize")
    int oneconfig$getVertexSize();

    @Accessor("elementsToFill")
    int oneconfig$getElementsToFill();
}
//?}
