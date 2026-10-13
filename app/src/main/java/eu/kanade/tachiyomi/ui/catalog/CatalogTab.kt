package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.main.MainActivity
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Explore
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

data object CatalogTab : Tab {

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            index = 0u,
            title = stringResource(MR.strings.label_home),
            icon = rememberVectorPainter(MaterialSymbols.Rounded.Explore),
        )

    @Composable
    override fun Content() {
        val context = LocalContext.current
        CatalogHomeContent()
        LaunchedEffect(Unit) {
            (context as? MainActivity)?.ready = true
        }
    }
}
