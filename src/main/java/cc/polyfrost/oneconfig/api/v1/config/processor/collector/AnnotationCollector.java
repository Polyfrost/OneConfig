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

package cc.polyfrost.oneconfig.api.v1.config.processor.collector;

import cc.polyfrost.oneconfig.api.v1.config.OneConfig;
import cc.polyfrost.oneconfig.api.v1.config.option.OptionHolder;
import cc.polyfrost.oneconfig.api.v1.config.option.OptionManager;
import cc.polyfrost.oneconfig.api.v1.config.option.type.OptionType;
import cc.polyfrost.oneconfig.api.v1.config.option.type.annotations.Accordion;
import cc.polyfrost.oneconfig.utils.ConfigUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AnnotationCollector implements Collector {
    @Override
    public List<OptionHolder> collect(OneConfig config) {
        Field[] fields = config.getClass().getDeclaredFields();
        Method[] methods = config.getClass().getDeclaredMethods();
        List<OptionHolder> options = new ArrayList<>(fields.length + methods.length);
        for (Field field : fields) {
            OptionType.TypeTarget annotation = ConfigUtils.findAnnotation(field, OptionType.TypeTarget.class);
            if (annotation != null) {
                OptionType optionType = OptionManager.INSTANCE.getOption(annotation.value());
                if (optionType == null) {
                    throw new RuntimeException("Option " + annotation.value() + " does not exist!");
                }
                options.add(new FieldAnnotationOptionHolder(optionType, field, config));
            }
        }
        for (Method method : methods) {
            OptionType.TypeTarget annotation = ConfigUtils.findAnnotation(method, OptionType.TypeTarget.class);
            if (annotation != null) {
                OptionType optionType = OptionManager.INSTANCE.getOption(annotation.value());
                if (optionType == null) {
                    throw new RuntimeException("Option " + annotation.value() + " does not exist!");
                }
                options.add(new MethodAnnotationOptionHolder(optionType, method, config));
            }
        }
        return options;
    }

    private static final class FieldAnnotationOptionHolder implements OptionHolder {
        private final OptionType optionType;
        private final Field field;
        private final Object instance;

        public FieldAnnotationOptionHolder(OptionType optionType, Field field, Object instance) {
            this.optionType = optionType;
            this.field = field;
            this.instance = instance;
        }

        @Override
        public OptionType getOptionType() {
            return optionType;
        }

        @Override
        public Object invoke() {
            return ConfigUtils.getField(field, instance);
        }

        @Override
        public void set(Object value) {
            ConfigUtils.setField(field, value, instance);
        }

        @Override
        public String getJavaName() {
            if (!optionType.serializable()) {
                throw new RuntimeException("Option " + optionType.name() + " is not serializable!");
            }
            return field.getName();
        }

        @Override
        public String getDisplayName() {
            return optionType.getName(field);
        }

        @Override
        public String getDisplayCategory() {
            return optionType.getCategory(field);
        }

        @Override
        public String getDisplaySubcategory() {
            return optionType.getSubcategory(field);
        }

        @Override
        public String[] getSearchTags() {
            return optionType.getTags(field);
        }

        @Override
        public String getDescription() {
            return optionType.getDescription(field);
        }

        @Override
        public Class<?> getJavaClass() {
            return field.getType();
        }

        @Override
        public boolean isAccordion() {
            return field.isAnnotationPresent(Accordion.class);
        }

        @Override
        public Accordion getAccordion() {
            return field.getAnnotation(Accordion.class);
        }
    }

    private static final class MethodAnnotationOptionHolder implements OptionHolder {
        private final OptionType optionType;
        private final Method method;
        private final Object instance;

        public MethodAnnotationOptionHolder(OptionType optionType, Method method, Object instance) {
            this.optionType = optionType;
            this.method = method;
            this.instance = instance;
        }

        @Override
        public OptionType getOptionType() {
            return optionType;
        }

        @Override
        public Object invoke() {
            return ConfigUtils.invokeMethod(method, instance);
        }

        @Override
        public void set(Object value) {
            throw new UnsupportedOperationException("Cannot set a method option!");
        }

        @Override
        public String getJavaName() {
            if (!optionType.serializable()) {
                throw new RuntimeException("Option " + optionType.name() + " is not serializable!");
            }
            return method.getName();
        }

        @Override
        public String getDisplayName() {
            return optionType.getName(method);
        }

        @Override
        public String getDisplayCategory() {
            return optionType.getCategory(method);
        }

        @Override
        public String getDisplaySubcategory() {
            return optionType.getSubcategory(method);
        }

        @Override
        public String[] getSearchTags() {
            return optionType.getTags(method);
        }

        @Override
        public String getDescription() {
            return optionType.getDescription(method);
        }

        @Override
        public Class<?> getJavaClass() {
            return Void.TYPE;
        }

        @Override
        public boolean isAccordion() {
            return method.isAnnotationPresent(Accordion.class);
        }

        @Override
        public Accordion getAccordion() {
            return method.getAnnotation(Accordion.class);
        }
    }
}
