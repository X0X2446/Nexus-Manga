package eu.kanade.tachiyomi.data.catalog

import dev.icerock.moko.resources.StringResource

data class CatalogManga(
    val id: String,
    val title: String,
    val coverUrl: String,
    val synopsis: String = "",
    val genres: List<String> = emptyList(),
    val score: Double? = null,
    val status: String = "",
    val trackingUrl: String = "",
    val author: String = "",
) : java.io.Serializable

enum class CatalogKind {
    TRACKER,
    EXTENSION,
}

data class CatalogOption(
    val id: String,
    val name: String,
    val descriptionRes: StringResource,
    val kind: CatalogKind,
    val logoRes: Int? = null,
    val sourceId: Long? = null,
)

interface CatalogProvider {
    val id: String
    val displayName: String
    suspend fun getTrending(page: Int): List<CatalogManga>
    suspend fun getPopular(page: Int): List<CatalogManga>
}

object CatalogIds {
    const val ANILIST = "tracker:anilist"
    const val MAL = "tracker:mal"
    const val KITSU = "tracker:kitsu"
    const val MANGABAKA = "tracker:mangabaka"

    fun extension(sourceId: Long) = "extension:$sourceId"

    fun isExtension(id: String) = id.startsWith("extension:")

    fun sourceIdOrNull(id: String): Long? =
        id.removePrefix("extension:").toLongOrNull().takeIf { isExtension(id) }
}
