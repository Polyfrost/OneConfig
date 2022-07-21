package cc.polyfrost.oneconfig.utils.notifications;

/**
 * @deprecated Reserved for future use, not implemented yet.
 */
@Deprecated
public final class Notification {
    private String title;
    private String message;
    private final float duration;
    private float x;
    private float y;

    private final Runnable action;
    private final Runnable onClose;

    Notification(String title, String message, float duration, float x, float y, Runnable action, Runnable onClose) {
        this.title = title;
        this.message = message;
        this.duration = duration;
        this.x = x;
        this.y = y;
        this.action = action;
        this.onClose = onClose;
    }

    void draw(final long vg) {

    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public float getDuration() {
        return duration;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public Runnable getAction() {
        return action;
    }

    public Runnable getOnClose() {
        return onClose;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    void setX(float x) {
        this.x = x;
    }

    void setY(float y) {
        this.y = y;
    }
}
