package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import dev.zacsweers.metrox.viewmodel.metroViewModel
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.tachiyomi.data.catalog.CatalogIds
import eu.kanade.tachiyomi.data.catalog.CatalogManga
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
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
    var showSwitcher by remember { mutableStateOf(false) }
    val gridState = rememberLazyGridState()

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0E1518),
                        Color(0xFF162226),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        when {
            state.loading && state.popular.isEmpty() && state.trending.isEmpty() -> {
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
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(118.dp),
                    state = gridState,
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = contentPadding.calculateTopPadding() + 72.dp,
                        bottom = contentPadding.calculateBottomPadding() + 96.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                text = stringResource(MR.strings.app_name),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE8F1F0),
                            )
                            Text(
                                text = state.activeOption?.name
                                    ?: stringResource(MR.strings.label_catalog),
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF9DB5B1),
                            )
                        }
                    }

                    if (state.trending.isNotEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = stringResource(MR.strings.catalog_trending),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFE8F1F0),
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                            )
                        }
                        items(state.trending.take(6), key = { "trend-${it.id}-${it.title}" }) { manga ->
                            CatalogMangaCard(
                                manga = manga,
                                onClick = {
                                    openManga(navigator, state, manga)
                                },
                            )
                        }
                    }

                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(MR.strings.popular),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE8F1F0),
                            modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                        )
                    }

                    items(state.popular, key = { "pop-${it.id}-${it.title}" }) { manga ->
                        CatalogMangaCard(
                            manga = manga,
                            onClick = {
                                openManga(navigator, state, manga)
                            },
                        )
                    }

                    if (state.loadingMore) {
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
                .padding(start = 16.dp, top = 10.dp),
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

private fun openManga(
    navigator: cafe.adriel.voyager.navigator.Navigator,
    state: CatalogViewModel.State,
    manga: CatalogManga,
) {
    if (state.isExtensionCatalog) {
        val sourceId = CatalogIds.sourceIdOrNull(state.activeCatalogId) ?: return
        navigator.push(BrowseSourceScreen(sourceId, listingQuery = manga.title))
    } else {
        navigator.push(
            TrackerMangaDetailScreen(
                catalogId = state.activeCatalogId,
                manga = manga,
            ),
        )
    }
}

@Composable
private fun CatalogSwitcherBubble(
    optionName: String,
    logoRes: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        shadowElevation = 6.dp,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (logoRes != null) {
                Image(
                    painter = painterResource(logoRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        MangaCover.Book(
            data = manga.coverUrl.ifBlank { null },
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium),
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = manga.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color(0xFFE8F1F0),
        )
    }
}
