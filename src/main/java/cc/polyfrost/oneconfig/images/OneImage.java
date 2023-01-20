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

package cc.polyfrost.oneconfig.images;

import cc.polyfrost.oneconfig.utils.IOUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import java.util.Base64;
import java.util.Objects;

/** An Image wrapper class that is used by the OneConfig system.*/
@SuppressWarnings("unused")
@ApiStatus.Experimental
public class OneImage {
    private static final Logger LOGGER = LogManager.getLogger("OneConfig Images");
    private BufferedImage image;
    private Graphics2D graphics = null;
    private final int width, height;

    /**
     * Create a new OneImage from the file. This can be as a resource location inside your JAR.
     * @param filePath The path to the image file.
     */
    public OneImage(String filePath) throws IOException {
        image = ImageIO.read(Objects.requireNonNull(OneImage.class.getResourceAsStream(filePath)));
        width = image.getWidth();
        height = image.getHeight();
    }

    /**
     * Create a new OneImage from the file.
     * @param is InputStream to the image file.
     */
    public OneImage(InputStream is) throws IOException {
        image = ImageIO.read(is);
        width = image.getWidth();
        height = image.getHeight();
    }

    /**
     * Create a new OneImage from the file.
     * @param file File to the image file.
     */
    public OneImage(File file) throws IOException {
            image = ImageIO.read(Objects.requireNonNull(file));
            width = image.getWidth();
            height = image.getHeight();
    }

    /**
     * Create a new OneImage from the BufferedImage.
     */
    public OneImage(BufferedImage image) {
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();
    }

    /** Create a new blank image with the specified width and height. */
    public OneImage(int width, int height) {
        this.width = width;
        this.height = height;
        image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    }

    /** Get the image as a BufferedImage. */
    public BufferedImage getImage() {
        return image;
    }

    protected void setImage(BufferedImage img) {
        image = img;
    }

    /** Get the graphics object associated with the image. */
    public Graphics2D getG2D() {
        if (graphics == null) {
            graphics = image.createGraphics();
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        }
        return graphics;
    }


    /** Dispose of the graphics object. */
    public void dispose() {
        if(graphics != null) {
            graphics.dispose();
            graphics = null;
        }
    }

    /** Get the width of the image. */
    public int getWidth() {
        return width;
    }

    /** Get the height of the image. */
    public int getHeight() {
        return height;
    }

    /** Crop the image to the specified width and height.
     * @param startX The x coordinate of the top-left corner of the crop.
     * @param startY The y coordinate of the top-left corner of the crop.
     * @param width The width of the crop.
     * @param height The height of the crop.
     */
    public void crop(int startX, int startY, int width, int height) {
        image = image.getSubimage(startX, startY, width, height);
    }

    /** Get the color of a pixel in the image. */
    public void getColorAtPos(int x, int y) {
        image.getRGB(x, y);
    }

    /** Set the color of a pixel in the image. */
    public void setColorAtPos(int x, int y, int argb) {
        image.setRGB(x, y, argb);
    }

    /** Attempt to save the image to the specified file. */
    public void save(String filePath) throws IOException {
        ImageIO.write(image, "png", new File(filePath));
    }

    /** Attempt to upload the image to Imgur, returning the JSON that the server replied with. */
    public JsonObject uploadToImgur() {
        try {
            // thanks stack overflow for the help with this :_)
            URL url = new URL("https://api.imgur.com/3/image");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setDoInput(true);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
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
            if(con.getResponseCode() != 200) {
                LOGGER.error("Error uploading image to Imgur: " + con.getResponseCode());
                return null;
            }

            BufferedReader rd = new BufferedReader(new InputStreamReader(con.getInputStream()));
            JsonObject object = new JsonParser().parse(rd).getAsJsonObject();
            rd.close();

            return object;
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error("Error uploading image to Imgur.");
            return null;
        }
    }

    /** Attempt to upload the image to Imgur, returning the URL that the image is hosted at.
     * @param copy weather or not to copy the URL to the clipboard as well. */
    public String uploadToImgur(boolean copy) {
        JsonObject object = uploadToImgur();
        String link = object.get("data").getAsJsonObject().get("link").getAsString();
        if(copy) IOUtils.copyStringToClipboard(link);
        return link;
    }

    /** Copy the image to the system clipboard and delete the graphics object. */
    public void copyToClipboard() {
        IOUtils.copyImageToClipboard(image);
        dispose();
    }

    // MASK METHODS
    public void setBrightness(float brightness) {
        maskColor(new Color(0f,0f,0f,brightness));
    }

    public void maskColor(Color color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(color);
        g2d.fillRect(0,0,width,height);
        dispose();
    }

    public void maskPaint(Paint paint) {
        Graphics2D g2d = getG2D();
        g2d.setPaint(paint);
        g2d.fillRect(0,0,width,height);
        dispose();
    }

    // LINE METHODS
    public void drawLine(Stroke stroke, int sx, int sy, int ex, int ey) {
        Graphics2D g2d = getG2D();
        g2d.setStroke(stroke);
        g2d.drawLine(sx, sy, ex, ey);
        dispose();
    }

    public void drawLine(int sx, int sy, int ex, int ey, int width) {
        drawLine(new BasicStroke(width), sx, sy, ex, ey);
    }

    // SHAPE METHODS
    public void drawTexturedRect(TexturePaint paint, int x, int y, int width, int height) {
        Graphics2D g2d = getG2D();
        g2d.setPaint(paint);
        g2d.fillRect(x, y, width, height);
        dispose();
    }

    public void drawRect(int x, int y, int width, int height, int color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(new Color(color, true));
        g2d.drawRect(x, y, width, height);
        dispose();
    }
    public void drawRoundedRect(int x, int y, int width, int height, int radius, int color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(new Color(color, true));
        g2d.fillRoundRect(x, y, width, height, radius, radius);
        dispose();
    }

    public void drawPolygon(int x, int y, Polygon polygon, int color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(new Color(color, true));
        g2d.translate(x, y);
        g2d.fillPolygon(polygon);
        dispose();
    }

    public void drawOval(int x, int y, int width, int height, int color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(new Color(color, true));
        g2d.fillOval(x, y, width, height);
        dispose();
    }

    public void drawTriangle(Point p1, Point p2, Point p3, int color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(new Color(color, true));
        g2d.fillPolygon(new int[] {p1.x, p2.x, p3.x}, new int[] {p1.y, p2.y, p3.y}, 3);
        dispose();
    }

    public void drawCircle(int x, int y, int radius, int color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(new Color(color, true));
        g2d.fillRoundRect(x, y, radius, radius, radius, radius);
        dispose();
    }






    // STRING METHODS
    public void drawString(String text, int x, int y, Font font, int color) {
        Graphics2D g2d = getG2D();
        g2d.setColor(new Color(color, true));
        g2d.setFont(font);
        g2d.drawString(text, x, y);
        dispose();
    }



    // IMAGE METHODS
    /** Scale the image by the given factor (1.0 = no change). */
    public void scale(double sx, double sy) {
        if(sx == 1.0 && sy == 1.0) return;
        BufferedImage old = image;
        image = new BufferedImage((int) (Math.abs(sx * image.getWidth())), (int) (Math.abs(image.getHeight() * sy)), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = getG2D();
        g2d.drawImage(old, new AffineTransformOp(AffineTransform.getScaleInstance(sx, sy), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        dispose();
    }
    /**
     * Scale the image to the specified width and height.
     */
    public void setSize(int width, int height) {
        if(width == this.width && height == this.height) return;
        scale(width / (double) this.width, height / (double) this.height);
    }

    /** Rotate the image by the given angle (in degrees). */
    public void rotate(double angle) {
        if(angle == 0 || angle == 360) return;
        Graphics2D g2d = getG2D();
        g2d.drawImage(image, new AffineTransformOp(AffineTransform.getRotateInstance(Math.toRadians(angle)), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        dispose();
    }

    /** Translate the image by the given amount. */
    public void translate(int moveX, int moveY) {
        Graphics2D g2d = getG2D();
        g2d.drawImage(image, new AffineTransformOp(AffineTransform.getTranslateInstance(moveX, moveY), AffineTransformOp.TYPE_BILINEAR), 0, 0);
        dispose();
    }

    /** Flip the image horizontally. */
    public void flipHorizontal() {
        scale(-1, 1);
    }

    /** Flip the image vertically. */
    public void flipVertical() {
        scale(1, -1);
    }
}
