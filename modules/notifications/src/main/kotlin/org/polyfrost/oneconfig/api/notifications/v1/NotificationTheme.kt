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

public object NotificationTheme {
    public var background: PolyColor by mutableStateOf(PolyColor.hex("#192126").withAlpha(0.85f))
        private set
    public var border: PolyColor by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.05f))
        private set
    public var textPrimary: PolyColor by mutableStateOf(PolyColor.hex("#DFEAFF"))
        private set
    public var textSecondary: PolyColor by mutableStateOf(PolyColor.hex("#78828F"))
        private set
    public var accent: PolyColor by mutableStateOf(PolyColor.hex("#2B4BFF"))
        private set
    public var success: PolyColor by mutableStateOf(PolyColor.hex("#3CAF77"))
        private set
    public var error: PolyColor by mutableStateOf(PolyColor.hex("#F2545A"))
        private set

    public var subtleButton: PolyColor by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.08f))
        private set

    public var actionPrimaryText: PolyColor by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.9f))
        private set

    public var actionSubtleText: PolyColor by mutableStateOf(PolyColor.hex("#FFFFFF").withAlpha(0.9f))
        private set

    public var fontTitle: String? by mutableStateOf<String?>("poppins-medium")
        private set

    public var fontBody: String? by mutableStateOf<String?>(null)
        private set

    public var radiusCard: Float by mutableStateOf(12f)
        private set

    public var radiusButton: Float by mutableStateOf(4f)
        private set

    public var radiusProgress: Float by mutableStateOf(3f)
        private set

    public var iconFolder: String? by mutableStateOf<String?>(null)
        private set

    public fun accentFor(type: NotificationType): PolyColor = when (type) {
        NotificationType.INFO -> textPrimary
        NotificationType.SUCCESS -> success
        NotificationType.ERROR -> error
        NotificationType.PROGRESS -> accent
    }

    @ApiStatus.Internal
    public fun set(
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

    @ApiStatus.Internal
    public fun useIconFolder(iconFolder: String?) {
        this.iconFolder = iconFolder
    }
}
