package org.polyfrost.oneconfig.internal.legacy;

//? if = 1.8.9 {
/*import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class LegacyOptionsFormat {
    private static final int NAMED_KEYS_VERSION = 1446; // OptionsKeyTranslationFix
    private static final int LOWERCASE_LANG_VERSION = 816; // OptionsLowerCaseLanguageFix
    private static final int BOOLEAN_AO_VERSION = 3214; // OptionsAmbientOcclusionFix

    private static final String UNKNOWN = "key.keyboard.unknown";
    private static final String MOUSE_PREFIX = "key.mouse.";
    private static final String KEY_PREFIX = "key_";
    private static final String LANG_PREFIX = "lang:";
    private static final String AO_PREFIX = "ao:";
    private static final Map<Integer, String> NAMES = new HashMap<>();
    private static final Map<String, Integer> CODES = new HashMap<>();

    // Follows vanilla OptionsKeyLwjgl3Fix table
    static {
        put(1, "key.keyboard.escape");
        put(2, "key.keyboard.1");
        put(3, "key.keyboard.2");
        put(4, "key.keyboard.3");
        put(5, "key.keyboard.4");
        put(6, "key.keyboard.5");
        put(7, "key.keyboard.6");
        put(8, "key.keyboard.7");
        put(9, "key.keyboard.8");
        put(10, "key.keyboard.9");
        put(11, "key.keyboard.0");
        put(12, "key.keyboard.minus");
        put(13, "key.keyboard.equal");
        put(14, "key.keyboard.backspace");
        put(15, "key.keyboard.tab");
        put(16, "key.keyboard.q");
        put(17, "key.keyboard.w");
        put(18, "key.keyboard.e");
        put(19, "key.keyboard.r");
        put(20, "key.keyboard.t");
        put(21, "key.keyboard.y");
        put(22, "key.keyboard.u");
        put(23, "key.keyboard.i");
        put(24, "key.keyboard.o");
        put(25, "key.keyboard.p");
        put(26, "key.keyboard.left.bracket");
        put(27, "key.keyboard.right.bracket");
        put(28, "key.keyboard.enter");
        put(29, "key.keyboard.left.control");
        put(30, "key.keyboard.a");
        put(31, "key.keyboard.s");
        put(32, "key.keyboard.d");
        put(33, "key.keyboard.f");
        put(34, "key.keyboard.g");
        put(35, "key.keyboard.h");
        put(36, "key.keyboard.j");
        put(37, "key.keyboard.k");
        put(38, "key.keyboard.l");
        put(39, "key.keyboard.semicolon");
        put(40, "key.keyboard.apostrophe");
        put(41, "key.keyboard.grave.accent");
        put(42, "key.keyboard.left.shift");
        put(43, "key.keyboard.backslash");
        put(44, "key.keyboard.z");
        put(45, "key.keyboard.x");
        put(46, "key.keyboard.c");
        put(47, "key.keyboard.v");
        put(48, "key.keyboard.b");
        put(49, "key.keyboard.n");
        put(50, "key.keyboard.m");
        put(51, "key.keyboard.comma");
        put(52, "key.keyboard.period");
        put(53, "key.keyboard.slash");
        put(54, "key.keyboard.right.shift");
        put(55, "key.keyboard.keypad.multiply");
        put(56, "key.keyboard.left.alt");
        put(57, "key.keyboard.space");
        put(58, "key.keyboard.caps.lock");
        put(59, "key.keyboard.f1");
        put(60, "key.keyboard.f2");
        put(61, "key.keyboard.f3");
        put(62, "key.keyboard.f4");
        put(63, "key.keyboard.f5");
        put(64, "key.keyboard.f6");
        put(65, "key.keyboard.f7");
        put(66, "key.keyboard.f8");
        put(67, "key.keyboard.f9");
        put(68, "key.keyboard.f10");
        put(69, "key.keyboard.num.lock");
        put(70, "key.keyboard.scroll.lock");
        put(71, "key.keyboard.keypad.7");
        put(72, "key.keyboard.keypad.8");
        put(73, "key.keyboard.keypad.9");
        put(74, "key.keyboard.keypad.subtract");
        put(75, "key.keyboard.keypad.4");
        put(76, "key.keyboard.keypad.5");
        put(77, "key.keyboard.keypad.6");
        put(78, "key.keyboard.keypad.add");
        put(79, "key.keyboard.keypad.1");
        put(80, "key.keyboard.keypad.2");
        put(81, "key.keyboard.keypad.3");
        put(82, "key.keyboard.keypad.0");
        put(83, "key.keyboard.keypad.decimal");
        put(87, "key.keyboard.f11");
        put(88, "key.keyboard.f12");
        put(100, "key.keyboard.f13");
        put(101, "key.keyboard.f14");
        put(102, "key.keyboard.f15");
        put(103, "key.keyboard.f16");
        put(104, "key.keyboard.f17");
        put(105, "key.keyboard.f18");
        put(113, "key.keyboard.f19");
        put(141, "key.keyboard.keypad.equal");
        put(156, "key.keyboard.keypad.enter");
        put(157, "key.keyboard.right.control");
        put(181, "key.keyboard.keypad.divide");
        put(183, "key.keyboard.print.screen");
        put(184, "key.keyboard.right.alt");
        put(197, "key.keyboard.pause");
        put(199, "key.keyboard.home");
        put(200, "key.keyboard.up");
        put(201, "key.keyboard.page.up");
        put(203, "key.keyboard.left");
        put(205, "key.keyboard.right");
        put(207, "key.keyboard.end");
        put(208, "key.keyboard.down");
        put(209, "key.keyboard.page.down");
        put(210, "key.keyboard.insert");
        put(211, "key.keyboard.delete");
        put(219, "key.keyboard.left.win");
        put(220, "key.keyboard.right.win");
        put(221, "key.keyboard.menu");
        // codes Pylon adds beyond LWJGL2
        put(161, "key.keyboard.world.1");
        put(162, "key.keyboard.world.2");
        put(309, "key.keyboard.f20");
        put(310, "key.keyboard.f21");
        put(311, "key.keyboard.f22");
        put(312, "key.keyboard.f23");
        put(313, "key.keyboard.f24");
        put(314, "key.keyboard.f25");
    }

    private LegacyOptionsFormat() {}

    private static String toName(int code) {
        if (code == 0) return UNKNOWN;
        if (code < 0) {
            int button = code + 100;
            return switch (button) {
                case 0 -> MOUSE_PREFIX + "left";
                case 1 -> MOUSE_PREFIX + "right";
                case 2 -> MOUSE_PREFIX + "middle";
                default -> button > 2 ? MOUSE_PREFIX + (button + 1) : UNKNOWN;
            };
        }
        return NAMES.getOrDefault(code, UNKNOWN);
    }

    private static Integer toCode(String name) {
        if (UNKNOWN.equals(name)) return 0;
        if (!name.startsWith(MOUSE_PREFIX)) return CODES.get(name);
        String button = name.substring(MOUSE_PREFIX.length());
        return switch (button) {
            case "left" -> -100;
            case "right" -> -99;
            case "middle" -> -98;
            default -> {
                try {
                    int number = Integer.parseInt(button);
                    yield number > 3 ? number - 1 - 100 : null;
                } catch (NumberFormatException e) {
                    yield null;
                }
            }
        };
    }

    public static String toLegacyLine(String line) {
        if (line.startsWith(KEY_PREFIX)) {
            int colon = line.indexOf(':');
            Integer code = toCode(line.substring(colon + 1));
            return code == null ? line : line.substring(0, colon + 1) + code;
        }
        if (line.startsWith(LANG_PREFIX)) {
            int region = line.lastIndexOf('_') + 1;
            if (region > LANG_PREFIX.length()) return line.substring(0, region) + line.substring(region).toUpperCase(Locale.ROOT);
        }
        return line;
    }

    public static String toModernLine(String line, int dataVersion) {
        try {
            if (line.startsWith(KEY_PREFIX) && dataVersion >= NAMED_KEYS_VERSION) {
                int colon = line.indexOf(':');
                return line.substring(0, colon + 1) + toName(Integer.parseInt(line.substring(colon + 1)));
            }
            if (line.startsWith(LANG_PREFIX) && dataVersion >= LOWERCASE_LANG_VERSION) {
                return line.toLowerCase(Locale.ROOT);
            }
            if (line.startsWith(AO_PREFIX) && dataVersion >= BOOLEAN_AO_VERSION) {
                return AO_PREFIX + (Integer.parseInt(line.substring(AO_PREFIX.length())) != 0);
            }
        } catch (NumberFormatException ignored) {
        }
        return line;
    }

    private static void put(int code, String name) {
        NAMES.put(code, name);
        CODES.put(name, code);
    }
}
*///?}
