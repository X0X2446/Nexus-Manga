package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.components.GlassShapeLarge
import eu.kanade.presentation.components.GlassShapeMedium
import eu.kanade.presentation.components.LiquidGlassSurface
import eu.kanade.presentation.components.liquidGlass
import eu.kanade.presentation.components.nexusAtmosphereBrush
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.data.catalog.CatalogIds
import eu.kanade.tachiyomi.data.catalog.CatalogManga
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Refresh
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.EmptyScreenAction
import tachiyomi.presentation.core.screens.LoadingScreen

@Composable
fun CatalogHomeContent(
    contentPadding: PaddingValues = PaddingValues(),
) {
    val viewModel = metroViewModel<CatalogViewModel>()
    val state by viewModel.state.collectAsState()
    val navigator = LocalNavigator.currentOrThrow
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSwitcher by remember { mutableStateOf(false) }
    var openingTitle by remember { mutableStateOf<String?>(null) }
    val gridState = rememberLazyGridState()
    val isDark = isSystemInDarkTheme()

    val shouldLoadMore by remember {
        derivedStateOf {
            val info = gridState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= info.totalItemsCount - 4
        }
    }
    LaunchedEffect(shouldLoadMore, state.loading, state.loadingMore, state.hasMore) {
        if (shouldLoadMore && !state.loading && !state.loadingMore && state.hasMore) {
            viewModel.loadNextPage()
        }
    }

    fun openManga(manga: CatalogManga) {
        if (state.isExtensionCatalog) {
            val sourceId = CatalogIds.sourceIdOrNull(state.activeCatalogId) ?: return
            if (openingTitle != null) return
            openingTitle = manga.title
            scope.launch {
                val local = viewModel.resolveMangaOnSource(sourceId, manga.title)
                openingTitle = null
                if (local != null) {
                    navigator.push(MangaScreen(local.id))
                } else {
                    context.toast(MR.strings.catalog_no_match)
                }
            }
        } else {
            navigator.push(
                TrackerMangaDetailScreen(
                    catalogId = state.activeCatalogId,
                    manga = manga,
                ),
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(nexusAtmosphereBrush(isDark)),
    ) {
        when {
            state.loading && state.popular.isEmpty() && state.featured.isEmpty() -> {
                LoadingScreen()
            }
            state.error != null && state.popular.isEmpty() -> {
                EmptyScreen(
                    message = stringResource(MR.strings.catalog_error),
                    actions = listOf(
                        EmptyScreenAction(
                            stringRes = MR.strings.action_retry,
                            icon = MaterialSymbols.Rounded.Refresh,
                            onClick = viewModel::retry,
                        ),
                    ),
                )
            }
            state.emptyExtensions -> {
                EmptyScreen(stringRes = MR.strings.catalog_no_extensions)
            }
            else -> {
                val coverDp = state.coverSizeDp.coerceIn(96, 168).dp
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(coverDp),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        // Keep content below the floating catalog switcher + status bar.
                        top = contentPadding.calculateTopPadding() + 108.dp,
                        bottom = contentPadding.calculateBottomPadding() + 96.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.featured.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            FeaturedCarousel(
                                items = state.featured.take(12),
                                onClick = ::openManga,
                            )
                        }
                    }

                    if (state.isTrackerCatalog && state.continueReading.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            SectionLabel(stringResource(MR.strings.catalog_continue))
                        }
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            ContinueRow(
                                items = state.continueReading,
                                coverWidth = coverDp,
                                onClick = { navigator.push(MangaScreen(it)) },
                            )
                        }
                    }

                    if (state.isTrackerCatalog) {
                        state.trackedSections.forEach { section ->
                            item(span = { GridItemSpan(maxLineSpan) }, key = "tracked-${section.status}") {
                                SectionLabel(stringResource(section.titleRes))
                            }
                            item(span = { GridItemSpan(maxLineSpan) }, key = "tracked-row-${section.status}") {
                                ContinueRow(
                                    items = section.items,
                                    coverWidth = coverDp,
                                    onClick = { navigator.push(MangaScreen(it)) },
                                )
                            }
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionLabel(stringResource(MR.strings.popular))
                    }

                    items(state.popular, key = { "pop-${it.id}-${it.title}" }) { manga ->
                        CatalogMangaCard(
                            manga = manga,
                            onClick = { openManga(manga) },
                        )
                    }

                    if (state.loadingMore || openingTitle != null) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                    }
                }
            }
        }

        CatalogSwitcherBubble(
            optionName = state.activeOption?.name ?: stringResource(MR.strings.label_catalog),
            logoRes = state.activeOption?.logoRes,
            onClick = { showSwitcher = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 12.dp, top = 8.dp),
        )
    }

    if (showSwitcher) {
        CatalogSwitcherSheet(
            trackers = state.trackerOptions,
            extensions = state.extensionOptions,
            selectedId = state.activeCatalogId,
            onSelect = {
                viewModel.setActiveCatalog(it)
                showSwitcher = false
            },
            onDismiss = { showSwitcher = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun FeaturedCarousel(
    items: List<CatalogManga>,
    onClick: (CatalogManga) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { items.size })
    HorizontalPager(
        state = pagerState,
        contentPadding = PaddingValues(end = 36.dp),
        pageSpacing = 12.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(176.dp),
    ) { page ->
        val manga = items[page]
        FeaturedCarouselCard(manga = manga, onClick = { onClick(manga) })
    }
}

@Composable
private fun FeaturedCarouselCard(
    manga: CatalogManga,
    onClick: () -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClick),
        shape = GlassShapeLarge,
        elevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            MangaCover.Book(
                data = manga.coverUrl.ifBlank { null },
                modifier = Modifier
                    .width(104.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(18.dp)),
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (manga.author.isNotBlank()) {
                    Text(
                        text = manga.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                if (manga.startDate.isNotBlank()) {
                    Text(
                        text = stringResource(MR.strings.catalog_release_date, manga.startDate),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp),
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
            }
        }
    }
}

@Composable
private fun ContinueRow(
    items: List<CatalogViewModel.ContinueItem>,
    coverWidth: androidx.compose.ui.unit.Dp,
    onClick: (Long) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(end = 8.dp),
    ) {
        items(items, key = { it.mangaId }) { item ->
            LiquidGlassSurface(
                modifier = Modifier
                    .width(coverWidth)
                    .clickable { onClick(item.mangaId) },
                shape = GlassShapeMedium,
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    MangaCover.Book(
                        data = item.coverUrl.ifBlank { null },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp)),
                    )
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun CatalogSwitcherBubble(
    optionName: String,
    logoRes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .liquidGlass(shape = CircleShape, elevation = 12.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (logoRes != null) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                )
            }
            Text(
                text = optionName,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun CatalogMangaCard(
    manga: CatalogManga,
    onClick: () -> Unit,
) {
    LiquidGlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = GlassShapeMedium,
        elevation = 4.dp,
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            MangaCover.Book(
                data = manga.coverUrl.ifBlank { null },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp)),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = manga.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
