package eu.kanade.tachiyomi.data.catalog

import eu.kanade.tachiyomi.network.POST
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.jsonMime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody

class AniListCatalogProvider(
    private val client: OkHttpClient,
    private val json: Json,
) : CatalogProvider {

    override val id: String = CatalogIds.ANILIST
    override val displayName: String = "AniList"

    override suspend fun getTrending(page: Int, pageSize: Int): List<CatalogManga> =
        browse(page, pageSize, sort = "TRENDING_DESC")

    override suspend fun getPopular(page: Int, pageSize: Int): List<CatalogManga> =
        browse(page, pageSize, sort = "POPULARITY_DESC")

    private suspend fun browse(page: Int, pageSize: Int, sort: String): List<CatalogManga> {
        val perPage = pageSize.coerceIn(6, 48)
        val query = """
            query (${'$'}page: Int, ${'$'}perPage: Int) {
              Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                media(type: MANGA, format_not_in: [NOVEL], sort: [$sort]) {
                  id
                  title { userPreferred romaji english }
                  coverImage { large }
                  description(asHtml: false)
                  genres
                  averageScore
                  status
                  siteUrl
                  chapters
                  startDate { year month day }
                  staff(perPage: 1, sort: [RELEVANCE]) {
                    nodes { name { userPreferred } }
                  }
                }
              }
            }
        """.trimIndent()

        val body = buildJsonObject {
            put("query", query)
            put(
                "variables",
                buildJsonObject {
                    put("page", page)
                    put("perPage", perPage)
                },
            )
        }.toString().toRequestBody(jsonMime)

        val response = client.newCall(POST(GRAPHQL_URL, body = body)).awaitSuccess()
        val root = json.parseToJsonElement(response.body.string()).jsonObject
        val media = root["data"]
            ?.jsonObject
            ?.get("Page")
            ?.jsonObject
            ?.get("media")
            ?.jsonArray
            ?: return emptyList()

        return media.mapNotNull { element ->
            val item = element as? JsonObject ?: return@mapNotNull null
            val titles = item["title"]?.jsonObject
            val title = titles?.get("userPreferred")?.jsonPrimitive?.contentOrNull
                ?: titles?.get("english")?.jsonPrimitive?.contentOrNull
                ?: titles?.get("romaji")?.jsonPrimitive?.contentOrNull
                ?: return@mapNotNull null
            val id = item["id"]?.jsonPrimitive?.intOrNull ?: return@mapNotNull null
            CatalogManga(
                id = id.toString(),
                title = title,
                coverUrl = item["coverImage"]?.jsonObject?.get("large")?.jsonPrimitive?.contentOrNull.orEmpty(),
                synopsis = item["description"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                genres = item["genres"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
                score = item["averageScore"]?.jsonPrimitive?.doubleOrNull?.div(10.0),
                status = item["status"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                trackingUrl = item["siteUrl"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                author = item["staff"]
                    ?.jsonObject
                    ?.get("nodes")
                    ?.jsonArray
                    ?.firstOrNull()
                    ?.jsonObject
                    ?.get("name")
                    ?.jsonObject
                    ?.get("userPreferred")
                    ?.jsonPrimitive
                    ?.contentOrNull
                    .orEmpty(),
                startDate = item["startDate"]?.jsonObject?.let { date ->
                    listOfNotNull(
                        date["year"]?.jsonPrimitive?.contentOrNull,
                        date["month"]?.jsonPrimitive?.contentOrNull?.padStart(2, '0'),
                        date["day"]?.jsonPrimitive?.contentOrNull?.padStart(2, '0'),
                    ).joinToString("-").takeIf { it.isNotBlank() && it != "--" }.orEmpty()
                }.orEmpty(),
                chaptersCount = item["chapters"]?.jsonPrimitive?.intOrNull,
            )
        }
    }

    companion object {
        private const val GRAPHQL_URL = "https://graphql.anilist.co"
    }
}
