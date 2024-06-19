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

package org.polyfrost.oneconfig.utils.v1;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

/**
 * An Image wrapper class that is used by the OneConfig system.
 */
@SuppressWarnings("unused")
@ApiStatus.Experimental
public final class OneImage {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig/Images");
    public final int width, height;
    public final BufferedImage image;
    private Graphics2D graphics = null;

    /**
     * Create a new OneImage from the BufferedImage.
     */
    public OneImage(BufferedImage image) {
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();
    }

    /**
     * Create a new OneImage from the file.
     *
     * @param is InputStream to the image file.
     */
    public OneImage(InputStream is) throws IOException {
        this(ImageIO.read(is));
    }

    /**
     * Create a new OneImage from the file.
     *
     * @param path path to the image file.
     */
    public OneImage(Path path) throws IOException {
        this(new BufferedInputStream(Files.newInputStream(path)));
    }

    /**
     * Create a new blank image with the specified width and height.
     */
    public OneImage(int width, int height) {
        this(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB));
    }

    /**
     * @return the graphics object associated with the image.
     */
    public Graphics2D getG2D() {
        if (graphics == null) {
            graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        return graphics;
    }


    /**
     * Dispose of the graphics object.
     */
    public void dispose() {
        if (graphics != null) {
            graphics.dispose();
            graphics = null;
        }
    }

    /**
     * Copy and crop the image to the specified width and height.
     *
     * @param startX The x coordinate of the top-left corner of the crop.
     * @param startY The y coordinate of the top-left corner of the crop.
     * @param width  The width of the crop.
     * @param height The height of the crop.
     */
    public OneImage crop(int startX, int startY, int width, int height) {
        return new OneImage(image.getSubimage(startX, startY, width, height));
    }

    /**
     * Get the color of a pixel in the image.
     */
    public int getColorAtPos(int x, int y) {
        return image.getRGB(x, y);
    }

    /**
     * Set the color of a pixel in the image.
     */
    public void setColorAtPos(int x, int y, int argb) {
        image.setRGB(x, y, argb);
    }

    /**
     * Attempt to save the image to the specified file.
     */
    public void save(Path path) throws IOException {
        ImageIO.write(image, "png", new BufferedOutputStream(Files.newOutputStream(path)));
    }

    /**
     * Attempt to upload the image to Imgur, returning the URL that the server replied with, or null if it failed for any reason.
     */
    public String uploadToImgur() {
        try {
            // thanks stack overflow for the help with this :_)
            URL url = new URL("https://api.imgur.com/3/image");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("User-Agent", NetworkUtils.DEF_AGENT);
            con.setRequestProperty("Authorization", "Client-ID " + "6cfc432a9954f4d");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.connect();
            ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
            ImageIO.write(image, "png", byteOut);
            String encoded = URLEncoder.encode("image", "UTF-8") + "=" + URLEncoder.encode(Base64.getEncoder().encodeToString(byteOut.toByteArray()), "UTF-8");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(con.getOutputStream()));
            writer.write(encoded);
            byteOut.close();
            writer.close();
            if (con.getResponseCode() != 200) {
                LOGGER.error("Error uploading image to Imgur: {}", con.getResponseCode());
                return null;
            }

            try (BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                String ln;
                while ((ln = rd.readLine()) != null) {
                    if (!ln.contains("\"link\": ")) continue;
                    int e = ln.lastIndexOf('\"') - 1;
                    int s = ln.lastIndexOf('\"', e);
                    return ln.substring(s, e);
                }
            }

            return null;
        } catch (Exception e) {
            LOGGER.error("Error uploading image to Imgur!", e);
            return null;
        }
    }

    /**
     * Copy the image to the system clipboard and delete the graphics object.
     */
    public void copyToClipboard() {
        IOUtils.copyImageToClipboard(image);
        dispose();
    }

    // MASK METHODS
    public void setBrightness(float brightness) {
        maskColor(new Color(0f, 0f, 0f, brightness));
    }

    public void maskColor(Color color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(color);
        g2d.fillRect(0, 0, width, height);
        dispose();
    }

    public void maskPaint(Paint paint) {
        Graphics2D g2d = getG2D();
        g2d.setPaint(paint);
        g2d.fillRect(0, 0, width, height);
        dispose();
    }


    // IMAGE METHODS

    /**
     * Return a new OneImage, with the image scaled by the given factor (1.0 = no change).
     */
    public OneImage scale(double sx, double sy) {
        OneImage out = new OneImage((int) (width * sx), (int) (height * sy));
        Graphics2D g2d = out.getG2D();
        g2d.drawImage(image, new AffineTransformOp(AffineTransform.getScaleInstance(sx, sy), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        out.dispose();
        return out;
    }

    /**
     * Return a copy of this OneImage with the image scaled to the specified width and height.
     */
    public OneImage scaleToSize(int width, int height) {
        return scale(width / (double) this.width, height / (double) this.height);
    }

    /**
     * Return a copy of this OneImage with the image rotated by the specified number of degrees.
     */
    public OneImage rotate(double degrees) {
        OneImage out = new OneImage(width, height);
        Graphics2D g2d = out.getG2D();
        g2d.drawImage(image, new AffineTransformOp(AffineTransform.getRotateInstance(Math.toRadians(degrees)), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        out.dispose();
        return out;
    }

    /**
     * Return a copy of this OneImage flipped horizontally.
     */
    public OneImage flipHorizontal() {
        return scale(-1, 1);
    }

    /**
     * Return a copy of this OneImage flipped vertically.
     */
    public OneImage flipVertical() {
        return scale(1, -1);
    }
}
