package eu.kanade.tachiyomi.data.catalog

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.data.track.TrackerManager
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.CatalogueSource
import kotlinx.serialization.json.Json
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource

@Inject
@SingleIn(AppScope::class)
class CatalogRepository(
    networkHelper: NetworkHelper,
    private val json: Json,
    private val sourceManager: SourceManager,
    private val trackerManager: TrackerManager,
) {

    private val client = networkHelper.client

    private val trackerProviders: Map<String, CatalogProvider> = listOf(
        AniListCatalogProvider(client, json),
        MalCatalogProvider(client, json),
        KitsuCatalogProvider(client, json),
        MangaBakaCatalogProvider(client, json),
    ).associateBy { it.id }

    fun trackerOptions(): List<CatalogOption> = listOf(
        CatalogOption(
            id = CatalogIds.ANILIST,
            name = "AniList",
            descriptionRes = MR.strings.catalog_tracker_desc_anilist,
            kind = CatalogKind.TRACKER,
            logoRes = trackerManager.aniList.getLogo(),
        ),
        CatalogOption(
            id = CatalogIds.MAL,
            name = "MyAnimeList",
            descriptionRes = MR.strings.catalog_tracker_desc_mal,
            kind = CatalogKind.TRACKER,
            logoRes = trackerManager.myAnimeList.getLogo(),
        ),
        CatalogOption(
            id = CatalogIds.KITSU,
            name = "Kitsu",
            descriptionRes = MR.strings.catalog_tracker_desc_kitsu,
            kind = CatalogKind.TRACKER,
            logoRes = trackerManager.kitsu.getLogo(),
        ),
        CatalogOption(
            id = CatalogIds.MANGABAKA,
            name = "MangaBaka",
            descriptionRes = MR.strings.catalog_tracker_desc_mangabaka,
            kind = CatalogKind.TRACKER,
            logoRes = trackerManager.mangaBaka.getLogo(),
        ),
    )

    suspend fun extensionOptions(): List<CatalogOption> {
        return getCatalogueSources().map { source ->
            CatalogOption(
                id = CatalogIds.extension(source.id),
                name = source.name,
                descriptionRes = MR.strings.catalog_extension_desc,
                kind = CatalogKind.EXTENSION,
                sourceId = source.id,
            )
        }
    }

    suspend fun resolveProvider(catalogId: String): CatalogProvider? {
        trackerProviders[catalogId]?.let { return it }
        val sourceId = CatalogIds.sourceIdOrNull(catalogId) ?: return null
        val source = sourceManager.get(sourceId) as? CatalogueSource ?: return null
        return ExtensionCatalogProvider(source)
    }

    suspend fun getCatalogueSources(): List<CatalogueSource> {
        return sourceManager.getOnlineSources()
            .filter { it.id != LocalSource.ID }
            .filterIsInstance<CatalogueSource>()
            .sortedBy { it.name.lowercase() }
    }
}
