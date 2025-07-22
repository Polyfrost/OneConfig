package org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl;

import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.polyui.unit.Vec4;

public class Vec4Adapter extends Adapter<Vec4, float[]> {
    @Override
    public Vec4 deserialize(float[] in) {
        return Vec4.of(in[0], in[1], in[2], in[3]);
    }

    @Override
    public float[] serialize(Vec4 in) {
        return new float[]{in.getX(), in.getY(), in.getW(), in.getH()};
    }

    @Override
    public Class<Vec4> getTargetClass() {
        return Vec4.class;
    }

    @Override
    public Class<float[]> getOutputClass() {
        return float[].class;
    }
}
