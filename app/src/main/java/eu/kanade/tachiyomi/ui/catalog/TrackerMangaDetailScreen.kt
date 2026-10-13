package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.manga.components.MangaCover
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.data.catalog.CatalogManga
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.ui.browse.source.browse.BrowseSourceScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mihon.app.di.appGraph
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.roundedfilled.Extension
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
        var sources by remember { mutableStateOf<List<CatalogueSource>>(emptyList()) }
        var showPicker by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            sources = withContext(Dispatchers.IO) {
                context.appGraph.sourceManager.getOnlineSources()
                    .filter { it.id != LocalSource.ID }
                    .filterIsInstance<CatalogueSource>()
                    .sortedBy { it.name.lowercase() }
            }
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(manga.title, maxLines = 1) },
                    navigationIcon = {
                        IconButton(onClick = navigator::pop) {
                            Icon(
                                imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                                contentDescription = null,
                            )
                        }
                    },
                )
            },
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
                    MangaCover.Book(
                        data = manga.coverUrl.ifBlank { null },
                        modifier = Modifier
                            .width(180.dp)
                            .padding(bottom = 16.dp),
                    )
                }

                Text(
                    text = manga.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (manga.author.isNotBlank()) {
                    Text(
                        text = manga.author,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
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
                            AssistChip(
                                onClick = {},
                                label = { Text(genre) },
                            )
                        }
                    }
                }

                if (manga.synopsis.isNotBlank()) {
                    Text(
                        text = manga.synopsis
                            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
                            .replace(Regex("<[^>]+>"), ""),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (sources.isNotEmpty()) {
                    Button(
                        onClick = { showPicker = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.RoundedFilled.Extension,
                            contentDescription = null,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(MR.strings.action_read))
                    }
                }
            }
        }

        if (showPicker) {
            ExtensionPickerSheet(
                sources = sources,
                onSelect = { source ->
                    showPicker = false
                    navigator.push(BrowseSourceScreen(source.id, listingQuery = manga.title))
                },
                onDismiss = { showPicker = false },
            )
        }
    }
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
