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

package cc.polyfrost.oneconfig.internal.assets;

import java.awt.*;

@SuppressWarnings("unused")
public class Colors {
    // the color library
    public static final int TRANSPARENT = new Color(0, 0, 0, 0).getRGB();                // Transparent
    public static final int BLACK = new Color(0, 0, 0, 255).getRGB();                     // Black
    public static final int GRAY_900 = new Color(13, 14, 15, 255).getRGB();           // Gray 900
    public static final int GRAY_900_80 = new Color(13, 14, 15, 204).getRGB();         // Gray 900 80%
    public static final int GRAY_800 = new Color(21, 22, 23, 255).getRGB();           // Gray 800
    public static final int GRAY_800_95 = new Color(21, 22, 23, 242).getRGB();
    public static final int GRAY_850 = new Color(21, 23, 25, 255).getRGB();           // Gray 850
    public static final int GRAY_700 = new Color(34, 35, 38, 255).getRGB();           // Gray 700
    public static final int GRAY_600 = new Color(42, 44, 48, 255).getRGB();           // Gray 600
    public static final int GRAY_500 = new Color(49, 51, 56, 255).getRGB();           // Gray 500         // button sidebar hover, button gray normal
    public static final int GRAY_500_80 = new Color(49, 51, 56, 204).getRGB();        // Gray 500 80%     // button sidebar pressed
    public static final int GRAY_400 = new Color(55, 59, 69, 255).getRGB();           // Gray 400
    public static final int GRAY_400_40 = new Color(55, 59, 69, 102).getRGB();        // Gray 400 40%
    public static final int GRAY_400_60 = new Color(55, 59, 69, 153).getRGB();        // Gray 400 60%
    public static final int GRAY_300 = new Color(73, 79, 92, 255).getRGB();           // Gray 300         // button gray hover
    public static final int GRAY_400_80 = new Color(55, 59, 69, 204).getRGB();        // Gray 400 80%     // button gray pressed
    public static final int PRIMARY_800 = new Color(13, 51, 128, 255).getRGB();          // Blue 800
    public static final int PRIMARY_700 = new Color(18, 71, 178, 255).getRGB();          // Blue 700
    public static final int PRIMARY_700_80 = new Color(18, 71, 178, 204).getRGB();       // Blue 700 80%
    public static final int PRIMARY_600 = new Color(20, 82, 204, 255).getRGB();          // Blue 600         // button blue normal
    public static final int PRIMARY_500 = new Color(25, 103, 255, 255).getRGB();         // Blue 500         // button blue hover
    public static final int PRIMARY_400 = new Color(48, 129, 242, 255).getRGB();
    public static final int WHITE_50 = new Color(255, 255, 255, 127).getRGB();        // White 50%
    public static final int WHITE_60 = new Color(255, 255, 255, 153).getRGB();        // White 60%
    public static final int WHITE_80 = new Color(255, 255, 255, 204).getRGB();        // White 80%
    public static final int WHITE_90 = new Color(255, 255, 255, 229).getRGB();        // White 90%
    public static final int WHITE_95 = new Color(255, 255, 255, 242).getRGB();        // White 95%
    public static final int WHITE = new Color(255, 255, 255, 255).getRGB();           // White 100%
    public static final int SUCCESS_600 = new Color(3, 152, 85).getRGB();
    public static final int SUCCESS_700 = new Color(2, 121, 72).getRGB();
    public static final int WARNING_500 = new Color(247, 144, 9).getRGB();
    public static final int WARNING_600 = new Color(220, 104, 3).getRGB();
    public static final int ERROR_600_80 = new Color(217, 32, 32, 204).getRGB();
    public static final int ERROR_600 = new Color(217, 32, 32).getRGB();
    public static final int ERROR_700 = new Color(180, 24, 24).getRGB();         // Red 700
    public static final int ERROR_800 = new Color(145, 24, 24).getRGB();         // Red 800
    public static final int ERROR_800_80 = new Color(145, 24, 24, 204).getRGB();         // Red 800
    public static final int ERROR_300 = new Color(253, 155, 155).getRGB();
    public static final int ERROR_300_80 = new Color(253, 155, 155, 204).getRGB();

    /** List of all colors used by Minecraft in its code.
     * Source: <a href="https://www.digminecraft.com/lists/color_list_pc.php">Click Here</a>
     * */
    public static class MinecraftColors {
        /** Letter Code used before colors in Minecraft */
        public static final String CODE = "\u00a7";

        public static final String RESET = "\u00a70";

        public static final int DARK_RED = new Color(170, 0, 0).getRGB();           // Mapped to 4
        public static final int RED = new Color(255, 85, 85).getRGB();              // Mapped to c
        public static final int GOLD = new Color(255, 170, 0).getRGB();             // Mapped to 6
        public static final int YELLOW = new Color(255, 255, 85).getRGB();          // Mapped to e
        public static final int DARK_GREEN = new Color(0, 170, 0).getRGB();         // Mapped to 2
        public static final int GREEN = new Color(85, 255, 85).getRGB();            // Mapped to a
        public static final int AQUA = new Color(85, 255, 255).getRGB();            // Mapped to b
        public static final int DARK_AQUA = new Color(0, 170, 170).getRGB();        // Mapped to 3
        public static final int DARK_BLUE = new Color(0, 0, 170).getRGB();          // Mapped to 1
        public static final int BLUE = new Color(85, 85, 255).getRGB();             // Mapped to 9
        public static final int LIGHT_PURPLE = new Color(255, 85, 255).getRGB();    // Mapped to d
        public static final int DARK_PURPLE = new Color(170, 0, 170).getRGB();      // Mapped to 5
        public static final int WHITE = new Color(255, 255, 255).getRGB();          // Mapped to f
        public static final int GRAY = new Color(170, 170, 170).getRGB();           // Mapped to 7
        public static final int DARK_GRAY = new Color(85, 85, 85).getRGB();         // Mapped to 8
        public static final int BLACK = new Color(0, 0, 0).getRGB();                // Mapped to 0
    }
}
