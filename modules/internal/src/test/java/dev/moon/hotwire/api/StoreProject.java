package dev.moon.hotwire.api;

import java.util.List;

public record StoreProject(List<Version> versions) {
    public record Version(String id) {
    }
}
