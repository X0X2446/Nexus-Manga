package eu.kanade.tachiyomi.data.catalog

import androidx.core.net.toUri
import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.awaitSuccess
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Headers
import okhttp3.OkHttpClient

class MalCatalogProvider(
    private val client: OkHttpClient,
    private val json: Json,
) : CatalogProvider {

    override val id: String = CatalogIds.MAL
    override val displayName: String = "MyAnimeList"

    override suspend fun getTrending(page: Int): List<CatalogManga> =
        ranking(page, rankingType = "bypopularity")

    override suspend fun getPopular(page: Int): List<CatalogManga> =
        ranking(page, rankingType = "manga")

    private suspend fun ranking(page: Int, rankingType: String): List<CatalogManga> {
        val limit = 24
        val offset = (page - 1).coerceAtLeast(0) * limit
        val url = "$BASE_API/manga/ranking".toUri().buildUpon()
            .appendQueryParameter("ranking_type", rankingType)
            .appendQueryParameter("limit", limit.toString())
            .appendQueryParameter("offset", offset.toString())
            .appendQueryParameter("nsfw", "true")
            .appendQueryParameter(
                "fields",
                "id,title,main_picture,synopsis,mean,status,genres,authors{first_name,last_name},media_type",
            )
            .build()
            .toString()

        val headers = Headers.headersOf("X-MAL-CLIENT-ID", CLIENT_ID)
        val response = client.newCall(GET(url, headers)).awaitSuccess()
        val root = json.parseToJsonElement(response.body.string()).jsonObject
        val data = root["data"]?.jsonArray ?: return emptyList()

        return data.mapNotNull { element ->
            val node = element.jsonObject["node"]?.jsonObject ?: return@mapNotNull null
            val id = node["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            val title = node["title"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val mediaType = node["media_type"]?.jsonPrimitive?.contentOrNull.orEmpty()
            if (mediaType.contains("novel", ignoreCase = true)) return@mapNotNull null

            val cover = node["main_picture"]?.jsonObject
                ?.let { it["large"]?.jsonPrimitive?.contentOrNull ?: it["medium"]?.jsonPrimitive?.contentOrNull }
                .orEmpty()
            val authors = node["authors"]?.jsonArray.orEmpty().mapNotNull { author ->
                val person = author.jsonObject["node"]?.jsonObject ?: return@mapNotNull null
                listOfNotNull(
                    person["first_name"]?.jsonPrimitive?.contentOrNull,
                    person["last_name"]?.jsonPrimitive?.contentOrNull,
                ).joinToString(" ").trim().ifBlank { null }
            }

            CatalogManga(
                id = id.toString(),
                title = title,
                coverUrl = cover,
                synopsis = node["synopsis"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                genres = node["genres"]?.jsonArray?.mapNotNull {
                    it.jsonObject["name"]?.jsonPrimitive?.contentOrNull
                }.orEmpty(),
                score = node["mean"]?.jsonPrimitive?.doubleOrNull,
                status = node["status"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                trackingUrl = "https://myanimelist.net/manga/$id",
                author = authors.firstOrNull().orEmpty(),
            )
        }
    }

    companion object {
        private const val BASE_API = "https://api.myanimelist.net/v2"
        private const val CLIENT_ID = "c46c9e24640a64dad5be5ca7a1a53a0f"
    }
}
