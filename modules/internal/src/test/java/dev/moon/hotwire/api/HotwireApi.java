package dev.moon.hotwire.api;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Stands in for the optional Hotwire mod, with the signatures of its real API, so the reflective bridge has something to find */
public final class HotwireApi {
    private static final HotwireApi INSTANCE = new HotwireApi();

    public static final List<Object> DOWNLOADS = new ArrayList<>();
    public static final List<String> CONTROLS = new ArrayList<>();
    public static ChangePlan plan;
    public static final List<PendingTask> PENDING = new ArrayList<>();
    public static final List<ModUpdate> UPDATES = new ArrayList<>();

    public static HotwireApi getInstance() {
        return INSTANCE;
    }

    public int getRevision() {
        return 8;
    }

    public List<PendingTask> getPendingTasks() {
        return PENDING;
    }

    public CompletableFuture<ModUpdates> checkUpdates() {
        return CompletableFuture.completedFuture(new ModUpdates(UPDATES, ""));
    }

    public void addListener(Consumer<?> listener) {
    }

    public CompletableFuture<ChangePlan> planChange(ChangeRequest request) {
        return CompletableFuture.completedFuture(plan);
    }

    public List<Object> getStoreDownloads() {
        return DOWNLOADS;
    }

    public boolean pauseDownload(int id) {
        return CONTROLS.add("pause " + id);
    }

    public boolean resumeDownload(int id) {
        return CONTROLS.add("resume " + id);
    }

    public boolean stopDownload(int id) {
        return CONTROLS.add("stop " + id);
    }
}
