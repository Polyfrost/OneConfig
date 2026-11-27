package org.polyfrost.oneconfig.internal;

//? if > 1.8.9 {
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.BossEvent;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.internal.mixin.render.BossHealthOverlayAccessor;
import org.polyfrost.oneconfig.internal.mixin.render.HudSampleAccessor;
import org.polyfrost.oneconfig.internal.ui.hud.VanillaHudFootprint;

//? if >= 26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
//?} else if >= 1.21.8 {
/*import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
*///?} else {
/*import net.minecraft.client.gui.GuiGraphics;
*///?}

public final class VanillaHudSampler {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/VanillaHudSampler");
    private static final int SAMPLE_INTERVAL = 10;
    private static final int SIDEBAR_LINES = 6;
    private static final UUID SAMPLE_BOSS = UUID.fromString("6f6e6563-6f6e-6669-6773-616d706c6521");

    private static int frame;
    private static Objective sampleObjective;
    private static boolean warned;

    private VanillaHudSampler() {
    }

    public static void sample(Object hud) {
        if (!HudManager.INSTANCE.isEditorOpen()) {
            VanillaHudFootprint.clearSamples();
            return;
        }
        if (frame++ % SAMPLE_INTERVAL != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || !(hud instanceof HudSampleAccessor accessor)) return;
        //? if >= 26.1 {
        GuiGraphicsExtractor graphics = new GuiGraphicsExtractor(minecraft, new GuiRenderState(), -1, -1);
        //?} else if >= 1.21.11 {
        /*GuiGraphics graphics = new GuiGraphics(minecraft, new GuiRenderState(), -1, -1);
        *///?} else if >= 1.21.8 {
        /*GuiGraphics graphics = new GuiGraphics(minecraft, new GuiRenderState());
        *///?} else {
        /*GuiGraphics graphics = new GuiGraphics(minecraft, minecraft.renderBuffers().bufferSource());
        *///?}
        //? if >= 1.21.4 {
        DeltaTracker deltaTracker = minecraft.getDeltaTracker();
        //?} else
        //DeltaTracker deltaTracker = minecraft.getTimer();
        VanillaHudFootprint.begin(true);
        HudManager.isSamplingVanilla = true;
        try {
            run(() -> sampleBossBar(accessor, graphics, deltaTracker));
            run(() -> sampleSidebar(accessor, graphics, minecraft));
            run(() -> sampleActionBar(accessor, graphics, deltaTracker));
        } finally {
            HudManager.isSamplingVanilla = false;
            VanillaHudFootprint.end(HudManager.guiScreenWidth, HudManager.guiScreenHeight);
        }
    }

    private static void run(Runnable step) {
        try {
            step.run();
        } catch (Throwable t) {
            if (!warned) {
                warned = true;
                LOGGER.warn("Failed to sample a vanilla HUD element for the HUD editor", t);
            }
        }
    }

    //? if >= 26.1 {
    private static void sampleBossBar(HudSampleAccessor accessor, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    //?} else
    //private static void sampleBossBar(HudSampleAccessor accessor, GuiGraphics graphics, DeltaTracker deltaTracker) {
        Map<UUID, LerpingBossEvent> events = ((BossHealthOverlayAccessor) accessor.oneconfig$bossOverlay()).oneconfig$events();
        if (!events.isEmpty()) return;
        events.put(SAMPLE_BOSS, new LerpingBossEvent(
            SAMPLE_BOSS, Component.literal("Boss bar"), 1f,
            BossEvent.BossBarColor.PURPLE, BossEvent.BossBarOverlay.PROGRESS, false, false, false
        ));
        try {
            //? if >= 1.21.8 {
            accessor.oneconfig$drawBossOverlay(graphics, deltaTracker);
            //?} else
            //accessor.oneconfig$bossOverlay().render(graphics);
        } finally {
            events.remove(SAMPLE_BOSS);
        }
    }

    //? if >= 26.1 {
    private static void sampleSidebar(HudSampleAccessor accessor, GuiGraphicsExtractor graphics, Minecraft minecraft) {
    //?} else
    //private static void sampleSidebar(HudSampleAccessor accessor, GuiGraphics graphics, Minecraft minecraft) {
        if (minecraft.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR) != null) return;
        accessor.oneconfig$drawSidebar(graphics, sampleObjective());
    }

    private static Objective sampleObjective() {
        if (sampleObjective != null) return sampleObjective;
        Scoreboard scoreboard = new Scoreboard();
        Objective objective = scoreboard.addObjective(
            "oneconfig_sample", ObjectiveCriteria.DUMMY, Component.literal("Scoreboard"),
            ObjectiveCriteria.RenderType.INTEGER, false, null
        );
        for (int i = 1; i <= SIDEBAR_LINES; i++) {
            scoreboard.getOrCreatePlayerScore(ScoreHolder.forNameOnly("Scoreboard line " + i), objective).set(SIDEBAR_LINES - i);
        }
        sampleObjective = objective;
        return objective;
    }

    //? if >= 26.1 {
    private static void sampleActionBar(HudSampleAccessor accessor, GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
    //?} else
    //private static void sampleActionBar(HudSampleAccessor accessor, GuiGraphics graphics, DeltaTracker deltaTracker) {
        if (accessor.oneconfig$overlayMessageTime() > 0) return;
        Component message = accessor.oneconfig$overlayMessage();
        int time = accessor.oneconfig$overlayMessageTime();
        boolean animate = accessor.oneconfig$animateOverlayMessage();
        accessor.oneconfig$setOverlayMessage(Component.literal("Action bar message"));
        accessor.oneconfig$setOverlayMessageTime(60);
        accessor.oneconfig$setAnimateOverlayMessage(false);
        try {
            accessor.oneconfig$drawOverlayMessage(graphics, deltaTracker);
        } finally {
            accessor.oneconfig$setOverlayMessage(message);
            accessor.oneconfig$setOverlayMessageTime(time);
            accessor.oneconfig$setAnimateOverlayMessage(animate);
        }
    }
}
//?}
