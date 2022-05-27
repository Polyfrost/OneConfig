package cc.polyfrost.oneconfig.utils;

import cc.polyfrost.oneconfig.libs.universal.UDesktop;
import com.google.gson.JsonElement;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class NetworkUtils {
    private static InputStream setupConnection(String url, String userAgent, int timeout, boolean useCaches) throws IOException {
        HttpURLConnection connection = ((HttpURLConnection) new URL(url).openConnection());
        connection.setRequestMethod("GET");
        connection.setUseCaches(useCaches);
        connection.addRequestProperty("User-Agent", userAgent);
        connection.setReadTimeout(timeout);
        connection.setConnectTimeout(timeout);
        connection.setDoOutput(true);
        return connection.getInputStream();
    }

    public static String getString(String url, String userAgent, int timeout, boolean useCaches) {
        try (InputStreamReader input = new InputStreamReader(setupConnection(url, userAgent, timeout, useCaches), StandardCharsets.UTF_8)) {
            return IOUtils.toString(input);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getString(String url) {
        return getString(url, "OneConfig/1.0.0", 5000, false);
    }

    public static JsonElement getJsonElement(String url, String userAgent, int timeout, boolean useCaches) {
        return JsonUtils.parseString(getString(url, userAgent, timeout, useCaches));
    }

    public static JsonElement getJsonElement(String url) {
        return getJsonElement(url, "OneConfig/1.0.0", 5000, false);
    }


    public static boolean downloadFile(String url, File file) {
        return downloadFile(url, file, "OneConfig/1.0.0", 5000, false);
    }

    public static boolean downloadFile(String url, File file, String userAgent, int timeout, boolean useCaches) {
        url = url.replace(" ", "%20");
        try (FileOutputStream fileOut = new FileOutputStream(file); BufferedInputStream in = new BufferedInputStream(setupConnection(url, userAgent, timeout, useCaches))) {
            IOUtils.copy(in, fileOut);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getFileChecksum(String filename) {
        try (FileInputStream inputStream = new FileInputStream(filename)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytesBuffer = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(bytesBuffer)) != -1) {
                digest.update(bytesBuffer, 0, bytesRead);
            }

            return convertByteArrayToHexString(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private static String convertByteArrayToHexString(byte[] arrayBytes) {
        StringBuilder stringBuffer = new StringBuilder();
        for (byte arrayByte : arrayBytes) {
            stringBuffer.append(Integer.toString((arrayByte & 0xff) + 0x100, 16)
                    .substring(1));
        }
        return stringBuffer.toString();
    }

    public static void browseLink(String uri) {
        UDesktop.browse(URI.create(uri));
    }
}
