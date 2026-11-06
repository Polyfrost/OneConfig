package dev.moon.hotwire.api;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ChangePlan {
    boolean canApply();

    Reason getReason();

    List<String> getLines();

    CompletableFuture<SwitchResult> accept();

    boolean needsReopen();

    boolean canWaitForNextStart();

    CompletableFuture<SwitchResult> acceptAtNextStart();

    void cancel();

    enum Reason {
        NONE,
        NEEDS_RESTART
    }
}
