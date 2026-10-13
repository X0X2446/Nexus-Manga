package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.browse.source.globalsearch.GlobalSearchScreen
import eu.kanade.tachiyomi.ui.catalog.BubbleDestination
import eu.kanade.tachiyomi.ui.catalog.CatalogTab
import eu.kanade.tachiyomi.ui.catalog.FloatingBubbleNav
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.manga.MangaScreen
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        TabNavigator(
            tab = CatalogTab,
            key = TabNavigatorKey,
        ) { tabNavigator ->
            CompositionLocalProvider(LocalNavigator provides navigator) {
                var bubbleSelected by remember {
                    mutableStateOf(BubbleDestination.Home)
                }
                var showBubble by remember { mutableStateOf(true) }

                LaunchedEffect(Unit) {
                    showBottomNavEvent.receiveAsFlow().collectLatest { show ->
                        showBubble = show
                    }
                }

                LaunchedEffect(tabNavigator.current) {
                    bubbleSelected = when (tabNavigator.current) {
                        BrowseTab -> BubbleDestination.Extensions
                        CatalogTab -> BubbleDestination.Home
                        else -> bubbleSelected
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    AnimatedContent(
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            materialFadeThroughIn(
                                initialScale = 1f,
                                durationMillis = TabFadeDuration,
                            ) togetherWith materialFadeThroughOut(durationMillis = TabFadeDuration)
                        },
                        label = "tabContent",
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        tabNavigator.saveableState(key = "currentTab", it) {
                            it.Content()
                        }
                    }

                    if (showBubble) {
                        FloatingBubbleNav(
                            selected = bubbleSelected,
                            onDestination = { destination ->
                                bubbleSelected = destination
                                when (destination) {
                                    BubbleDestination.Home -> {
                                        tabNavigator.current = CatalogTab
                                    }
                                    BubbleDestination.Search -> {
                                        navigator.push(GlobalSearchScreen())
                                    }
                                    BubbleDestination.Extensions -> {
                                        BrowseTab.showExtension()
                                        tabNavigator.current = BrowseTab
                                    }
                                    BubbleDestination.Settings -> {
                                        navigator.push(SettingsScreen())
                                    }
                                }
                            },
                        )
                    }
                }
            }

            val goToCatalogTab = { tabNavigator.current = CatalogTab }

            BackHandler(
                enabled = tabNavigator.current != CatalogTab,
                onBack = goToCatalogTab,
            )

            LaunchedEffect(Unit) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = LibraryTab
                        LibraryTab.search(it)
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        tabNavigator.current = when (it) {
                            is Tab.Library -> LibraryTab
                            Tab.Updates -> UpdatesTab
                            Tab.History -> HistoryTab
                            is Tab.Browse -> {
                                if (it.toExtensions) {
                                    BrowseTab.showExtension()
                                }
                                BrowseTab
                            }
                            is Tab.More -> MoreTab
                            Tab.Catalog -> CatalogTab
                        }

                        if (it is Tab.Library && it.mangaIdToOpen != null) {
                            navigator.push(MangaScreen(it.mangaIdToOpen))
                        }
                        if (it is Tab.More && it.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    sealed interface Tab {
        data object Catalog : Tab
        data class Library(val mangaIdToOpen: Long? = null) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
    }
}
