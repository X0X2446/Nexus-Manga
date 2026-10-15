package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.icerock.moko.resources.StringResource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.tachiyomi.data.catalog.CatalogIds
import eu.kanade.tachiyomi.data.catalog.CatalogKind
import eu.kanade.tachiyomi.data.catalog.CatalogManga
import eu.kanade.tachiyomi.data.catalog.CatalogOption
import eu.kanade.tachiyomi.data.catalog.CatalogRepository
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.domain.manga.model.toDomainManga
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.manga.interactor.GetLibraryManga
import tachiyomi.domain.manga.interactor.NetworkToLocalManga
import tachiyomi.domain.manga.model.Manga
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.domain.track.interactor.GetTracksPerManga

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
    private val sourcePreferences: SourcePreferences,
    private val sourceManager: SourceManager,
    private val getLibraryManga: GetLibraryManga,
    private val networkToLocalManga: NetworkToLocalManga,
    private val getTracksPerManga: GetTracksPerManga,
    private val trackerManager: TrackerManager,
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    init {
        state.update {
            it.copy(
                trackerOptions = catalogRepository.trackerOptions(),
                coverSizeDp = sourcePreferences.catalogCoverSize.get(),
                pageSize = sourcePreferences.catalogPageSize.get(),
            )
        }

        viewModelScope.launch {
            val catalogFlow = combine(
                sourcePreferences.activeCatalogId.changes(),
                catalogRepository.extensionOptionsFlow(),
                getLibraryManga.subscribe(),
                getTracksPerManga.subscribe(),
            ) { catalogId, extensions, library, tracksByManga ->
                Quadruple(catalogId, extensions, library, tracksByManga)
            }
            val prefsFlow = combine(
                sourcePreferences.catalogCoverSize.changes(),
                sourcePreferences.catalogPageSize.changes(),
            ) { coverSize, pageSize -> coverSize to pageSize }

            combine(catalogFlow, prefsFlow) { catalog, prefs ->
                CatalogInputs(
                    catalogId = catalog.first,
                    extensions = catalog.second,
                    library = catalog.third,
                    tracksByManga = catalog.fourth,
                    coverSize = prefs.first,
                    pageSize = prefs.second,
                )
            }.collectLatest { inputs ->
                val trackers = catalogRepository.trackerOptions()
                val active = (trackers + inputs.extensions).find { it.id == inputs.catalogId }
                    ?: trackers.first()
                val continueItems = inputs.library
                    .asSequence()
                    .filter { it.hasStarted }
                    .sortedByDescending { it.lastRead }
                    .take(16)
                    .map {
                        ContinueItem(
                            mangaId = it.manga.id,
                            title = it.manga.title,
                            coverUrl = it.manga.thumbnailUrl.orEmpty(),
                            unreadCount = it.unreadCount,
                        )
                    }
                    .toList()

                val trackedSections = buildTrackedSections(
                    catalogId = inputs.catalogId,
                    library = inputs.library,
                    tracksByManga = inputs.tracksByManga,
                )

                state.update {
                    it.copy(
                        activeCatalogId = inputs.catalogId,
                        trackerOptions = trackers,
                        extensionOptions = inputs.extensions,
                        activeOption = active,
                        continueReading = continueItems,
                        trackedSections = trackedSections,
                        isExtensionCatalog = CatalogIds.isExtension(inputs.catalogId),
                        coverSizeDp = inputs.coverSize,
                        pageSize = inputs.pageSize,
                    )
                }
                loadCatalog(inputs.catalogId)
            }
        }
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D,
    )

    fun setActiveCatalog(catalogId: String) {
        sourcePreferences.activeCatalogId.set(catalogId)
    }

    fun retry() {
        viewModelScope.launch {
            loadCatalog(state.value.activeCatalogId)
        }
    }

    fun loadNextPage() {
        val current = state.value
        if (current.loading || current.loadingMore || !current.hasMore) return
        viewModelScope.launch {
            loadCatalog(current.activeCatalogId, page = current.page + 1, append = true)
        }
    }

    suspend fun resolveMangaOnSource(sourceId: Long, title: String): Manga? {
        val source = sourceManager.get(sourceId) as? CatalogueSource ?: return null
        val results = source.getSearchManga(1, title, source.getFilterList()).mangas
        if (results.isEmpty()) return null
        val match = results.firstOrNull { it.title.equals(title, ignoreCase = true) }
            ?: results.firstOrNull {
                it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
            }
            ?: results.first()
        return networkToLocalManga(match.toDomainManga(source.id))
    }

    private fun buildTrackedSections(
        catalogId: String,
        library: List<tachiyomi.domain.library.model.LibraryManga>,
        tracksByManga: Map<Long, List<tachiyomi.domain.track.model.Track>>,
    ): List<TrackedSection> {
        if (CatalogIds.isExtension(catalogId)) return emptyList()
        val trackerId = CatalogIds.trackerServiceId(catalogId) ?: return emptyList()
        val tracker = trackerManager.get(trackerId) ?: return emptyList()

        return tracker.getStatusList().mapNotNull { status ->
            val items = library.mapNotNull { lib ->
                val match = tracksByManga[lib.manga.id]
                    ?.firstOrNull { it.trackerId == trackerId && it.status == status }
                    ?: return@mapNotNull null
                ContinueItem(
                    mangaId = lib.manga.id,
                    title = lib.manga.title,
                    coverUrl = lib.manga.thumbnailUrl.orEmpty(),
                    unreadCount = lib.unreadCount,
                )
            }
            if (items.isEmpty()) return@mapNotNull null
            TrackedSection(
                status = status,
                titleRes = tracker.getStatus(status) ?: return@mapNotNull null,
                items = items.take(24),
            )
        }
    }

    private suspend fun loadCatalog(catalogId: String, page: Int = 1, append: Boolean = false) {
        val pageSize = sourcePreferences.catalogPageSize.get().coerceIn(6, 48)
        state.update {
            it.copy(
                loading = !append,
                loadingMore = append,
                error = null,
            )
        }
        try {
            val provider = catalogRepository.resolveProvider(catalogId)
            if (provider == null) {
                if (CatalogIds.isExtension(catalogId)) {
                    state.update {
                        it.copy(
                            loading = false,
                            loadingMore = false,
                            featured = emptyList(),
                            popular = emptyList(),
                            error = null,
                            emptyExtensions = true,
                        )
                    }
                } else {
                    sourcePreferences.activeCatalogId.set(CatalogIds.ANILIST)
                }
                return
            }

            val featured = if (page == 1) provider.getTrending(1, pageSize) else state.value.featured
            val popular = provider.getPopular(page, pageSize)
            state.update {
                it.copy(
                    loading = false,
                    loadingMore = false,
                    emptyExtensions = false,
                    page = page,
                    pageSize = pageSize,
                    hasMore = popular.isNotEmpty(),
                    featured = if (append) it.featured else featured,
                    popular = if (append) it.popular + popular else popular,
                    isExtensionCatalog = CatalogIds.isExtension(catalogId),
                )
            }
        } catch (e: Exception) {
            logcat(LogPriority.ERROR, e) { "Catalog load failed for $catalogId" }
            state.update {
                it.copy(
                    loading = false,
                    loadingMore = false,
                    error = e.message ?: "error",
                )
            }
        }
    }

    private data class CatalogInputs(
        val catalogId: String,
        val extensions: List<CatalogOption>,
        val library: List<tachiyomi.domain.library.model.LibraryManga>,
        val tracksByManga: Map<Long, List<tachiyomi.domain.track.model.Track>>,
        val coverSize: Int,
        val pageSize: Int,
    )

    @Immutable
    data class ContinueItem(
        val mangaId: Long,
        val title: String,
        val coverUrl: String,
        val unreadCount: Long,
    )

    @Immutable
    data class TrackedSection(
        val status: Long,
        val titleRes: StringResource,
        val items: List<ContinueItem>,
    )

    @Immutable
    data class State(
        val activeCatalogId: String = CatalogIds.ANILIST,
        val activeOption: CatalogOption? = null,
        val trackerOptions: List<CatalogOption> = emptyList(),
        val extensionOptions: List<CatalogOption> = emptyList(),
        val featured: List<CatalogManga> = emptyList(),
        val popular: List<CatalogManga> = emptyList(),
        val continueReading: List<ContinueItem> = emptyList(),
        val trackedSections: List<TrackedSection> = emptyList(),
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val error: String? = null,
        val page: Int = 1,
        val pageSize: Int = 24,
        val coverSizeDp: Int = 118,
        val hasMore: Boolean = true,
        val isExtensionCatalog: Boolean = false,
        val emptyExtensions: Boolean = false,
    ) {
        val isTrackerCatalog: Boolean
            get() = activeOption?.kind == CatalogKind.TRACKER ||
                (!isExtensionCatalog && activeOption == null)
    }
}
