package org.polyfrost.oneconfig.internal;

//? if = 1.8.9 {
/*import java.nio.FloatBuffer;
import java.util.Arrays;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.BossBar;
import net.minecraft.client.render.Window;
import net.minecraft.scoreboard.criterion.ScoreboardCriterion;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.polyfrost.oneconfig.api.hud.v1.HudManager;
import org.polyfrost.oneconfig.internal.mixin.render.LegacyHudSampleAccessor;
import org.polyfrost.oneconfig.internal.ui.hud.VanillaHudFootprint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LegacyVanillaHudSampler {
    private static final Logger LOGGER = LoggerFactory.getLogger("OneConfig/LegacyVanillaHudSampler");
    private static final int SAMPLE_INTERVAL = 10;
    private static final int SIDEBAR_LINES = 6;
    private static final int MAX_INLINE_RECTS = 256;
    private static final FloatBuffer MODELVIEW = BufferUtils.createFloatBuffer(16);

    private static int frame;
    private static Objective sampleObjective;
    private static boolean warned;

    private static boolean inlineArmed;
    private static boolean inlineRecording;
    private static float[] inlineRects = new float[32 * 4];
    private static int inlineCount;

    private static boolean actionBarFaked;
    private static String savedOverlayMessage;
    private static int savedOverlayCooldown;
    private static boolean savedOverlayTinted;

    private static boolean titleFaked;
    private static String savedTitle;
    private static String savedSubtitle;
    private static int savedTitleTime;
    private static int savedTitleFadeIn;
    private static int savedTitleDuration;
    private static int savedTitleFadeOut;

    private LegacyVanillaHudSampler() {
    }

    public static boolean isMeasuring() {
        return inlineRecording || VanillaHudFootprint.isCapturing() && HudManager.INSTANCE.isEditorOpen();
    }

    public static boolean record(float x1, float y1, float x2, float y2) {
        FloatBuffer m = MODELVIEW;
        m.clear();
        GL11.glGetFloatv(GL11.GL_MODELVIEW_MATRIX, m);
        float a = m.get(0), b = m.get(1), c = m.get(4), d = m.get(5), tx = m.get(12), ty = m.get(13);
        float ax = a * x1 + c * y1 + tx, ay = b * x1 + d * y1 + ty;
        float bx = a * x2 + c * y1 + tx, by = b * x2 + d * y1 + ty;
        float cx = a * x1 + c * y2 + tx, cy = b * x1 + d * y2 + ty;
        float dx = a * x2 + c * y2 + tx, dy = b * x2 + d * y2 + ty;
        float left = Math.min(Math.min(ax, bx), Math.min(cx, dx));
        float top = Math.min(Math.min(ay, by), Math.min(cy, dy));
        float right = Math.max(Math.max(ax, bx), Math.max(cx, dx));
        float bottom = Math.max(Math.max(ay, by), Math.max(cy, dy));
        if (inlineRecording) {
            storeInline(left, top, right, bottom);
            return true;
        }
        VanillaHudFootprint.add(left, top, right, bottom);
        return VanillaHudFootprint.isSampling();
    }

    private static void storeInline(float left, float top, float right, float bottom) {
        if (inlineCount >= MAX_INLINE_RECTS) return;
        int i = inlineCount * 4;
        if (i + 4 > inlineRects.length) inlineRects = Arrays.copyOf(inlineRects, inlineRects.length * 2);
        inlineRects[i] = left;
        inlineRects[i + 1] = top;
        inlineRects[i + 2] = right;
        inlineRects[i + 3] = bottom;
        inlineCount++;
    }

    public static void beginInlineActionBar(Object gameGui) {
        LegacyHudSampleAccessor accessor = (LegacyHudSampleAccessor) gameGui;
        restoreInline(accessor);
        inlineCount = 0;
        inlineArmed = HudManager.INSTANCE.isEditorOpen() && frame % SAMPLE_INTERVAL == 0;
        if (!inlineArmed || accessor.oneconfig$overlayMessageCooldown() > 0) return;
        savedOverlayMessage = accessor.oneconfig$overlayMessage();
        savedOverlayCooldown = accessor.oneconfig$overlayMessageCooldown();
        savedOverlayTinted = accessor.oneconfig$overlayMessageTinted();
        accessor.oneconfig$setOverlayMessage("Action bar message");
        accessor.oneconfig$setOverlayMessageCooldown(60);
        accessor.oneconfig$setOverlayMessageTinted(false);
        actionBarFaked = true;
        inlineRecording = true;
        HudManager.isSamplingVanilla = true;
    }

    public static void beginInlineTitle(Object gameGui) {
        LegacyHudSampleAccessor accessor = (LegacyHudSampleAccessor) gameGui;
        restoreActionBar(accessor);
        inlineRecording = false;
        HudManager.isSamplingVanilla = false;
        if (!inlineArmed || accessor.oneconfig$titleTime() > 0) return;
        savedTitle = accessor.oneconfig$title();
        savedSubtitle = accessor.oneconfig$subtitle();
        savedTitleTime = accessor.oneconfig$titleTime();
        savedTitleFadeIn = accessor.oneconfig$titleFadeInTime();
        savedTitleDuration = accessor.oneconfig$titleDuration();
        savedTitleFadeOut = accessor.oneconfig$titleFadeOutTime();
        accessor.oneconfig$setTitle("Title");
        accessor.oneconfig$setSubtitle("Subtitle text");
        accessor.oneconfig$setTitleFadeInTime(10);
        accessor.oneconfig$setTitleDuration(70);
        accessor.oneconfig$setTitleFadeOutTime(20);
        accessor.oneconfig$setTitleTime(60);
        titleFaked = true;
        inlineRecording = true;
        HudManager.isSamplingVanilla = true;
    }

    public static void endInline(Object gameGui) {
        restoreInline((LegacyHudSampleAccessor) gameGui);
    }

    private static void restoreInline(LegacyHudSampleAccessor accessor) {
        inlineRecording = false;
        HudManager.isSamplingVanilla = false;
        restoreActionBar(accessor);
        restoreTitle(accessor);
    }

    private static void restoreActionBar(LegacyHudSampleAccessor accessor) {
        if (!actionBarFaked) return;
        actionBarFaked = false;
        accessor.oneconfig$setOverlayMessage(savedOverlayMessage);
        accessor.oneconfig$setOverlayMessageCooldown(savedOverlayCooldown);
        accessor.oneconfig$setOverlayMessageTinted(savedOverlayTinted);
        savedOverlayMessage = null;
    }

    private static void restoreTitle(LegacyHudSampleAccessor accessor) {
        if (!titleFaked) return;
        titleFaked = false;
        accessor.oneconfig$setTitle(savedTitle);
        accessor.oneconfig$setSubtitle(savedSubtitle);
        accessor.oneconfig$setTitleTime(savedTitleTime);
        accessor.oneconfig$setTitleFadeInTime(savedTitleFadeIn);
        accessor.oneconfig$setTitleDuration(savedTitleDuration);
        accessor.oneconfig$setTitleFadeOutTime(savedTitleFadeOut);
        savedTitle = null;
        savedSubtitle = null;
    }

    public static void sample(Object gameGui) {
        if (!HudManager.INSTANCE.isEditorOpen()) {
            inlineCount = 0;
            VanillaHudFootprint.clearSamples();
            return;
        }
        if (frame++ % SAMPLE_INTERVAL != 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || !(gameGui instanceof LegacyHudSampleAccessor)) return;
        final LegacyHudSampleAccessor accessor = (LegacyHudSampleAccessor) gameGui;
        restoreInline(accessor);
        VanillaHudFootprint.begin(true);
        HudManager.isSamplingVanilla = true;
        try {
            if (inlineArmed) {
                for (int k = 0; k < inlineCount; k++) {
                    int i = k * 4;
                    VanillaHudFootprint.add(inlineRects[i], inlineRects[i + 1], inlineRects[i + 2], inlineRects[i + 3]);
                }
            }
            run(() -> sampleBossBar(accessor));
            run(() -> sampleSidebar(accessor, minecraft));
        } finally {
            HudManager.isSamplingVanilla = false;
            inlineArmed = false;
            inlineCount = 0;
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

    private static void sampleBossBar(LegacyHudSampleAccessor accessor) {
        String name = BossBar.name;
        float health = BossBar.health;
        int timer = BossBar.timer;
        if (name != null && timer > 0) return;
        BossBar.name = "Boss bar";
        BossBar.health = 1f;
        BossBar.timer = 100;
        try {
            accessor.oneconfig$renderBossBars();
        } finally {
            BossBar.name = name;
            BossBar.health = health;
            BossBar.timer = timer;
        }
    }

    private static void sampleSidebar(LegacyHudSampleAccessor accessor, Minecraft minecraft) {
        Scoreboard scoreboard = minecraft.level.getScoreboard();
        if (scoreboard.getDisplayObjective(1) != null) return;
        PlayerTeam team = scoreboard.getTeamOfMember(minecraft.player.getName());
        if (team != null) {
            int color = team.getColor().getId();
            if (color >= 0 && scoreboard.getDisplayObjective(3 + color) != null) return;
        }
        accessor.oneconfig$renderScoreboardObjective(sampleObjective(), new Window(minecraft));
    }

    private static Objective sampleObjective() {
        if (sampleObjective != null) return sampleObjective;
        Scoreboard scoreboard = new Scoreboard();
        Objective objective = scoreboard.createObjective("oneconfig_sample", ScoreboardCriterion.DUMMY);
        objective.setDisplayName("Scoreboard");
        for (int i = 1; i <= SIDEBAR_LINES; i++) {
            scoreboard.getScore("Scoreboard line " + i, objective).set(SIDEBAR_LINES - i);
        }
        sampleObjective = objective;
        return objective;
    }
}
*///?}
