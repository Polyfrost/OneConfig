package cc.polyfrost.oneconfig.images;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

/** An Image wrapper class that is used by the OneConfig system.*/
@SuppressWarnings("unused")
public class Image {
    private BufferedImage image;
    private Graphics2D graphics = null;
    private final int width, height;


    /**
     * Create a new Image from the file. This can be as a resource location inside your JAR.
     * @param filePath The path to the image file.
     */
    public Image(String filePath) throws IOException {
        image = ImageIO.read(Objects.requireNonNull(Image.class.getResourceAsStream(filePath)));
        width = image.getWidth();
        height = image.getHeight();
    }

    /**
     * Create a new Image from the file.
     * @param is InputStream to the image file.
     */
    public Image(InputStream is) throws IOException {
        image = ImageIO.read(is);
        width = image.getWidth();
        height = image.getHeight();
    }

    /**
     * Create a new Image from the file.
     * @param file File to the image file.
     */
    public Image(File file) throws IOException {
            image = ImageIO.read(Objects.requireNonNull(file));
            width = image.getWidth();
            height = image.getHeight();
    }

    /**
     * Create a new Image from the BufferedImage.
     */
    public Image(BufferedImage image) {
        this.image = image;
        width = image.getWidth();
        height = image.getHeight();
    }

    /** Create a new blank image with the specified width and height. */
    public Image(int width, int height) {
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
