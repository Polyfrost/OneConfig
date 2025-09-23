package org.polyfrost.oneconfig.api.ui.v1.api;

import java.nio.ByteBuffer;

public interface NanoSvgApi {
    SVG parse(ByteBuffer data);

    void delete(long address);

    void rasterize(long address, float x, float y, float scale, ByteBuffer data, int w, int h, int stride);


    final class SVG {
        public final long address;
        public final float width, height;

        public SVG(long address, float width, float height) {
            this.address = address;
            this.width = width;
            this.height = height;
        }
    }
}
