package dev.moon.hotwire.api;

public record StoreQuery(String words) {
    public static StoreQuery of(String words) {
        return new StoreQuery(words);
    }
}
