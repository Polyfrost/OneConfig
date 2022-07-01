package cc.polyfrost.oneconfig.platform;

public interface LoaderPlatform {
    boolean isModLoaded(String id);
    boolean hasActiveModContainer();
    ActiveMod getActiveModContainer();

    class ActiveMod {
        public final String name;
        public final String id;
        public final String version;

        public ActiveMod(String name, String id, String version) {
            this.name = name;
            this.id = id;
            this.version = version;
        }
    }
}
