package io.polyfrost.oneconfig.lwjgl;

import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class IOUtil {

    private IOUtil() {
    }

    /**
     * Taken from legui under MIT License
     * https://github.com/SpinyOwl/legui/blob/develop/LICENSE
     */
    @SuppressWarnings("RedundantCast")
    public static ByteBuffer resourceToByteBuffer(String path) throws IOException {
        byte[] bytes;
        path = path.trim();
        if (path.startsWith("http")) {
            bytes = IOUtils.toByteArray(new URL(path));
        } else {
            InputStream stream;
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                stream = new FileInputStream(file);
            } else {
                stream = IOUtil.class.getResourceAsStream(path);
            }
            if (stream == null) {
                throw new FileNotFoundException(path);
            }
            bytes = IOUtils.toByteArray(stream);
        }
        ByteBuffer data = ByteBuffer.allocateDirect(bytes.length).order(ByteOrder.nativeOrder())
                .put(bytes);
        ((Buffer) data).flip();
        return data;
    }

    public static ByteBuffer resourceToByteBufferNullable(String path) {
        try {
            return resourceToByteBuffer(path);
        } catch (Exception ignored) {
            return null;
        }
    }

}