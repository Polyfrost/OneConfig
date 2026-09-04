package org.polyfrost.oneconfig.internal.ui.compose

import net.fabricmc.loader.api.FabricLoader
import net.minecraft.client.Minecraft
import org.polyfrost.oneconfig.api.notifications.v1.NotificationsManager
import org.polyfrost.oneconfig.api.platform.v1.Platform
import org.polyfrost.oneconfig.internal.ui.api.ConfigRegistry
import org.polyfrost.oneconfig.internal.ui.compose.impls.HudEditorUIScreen
import org.polyfrost.oneconfig.internal.ui.compose.impls.OneConfigUIScreen
import org.polyfrost.oneconfig.utils.v1.Multithreading
import org.slf4j.LoggerFactory
import java.nio.file.Files

/**
 * Builds the OneConfig UI while nobody is waiting for a frame, so that opening it costs nothing
 */
object ComposePreloader {
    private val LOG = LoggerFactory.getLogger(ComposePreloader::class.java)

    @Volatile
    private var gpuWarmed = false

    fun preloadGpuWarmup() {
        if (gpuWarmed) return
        gpuWarmed = true
        NotificationsManager.ensureInitialized()
        warmClasses()
        SkiaCtx.queueWarmup(::warmUp)
    }

    /**
     * loads the classes behind every settings control, off the render thread
     *
     * a class is only loaded when something first reaches it, which happens inside a composition on
     * the frame that shows it, and on a large pack that costs 40 to 50 ms each. linking is enough.
     */
    private fun warmClasses() {
        val loader = ComposePreloader::class.java.classLoader ?: return
        Multithreading.submit {
            val startNanos = System.nanoTime()
            val names = ourUiClasses() + LAZY_LIBRARY_CLASSES
            val warmed = names.count { runCatching { Class.forName(it, false, loader) }.isSuccess }
            LOG.info("Warmed {} of {} UI classes in {} ms", warmed, names.size,
                (System.nanoTime() - startNanos) / 1_000_000)
        }
    }

    /** every class under these is a first touch waiting to land on the frame that shows it */
    private val WARM_PACKAGES = listOf(
        "org/polyfrost/oneconfig/internal/ui/api/",
        "org/polyfrost/oneconfig/internal/ui/components/",
        "org/polyfrost/oneconfig/internal/ui/hud/",
        "org/polyfrost/oneconfig/internal/ui/keybind/",
        "org/polyfrost/oneconfig/internal/ui/screens/",
        "org/polyfrost/oneconfig/internal/ui/search/",
        "org/polyfrost/oneconfig/internal/ui/themes/",
    )

    /** read off our own jars, so renaming or adding a screen never leaves this list behind */
    private fun ourUiClasses(): List<String> = FabricLoader.getInstance().allMods
        .filter { it.metadata.id.startsWith("org_polyfrost_oneconfig") }
        .flatMap { it.rootPaths }
        .flatMap { root ->
            runCatching {
                Files.walk(root).use { paths ->
                    paths.map { path -> root.relativize(path).joinToString("/") }
                        .filter { name -> name.endsWith(".class") && WARM_PACKAGES.any(name::startsWith) }
                        .map { name -> name.removeSuffix(".class").replace('/', '.') }
                        .toList()
                }
            }.getOrDefault(emptyList())
        }

    // named by the stall watch, so these are listed rather than derived
    private val LAZY_LIBRARY_CLASSES = listOf(
        "androidx.compose.ui.graphics.SkiaBackedPath_skikoKt",
        "androidx.compose.foundation.lazy.LazyListItemProviderKt",
        "androidx.compose.ui.text.SkiaParagraph",
        "androidx.compose.ui.text.platform.DesktopFont_desktopKt",
        "androidx.compose.ui.text.platform.FontCache",
        "androidx.compose.foundation.lazy.LazyListKt",
        "androidx.compose.foundation.lazy.LazyListMeasureKt",
        "androidx.compose.foundation.lazy.LazyListState",
        "androidx.compose.foundation.lazy.LazyListMeasuredItem",
        "androidx.compose.foundation.lazy.LazyListMeasuredItemProvider",
        "androidx.compose.foundation.lazy.LazyListMeasureResult",
        "androidx.compose.foundation.lazy.LazyListIntervalContent",
        "androidx.compose.foundation.lazy.LazyListItemProviderImpl",
        "androidx.compose.foundation.lazy.LazyDslKt",
        "androidx.compose.foundation.lazy.layout.LazyLayoutMeasureScopeImpl",
        "androidx.compose.foundation.lazy.layout.LazyLayoutItemContentFactory",
        "androidx.compose.foundation.lazy.layout.LazySaveableStateHolder",
        "androidx.compose.foundation.lazy.layout.LazyLayoutPinnableItemKt",
        "kotlinx.coroutines.flow.internal.ChannelFlowTransformLatest",
        "kotlinx.coroutines.flow.internal.ChannelFlowOperator",
        "kotlinx.coroutines.flow.internal.ChannelFlow",
        "kotlinx.coroutines.flow.internal.MergeKt",
        "org.commonmark.parser.Parser",
        "org.commonmark.internal.DocumentParser",
        "org.commonmark.internal.ParagraphParser",
        "org.commonmark.internal.LinkReferenceDefinitionParser",
        "androidx.compose.ui.text.TextMeasurer",
        "androidx.compose.ui.text.TextLayoutCache",
        "androidx.compose.ui.text.SpanStyle",
        "androidx.compose.ui.text.TextStyle",
        "androidx.compose.ui.text.ParagraphStyle",
        "androidx.compose.ui.text.ParagraphKt",
        "androidx.compose.ui.text.platform.ParagraphBuilder",
        "androidx.compose.foundation.text.TextFieldDelegateKt",
        "androidx.compose.foundation.text.TextFieldSize",
        "androidx.compose.foundation.text.TextFieldScrollKt",
        "androidx.compose.foundation.text.KeyMapping_skikoKt",
        "androidx.compose.foundation.text.TextFieldKeyInput",
    )

    /**
     * What the UI composes against
     *
     * Joining a world is the last of these to settle, and it is also the point after which every
     * frame belongs to someone, so a pass that runs in one ends the watch.
     */
    private data class Inputs(val width: Int, val height: Int, val configs: Int, val inWorld: Boolean)

    /** The inputs of the last pass that succeeded, or null while none has */
    private var warmed: Inputs? = null
    private var passes = 0

    /** A pass is spread over frames, so its cost and length are accumulated rather than timed once */
    private var passNanos = 0L
    private var passFrames = 0

    // the total says what the warm-up cost, only the worst frame says whether it was felt: the same
    // pass has come in at 3.7 s and at 15 s, and those are the same number spread very differently
    private var worstFrameNanos = 0L
    private var deadline = 0L

    /**
     * Composes the shared screen offscreen until it is composing the real thing
     *
     * The first frame that can warm up is not the frame worth warming: the window is still 854x480
     * and most mods have not registered, so one pass builds the wrong size against a fraction of the
     * data. So it watches instead, and a pass that finds nothing changed costs three reads.
     */
    private fun warmUp() {
        if (deadline == 0L) deadline = System.nanoTime() + WATCH_NANOS
        val inputs = Inputs(
            width = Platform.screen().windowWidth(),
            height = Platform.screen().windowHeight(),
            configs = ConfigRegistry.configs.size,
            inWorld = Minecraft.getInstance().level != null,
        )

        if (inputs != warmed) {
            // the config screen warms a few frames per call, so a pass spans several real frames:
            // count it, and report its cost, only once it has actually finished
            val startNanos = System.nanoTime()
            val done = OneConfigUIScreen.prewarmShared()
            val frameNanos = System.nanoTime() - startNanos
            passNanos += frameNanos
            passFrames++
            if (frameNanos > worstFrameNanos) worstFrameNanos = frameNanos
            if (done) {
                passes++
                // the editor is warmed after, so a failure there cannot cost the config screen its own
                HudEditorUIScreen.prewarmShared()
                warmed = inputs
                LOG.info(
                    "OneConfig UI warm-up pass {} in {} ms over {} frame(s), worst {} ms ({} configs, {}x{}, {})",
                    passes, passNanos / 1_000_000, passFrames, worstFrameNanos / 1_000_000,
                    inputs.configs, inputs.width, inputs.height,
                    if (inputs.inWorld) "in world" else "no world",
                )
                passNanos = 0L
                passFrames = 0
                worstFrameNanos = 0L
            }
        }

        // mods register their configs across the loading screen and the window is resized part way
        // through, so keep watching until a pass has run against a world
        if (warmed?.inWorld != true && passes < MAX_PASSES && System.nanoTime() < deadline) {
            SkiaCtx.queueWarmup(::warmUp)
        }
    }

    /** Enough passes to follow the window, the mod list and the world, few enough to stay bounded */
    private const val MAX_PASSES = 8

    /** Long enough to reach a world on a slow load, short enough not to watch a menu forever */
    private const val WATCH_NANOS = 300_000_000_000L
}
