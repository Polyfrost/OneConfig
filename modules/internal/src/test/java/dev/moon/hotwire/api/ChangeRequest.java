package dev.moon.hotwire.api;

import java.nio.file.Path;
import java.util.List;

public record ChangeRequest(List<String> off, List<String> on, List<String> update, List<String> add, List<Path> jars) {
}
