package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unstable")
public abstract class ArgumentParser<T> {
    private final TypeToken<T> type = new TypeToken<T>(getClass()) {};
    public final Class<?> typeClass = type.getRawType();
    @Nullable
    public abstract T parse(Arguments arguments);
}
