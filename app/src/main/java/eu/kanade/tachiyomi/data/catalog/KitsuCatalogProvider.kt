package eu.kanade.tachiyomi.data.catalog

import androidx.core.net.toUri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient

class KitsuCatalogProvider(
    private val client: OkHttpClient,
    private val json: Json,
) : CatalogProvider {

    override val id: String = CatalogIds.KITSU
    override val displayName: String = "Kitsu"

    override suspend fun getTrending(page: Int): List<CatalogManga> =
        browse(page, sort = "-userCount")

    override suspend fun getPopular(page: Int): List<CatalogManga> =
        browse(page, sort = "-averageRating")

    private suspend fun browse(page: Int, sort: String): List<CatalogManga> {
        val limit = 24
        val offset = (page - 1).coerceAtLeast(0) * limit
        val url = "$BASE_API/manga".toUri().buildUpon()
            .appendQueryParameter("sort", sort)
            .appendQueryParameter("page[limit]", limit.toString())
            .appendQueryParameter("page[offset]", offset.toString())
            .appendQueryParameter("filter[subtype]", "manga,manhwa,manhua")
            .build()
            .toString()

        val response = client.newCall(GET(url)).awaitSuccess()
        val root = json.parseToJsonElement(response.body.string()).jsonObject
        val data = root["data"]?.jsonArray ?: return emptyList()

        return data.mapNotNull { element ->
            val item = element.jsonObject
            val id = item["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val attrs = item["attributes"]?.jsonObject ?: return@mapNotNull null
            val titles = attrs["titles"]?.jsonObject
            val title = attrs["canonicalTitle"]?.jsonPrimitive?.contentOrNull
                ?: titles?.get("en")?.jsonPrimitive?.contentOrNull
                ?: titles?.get("en_jp")?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val poster = attrs["posterImage"]?.jsonObject
            val cover = poster?.get("large")?.jsonPrimitive?.contentOrNull
                ?: poster?.get("medium")?.jsonPrimitive?.contentOrNull
                ?: poster?.get("original")?.jsonPrimitive?.contentOrNull
                .orEmpty()
            val slug = attrs["slug"]?.jsonPrimitive?.contentOrNull

            CatalogManga(
                id = id,
                title = title,
                coverUrl = cover,
                synopsis = attrs["synopsis"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                genres = emptyList(),
                score = attrs["averageRating"]?.jsonPrimitive?.contentOrNull?.toDoubleOrNull()?.div(10.0)
                    ?: attrs["averageRating"]?.jsonPrimitive?.doubleOrNull?.div(10.0),
                status = attrs["status"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                trackingUrl = slug?.let { "https://kitsu.app/manga/$it" }.orEmpty(),
                startDate = attrs["startDate"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                chaptersCount = attrs["chapterCount"]?.jsonPrimitive?.contentOrNull?.toIntOrNull(),
            )
        }
    }

    companion object {
        private const val BASE_API = "https://kitsu.app/api/edge"
    }
}
