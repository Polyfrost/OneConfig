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

package org.polyfrost.oneconfig.utils.dsl

import org.polyfrost.oneconfig.libs.universal.UDesktop
import org.polyfrost.oneconfig.utils.NetworkUtils
import java.io.File

/**
 * Downloads the given [url] to the given [File].
 *
 * @see NetworkUtils.downloadFile
 */
fun File.download(url: String, userAgent: String = "OneConfig/1.0.0", timeout: Int = 5000, useCaches: Boolean = false) =
    NetworkUtils.downloadFile(url, this, userAgent, timeout, useCaches)

/**
 * Launches a URL in the default browser.
 *
 * @see NetworkUtils.browseLink
 */
@Suppress("unused", "UnusedReceiverParameter")
fun UDesktop.browseLink(uri: String) = NetworkUtils.browseLink(uri)