package org.polyfrost.oneconfig.api.ui.v1.api;

import java.nio.ByteBuffer;

public interface StbApi {

    ByteBuffer loadFromMemory(ByteBuffer buffer, int[] widthOutput, int[] heightOutput, int[] channelsOutput, int desiredChannels);

}
