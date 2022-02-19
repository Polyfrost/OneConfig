package io.polyfrost.oneconfig.renderer;

import net.minecraft.client.renderer.GlStateManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;


/**
 * A TrueType font implementation originally for Slick, then edited for Bobjob's Engine, now for Minecraft
 *
 * @author James Chambers (Jimmy) (original in Slick)
 * @author Jeremy Adams (elias4444) (original in Slick)
 * @author Kevin Glass (kevglass) (original in Slick)
 * @author Peter Korzuszek (genail) (original in Slick)
 * @author version edited by David Aaron Muhar (bobjob) (modified in Bobjob's Engine)
 */
public class TrueTypeFont {
    public final static int
            ALIGN_LEFT = 0,
            ALIGN_RIGHT = 1,
            ALIGN_CENTER = 2;
    /**
     * Array that holds necessary information about the font characters
     */
    private final IntObject[] charArray = new IntObject[256];

    /**
     * Map of user defined font characters (Character <-> IntObject)
     */
    private final Map<Character, IntObject> customChars = new HashMap<>();

    /**
     * Boolean flag on whether AntiAliasing is enabled or not
     */
    private final boolean antiAlias;

    /**
     * Font's size
     */
    private final int fontSize;

    /**
     * Font's height
     */
    private int fontHeight = 0;

    /**
     * Texture used to cache the font 0-255 characters
     */
    private int fontTextureID;

    /**
     * Default font texture width
     */
    private final int textureWidth = 512;

    /**
     * Default font texture height
     */
    private final int textureHeight = 512;

    /**
     * A reference to Java's AWT Font that we create our font texture from
     */
    private final Font font;


    private static class IntObject {
        /**
         * Character's width
         */
        public int width;

        /**
         * Character's height
         */
        public int height;

        /**
         * Character's stored x position
         */
        public int storedX;

        /**
         * Character's stored y position
         */
        public int storedY;
    }


    public TrueTypeFont(Font font, boolean antiAlias, char[] additionalChars) {
        this.font = font;
        this.fontSize = font.getSize() + 3;
        this.antiAlias = antiAlias;

        createSet(additionalChars);

        fontHeight -= 1;
        if (fontHeight <= 0) fontHeight = 1;
    }

    public TrueTypeFont(Font font, boolean antiAlias) {
        this(font, antiAlias, null);
    }

    private BufferedImage getFontImage(char ch) {
        // Create a temporary image to extract the character's size
        BufferedImage tempfontImage = new BufferedImage(1, 1,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) tempfontImage.getGraphics();
        if (antiAlias) {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        g.setFont(font);
        FontMetrics fontMetrics = g.getFontMetrics();
        int charwidth = fontMetrics.charWidth(ch) + 8;

        if (charwidth <= 0) {
            charwidth = 7;
        }
        int charheight = fontMetrics.getHeight() + 3;
        if (charheight <= 0) {
            charheight = fontSize;
        }

        // Create another image holding the character we are creating
        BufferedImage fontImage;
        fontImage = new BufferedImage(charwidth, charheight,
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D gt = (Graphics2D) fontImage.getGraphics();
        if (antiAlias) {
            gt.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
        }
        gt.setFont(font);

        gt.setColor(Color.WHITE);
        int charx = 3;
        int chary = 1;
        gt.drawString(String.valueOf(ch), (charx), (chary)
                + fontMetrics.getAscent());

        return fontImage;

    }

    private void createSet(char[] customCharsArray) {
        try {
            BufferedImage imgTemp = new BufferedImage(textureWidth, textureHeight, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = (Graphics2D) imgTemp.getGraphics();

            g.setColor(new Color(0, 0, 0, 1));
            g.fillRect(0, 0, textureWidth, textureHeight);

            int customCharsLength = (customCharsArray != null) ? customCharsArray.length : 0;
            int rowHeight = 0;
            int positionX = 0;
            int positionY = 0;

            // ignore some characters because they don't have visual representation
            for (int i = 0; i < 224 + customCharsLength; i++) {
                if (i >= 95 && i <= 128) continue;

                char ch = (i < 224) ? (char) (i + 32) : customCharsArray[i - 224];

                BufferedImage fontImage = getFontImage(ch);

                IntObject newIntObject = new IntObject();

                newIntObject.width = fontImage.getWidth();
                newIntObject.height = fontImage.getHeight();

                if (positionX + newIntObject.width >= textureWidth) {
                    positionX = 0;
                    positionY += rowHeight;
                    rowHeight = 0;
                }

                newIntObject.storedX = positionX;
                newIntObject.storedY = positionY;

                if (newIntObject.height > fontHeight) {
                    fontHeight = newIntObject.height;
                }

                if (newIntObject.height > rowHeight) {
                    rowHeight = newIntObject.height;
                }

                // Draw it here
                g.drawImage(fontImage, positionX, positionY, null);

                positionX += newIntObject.width;

                if (i < 224) { // standard characters
                    charArray[i + 32] = newIntObject;
                } else { // custom characters
                    customChars.put(ch, newIntObject);
                }
            }

            fontTextureID = loadImage(imgTemp);

            //ImageIO.write(imgTemp, "png", new File("./OneConfig/bitmap.png"));
        } catch (Exception e) {
            System.err.println("Failed to create font.");
            e.printStackTrace();
        }
    }

    private void drawQuad(float drawX, float drawY, float drawX2, float drawY2,
                          float srcX, float srcY, float srcX2, float srcY2) {
        float DrawWidth = drawX2 - drawX;
        float DrawHeight = drawY2 - drawY;
        float TextureSrcX = srcX / textureWidth;
        float TextureSrcY = srcY / textureHeight;
        float SrcWidth = srcX2 - srcX;
        float SrcHeight = srcY2 - srcY;
        float RenderWidth = (SrcWidth / textureWidth);
        float RenderHeight = (SrcHeight / textureHeight);

        GlStateManager.bindTexture(fontTextureID);

        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        GL11.glTexCoord2f(TextureSrcX + RenderWidth, TextureSrcY); // 2
        GL11.glVertex2f(drawX + DrawWidth, drawY + DrawHeight); // 1
        GL11.glTexCoord2f(TextureSrcX, TextureSrcY); // 1
        GL11.glVertex2f(drawX, drawY + DrawHeight); // 2
        GL11.glTexCoord2f(TextureSrcX + RenderWidth, TextureSrcY + RenderHeight); // 4
        GL11.glVertex2f(drawX + DrawWidth, drawY); // 3
        GL11.glTexCoord2f(TextureSrcX, TextureSrcY + RenderHeight); // 3
        GL11.glVertex2f(drawX, drawY); // 4
        GL11.glEnd();
    }

    public int getWidth(String text) {
        int totalWidth = 0;
        IntObject intObject = null;
        int currentChar = 0;
        for (int i = 0; i < text.length(); i++) {
            currentChar = text.charAt(i);
            if (currentChar < 256) {
                intObject = charArray[currentChar];
            } else {
                intObject = customChars.get((char) currentChar);
            }

            if (intObject != null)
                totalWidth += intObject.width;
        }
        return totalWidth;
    }

    public int getHeight() {
        return fontHeight;
    }

    public void drawString(String text, float x, float y, float scaleX, float scaleY, int color) {
        drawString(text, x, y, scaleX, scaleY, ALIGN_LEFT, color);
    }


    public void drawString(String text, float x, float y, float scaleX, float scaleY, int format, int color) {
        float f = (float) (color >> 16 & 255) / 255.0F;
        float f1 = (float) (color >> 8 & 255) / 255.0F;
        float f3 = (float) (color >> 24 & 255) / 255.0F;
        float f2 = (float) (color & 255) / 255.0F;
        int startIndex = 0;
        int endIndex = text.length() - 1;
        IntObject intObject;
        int charCurrent;


        int totalWidth = 0;
        int i = startIndex, d, c;
        float startY = 0;

        switch (format) {
            case ALIGN_RIGHT: {
                d = -1;
                c = 8;

                while (i < endIndex) {
                    if (text.charAt(i) == '\n') startY -= fontHeight;
                    i++;
                }
                break;
            }
            case ALIGN_CENTER: {
                for (int l = startIndex; l <= endIndex; l++) {
                    charCurrent = text.charAt(l);
                    if (charCurrent == '\n') break;
                    if (charCurrent < 256) {
                        intObject = charArray[charCurrent];
                    } else {
                        intObject = customChars.get((char) charCurrent);
                    }
                    totalWidth += intObject.width - 9;
                }
                totalWidth /= -9;
            }
            case ALIGN_LEFT:
            default: {
                d = 1;
                c = 9;
                break;
            }

        }

        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();

        GlStateManager.color(f, f1, f2, f3);
        while (i >= startIndex && i <= endIndex) {
            charCurrent = text.charAt(i);
            if (charCurrent < 256) {
                intObject = charArray[charCurrent];
            } else {
                intObject = customChars.get((char) charCurrent);
            }

            if (intObject != null) {
                if (d < 0) totalWidth += (intObject.width - c) * d;
                drawQuad((totalWidth + intObject.width) * scaleX + x, startY * scaleY + y,
                        totalWidth * scaleX + x,
                        (startY + intObject.height) * scaleY + y, intObject.storedX + intObject.width,
                        intObject.storedY + intObject.height, intObject.storedX,
                        intObject.storedY);
                if (d > 0) totalWidth += (intObject.width - c) * d;
            } else if (charCurrent == '\n') {
                startY += fontHeight * d;
                totalWidth = 0;
                if (format == ALIGN_CENTER) {
                    for (int l = i + 1; l <= endIndex; l++) {
                        charCurrent = text.charAt(l);
                        if (charCurrent == '\n') break;
                        if (charCurrent < 256) {
                            intObject = charArray[charCurrent];
                        } else {
                            intObject = customChars.get((char) charCurrent);
                        }
                        totalWidth += intObject.width - 9;
                    }
                    totalWidth /= -2;
                }
            }
            i += d;
        }
        GlStateManager.disableBlend();
        GlStateManager.color(1f,1f,1f,1f);
    }

    public static int loadImage(BufferedImage bufferedImage) {
        try {
            short width = (short) bufferedImage.getWidth();
            short height = (short) bufferedImage.getHeight();
            //textureLoader.bpp = bufferedImage.getColorModel().hasAlpha() ? (byte)32 : (byte)24;
            int bpp = (byte) bufferedImage.getColorModel().getPixelSize();
            ByteBuffer byteBuffer;
            DataBuffer db = bufferedImage.getData().getDataBuffer();
            if (db instanceof DataBufferInt) {
                int[] intI = ((DataBufferInt) (bufferedImage.getData().getDataBuffer())).getData();
                byte[] newI = new byte[intI.length * 4];
                for (int i = 0; i < intI.length; i++) {
                    byte[] b = intToByteArray(intI[i]);
                    int newIndex = i * 4;

                    newI[newIndex] = b[1];
                    newI[newIndex + 1] = b[2];
                    newI[newIndex + 2] = b[3];
                    newI[newIndex + 3] = b[0];
                }

                byteBuffer = ByteBuffer.allocateDirect(
                                width * height * (bpp / 8))
                        .order(ByteOrder.nativeOrder())
                        .put(newI);
            } else {
                byteBuffer = ByteBuffer.allocateDirect(
                                width * height * (bpp / 8))
                        .order(ByteOrder.nativeOrder())
                        .put(((DataBufferByte) (bufferedImage.getData().getDataBuffer())).getData());
            }
            byteBuffer.flip();


            int internalFormat = GL11.GL_RGBA8,
                    format = GL11.GL_RGBA;
            IntBuffer textureId = BufferUtils.createIntBuffer(1);

            GL11.glGenTextures(textureId);
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId.get(0));


            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_CLAMP);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_CLAMP);

            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);

            GL11.glTexEnvf(GL11.GL_TEXTURE_ENV, GL11.GL_TEXTURE_ENV_MODE, GL11.GL_MODULATE);


            GLU.gluBuild2DMipmaps(GL11.GL_TEXTURE_2D,
                    internalFormat,
                    width,
                    height,
                    format,
                    GL11.GL_UNSIGNED_BYTE,
                    byteBuffer);
            return textureId.get(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static boolean isSupported(String fontname) {
        Font[] font = getFonts();
        for (int i = font.length - 1; i >= 0; i--) {
            if (font[i].getName().equalsIgnoreCase(fontname))
                return true;
        }
        return false;
    }

    public static Font[] getFonts() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts();
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 24),
                (byte) (value >>> 16),
                (byte) (value >>> 8),
                (byte) value};
    }

    public void destroy() {
        IntBuffer scratch = BufferUtils.createIntBuffer(1);
        scratch.put(0, fontTextureID);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);
        GL11.glDeleteTextures(scratch);
    }
}
