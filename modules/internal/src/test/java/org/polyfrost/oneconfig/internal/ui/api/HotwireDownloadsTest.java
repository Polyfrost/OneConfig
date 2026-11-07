package org.polyfrost.oneconfig.internal.ui.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.moon.hotwire.api.HotwireApi;
import dev.moon.hotwire.api.StoreDownload;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HotwireDownloadsTest {
    /** Still running as far as the list can tell, but with none of the fields read after that */
    public static final class Renamed {
        public boolean over() {
            return false;
        }
    }

    /** Ends once a test says so, while the bridge is polling it from another thread */
    public static final class Ending {
        volatile boolean over;

        public boolean over() {
            return over;
        }

        public int id() {
            return 1;
        }
    }

    private static void running(int id, long done, StoreDownload.State state) {
        HotwireApi.DOWNLOADS.add(new StoreDownload("mod.jar", done, 100, false, false, id, state));
    }

    private static void finished(int id) {
        HotwireApi.DOWNLOADS.add(new StoreDownload("mod.jar", 100, 100, true, true, id, StoreDownload.State.DONE));
    }

    @AfterEach
    void clear() {
        HotwireApi.DOWNLOADS.clear();
        HotwireApi.CONTROLS.clear();
        HotwireBridge.INSTANCE.isStoppingDownloads();
    }

    @Test
    void finishedDownloadsAreNotProgress() {
        finished(1);
        assertNull(HotwireBridge.INSTANCE.downloadProgress());
    }

    @Test
    void progressCoversEveryRunningJar() {
        finished(1);
        running(2, 10, StoreDownload.State.GETTING);
        running(3, 30, StoreDownload.State.PAUSED);
        HotwireBridge.DownloadProgress progress = HotwireBridge.INSTANCE.downloadProgress();
        assertEquals(0.2f, progress.getFraction());
        assertEquals("mod.jar", progress.getName());
        assertFalse(progress.getPaused());
    }

    @Test
    void itIsPausedOnceEveryRunningJarIs() {
        running(1, 10, StoreDownload.State.PAUSED);
        assertTrue(HotwireBridge.INSTANCE.downloadProgress().getPaused());
    }

    @Test
    void aFinishedJarLeavingDoesNotPullTheBarBack() {
        running(1, 90, StoreDownload.State.GETTING);
        running(2, 90, StoreDownload.State.GETTING);
        HotwireBridge.DownloadProgress before = HotwireBridge.INSTANCE.downloadProgress();

        HotwireApi.DOWNLOADS.clear();
        running(2, 50, StoreDownload.State.GETTING);
        assertEquals(0.9f, HotwireBridge.INSTANCE.downloadProgress().shownAfter(before, 0.9f));
    }

    @Test
    void aNewJarStartsTheBarOver() {
        running(1, 100, StoreDownload.State.GETTING);
        HotwireBridge.DownloadProgress before = HotwireBridge.INSTANCE.downloadProgress();

        HotwireApi.DOWNLOADS.clear();
        running(2, 10, StoreDownload.State.GETTING);
        assertEquals(0.1f, HotwireBridge.INSTANCE.downloadProgress().shownAfter(before, 1f));
    }

    @Test
    void aFieldHotwireRenamedGivesNoProgressRatherThanAnError() {
        HotwireApi.DOWNLOADS.add(new Renamed());
        assertNull(HotwireBridge.INSTANCE.downloadProgress());
        HotwireBridge.INSTANCE.stopDownloads();
        assertEquals(List.of(), HotwireApi.CONTROLS);
    }

    @Test
    void controlsReachEachRunningDownloadOnly() {
        finished(1);
        running(2, 10, StoreDownload.State.GETTING);
        running(3, 10, StoreDownload.State.GETTING);
        HotwireBridge.INSTANCE.pauseDownloads(true);
        HotwireBridge.INSTANCE.pauseDownloads(false);
        HotwireBridge.INSTANCE.stopDownloads();
        assertEquals(List.of("pause 2", "pause 3", "resume 2", "resume 3", "stop 2", "stop 3"), HotwireApi.CONTROLS);
    }

    @Test
    void aStoppedDownloadHoldsBackANewPlanUntilItEnds() {
        running(1, 10, StoreDownload.State.GETTING);
        HotwireBridge.INSTANCE.stopDownloads();
        assertTrue(HotwireBridge.INSTANCE.isStoppingDownloads());

        HotwireApi.DOWNLOADS.clear();
        finished(1);
        assertFalse(HotwireBridge.INSTANCE.isStoppingDownloads());
    }

    @Test
    void aDownloadRunningBeforeTheStoppedPlanHoldsNothingBack() {
        running(1, 10, StoreDownload.State.GETTING);
        HotwireBridge.INSTANCE.stopDownloads(HotwireBridge.INSTANCE.runningDownloadIds());
        assertFalse(HotwireBridge.INSTANCE.isStoppingDownloads());
    }

    @Test
    void theWaitForHotwiresOwnUpdateLastsUntilItsDownloadEnds() throws Exception {
        Ending download = new Ending();
        HotwireApi.DOWNLOADS.add(download);
        HotwireBridge.StoreUpdate self = new HotwireBridge.StoreUpdate("hotwire", "project", "Hotwire", "1", "2");
        HotwireBridge.INSTANCE.planUpdate(List.of(self)).getActions().get(0).attempt();

        // past where a wait with nothing to see for itself gives up
        Thread ender = new Thread(() -> {
            try {
                Thread.sleep(3_500);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            download.over = true;
        });
        ender.start();
        HotwireBridge.INSTANCE.awaitIdle();
        assertTrue(download.over, "the update only gets queued once it has downloaded");
        ender.join();
    }

    @Test
    void downloadsRunningBeforeAPlanAreNotItsToShow() {
        running(1, 90, StoreDownload.State.GETTING);
        Set<Integer> others = HotwireBridge.INSTANCE.runningDownloadIds();
        running(2, 10, StoreDownload.State.GETTING);
        assertEquals(0.1f, HotwireBridge.INSTANCE.downloadProgress(others).getFraction());
    }

    @Test
    void aPlanBesideOtherDownloadsCannotStopTheNextJarOfOne() {
        running(1, 10, StoreDownload.State.GETTING);
        Set<Integer> others = HotwireBridge.INSTANCE.runningDownloadIds();
        // the plan's own and the next jar of what was already running, which Hotwire's list can't tell apart
        running(2, 10, StoreDownload.State.GETTING);
        running(3, 10, StoreDownload.State.GETTING);
        HotwireBridge.INSTANCE.stopDownloads(others);
        HotwireBridge.INSTANCE.pauseDownloads(true, others);
        assertEquals(List.of(), HotwireApi.CONTROLS);
    }
}
