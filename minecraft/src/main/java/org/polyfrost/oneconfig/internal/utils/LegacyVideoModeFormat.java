package org.polyfrost.oneconfig.internal.utils;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class LegacyVideoModeFormat {
    public static final String OPTION_PREFIX = "fullscreenResolution:";
    private static final Pattern LEGACY = Pattern.compile("(\\d+)x(\\d+)(?:@(\\d+)(?::(\\d+))?)?");
    private static final Pattern JSON_FIELD = Pattern.compile("\"(width|height|refresh_rate)\"\\s*:\\s*(\\d+(?:\\.\\d+)?)");

    private LegacyVideoModeFormat() {}

    public static boolean isLegacy(String mode) {
        return mode != null && !toJson(mode).equals(mode);
    }

    public static String toJson(String mode) {
        if (mode == null) return null;
        Matcher m = LEGACY.matcher(mode.trim());
        if (!m.matches()) return mode;
        try {
            int rate = m.group(3) == null ? 60 : Integer.parseInt(m.group(3));
            int channelBits = (m.group(4) == null ? 24 : Integer.parseInt(m.group(4))) / 3;
            return String.format(Locale.ROOT,
                "{\"width\":%d,\"height\":%d,\"red_bits\":%d,\"green_bits\":%d,\"blue_bits\":%d,\"refresh_rate\":%d.0}",
                Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)), channelBits, channelBits, channelBits, rate);
        } catch (NumberFormatException e) {
            return mode;
        }
    }

    public static int[] toSize(String mode) {
        if (mode == null) return null;
        int[] size = new int[3];
        int found = 0;
        Matcher m = JSON_FIELD.matcher(toJson(mode));
        while (m.find()) {
            int value = (int) Math.round(Double.parseDouble(m.group(2)));
            switch (m.group(1)) {
                case "width" -> { size[0] = value; found |= 1; }
                case "height" -> { size[1] = value; found |= 2; }
                default -> { size[2] = value; found |= 4; }
            }
        }
        return found == 7 && size[0] > 0 && size[1] > 0 ? size : null;
    }
}
