package eu.kanade.tachiyomi.data.catalog

import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.model.MangasPage

class ExtensionCatalogProvider(
    private val source: CatalogueSource,
) : CatalogProvider {

    override val id: String = CatalogIds.extension(source.id)
    override val displayName: String = source.name

    override suspend fun getTrending(page: Int, pageSize: Int): List<CatalogManga> =
        mapPage(source.getPopularManga(page))

    override suspend fun getPopular(page: Int, pageSize: Int): List<CatalogManga> {
        return if (source.supportsLatest) {
            mapPage(source.getLatestUpdates(page))
        } else {
            mapPage(source.getPopularManga(page))
        }
    }

    private fun mapPage(page: MangasPage): List<CatalogManga> {
        return page.mangas.map { manga ->
            CatalogManga(
                id = manga.url,
                title = manga.title,
                coverUrl = manga.thumbnail_url.orEmpty(),
                synopsis = manga.description.orEmpty(),
                genres = manga.getGenres().orEmpty(),
                status = manga.status.toString(),
                author = manga.author.orEmpty(),
            )
        }
    }
}
