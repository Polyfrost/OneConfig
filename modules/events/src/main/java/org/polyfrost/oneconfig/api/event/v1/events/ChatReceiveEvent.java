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

package org.polyfrost.oneconfig.api.event.v1.events;

import org.polyfrost.oneconfig.api.platform.v1.Platform;

/**
 * Called when a chat message is received.
 */
public class ChatReceiveEvent extends Event.Cancellable {
    /**
     * The message that was received.
     */
    private Object message;

    public ChatReceiveEvent(Object message) {
        this.message = message;
    }

    /**
     * @see org.polyfrost.oneconfig.api.platform.v1.I18nPlatform#getUnformattedText(Object) Platform.i18n().getUnformattedText()
     */
    public String getFullyUnformattedMessage() {
        return Platform.i18n().getUnformattedText(message);
    }

    /**
     * Due to differences across Minecraft versions, this is a Duck method, meaning that it will return the expected type for that minecraft version.
     * <ul>
     *     <li>For legacy forge, this will be a ITextComponent.</li>
     *     <li>For modern forge, this will be a Component.</li>
     *     <li>For fabric, this will be a Text.</li>
     * </ul>
     * <b>Note: the toString() method on the returned object will always be the correct text of the message.</b>
     * @see org.polyfrost.oneconfig.api.platform.v1.I18nPlatform#getUnformattedText(Object) Platform.i18n().getUnformattedText()
     * @see #getFullyUnformattedMessage()
     */
    @SuppressWarnings("unchecked")
    public <T> T getMessage() {
        return (T) message;
    }


    /**
     * Due to differences across Minecraft versions, this is a Duck method, meaning that it expects a different type for different minecraft versions.
     * <ul>
     *     <li>For legacy forge, this will be a ITextComponent.</li>
     *     <li>For modern forge, this will be a Component.</li>
     *     <li>For fabric, this will be a Text.</li>
     * </ul>
     */
    public <T> void setMessage(T message) {
        this.message = message;
    }
}
