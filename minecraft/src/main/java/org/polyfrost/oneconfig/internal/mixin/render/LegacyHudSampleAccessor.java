package org.polyfrost.oneconfig.internal.mixin.render;

//? if = 1.8.9 {
/*import net.minecraft.client.gui.GameGui;
import net.minecraft.client.render.Window;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameGui.class)
public interface LegacyHudSampleAccessor {
    @Invoker("renderBossBars")
    void oneconfig$renderBossBars();

    @Invoker("renderScoreboardObjective")
    void oneconfig$renderScoreboardObjective(Objective objective, Window window);

    @Accessor("overlayMessage")
    String oneconfig$overlayMessage();

    @Accessor("overlayMessage")
    void oneconfig$setOverlayMessage(String message);

    @Accessor("overlayMessageCooldown")
    int oneconfig$overlayMessageCooldown();

    @Accessor("overlayMessageCooldown")
    void oneconfig$setOverlayMessageCooldown(int cooldown);

    @Accessor("overlayMessageTinted")
    boolean oneconfig$overlayMessageTinted();

    @Accessor("overlayMessageTinted")
    void oneconfig$setOverlayMessageTinted(boolean tinted);

    @Accessor("titleTime")
    int oneconfig$titleTime();

    @Accessor("titleTime")
    void oneconfig$setTitleTime(int time);

    @Accessor("title")
    String oneconfig$title();

    @Accessor("title")
    void oneconfig$setTitle(String title);

    @Accessor("subtitle")
    String oneconfig$subtitle();

    @Accessor("subtitle")
    void oneconfig$setSubtitle(String subtitle);

    @Accessor("titleFadeInTime")
    int oneconfig$titleFadeInTime();

    @Accessor("titleFadeInTime")
    void oneconfig$setTitleFadeInTime(int time);

    @Accessor("titleDuration")
    int oneconfig$titleDuration();

    @Accessor("titleDuration")
    void oneconfig$setTitleDuration(int time);

    @Accessor("titleFadeOutTime")
    int oneconfig$titleFadeOutTime();

    @Accessor("titleFadeOutTime")
    void oneconfig$setTitleFadeOutTime(int time);
}
*///?}
