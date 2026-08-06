package org.polyfrost.oneconfig.internal.ui.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.apache.logging.log4j.LogManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.milliseconds

/** How long to wait for further config registrations before rebuilding. */
private const val REBUILD_DEBOUNCE_MS = 250L

/**
 * Owns the searchable corpus and keeps every registered [SearchProvider] indexed in the background.
 *
 * Indexing is push-based and incremental. When a config is ready it will be registered, and then the
 * search documents for this config can be built. Only what actually changed is forwarded to the search
 * providers.
 */
object SearchCorpus {
    private val LOGGER = LogManager.getLogger("OneConfig/Search")

    private var initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val rebuildMutex = Mutex()
    private val sources = ArrayList<SearchDocumentSource>()

    /** Sources which need to be run again on the next rebuild. */
    private val dirtySources = HashSet<SearchDocumentSource>()

    /** What every source built, only touched under [rebuildMutex]. */
    private val produced = HashMap<SearchDocumentSource, List<SearchDocument<*>>>()
    private var rebuildJob: Job? = null

    @Volatile
    var corpus: Map<String, SearchDocument<*>> = emptyMap()
        private set

    init {
        registerSource(ConfigDocumentSource)
        registerSource(KeybindDocumentSource)
        registerSource(HudDocumentSource)
        registerSource(HudModCardDocumentSource)
    }

    fun registerSource(source: SearchDocumentSource) {
        synchronized(sources) {
            if (source in sources) return
            sources += source
        }
        invalidate(source)
    }

    fun unregisterSource(source: SearchDocumentSource) {
        synchronized(sources) {
            if (!sources.remove(source)) return
        }
        schedule()
    }

    /**
     * Called when resources are done loading, prevents a lot of corpus builds during initial loading
     */
    fun init() {
        if (initialized.getAndSet(true)) return
        invalidate()
    }

    /**
     * Schedule a background rebuild
     */
    fun invalidate(vararg invalidated: SearchDocumentSource) {
        synchronized(sources) {
            if (invalidated.isEmpty()) {
                dirtySources += sources  // All sources
            } else {
                dirtySources += invalidated
            }
        }
        schedule()
    }

    private fun schedule() {
        if (!initialized.get()) return
        synchronized(this) {
            rebuildJob?.cancel()
            rebuildJob = scope.launch {
                delay(REBUILD_DEBOUNCE_MS.milliseconds)
                rebuild()
            }
        }
    }

    /**
     * Runs [query] against [scope] on the highest-priority available provider.
     */
    fun search(query: String, scope: Set<SearchScope>): List<SearchDocument<*>> {
        if (query.isBlank()) return emptyList()
        val provider = SearchProviderRegistry.get()
        val hits = try {
            provider.search(query, scope)
        } catch (e: Throwable) {
            LOGGER.error("Search provider ${provider.javaClass.name} failed, falling back to the default", e)
            if (provider === DefaultSearchProvider) return emptyList()
            runCatching { DefaultSearchProvider.search(query, scope) }.onFailure {
                LOGGER.error("Default search provider failed", it)
            }.getOrDefault(emptyList())
        }

        // Re-resolve against current corpus in case of stale search results
        val current = corpus
        return hits.mapNotNull { hit -> current[hit.id] }
    }

    fun <T> searchGrouped(
        query: String,
        scope: Set<SearchScope>,
        grouper: (SearchDocument<*>) -> T
    ): Map<T, List<SearchDocument<*>>> {
        if (query.isBlank()) return emptyMap()
        val provider = SearchProviderRegistry.get()
        val hits = try {
            provider.searchGrouped(query, scope, grouper)
        } catch (e: Throwable) {
            LOGGER.error("Search provider ${provider.javaClass.name} failed, falling back to the default", e)
            if (provider === DefaultSearchProvider) return emptyMap()
            runCatching { DefaultSearchProvider.searchGrouped(query, scope, grouper) }.onFailure {
                LOGGER.error("Default search provider failed", it)
            }.getOrDefault(emptyMap())
        }

        // Re-resolve against current corpus in case of stale search results
        val current = corpus
        return hits.mapValues { (_, group) -> group.mapNotNull { hit -> current[hit.id] } }
    }

    /** Hands a freshly registered provider the corpus that already exists. */
    internal fun seed(provider: SearchProvider) {
        if (!initialized.get()) return
        scope.launch {
            val documents = rebuildMutex.withLock { corpus.values.toList() }
            if (documents.isEmpty()) return@launch
            runCatching { provider.onCorpusUpdate(documents, emptySet()) }
                .onFailure { LOGGER.error("Failed to seed search provider ${provider.javaClass.name}", it) }
        }
    }

    private suspend fun rebuild() = rebuildMutex.withLock {
        // Collect rebuild parameters in a thread safe manner
        val start = System.currentTimeMillis()
        val snapshot: List<SearchDocumentSource>
        val dirty: Set<SearchDocumentSource>
        synchronized(sources) {
            snapshot = sources.toList()
            synchronized(dirtySources) {
                dirty = expandDirty(dirtySources, snapshot)
            }
        }

        val previous = corpus
        val documents = LinkedHashMap<String, SearchDocument<*>>(previous.size.coerceAtLeast(16))
        val upserted = ArrayList<SearchDocument<*>>()
        var asked = 0
        for (source in snapshot) {
            val cached = produced[source]
            val sourceDocuments = if (cached != null && source !in dirty) cached else {
                asked++
                try {
                    // Keep the previous element of the corpus if nothing changed
                    source.documents().map { document ->
                        previous[document.id]?.takeIf { it.equivalentTo(document) } ?: document
                    }
                } catch (e: Throwable) {
                    LOGGER.error("Search document source ${source.javaClass.name} failed", e)
                    cached ?: continue
                }
            }
            produced[source] = sourceDocuments

            for (document in sourceDocuments) {
                if (documents.putIfAbsent(document.id, document) != null) {
                    LOGGER.warn("Duplicate document: $document")
                    continue
                }
                if (previous[document.id] !== document) upserted += document
            }
        }
        // Remove other sources from the produced if they haven't been re-run/got removed
        produced.keys.retainAll(snapshot.toHashSet())

        // Get removed keys, if no upserted and same size -> no removed
        val removed = if (upserted.isEmpty() && documents.size == previous.size) emptySet()
        else previous.keys - documents.keys
        if (upserted.isEmpty() && removed.isEmpty()) {
            LOGGER.debug(
                "Corpus unchanged, asked $asked/${snapshot.size} sources," +
                        " took ${System.currentTimeMillis() - start}ms"
            )
            return@withLock
        }

        // Swap to new corpus, non-cancellable to prevent desyncs with search providers
        withContext(NonCancellable) {
            corpus = documents
            // Update sources that are no longer dirty
            dirtySources -= dirty

            if (ConfigDocumentSource in dirty) GlobalSettingIndex.rebuild()
            LOGGER.info(
                "Rebuilt corpus from $asked/${snapshot.size} sources, " +
                        "took ${System.currentTimeMillis() - start}ms, " +
                        "added ${upserted.size}, removed ${removed.size}"
            )

            SearchProviderRegistry.all().forEach { provider ->
                runCatching { provider.onCorpusUpdate(upserted, removed) }
                    .onFailure { LOGGER.error("Failed to index into ${provider.javaClass.name}", it) }
            }
        }
    }

    private fun expandDirty(
        dirty: Set<SearchDocumentSource>,
        snapshot: List<SearchDocumentSource>
    ): Set<SearchDocumentSource> {
        if (dirty.isEmpty()) return emptySet()
        val expanded = HashSet(dirty)
        var added = true
        while (added) {
            if (expanded.size == snapshot.size) {
                return expanded
            }

            added = false
            for (source in snapshot) {
                if (source in expanded) continue
                if (source.dependencies.any { it in expanded }) {
                    expanded += source
                    added = true
                }
            }
        }
        return expanded
    }
}
