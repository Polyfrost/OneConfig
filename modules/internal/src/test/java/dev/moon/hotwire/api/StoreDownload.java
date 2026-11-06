package dev.moon.hotwire.api;

public record StoreDownload(String name, long done, long total, boolean over, boolean ok, int id, State state) {
    public enum State {
        WAITING,
        GETTING,
        PAUSED,
        DONE,
        FAILED,
        STOPPED
    }
}
