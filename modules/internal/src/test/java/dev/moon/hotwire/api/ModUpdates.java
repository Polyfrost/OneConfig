package dev.moon.hotwire.api;

import java.util.List;

public record ModUpdates(List<ModUpdate> updates, String failure) {
}
