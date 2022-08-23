package cc.polyfrost.oneconfig.config.gson.gsoninterface;

/*
 * This file is part of OneConfig.
 * OneConfig - Next Generation Config Library for Minecraft: Java Edition
 * Copyright (C) 2021, 2022 Polyfrost.
 *
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

 * This file contains an adaptation of code from gson-interface
 * Project found at <https://github.com/mintern/gson-interface>
 * For the avoidance of doubt, this file is still licensed under the terms
 * of OneConfig's Licensing.
 *
 *                 LICENSE NOTICE FOR ADAPTED CODE
 *
 * Copyright (C) 2012, Brandon Mintern, EasyESI, Berkeley, CA
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *   * Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *   * Neither gson-interface nor the names of its contributors may be used
 *     to endorse or promote products derived from this software without
 *     specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL BRANDON MINTERN OR EASYESI BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF
 * THE POSSIBILITY OF SUCH DAMAGE.
 */

import com.google.gson.*;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mintern
 */
public class InterfaceAdapterFactory implements TypeAdapterFactory {
    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> tt) {
        Class<T> rawType = Reflection.classOfType(tt.getRawType());
        boolean serializes = JsonSerialization.class.isAssignableFrom(rawType);
        Constructor<JsonDeserializes<T>> deserializerConstructor = null;
        Class<JsonDeserializes<T>>[] typeParameters = Reflection.getTypeParameters(rawType, JsonDeserialization.class);
        if (typeParameters != null) {
            deserializerConstructor = Reflection.getConstructor(typeParameters[0]);
        }
        if (serializes || deserializerConstructor != null) {
            return new InterfaceTypeAdapter(serializes, deserializerConstructor, gson, tt, this);
        }
        return null;
    }

    public static class InterfaceTypeAdapter<T> extends TypeAdapter<T> {
        // This map ensures that only one deserializer of each type exists.
        private static final Map<Class, JsonDeserializes<?>> deserializerInstances = new HashMap();

        // Fields set in the constructor
        private final boolean selfSerializing;
        private final Constructor<JsonDeserializes<T>> deserializerConstructor;
        private final Gson gson;
        private final TypeToken<T> typeToken;
        private final TypeAdapterFactory thisFactory;

        // Adapters that follow this one in the chain for the indicated type
        private final Map<Type, TypeAdapter> nextAdapters = new HashMap();

        // Lazily-initialized fields. Call their corresponding getters in
        // order to access them.
        private TypeAdapter<T> delegate;
        private GsonContext gsonContext;

        private InterfaceTypeAdapter(
                boolean serializes,
                Constructor<JsonDeserializes<T>> dsc,
                Gson g,
                TypeToken<T> tt,
                TypeAdapterFactory factory) {
            selfSerializing = serializes;
            if (dsc != null) {
                dsc.setAccessible(true);
            }
            deserializerConstructor = dsc;
            gson = g;
            typeToken = tt;
            thisFactory = factory;
        }

        @Override
        public void write(JsonWriter writer, T value) throws IOException {
            if (!selfSerializing) {
                getDelegate().write(writer, value);
            } else if (value == null) {
                writer.nullValue();
            } else {
                JsonElement tree = ((JsonSerialization) value).toJsonTree(gsonContext());
                Streams.write(tree, writer);
            }
        }

        @Override
        public T read(JsonReader reader) throws IOException {
            if (deserializerConstructor == null) {
                return getDelegate().read(reader);
            }
            JsonElement json = Streams.parse(reader);
            if (json.isJsonNull()) {
                return null;
            }
            return (T) deserializer().fromJsonTree(json, typeToken.getType(), gsonContext());
        }

        synchronized TypeAdapter<T> getDelegate() {
            if (delegate == null) {
                delegate = gson.getDelegateAdapter(thisFactory, typeToken);
            }
            return delegate;
        }

        private synchronized GsonContext gsonContext() {
            if (gsonContext == null) {
                gsonContext = new GsonContext(gson, this);
            }
            return gsonContext;
        }

        synchronized <C extends T> TypeAdapter<C> getNextAdapter(Type typeOfC) {
            TypeAdapter<C> nextAdapter = nextAdapters.get(typeOfC);
            if (nextAdapter == null) {
                nextAdapter = gson.getDelegateAdapter(thisFactory, (TypeToken<C>) TypeToken.get(typeOfC));
                nextAdapters.put(typeOfC, nextAdapter);
            }
            return nextAdapter;
        }

        private JsonDeserializes<T> deserializer() {
            synchronized (deserializerInstances) {
                Class<JsonDeserializes<T>> c = deserializerConstructor.getDeclaringClass();
                JsonDeserializes<T> deserializer = (JsonDeserializes<T>) deserializerInstances.get(c);
                if (deserializer == null) {
                    try {
                        deserializer = deserializerConstructor.newInstance();
                    } catch (Exception e) {
                        throw new JsonParseException(e);
                    }
                    deserializerInstances.put(c, deserializer);
                }
                return deserializer;
            }
        }
    }
}
