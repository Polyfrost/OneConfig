package cc.polyfrost.oneconfig.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Various utility methods for working with JSON.
 */
public final class JsonUtils {

    /**
     * The instance of the parser.
     */
    public static final JsonParser PARSER = new JsonParser();

    /**
     * Parses a string into a {@link JsonElement}.
     *
     * @param string          The string to parse.
     * @param catchExceptions Whether to catch exceptions.
     * @return The {@link JsonElement}.
     * @see JsonParser#parse(String)
     */
    public static JsonElement parseString(String string, boolean catchExceptions) {
        try {
            return PARSER.parse(string);
        } catch (Exception e) {
            if (catchExceptions) {
                return null;
            } else {
                throw e;
            }
        }
    }

    /**
     * Parses a string into a {@link JsonElement}.
     *
     * @param string The string to parse.
     * @return The {@link JsonElement}.
     * @see JsonUtils#parseString(String, boolean)
     */
    public static JsonElement parseString(String string) {
        return parseString(string, true);
    }
}
