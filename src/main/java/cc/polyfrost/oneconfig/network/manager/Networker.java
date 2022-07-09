package cc.polyfrost.oneconfig.network.manager;

import cc.polyfrost.oneconfig.libs.caffeine.cache.*;
import cc.polyfrost.oneconfig.libs.websocket.client.WebSocketClient;
import cc.polyfrost.oneconfig.network.adapters.DateUtilsAdapter;
import cc.polyfrost.oneconfig.network.adapters.UUIDAdapter;
import cc.polyfrost.oneconfig.network.packet.Packet;
import cc.polyfrost.oneconfig.utils.DateUtils;
import cc.polyfrost.oneconfig.utils.ThreadUtils;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

public class Networker extends WebSocketClient {
    private final byte[] bytesNull = new byte[0];
    private final Cache<UUID, Pair<Long, Consumer<Optional<Packet>>>> responseCache = Caffeine.newBuilder().maximumSize(10000L).executor(ThreadUtils.getScheduledExecutor()).scheduler(Scheduler.forScheduledExecutorService(ThreadUtils.getScheduledExecutor())).expireAfter(new Expiry<UUID, Pair<Long, Consumer<Optional<Packet>>>>() {
        public long expireAfterUpdate(UUID id, Pair<Long, Consumer<Optional<Packet>>> valDat, long time, long duration) {
            return duration;
        }
        public long expireAfterRead(UUID id, Pair<Long, Consumer<Optional<Packet>>> valDat, long time, long duration) {
            return duration;
        }
        public long expireAfterCreate(UUID packetId, Pair<Long, Consumer<Optional<Packet>>> valueData, long currentTime) {
            return valueData.getKey();
        }
    }).evictionListener((key, val, cause) -> {
        if (val != null && (RemovalCause.EXPIRED == cause || RemovalCause.SIZE == cause)) {
            Consumer<Optional<Packet>> handler = val.getRight();
            ThreadUtils.getScheduledExecutor().execute(() -> {
                handler.accept(Optional.empty());
            });
        }

    }).build();
    private final String packetPackage = "cc.polyfrost.oneconfig.network.packet.";
    private int connectsFailed = 0;
    private final Map<Class<? extends Packet>, NetworkHandler<?>> handlers = Maps.newHashMap();
    private final AtomicInteger packetId = new AtomicInteger();
    private final Map<Integer, String> incomingPacketID = Maps.newConcurrentMap();
    private final Map<Integer, String> outgoingPacketID = Maps.newConcurrentMap();

    private final Gson gson = (new GsonBuilder()).registerTypeAdapter(UUID.class, new UUIDAdapter()).registerTypeAdapter(DateUtils.class, new DateUtilsAdapter()).create();
    private final NetworkManager manager;
    private final Lock secureLock = new ReentrantLock();
    private long lastRecievedPing;
    private long connectedAt = System.currentTimeMillis();

    private boolean hasConnectedPreviously = false;
    private String closeData;
}
