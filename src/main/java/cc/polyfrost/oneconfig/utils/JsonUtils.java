package cc.polyfrost.oneconfig.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public final class JsonUtils {
    public static final JsonParser PARSER = new JsonParser();

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

    public static JsonElement parseString(String string) {
        return parseString(string, true);
    }
}
