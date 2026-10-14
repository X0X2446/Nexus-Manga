package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.GlassShapeMedium
import eu.kanade.presentation.components.LiquidGlassSurface
import eu.kanade.presentation.components.nexusAtmosphereBrush
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.catalog.CatalogManga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import mihon.domain.manga.model.toDomainManga
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.roundedfilled.Extension
import tachiyomi.domain.manga.model.Manga
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.LocalSource

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
        var sources by remember { mutableStateOf<List<CatalogueSource>>(emptyList()) }
        var showPicker by remember { mutableStateOf(false) }
        var opening by remember { mutableStateOf(false) }
        var descriptionExpanded by remember { mutableStateOf(false) }
        val isDark = isSystemInDarkTheme()
        val cleanSynopsis = remember(manga.synopsis) {
            manga.synopsis
                .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                .replace(Regex("<[^>]+>"), "")
                .trim()
        }

        LaunchedEffect(Unit) {
            sources = withContext(Dispatchers.IO) {
                val graph = context.appGraph
                graph.extensionManager.getLoadedExtensions()
                graph.sourceManager.getOnlineSources()
                    .filter { it.id != LocalSource.ID }
                    .filterIsInstance<CatalogueSource>()
                    .sortedBy { it.name.lowercase() }
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    LiquidGlassSurface(shape = GlassShapeMedium) {
                        MangaCover.Book(
                            data = manga.coverUrl.ifBlank { null },
                            modifier = Modifier
                                .width(180.dp)
                                .padding(10.dp),
                        )
                    }
                }

                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 16.dp),
                )
                if (manga.author.isNotBlank()) {
                    Text(
                        text = manga.author,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (manga.startDate.isNotBlank()) {
                    Text(
                        text = stringResource(MR.strings.catalog_release_date, manga.startDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (manga.chaptersCount != null && manga.chaptersCount > 0) {
                    Text(
                        text = stringResource(MR.strings.catalog_chapters_count, manga.chaptersCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
                if (manga.score != null) {
                    Text(
                        text = "★ %.1f".format(manga.score),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }

                if (manga.genres.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 12.dp),
                    ) {
                        manga.genres.take(12).forEach { genre ->
                            AssistChip(onClick = {}, label = { Text(genre) })
                        }
                    }
                }

                if (cleanSynopsis.isNotBlank()) {
                    LiquidGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .clickable { descriptionExpanded = !descriptionExpanded },
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

                Spacer(modifier = Modifier.height(24.dp))

                if (sources.isNotEmpty()) {
                    LiquidGlassSurface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !opening) { showPicker = true },
                        shape = GlassShapeMedium,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (opening) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                    Text(stringResource(MR.strings.catalog_opening_manga))
                                }
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = MaterialSymbols.RoundedFilled.Extension,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Text(
                                        text = stringResource(MR.strings.action_read),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showPicker) {
            ExtensionPickerSheet(
                sources = sources,
                onSelect = { source ->
                    showPicker = false
                    opening = true
                    scope.launch {
                        val localManga = withContext(Dispatchers.IO) {
                            openMangaFromSource(context, source, manga.title)
                        }
                        opening = false
                        if (localManga != null) {
                            navigator.push(MangaScreen(localManga.id))
                        } else {
                            context.toast(MR.strings.catalog_no_match)
                        }
                    }
                },
                onDismiss = { showPicker = false },
            )
        }
    }
}

private suspend fun openMangaFromSource(
    context: android.content.Context,
    source: CatalogueSource,
    title: String,
): Manga? {
    val results = source.getSearchManga(1, title, source.getFilterList()).mangas
    if (results.isEmpty()) return null
    val match = results.firstOrNull { it.title.equals(title, ignoreCase = true) }
        ?: results.firstOrNull {
            it.title.contains(title, ignoreCase = true) || title.contains(it.title, ignoreCase = true)
        }
        ?: results.first()
    return context.appGraph.networkToLocalManga(match.toDomainManga(source.id))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExtensionPickerSheet(
    sources: List<CatalogueSource>,
    onSelect: (CatalogueSource) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(MR.strings.catalog_choose_extension),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            sources.forEach { source ->
                ListItem(
                    headlineContent = { Text(source.name) },
                    supportingContent = { Text(source.lang.uppercase()) },
                    leadingContent = {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Extension,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(source) },
                )
            }
        }
    }
}
