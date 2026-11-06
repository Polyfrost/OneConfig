package dev.moon.hotwire.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StoreAdd {
    String getTitle();

    List<String> getLines();

    boolean needsReopen();

    boolean canAdd();

    CompletableFuture<SwitchResult> accept();

    void cancel();
}
