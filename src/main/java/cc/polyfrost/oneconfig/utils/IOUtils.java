package cc.polyfrost.oneconfig.utils;

import java.awt.*;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;

public final class IOUtils {

    private IOUtils() {
    }

    /**
     * Taken from legui under MIT License
     * <a href="https://github.com/SpinyOwl/legui/blob/develop/LICENSE">https://github.com/SpinyOwl/legui/blob/develop/LICENSE</a>
     */
    @SuppressWarnings("RedundantCast")
    public static ByteBuffer resourceToByteBuffer(String path) throws IOException {
        byte[] bytes;
        path = path.trim();
        if (path.startsWith("http")) {
            bytes = org.apache.commons.io.IOUtils.toByteArray(new URL(path));
        } else {
            InputStream stream;
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                stream = Files.newInputStream(file.toPath());
            } else {
                stream = IOUtils.class.getResourceAsStream(path);
            }
            if (stream == null) {
                throw new FileNotFoundException(path);
            }
            bytes = org.apache.commons.io.IOUtils.toByteArray(stream);
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

    public static void browseLink(String uri) {
        try {
            browseLink(new URI(uri));
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Invalid URI: " + uri);
        }
    }
    public static void browseLink(URI uri) {
        if(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            try {
                Desktop.getDesktop().browse(uri);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to open URL in browser: " + uri);
            }
        }
    }

}