package eu.kanade.tachiyomi.ui.catalog

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.icons.materialsymbols.rounded.Explore
import mihon.icons.materialsymbols.rounded.Search
import mihon.icons.materialsymbols.rounded.Settings
import mihon.icons.materialsymbols.roundedfilled.Extension
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

enum class BubbleDestination {
    Home,
    Search,
    Extensions,
    Settings,
}

@Composable
fun BoxScope.FloatingBubbleNav(
    selected: BubbleDestination,
    onDestination: (BubbleDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(expanded) {
        progress.animateTo(
            targetValue = if (expanded) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        )
    }

    if (expanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { expanded = false },
                ),
        )
    }

    Box(
        modifier = modifier
            .align(Alignment.BottomEnd)
            .navigationBarsPadding()
            .padding(end = 18.dp, bottom = 18.dp),
    ) {
        val destinations = listOf(
            BubbleDestination.Home to MaterialSymbols.Rounded.Explore,
            BubbleDestination.Search to MaterialSymbols.Rounded.Search,
            BubbleDestination.Extensions to MaterialSymbols.RoundedFilled.Extension,
            BubbleDestination.Settings to MaterialSymbols.Rounded.Settings,
        )
        destinations.forEachIndexed { index, (destination, icon) ->
            val offsetY = (-(68 * (index + 1))).dp * progress.value
            val scale = 0.55f + (0.45f * progress.value)
            if (progress.value > 0.01f) {
                BubbleButton(
                    icon = icon,
                    label = destination.label(),
                    selected = selected == destination,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = offsetY)
                        .scale(scale),
                    onClick = {
                        expanded = false
                        onDestination(destination)
                    },
                )
            }
        }

        Surface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(58.dp)
                .clip(CircleShape)
                .clickable {
                    scope.launch { expanded = !expanded }
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            shadowElevation = 8.dp,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (expanded) {
                        MaterialSymbols.Rounded.Close
                    } else {
                        MaterialSymbols.Rounded.Explore
                    },
                    contentDescription = stringResource(MR.strings.label_home),
                    tint = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

@Composable
private fun BubbleButton(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.CenterEnd) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontSize = 12.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .padding(end = 66.dp)
                .background(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                    shape = MaterialTheme.shapes.small,
                )
                .padding(horizontal = 10.dp, vertical = 4.dp),
        )
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(onClick = onClick),
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.secondary
            } else {
                MaterialTheme.colorScheme.surface
            },
            shadowElevation = 6.dp,
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (selected) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                )
            }
        }
    }
}

@Composable
private fun BubbleDestination.label(): String = when (this) {
    BubbleDestination.Home -> stringResource(MR.strings.label_home)
    BubbleDestination.Search -> stringResource(MR.strings.action_search)
    BubbleDestination.Extensions -> stringResource(MR.strings.label_extensions)
    BubbleDestination.Settings -> stringResource(MR.strings.label_settings)
}
