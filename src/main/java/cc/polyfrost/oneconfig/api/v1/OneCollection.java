/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021~2023 Polyfrost.
 *   <https://polyfrost.cc> <https://github.com/Polyfrost/>
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
 * <https://polyfrost.cc/legal/oneconfig/additional-terms>
 */

package cc.polyfrost.oneconfig.api.v1;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * <p>A wrapper around a collection of generic types.</p>
 *
 * <p><b>Note:</b> this class is not thread-safe.</p>
 *
 * @param <T> The type of the collection.
 */
public class OneCollection<T extends Namable> {
    private final List<T> elements = new ArrayList<>();

    @SafeVarargs
    public OneCollection(T... elements) {
        add(elements);
    }

    public void add(T element) {
        elements.add(element);
    }

    @SafeVarargs
    public final void add(T... elements) {
        add(Arrays.asList(elements));
    }

    public void add(Collection<T> elements) {
        this.elements.addAll(elements);
    }

    public void remove(T element) {
        elements.remove(element);
    }

    @SafeVarargs
    public final void remove(T... elements) {
        remove(Arrays.asList(elements));
    }

    public void remove(Collection<T> elements) {
        this.elements.removeAll(elements);
    }

    public void clear() {
        elements.clear();
    }

    public boolean contains(T element) {
        return elements.contains(element);
    }

    public boolean contains(String name) {
        for (T category : elements) {
            if (category.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public List<T> getElements() {
        return elements;
    }
}
