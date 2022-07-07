package cc.polyfrost.oneconfig.network.manager;

import cc.polyfrost.oneconfig.libs.websocket.client.WebSocketClient;

public class Networker extends WebSocketClient {
    private final byte[] emptyBytes = new byte[0];

    private final String packetPackage = "cc.polyfrost.oneconfig.network.packet.";

}
