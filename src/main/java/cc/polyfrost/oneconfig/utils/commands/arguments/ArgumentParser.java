package cc.polyfrost.oneconfig.utils.commands.arguments;

import com.google.common.reflect.TypeToken;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public abstract class ArgumentParser<T> {
    private final TypeToken<T> type = new TypeToken<T>(getClass()) {
    };
    public final Class<?> typeClass = type.getRawType();

    /**
     * Parses the given string into an object of the type specified by this parser.
     * Should return null if the string cannot be parsed.
     *
     * @param arguments The string to parse.
     * @return The parsed object, or null if the string cannot be parsed.
     */
    @Nullable
    public abstract T parse(Arguments arguments);

    /**
     * Returns possible completions for the given arguments.
     * Should return an empty list or null if no completions are possible.
     *
     * @param arguments The arguments to complete.
     * @param parameter The parameter to complete.
     * @return A list of possible completions, or an empty list or null if no completions are possible.
     */
    @Nullable
    public List<String> complete(Arguments arguments, Parameter parameter) {
        return Collections.emptyList();
    }
}
