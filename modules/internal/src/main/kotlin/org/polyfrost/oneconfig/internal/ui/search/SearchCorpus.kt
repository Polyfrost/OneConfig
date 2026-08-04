package org.polyfrost.oneconfig.internal.ui.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
 * Indexing is push-based and incremental. Configs register over the course of startup and mods may register
 * later still, so rebuilds are coalesced and only the documents whose text actually changed are forwarded -
 * otherwise a single late registration would re-index everything.
 */
object SearchCorpus {
    private val LOGGER = LogManager.getLogger("OneConfig/Search")

    private var initialized = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val rebuildMutex = Mutex()
    private val sources = ArrayList<SearchDocumentSource>()
    private var rebuildJob: Job? = null

    @Volatile
    var corpus: Map<String, SearchDocument<*>> = emptyMap()
        private set

    init {
        registerSource(ConfigDocumentSource)
        registerSource(KeybindDocumentSource)
    }

    fun registerSource(source: SearchDocumentSource) {
        synchronized(sources) {
            if (source in sources) return
            sources += source
        }
        invalidate()
    }

    fun unregisterSource(source: SearchDocumentSource) {
        synchronized(sources) {
            if (!sources.remove(source)) return
        }
        invalidate()
    }

    /**
     * Called when resources are done loading, prevents a lot of corpus builds during initial loading
     */
    fun init() {
        if (initialized.getAndSet(true)) return
        invalidate()
    }

    /**
     * Schedules a coalesced background rebuild. Cheap enough to call from every config mutation.
     */
    fun invalidate() {
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
        LOGGER.info("Rebuilding corpus")
        val start = System.currentTimeMillis()
        val snapshot = synchronized(sources) { sources.toList() }
        val documents = LinkedHashMap<String, SearchDocument<*>>()
        for (source in snapshot) {
            val produced = try {
                source.documents()
            } catch (e: Throwable) {
                LOGGER.error("Search document source ${source.javaClass.name} failed", e)
                continue
            }
            produced.forEach {
                if (documents.putIfAbsent(it.id, it) != null) {
                    LOGGER.warn("Duplicate document: $it")
                }
            }
        }

        // TODO: re-use previous if content & payload stayed the same
        val previous = corpus
        corpus = documents
        GlobalSettingIndex.rebuild()

        val upserted = documents.values.filter { previous[it.id]?.contentEquals(it) != true }
        val removed = previous.keys - documents.keys
        LOGGER.info("Rebuilt corpus, took ${System.currentTimeMillis() - start}ms, added ${upserted.size}, removed ${removed.size}")

        if (upserted.isNotEmpty() || removed.isNotEmpty()) {
            SearchProviderRegistry.all().forEach { provider ->
                runCatching { provider.onCorpusUpdate(upserted, removed) }
                    .onFailure { LOGGER.error("Failed to index into ${provider.javaClass.name}", it) }
            }
        }
    }
}
