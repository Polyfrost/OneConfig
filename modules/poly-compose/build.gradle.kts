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

plugins {
    id("org.jetbrains.kotlin.plugin.compose")
}

val skiaStub: SourceSet = sourceSets.create("skiaStub")

dependencies {
    compileOnly(skiaStub.output)
    compileOnly(libs.jetbrains.compose.runtime)
    compileOnly(libs.kotlinx.coroutines)
    compileOnly(libs.icu4j)
    api(libs.jetbrains.compose.foundation)
    api(libs.jetbrains.compose.material)
    api(libs.jetbrains.compose.ui)
    api(libs.jetbrains.compose.ui.util)
    implementation(libs.bundles.twelvemonkeys)
    testImplementation(libs.jetbrains.compose.runtime)
    testImplementation(libs.kotlinx.coroutines)
    testImplementation(libs.icu4j)
}
