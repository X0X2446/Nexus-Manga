package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import coil3.compose.AsyncImage
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.components.GlassShapeLarge
import eu.kanade.presentation.components.GlassShapeMedium
import eu.kanade.presentation.components.GlassShapeSmall
import eu.kanade.presentation.components.LiquidGlassSurface
import eu.kanade.presentation.components.nexusAtmosphereBrush
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.catalog.CatalogManga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.reader.ReaderActivity
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import mihon.domain.manga.model.toDomainManga
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Check
import mihon.icons.materialsymbols.rounded.KeyboardArrowDown
import mihon.icons.materialsymbols.rounded.KeyboardArrowUp
import mihon.icons.materialsymbols.rounded.Public
import mihon.icons.materialsymbols.roundedfilled.Circle
import mihon.icons.materialsymbols.roundedfilled.Extension
import tachiyomi.domain.chapter.model.Chapter
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.DISABLED_ALPHA
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TrackerMangaDetailScreen(
    private val catalogId: String,
    private val manga: CatalogManga,
) : Screen() {

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val isDark = isSystemInDarkTheme()

        var extensionGroups by remember { mutableStateOf<List<ExtensionGroup>>(emptyList()) }
        var showPicker by remember { mutableStateOf(false) }
        var selectedSource by remember { mutableStateOf<CatalogueSource?>(null) }
        var selectedExtensionName by remember { mutableStateOf<String?>(null) }
        var resolvedManga by remember { mutableStateOf<Manga?>(null) }
        var chapters by remember { mutableStateOf<List<Chapter>>(emptyList()) }
        var loadingChapters by remember { mutableStateOf(false) }
        var chapterError by remember { mutableStateOf(false) }
        var descriptionExpanded by remember { mutableStateOf(false) }

        val cleanSynopsis = remember(manga.synopsis) {
            manga.synopsis
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), "")
                .trim()
        }
        val dateFormatter = remember {
            UiPreferences.dateFormat("")
        }

        LaunchedEffect(Unit) {
            extensionGroups = withContext(Dispatchers.IO) {
                val graph = context.appGraph
                graph.extensionManager.getLoadedExtensions()
                    .mapNotNull { extension ->
                        val sources = extension.sources
                            .filterIsInstance<CatalogueSource>()
                            .filter { it.id != LocalSource.ID }
                            .sortedBy { it.lang.lowercase() }
                        if (sources.isEmpty()) return@mapNotNull null
                        ExtensionGroup(
                            name = extension.name.removePrefix("Tachiyomi: ").trim(),
                            pkgName = extension.pkgName,
                            sources = sources,
                        )
                    }
                    .sortedBy { it.name.lowercase() }
            }
        }

        fun loadFromSource(source: CatalogueSource, extensionName: String) {
            selectedSource = source
            selectedExtensionName = extensionName
            showPicker = false
            loadingChapters = true
            chapterError = false
            chapters = emptyList()
            resolvedManga = null
            scope.launch {
                val result = withContext(Dispatchers.IO) {
                    resolveAndLoadChapters(context, source, manga.title)
                }
                loadingChapters = false
                if (result == null) {
                    chapterError = true
                    context.toast(MR.strings.catalog_no_match)
                } else {
                    resolvedManga = result.manga
                    chapters = result.chapters
                    chapterError = false
                }
            }
        }

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text(manga.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(
                                imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
            modifier = Modifier.background(nexusAtmosphereBrush(isDark)),
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(bottom = 32.dp),
            ) {
                item {
                    DetailHero(
                        manga = manga,
                        cleanSynopsis = cleanSynopsis,
                        descriptionExpanded = descriptionExpanded,
                        onToggleDescription = { descriptionExpanded = !descriptionExpanded },
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    SourceSelectorCard(
                        extensionName = selectedExtensionName,
                        source = selectedSource,
                        hasExtensions = extensionGroups.isNotEmpty(),
                        loading = loadingChapters,
                        onClick = {
                            if (!loadingChapters && extensionGroups.isNotEmpty()) {
                                showPicker = true
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }

                if (resolvedManga != null) {
                    item {
                        Text(
                            text = stringResource(
                                MR.strings.catalog_matched_as,
                                resolvedManga!!.title,
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        )
                    }
                }

                item {
                    Text(
                        text = stringResource(MR.strings.catalog_chapters_section),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }

                when {
                    loadingChapters -> {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                                    Text(
                                        text = stringResource(MR.strings.catalog_loading_chapters),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 12.dp),
                                    )
                                }
                            }
                        }
                    }
                    chapterError -> {
                        item {
                            Text(
                                text = stringResource(MR.strings.catalog_chapter_error),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    }
                    selectedSource == null -> {
                        item {
                            Text(
                                text = stringResource(MR.strings.catalog_tap_choose_source),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    }
                    chapters.isEmpty() -> {
                        item {
                            Text(
                                text = stringResource(MR.strings.catalog_no_chapters),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            )
                        }
                    }
                    else -> {
                        items(chapters, key = { it.id }) { chapter ->
                            ChapterGlassRow(
                                chapter = chapter,
                                dateText = formatChapterDate(chapter.dateUpload, dateFormatter),
                                onClick = {
                                    context.startActivity(
                                        ReaderActivity.newIntent(context, chapter.mangaId, chapter.id),
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }
        }

        if (showPicker) {
            ExtensionLanguagePickerSheet(
                groups = extensionGroups,
                selectedSourceId = selectedSource?.id,
                onSelect = { group, source -> loadFromSource(source, group.name) },
                onDismiss = { showPicker = false },
            )
        }
    }
}

@Immutable
private data class ExtensionGroup(
    val name: String,
    val pkgName: String,
    val sources: List<CatalogueSource>,
) {
    val isMultiLang: Boolean get() = sources.size > 1
}

@Immutable
private data class ResolveResult(
    val manga: Manga,
    val chapters: List<Chapter>,
)

private suspend fun resolveAndLoadChapters(
    context: android.content.Context,
    source: CatalogueSource,
    title: String,
): ResolveResult? {
    val graph = context.appGraph
    val results = source.getSearchManga(1, title, source.getFilterList()).mangas
    if (results.isEmpty()) return null
    val match = results.firstOrNull { it.title.equals(title, ignoreCase = true) }
        ?: results.firstOrNull {
            it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
        }
        ?: results.first()
    val localManga = graph.networkToLocalManga(match.toDomainManga(source.id))
    val update = graph.updateMangaFromRemote(
        source = source,
        manga = localManga,
        fetchDetails = true,
        fetchChapters = true,
        manualFetch = true,
    ).getOrNull() ?: return ResolveResult(
        manga = localManga,
        chapters = graph.getChaptersByMangaId.await(localManga.id),
    )
    val chapters = graph.getChaptersByMangaId.await(update.manga.id)
        .sortedWith(
            compareByDescending<Chapter> { it.chapterNumber >= 0 }
                .thenByDescending { it.chapterNumber }
                .thenByDescending { it.dateUpload },
        )
    return ResolveResult(manga = update.manga, chapters = chapters)
}

private fun formatChapterDate(
    dateUpload: kotlin.time.Instant?,
    formatter: DateTimeFormatter,
): String? {
    if (dateUpload == null) return null
    return runCatching {
        formatter.format(
            Instant.ofEpochMilli(dateUpload.toEpochMilliseconds())
                .atZone(ZoneId.systemDefault())
                .toLocalDate(),
        )
    }.getOrNull()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DetailHero(
    manga: CatalogManga,
    cleanSynopsis: String,
    descriptionExpanded: Boolean,
    onToggleDescription: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp),
        ) {
            AsyncImage(
                model = manga.coverUrl.ifBlank { null },
                contentDescription = null,
                placeholder = ColorPainter(Color(0x1F888888)),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background.copy(alpha = 0.55f),
                                MaterialTheme.colorScheme.background,
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (manga.author.isNotBlank()) {
                    Text(
                        text = manga.author,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    if (manga.score != null) {
                        MetaBadge("★ %.1f".format(manga.score))
                    }
                    if (manga.chaptersCount != null && manga.chaptersCount > 0) {
                        MetaBadge(stringResource(MR.strings.catalog_chapters_count, manga.chaptersCount))
                    }
                    if (manga.startDate.isNotBlank()) {
                        MetaBadge(manga.startDate.take(4))
                    }
                }
            }
        }

        if (manga.genres.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                manga.genres.take(10).forEach { genre ->
                    AssistChip(onClick = {}, label = { Text(genre) })
                }
            }
        }

        if (cleanSynopsis.isNotBlank()) {
            LiquidGlassSurface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable(onClick = onToggleDescription),
                shape = GlassShapeMedium,
                elevation = 4.dp,
            ) {
                Column(
                    modifier = Modifier
                        .padding(14.dp)
                        .animateContentSize(),
                ) {
                    Text(
                        text = cleanSynopsis,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (descriptionExpanded) Int.MAX_VALUE else 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = stringResource(
                            if (descriptionExpanded) {
                                MR.strings.catalog_show_less
                            } else {
                                MR.strings.catalog_read_more
                            },
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun MetaBadge(text: String) {
    LiquidGlassSurface(shape = GlassShapeSmall, elevation = 2.dp) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun SourceSelectorCard(
    extensionName: String?,
    source: CatalogueSource?,
    hasExtensions: Boolean,
    loading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = hasExtensions && !loading, onClick = onClick),
        shape = GlassShapeLarge,
        elevation = 10.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            LiquidGlassSurface(
                modifier = Modifier.size(42.dp),
                shape = RoundedCornerShape(12.dp),
                elevation = 0.dp,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Extension,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        loading -> stringResource(MR.strings.catalog_loading_chapters)
                        extensionName != null -> extensionName.uppercase()
                        else -> stringResource(MR.strings.action_read)
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    if (source != null) {
                        LangBadge(source.lang.uppercase())
                        Text(
                            text = stringResource(MR.strings.catalog_change_source),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Text(
                            text = if (hasExtensions) {
                                stringResource(MR.strings.catalog_select_source)
                            } else {
                                stringResource(MR.strings.catalog_no_extensions)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Icon(
                imageVector = MaterialSymbols.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LangBadge(lang: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(
            text = lang,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun ChapterGlassRow(
    chapter: Chapter,
    dateText: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LiquidGlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = GlassShapeMedium,
        elevation = 3.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!chapter.read) {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Circle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(8.dp),
                        )
                    }
                    Text(
                        text = chapter.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (chapter.read) FontWeight.Normal else FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (chapter.read) DISABLED_ALPHA else 1f,
                        ),
                    )
                }
                val subtitle = buildList {
                    dateText?.let { add(it) }
                    chapter.scanlator?.takeIf { it.isNotBlank() }?.let { add(it) }
                    if (chapter.lastPageRead > 0 && !chapter.read) {
                        add("Page ${chapter.lastPageRead + 1}")
                    }
                }.joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtensionLanguagePickerSheet(
    groups: List<ExtensionGroup>,
    selectedSourceId: Long?,
    onSelect: (ExtensionGroup, CatalogueSource) -> Unit,
    onDismiss: () -> Unit,
) {
    var expandedPkg by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.RoundedFilled.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(MR.strings.catalog_select_source),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(start = 10.dp)
                        .weight(1f),
                )
                Text(
                    text = "${groups.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            groups.forEach { group ->
                val expanded = expandedPkg == group.pkgName
                val containsSelected = group.sources.any { it.id == selectedSourceId }

                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(),
                    shape = GlassShapeMedium,
                    elevation = if (containsSelected) 10.dp else 4.dp,
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (group.isMultiLang) {
                                        expandedPkg = if (expanded) null else group.pkgName
                                    } else {
                                        onSelect(group, group.sources.first())
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = MaterialSymbols.RoundedFilled.Extension,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = group.name.uppercase(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (containsSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = if (group.isMultiLang) {
                                        stringResource(MR.strings.catalog_languages_count, group.sources.size)
                                    } else {
                                        group.sources.first().lang.uppercase()
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            if (group.isMultiLang) {
                                Icon(
                                    imageVector = if (expanded) {
                                        MaterialSymbols.Rounded.KeyboardArrowUp
                                    } else {
                                        MaterialSymbols.Rounded.KeyboardArrowDown
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else if (containsSelected) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }

                        AnimatedVisibility(
                            visible = group.isMultiLang && expanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut(),
                        ) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 12.dp, bottom = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.padding(bottom = 2.dp),
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbols.Rounded.Public,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp),
                                    )
                                    Text(
                                        text = stringResource(MR.strings.catalog_language_variants),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                group.sources.forEach { source ->
                                    val selected = source.id == selectedSourceId
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (selected) {
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                                },
                                            )
                                            .clickable { onSelect(group, source) }
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    ) {
                                        LangBadge(source.lang.uppercase())
                                        Text(
                                            text = source.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                            color = if (selected) {
                                                MaterialTheme.colorScheme.primary
                                            } else {
                                                MaterialTheme.colorScheme.onSurface
                                            },
                                            modifier = Modifier.weight(1f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (selected) {
                                            Icon(
                                                imageVector = MaterialSymbols.Rounded.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
