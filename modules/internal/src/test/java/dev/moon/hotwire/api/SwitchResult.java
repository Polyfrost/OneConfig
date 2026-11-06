package dev.moon.hotwire.api;

public record SwitchResult(Kind kind, String message) {
    public enum Kind {
        DONE
    }
}
