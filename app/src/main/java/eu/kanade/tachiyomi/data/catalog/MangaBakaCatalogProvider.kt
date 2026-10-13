package eu.kanade.tachiyomi.data.catalog

import androidx.core.net.toUri
import eu.kanade.tachiyomi.data.track.mangabaka.dto.MangaBakaSearchResult
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.parseAs
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient

class MangaBakaCatalogProvider(
    private val client: OkHttpClient,
    private val json: Json,
) : CatalogProvider {

    override val id: String = CatalogIds.MANGABAKA
    override val displayName: String = "MangaBaka"

    override suspend fun getTrending(page: Int): List<CatalogManga> =
        searchPage(page, query = TRENDING_QUERIES[(page - 1).coerceAtLeast(0) % TRENDING_QUERIES.size])

    override suspend fun getPopular(page: Int): List<CatalogManga> =
        searchPage(page, query = POPULAR_QUERIES[(page - 1).coerceAtLeast(0) % POPULAR_QUERIES.size])

    private suspend fun searchPage(page: Int, query: String): List<CatalogManga> {
        val url = "$API_BASE/v1/series/search".toUri().buildUpon()
            .appendQueryParameter("q", query)
            .appendQueryParameter("type_not", "novel")
            .appendQueryParameter("page", page.coerceAtLeast(1).toString())
            .build()
            .toString()

        val result = with(json) {
            client.newCall(GET(url)).awaitSuccess().parseAs<MangaBakaSearchResult>()
        }

        return result.data.map { item ->
            CatalogManga(
                id = item.id.toString(),
                title = item.chooseBestTitle(),
                coverUrl = item.cover.x250.x1.orEmpty(),
                synopsis = item.description?.trim().orEmpty(),
                genres = emptyList(),
                score = item.rating,
                status = item.status,
                trackingUrl = "https://mangabaka.org/${item.id}",
                author = item.authors?.firstOrNull().orEmpty(),
            )
        }
    }

    companion object {
        private const val API_BASE = "https://api.mangabaka.org"
        private val TRENDING_QUERIES = listOf("one", "solo", "jujutsu", "chainsaw", "blue")
        private val POPULAR_QUERIES = listOf("naruto", "bleach", "attack", "demon", "spy")
    }
}
