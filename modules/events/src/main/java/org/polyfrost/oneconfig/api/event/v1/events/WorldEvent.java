package org.polyfrost.oneconfig.api.event.v1.events;

public abstract class WorldEvent implements Event {
    private final Object world;

    private WorldEvent(Object world) {
        this.world = world;
    }

    public static final class Load extends WorldEvent {
        public Load(Object world) {
            super(world);
        }
    }

    public static final class Unload extends WorldEvent {
        public Unload(Object world) {
            super(world);
        }
    }

    /**
     * This is a Duck method because Minecraft versions differ
     * <br> It returns the expected type for that minecraft version
     * <ul>
     *     <li>modern forge gives a ClientLevel</li>
     *     <li>fabric and forge pre-1.17 give a ClientWorld</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public <T> T getWorld() {
        return (T) world;
    }

    public Object component1() {
        return world;
    }
}
