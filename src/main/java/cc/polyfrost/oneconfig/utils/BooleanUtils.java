package cc.polyfrost.oneconfig.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class BooleanUtils {
    private static final Supplier<String> nullMessage = () -> {
        return "Cannot be null.";
    };
    private static final Supplier<String> trueMessage = () -> {
        return "Cannot be false.";
    };
    public static <T> T notNull(@Nullable T object) {
        return notNull(object, nullMessage);
    }

    public static <T> T notNull(@Nullable T object, @NotNull Supplier<String> message, @Nullable Object... objects) {
        if (object == null) {
            throw new NullPointerException(String.format((String)message.get(), objects));
        } else {
            return object;
        }
    }

    public static void isTrue(boolean bool) {
        isTrue(bool, trueMessage);
    }

    public static void isTrue(boolean bool, @NotNull Supplier<String> message, Object... objects) {
        if (!bool) {
            throw new IllegalArgumentException(String.format((String)message.get(), objects));
        }
    }
}
