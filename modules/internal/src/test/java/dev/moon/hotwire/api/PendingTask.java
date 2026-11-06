package dev.moon.hotwire.api;

public record PendingTask(String modId, String name, Kind kind) {
    public enum Kind {
        UPDATE,
        REVERTABLE,
        COPIED,
        OFF_AT_START
    }
}
