package cc.polyfrost.oneconfig.network.adapters;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

public class UUIDAdapter extends TypeAdapter<UUID> {
    public UUID read(JsonReader reader) throws IOException {
        if (JsonToken.NULL == reader.peek()) {
            reader.nextNull();
            return null;
        } else {
            return UUID.fromString(reader.nextString());
        }
    }

    public void write(JsonWriter writer, UUID val) throws IOException {
        if (val == null) {
            writer.nullValue();
        } else {
            writer.value(val.toString());
        }
    }
}
