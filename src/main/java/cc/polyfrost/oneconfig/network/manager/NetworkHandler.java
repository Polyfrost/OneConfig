package cc.polyfrost.oneconfig.network.manager;

import cc.polyfrost.oneconfig.network.packet.Packet;
import cc.polyfrost.oneconfig.utils.BooleanUtils;
import org.jetbrains.annotations.NotNull;

public abstract class NetworkHandler<P extends Packet> {
    protected boolean needsAuth;

    public NetworkHandler(boolean needsAuth) {
        this.needsAuth = needsAuth;
    }

    public void handle(NetworkManager manager, P packet) {
        BooleanUtils.isTrue(manager.isOpen(), () -> {
            return "Attempted to handle a Packet when the Connection Manager Connection was closed ('" + packet + "').";
        });

        if (!this.needsAuth || manager.isAuthenticated()) {
            this.onHandle(manager, packet);
        }
    }

    protected abstract void onHandle(@NotNull NetworkManager manager, @NotNull P packet);



}
