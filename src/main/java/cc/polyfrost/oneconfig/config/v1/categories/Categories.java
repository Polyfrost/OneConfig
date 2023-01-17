/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
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

package cc.polyfrost.oneconfig.config.v1.categories;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A list of categories that can be used to categorize {@link cc.polyfrost.oneconfig.config.v1.OneConfig}s.
 */
public class Categories {
    private final List<Category> categories = new ArrayList<>();

    public Categories(Category... categories) {
        addCategories(categories);
    }

    public List<Category> getCategories() {
        return categories;
    }

    public void addCategory(Category category) {
        categories.add(category);
    }

    public void addCategories(Category... categories) {
        addCategories(Arrays.asList(categories));
    }

    public void addCategories(List<Category> categories) {
        this.categories.addAll(categories);
    }

    public void removeCategory(Category category) {
        categories.remove(category);
    }

    public void removeCategories(Category... categories) {
        removeCategories(Arrays.asList(categories));
    }

    public void removeCategories(List<Category> categories) {
        this.categories.removeAll(categories);
    }

    public void clearCategories() {
        categories.clear();
    }

    public boolean containsCategory(Category category) {
        return categories.contains(category);
    }

    public boolean containsCategory(String name) {
        for (Category category : categories) {
            if (category.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }
}
