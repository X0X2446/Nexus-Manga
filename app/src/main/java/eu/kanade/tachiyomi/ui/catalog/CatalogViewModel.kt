package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.system.logcat

@Inject
@ViewModelKey
@ContributesIntoMap(AppScope::class, binding = binding<ViewModel>())
class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
    private val sourcePreferences: SourcePreferences,
) : ViewModel() {

    val state: StateFlow<State>
        field = MutableStateFlow(State())

    init {
        viewModelScope.launch {
            sourcePreferences.activeCatalogId.changes().collectLatest { catalogId ->
                state.update { it.copy(activeCatalogId = catalogId) }
                refreshOptions()
                loadCatalog(catalogId)
            }
        }
    }

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

    private suspend fun refreshOptions() {
        val trackers = catalogRepository.trackerOptions()
        val extensions = catalogRepository.extensionOptions()
        val activeId = state.value.activeCatalogId
        val active = (trackers + extensions).find { it.id == activeId }
            ?: trackers.first()
        state.update {
            it.copy(
                trackerOptions = trackers,
                extensionOptions = extensions,
                activeOption = active,
            )
        }
    }

    private suspend fun loadCatalog(catalogId: String, page: Int = 1, append: Boolean = false) {
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
                            trending = emptyList(),
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

            val trending = if (page == 1) provider.getTrending(1) else state.value.trending
            val popular = provider.getPopular(page)
            state.update {
                it.copy(
                    loading = false,
                    loadingMore = false,
                    emptyExtensions = false,
                    page = page,
                    hasMore = popular.isNotEmpty(),
                    trending = if (append) it.trending else trending,
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

    @Immutable
    data class State(
        val activeCatalogId: String = CatalogIds.ANILIST,
        val activeOption: CatalogOption? = null,
        val trackerOptions: List<CatalogOption> = emptyList(),
        val extensionOptions: List<CatalogOption> = emptyList(),
        val trending: List<CatalogManga> = emptyList(),
        val popular: List<CatalogManga> = emptyList(),
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val error: String? = null,
        val page: Int = 1,
        val hasMore: Boolean = true,
        val isExtensionCatalog: Boolean = false,
        val emptyExtensions: Boolean = false,
    ) {
        val isTrackerCatalog: Boolean
            get() = activeOption?.kind == CatalogKind.TRACKER ||
                (!isExtensionCatalog && activeOption == null)
    }
}
