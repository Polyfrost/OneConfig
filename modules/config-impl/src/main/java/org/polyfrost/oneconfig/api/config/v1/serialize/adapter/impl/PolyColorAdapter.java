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

import org.polyfrost.oneconfig.api.config.v1.serialize.adapter.Adapter;
import org.polyfrost.polyui.color.PolyColor;
import org.polyfrost.polyui.color.ColorUtils;

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
