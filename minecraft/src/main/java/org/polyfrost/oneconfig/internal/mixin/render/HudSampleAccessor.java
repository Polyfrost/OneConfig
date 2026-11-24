package org.polyfrost.oneconfig.internal.mixin.render;

//? if > 1.8.9 {
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

//? if >= 26.2 {
import net.minecraft.client.gui.Hud;
//?} else {
/*import net.minecraft.client.gui.Gui;
*///?}
//? if >= 26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

//? if >= 26.2 {
@Mixin(Hud.class)
//?} else
//@Mixin(Gui.class)
public interface HudSampleAccessor {
    @Accessor("bossOverlay")
    BossHealthOverlay oneconfig$bossOverlay();

    @Accessor("overlayMessageString")
    Component oneconfig$overlayMessage();

    @Accessor("overlayMessageString")
    void oneconfig$setOverlayMessage(Component message);

    @Accessor("overlayMessageTime")
    int oneconfig$overlayMessageTime();

    @Accessor("overlayMessageTime")
    void oneconfig$setOverlayMessageTime(int time);

    @Accessor("animateOverlayMessageColor")
    boolean oneconfig$animateOverlayMessage();

    @Accessor("animateOverlayMessageColor")
    void oneconfig$setAnimateOverlayMessage(boolean animate);

    @Accessor("title")
    Component oneconfig$title();

    @Accessor("title")
    void oneconfig$setTitle(Component title);

    @Accessor("subtitle")
    Component oneconfig$subtitle();

    @Accessor("subtitle")
    void oneconfig$setSubtitle(Component subtitle);

    @Accessor("titleTime")
    int oneconfig$titleTime();

    @Accessor("titleTime")
    void oneconfig$setTitleTime(int time);

    @Accessor("titleFadeInTime")
    int oneconfig$titleFadeIn();

    @Accessor("titleFadeInTime")
    void oneconfig$setTitleFadeIn(int time);

    @Accessor("titleStayTime")
    int oneconfig$titleStay();

    @Accessor("titleStayTime")
    void oneconfig$setTitleStay(int time);

    @Accessor("titleFadeOutTime")
    int oneconfig$titleFadeOut();

    @Accessor("titleFadeOutTime")
    void oneconfig$setTitleFadeOut(int time);

    //? if >= 26.1 {
    @Invoker("extractBossOverlay")
    void oneconfig$drawBossOverlay(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

    @Invoker("extractOverlayMessage")
    void oneconfig$drawOverlayMessage(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

    @Invoker("extractTitle")
    void oneconfig$drawTitle(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker);

    @Invoker("displayScoreboardSidebar")
    void oneconfig$drawSidebar(GuiGraphicsExtractor graphics, Objective objective);
    //?} else {
    /*//? if >= 1.21.8 {
    @Invoker("renderBossOverlay")
    void oneconfig$drawBossOverlay(GuiGraphics graphics, DeltaTracker deltaTracker);
    //?}

    @Invoker("renderOverlayMessage")
    void oneconfig$drawOverlayMessage(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("renderTitle")
    void oneconfig$drawTitle(GuiGraphics graphics, DeltaTracker deltaTracker);

    @Invoker("displayScoreboardSidebar")
    void oneconfig$drawSidebar(GuiGraphics graphics, Objective objective);
    *///?}
}
//?}
