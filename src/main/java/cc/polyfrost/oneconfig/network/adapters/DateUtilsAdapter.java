package cc.polyfrost.oneconfig.network.adapters;

import cc.polyfrost.oneconfig.utils.DateUtils;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class DateUtilsAdapter extends TypeAdapter<DateUtils> {
    public DateUtils read(JsonReader reader) throws IOException {
        if (JsonToken.NULL == reader.peek()) {
            reader.nextNull();
            return null;
        } else {
            return new DateUtils(reader.nextLong());
        }
    }

    public void write(JsonWriter writer, DateUtils val) throws IOException {
        if (val == null) {
            writer.nullValue();
        } else {
            writer.value(val.getTime());
        }
    }
}
