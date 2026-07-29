/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2024 Polyfrost.
 *   <https://polyfrost.org> <https://github.com/Polyfrost/>
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *   OneConfig is licensed under the terms of version 3 of the GNU Lesser
 * General Public License as published by the Free Software Foundation, AND
 * under the Additional Terms Applicable to OneConfig, as published by Polyfrost,
 * either version 1.0 of the Additional Terms, or (at your option) any later
 * version.
 *
 *   This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 *   You should have received a copy of the GNU Lesser General Public
 * License.  If not, see <https://www.gnu.org/licenses/>. You should
 * have also received a copy of the Additional Terms Applicable
 * to OneConfig, as published by Polyfrost. If not, see
 * <https://polyfrost.org/legal/oneconfig/additional-terms>
 */

package org.polyfrost.oneconfig.api.config.v1.serialize.adapter.impl;

import org.polyfrost.compose.render.PolyColor;
import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PolyColorAdapter extends Adapter<PolyColor, Object> {
	@Override
	public Object serialize(PolyColor in) {
		if (!in.getChroma()) {
			return rgba(in.getRawArgb());
		}

		Map<String, Object> out = new HashMap<>();
		out.put("rgba", rgba(in.getRawArgb()));
		out.put("chroma", true);
		out.put("chromaSpeed", in.getChromaSpeed());
		return out;
	}

	@Override
	public PolyColor deserialize(Object in) {
		if (in instanceof Map<?, ?>) {
			Map<?, ?> map = (Map<?, ?>) in;
			PolyColor color = deserialize(map.get("rgba"));
			Object speed = map.get("chromaSpeed");
			return color.withChroma(
				Boolean.TRUE.equals(map.get("chroma")),
				speed instanceof Number ? ((Number) speed).floatValue() : color.getChromaSpeed()
			);
		}
		if (in instanceof int[]) {
			int[] color = (int[]) in;
			return PolyColor.Companion.rgba(color[0], color[1], color[2], color[3]);
		}
		if (in instanceof List<?>) {
			List<?> color = (List<?>) in;
			return PolyColor.Companion.rgba(
				((Number) color.get(0)).intValue(),
				((Number) color.get(1)).intValue(),
				((Number) color.get(2)).intValue(),
				((Number) color.get(3)).intValue()
			);
		}
		throw new IllegalArgumentException("Unsupported PolyColor value: " + in);
	}

	@Override
	public Class<PolyColor> getTargetClass() {
		return PolyColor.class;
	}

	@Override
	public Class<Object> getOutputClass() {
		return Object.class;
	}

	private static int[] rgba(int argb) {
		return new int[]{
			(argb >> 16) & 0xFF,
			(argb >> 8) & 0xFF,
			argb & 0xFF,
			(argb >> 24) & 0xFF
		};
	}
}
