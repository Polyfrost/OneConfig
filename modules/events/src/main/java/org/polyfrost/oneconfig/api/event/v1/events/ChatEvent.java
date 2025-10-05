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

import dev.deftu.textile.Text;
import org.polyfrost.oneconfig.api.platform.v1.Platform;

public abstract class ChatEvent extends Event.Cancellable {
    public static final class Send extends ChatEvent {
        public final String message;

        public Send(String message) {
            this.message = message;
        }

        public String component1() {
            return message;
        }
    }

    public static final class Receive extends ChatEvent {
        private Text message;

        public Receive(Text message) {
            this.message = message;
        }

        public String getFullyUnformattedMessage() {
            return Platform.i18n().getUnformattedText(message);
        }

        public Text getMessage() {
            return message;
        }

        public void setMessage(Text message) {
            this.message = message;
        }

        public Text component1() {
            return message;
        }
    }
}
