package cc.polyfrost.oneconfig.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

public class JsonUtils {
    private static final JsonParser PARSER = new JsonParser();

    public static JsonParser getParser() {
        return PARSER;
    }

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
