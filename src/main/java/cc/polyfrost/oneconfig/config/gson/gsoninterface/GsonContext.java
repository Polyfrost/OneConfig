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

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.JsonTreeReader;
import com.google.gson.internal.bind.JsonTreeWriter;

import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author mintern
 */
public class GsonContext<T> {
    private final Gson gson;
    private final InterfaceAdapterFactory.InterfaceTypeAdapter<T> constructingAdapter;

    public GsonContext(Gson g, InterfaceAdapterFactory.InterfaceTypeAdapter<T> ita) {
        gson = g;
        constructingAdapter = ita;
    }

    public JsonElement toJsonTree(Object obj) {
        return gson.toJsonTree(obj);
    }

    public JsonElement thisToJsonTree(T obj) throws JsonIOException {
        JsonTreeWriter writer = new JsonTreeWriter();
        try {
            constructingAdapter.getDelegate().write(writer, obj);
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
        return writer.get();
    }

    public <C> C fromJsonTree(JsonElement json, Class<C> type) {
        return gson.fromJson(json, type);
    }

    public <C> C fromJsonTree(JsonElement json, Type typeOfC) {
        return (C) gson.fromJson(json, typeOfC);
    }

    public T thisFromJsonTree(JsonElement json) throws JsonIOException {
        try {
            return constructingAdapter.getDelegate().read(new JsonTreeReader(json));
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }

    public <C extends T> C thisFromJsonTree(JsonElement json, Type typeOfC) {
        TypeAdapter<C> adapter = constructingAdapter.getNextAdapter(typeOfC);
        try {
            return adapter.read(new JsonTreeReader(json));
        } catch (IOException e) {
            throw new JsonIOException(e);
        }
    }
}
