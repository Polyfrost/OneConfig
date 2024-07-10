package org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl;

import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.polyui.color.PolyColor;
import org.polyfrost.polyui.utils.ColorUtils;

public class PolyColorAdapter extends Adapter<PolyColor, int[]> {
	@Override
	public int[] serialize(PolyColor in) {
		return new int[]{in.red(), in.green(), in.blue(), in.alpha()};
	}

	@Override
	public PolyColor deserialize(int[] in) {
		return ColorUtils.rgba(in[0], in[1], in[2], in[3]);
	}

	@Override
	public Class<PolyColor> getTargetClass() {
		return PolyColor.class;
	}

	@Override
	public Class<int[]> getOutputClass() {
		return int[].class;
	}
}
