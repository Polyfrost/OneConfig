package cc.polyfrost.oneconfig.utils.hypixel;

import cc.polyfrost.oneconfig.events.EventManager;
import cc.polyfrost.oneconfig.events.event.*;
import cc.polyfrost.oneconfig.libs.eventbus.Subscribe;
import cc.polyfrost.oneconfig.libs.universal.UChat;
import cc.polyfrost.oneconfig.platform.Platform;
import cc.polyfrost.oneconfig.utils.JsonUtils;
import cc.polyfrost.oneconfig.utils.Multithreading;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Various utilities for Hypixel.
 * <p>
 * Locraw utilities taken from Seraph by Scherso under LGPL-2.1
 * <a href="https://github.com/Scherso/Seraph/blob/master/LICENSE">https://github.com/Scherso/Seraph/blob/master/LICENSE</a>
 * </p>
 */
public class HypixelUtils {
    public static final HypixelUtils INSTANCE = new HypixelUtils();
    private final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private int tick = 0;
    private int limboLoop = 0;

    private boolean sentCommand = false;
    private boolean cancel = false;
    private boolean sendPermitted = false;
    private boolean inGame;

    private LocrawInfo locraw;
    private LocrawInfo previousLocraw;

    private boolean initialized = false;
    private boolean isSeraph = false;

    public void initialize() {
        if (initialized) {
            return;
        }
        EventManager.INSTANCE.register(this);
        isSeraph = Platform.getLoaderPlatform().isModLoaded("seraph");
        initialized = true;
    }

    /**
     * Checks whether the player is on Hypixel.
     *
     * @return Whether the player is on Hypixel.
     * @see <a href="https://canary.discord.com/channels/864592657572560958/945075920664928276/978649312013725747">this discord link from jade / asbyth</a>
     */
    public boolean isHypixel() {
        if (!Platform.getServerPlatform().inMultiplayer()) return false;

        String serverBrand = Platform.getServerPlatform().getServerBrand();

        if (serverBrand == null) return false;

        return serverBrand.toLowerCase(Locale.ENGLISH).contains("hypixel");
    }

    /**
     * Queues a locraw update after the specified interval.
     *
     * @param interval The interval in milliseconds.
     */
    public void queueUpdate(long interval) {
        sendPermitted = true;
        Multithreading.schedule(() -> {
            if (sendPermitted) {
                cancel = true;
                UChat.say("/locraw");
            }
        }, interval, TimeUnit.MILLISECONDS);
    }

    @Subscribe
    private void onTick(TickEvent event) {
        if (event.stage == Stage.START) {
            tick++;

            if (tick % 20 == 0) {
                tick = 0;
                if (isHypixel() && !sentCommand && !isSeraph) {
                    queueUpdate(500);
                    sentCommand = true;
                }
            }
        }
    }

    @Subscribe
    private void onWorldLoad(WorldLoadEvent event) {
        locraw = null;
        sendPermitted = false;
        cancel = false;
        sentCommand = false;
        limboLoop = 0;
    }

    @Subscribe
    private void onMessageReceived(ChatReceiveEvent event) {
        try {
            final boolean didSendCommand = sentCommand;
            final String msg = event.getFullyUnformattedMessage();
            // Checking for rate limitation.
            if (!(msg.startsWith("{") && msg.endsWith("}"))) {
                if (sentCommand && msg.contains("You are sending too many commands! Please try again in a few seconds.")) // if you're being rate limited, the /locraw command will be resent in 5 seconds.
                    queueUpdate(5000);
                return;
            }

            JsonElement raw = JsonUtils.parseString(msg);
            if (!raw.isJsonObject()) return;
            JsonObject json = raw.getAsJsonObject();
            LocrawInfo parsed = GSON.fromJson(json, LocrawInfo.class);

            if (5 > limboLoop && parsed.getGameType() == LocrawInfo.GameType.LIMBO) {
                sentCommand = false;
                limboLoop++;
                queueUpdate(1000);
            } else locraw = parsed; // if the player isn't in limbo, the parsed info is used.

            if (locraw != null) {
                locraw.setGameType(LocrawInfo.GameType.getFromLocraw(this.locraw.getRawGameType()));
                if (parsed.getGameMode().equals("lobby")) {
                    inGame = false; // If your gamemode returns "lobby", boolean inGame is false.
                } else {
                    previousLocraw = parsed;
                    inGame = true; // If your gamemode does not return "lobby", boolean inGame is true.
                }
                EventManager.INSTANCE.post(new LocrawEvent(locraw));
                if (cancel && didSendCommand) {
                    cancel = false;
                    event.isCancelled = true;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns whether the player is in game.
     *
     * @return Whether the player is in game.
     */
    public boolean isInGame() {
        return this.inGame;
    }

    /**
     * Returns the current {@link LocrawInfo}.
     *
     * @return The current {@link LocrawInfo}.
     * @see LocrawInfo
     */
    public LocrawInfo getLocrawInfo() {
        return this.locraw;
    }

    /**
     * Returns the previous {@link LocrawInfo}.
     *
     * @return The previous {@link LocrawInfo}.
     * @see LocrawInfo
     */
    public LocrawInfo getPreviousLocraw() {
        return this.previousLocraw;
    }
}
