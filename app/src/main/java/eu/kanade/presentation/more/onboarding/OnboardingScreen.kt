package eu.kanade.presentation.more.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.components.GlassShapeMedium
import eu.kanade.presentation.components.GlassShapeTile
import eu.kanade.presentation.components.LiquidGlassSurface
import eu.kanade.presentation.components.nexusAtmosphereBrush
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ArrowBack
import mihon.icons.materialsymbols.rounded.Book
import mihon.icons.materialsymbols.rounded.Palette
import mihon.icons.materialsymbols.rounded.Security
import mihon.icons.materialsymbols.rounded.Storage
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

private enum class OnboardingTile(
    val titleRes: dev.icerock.moko.resources.StringResource,
    val icon: ImageVector,
) {
    Theme(MR.strings.onboarding_tile_theme, MaterialSymbols.Rounded.Palette),
    Storage(MR.strings.onboarding_tile_storage, MaterialSymbols.Rounded.Storage),
    Permissions(MR.strings.onboarding_tile_permissions, MaterialSymbols.Rounded.Security),
    Guides(MR.strings.onboarding_tile_guides, MaterialSymbols.Rounded.Book),
}

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    var selectedTile by rememberSaveable { mutableIntStateOf(-1) }
    val themeStep = remember { ThemeStep() }
    val storageStep = remember { StorageStep() }
    val permissionStep = remember { PermissionStep() }
    val guidesStep = remember { GuidesStep(onRestoreBackup = onRestoreBackup) }

    BackHandler(enabled = selectedTile >= 0) {
        selectedTile = -1
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(nexusAtmosphereBrush(isDark)),
    ) {
        AnimatedContent(
            targetState = selectedTile,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "onboarding",
            modifier = Modifier.fillMaxSize(),
        ) { tileIndex ->
            if (tileIndex < 0) {
                OnboardingTileGrid(
                    canFinish = storageStep.isComplete,
                    onTileClick = { selectedTile = it.ordinal },
                    onFinish = onComplete,
                )
            } else {
                val tile = OnboardingTile.entries[tileIndex]
                OnboardingTileDetail(
                    title = stringResource(tile.titleRes),
                    onBack = { selectedTile = -1 },
                    canFinish = storageStep.isComplete,
                    onFinish = onComplete,
                ) {
                    when (tile) {
                        OnboardingTile.Theme -> themeStep.Content()
                        OnboardingTile.Storage -> storageStep.Content()
                        OnboardingTile.Permissions -> permissionStep.Content()
                        OnboardingTile.Guides -> guidesStep.Content()
                    }
                }
            }
        }
    }
}

@Composable
private fun OnboardingTileGrid(
    canFinish: Boolean,
    onTileClick: (OnboardingTile) -> Unit,
    onFinish: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Text(
            text = stringResource(MR.strings.app_name),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(MR.strings.onboarding_glass_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp),
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 16.dp),
            modifier = Modifier.weight(1f),
        ) {
            items(OnboardingTile.entries) { tile ->
                LiquidGlassSurface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable { onTileClick(tile) },
                    shape = GlassShapeTile,
                    elevation = 10.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Icon(
                            imageVector = tile.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(tile.titleRes),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canFinish, onClick = onFinish),
            shape = GlassShapeMedium,
            elevation = 8.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(MR.strings.onboarding_action_finish),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canFinish) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        }
        if (!canFinish) {
            Text(
                text = stringResource(MR.strings.onboarding_storage_selection_required),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun OnboardingTileDetail(
    title: String,
    onBack: () -> Unit,
    canFinish: Boolean,
    onFinish: () -> Unit,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onBack),
            shape = GlassShapeMedium,
            elevation = 6.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            ) {
                    Icon(
                    imageVector = MaterialSymbols.AutoMirroredRounded.ArrowBack,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.align(Alignment.CenterStart),
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        LiquidGlassSurface(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(top = 14.dp),
            shape = GlassShapeTile,
            elevation = 8.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp),
            ) {
                content()
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LiquidGlassSurface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = canFinish, onClick = onFinish),
            shape = GlassShapeMedium,
            elevation = 8.dp,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(MR.strings.onboarding_action_finish),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (canFinish) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        }
    }
}
