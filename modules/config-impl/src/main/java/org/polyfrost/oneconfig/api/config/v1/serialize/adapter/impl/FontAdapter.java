package org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl;

import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.polyui.data.Font;

public class FontAdapter extends Adapter <Font, String> {
    @Override
    public Font deserialize(String in) {
        return Font.of(in);
    }

    @Override
    public String serialize(Font in) {
        return in.getResourcePath();
    }

    @Override
    public Class<Font> getTargetClass() {
        return Font.class;
    }

    @Override
    public Class<String> getOutputClass() {
        return String.class;
    }
}
