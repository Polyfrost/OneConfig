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

package org.polyfrost.oneconfig.api.notifications.v1

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.jetbrains.annotations.ApiStatus
import org.polyfrost.compose.render.PolyColor

object NotificationTheme {
    var background by mutableStateOf(PolyColor.hex("#192126").withAlpha(0.85f))
        private set
    var border by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.05f))
        private set
    var textPrimary by mutableStateOf(PolyColor.hex("#DFEAFF"))
        private set
    var textSecondary by mutableStateOf(PolyColor.hex("#78828F"))
        private set
    var accent by mutableStateOf(PolyColor.hex("#2B4BFF"))
        private set
    var success by mutableStateOf(PolyColor.hex("#3CAF77"))
        private set
    var error by mutableStateOf(PolyColor.hex("#F2545A"))
        private set

    var subtleButton by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.08f))
        private set

    var actionPrimaryText by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.9f))
        private set

    var actionSubtleText by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.9f))
        private set

    var fontTitle by mutableStateOf<String?>("poppins-medium")
        private set

    var fontBody by mutableStateOf<String?>(null)
        private set

    var radiusCard by mutableStateOf(12f)
        private set

    var radiusButton by mutableStateOf(4f)
        private set

    var radiusProgress by mutableStateOf(3f)
        private set

    fun accentFor(type: NotificationType): PolyColor = when (type) {
        NotificationType.INFO -> textPrimary
        NotificationType.SUCCESS -> success
        NotificationType.ERROR -> error
        NotificationType.PROGRESS -> accent
    }

    @ApiStatus.Internal
    fun set(
        background: PolyColor = this.background,
        border: PolyColor = this.border,
        textPrimary: PolyColor = this.textPrimary,
        textSecondary: PolyColor = this.textSecondary,
        accent: PolyColor = this.accent,
        subtleButton: PolyColor = this.subtleButton,
        actionPrimaryText: PolyColor = this.actionPrimaryText,
        actionSubtleText: PolyColor = this.actionSubtleText,
        fontTitle: String? = this.fontTitle,
        fontBody: String? = this.fontBody,
        radiusCard: Float = this.radiusCard,
        radiusButton: Float = this.radiusButton,
        radiusProgress: Float = this.radiusProgress,
    ) {
        this.background = background
        this.border = border
        this.textPrimary = textPrimary
        this.textSecondary = textSecondary
        this.accent = accent
        this.subtleButton = subtleButton
        this.actionPrimaryText = actionPrimaryText
        this.actionSubtleText = actionSubtleText
        this.fontTitle = fontTitle
        this.fontBody = fontBody
        this.radiusCard = radiusCard
        this.radiusButton = radiusButton
        this.radiusProgress = radiusProgress
    }
}
