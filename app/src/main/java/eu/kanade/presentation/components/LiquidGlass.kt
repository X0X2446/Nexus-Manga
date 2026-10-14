package eu.kanade.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val GlassShapeLarge = RoundedCornerShape(24.dp)
val GlassShapeMedium = RoundedCornerShape(18.dp)
val GlassShapeSmall = RoundedCornerShape(14.dp)
val GlassShapeTile = RoundedCornerShape(22.dp)

@Composable
fun glassFillColor(isDark: Boolean = isSystemInDarkTheme()): Color {
    val scheme = MaterialTheme.colorScheme
    return if (isDark) {
        scheme.surface.copy(alpha = 0.42f).compositeOverGlass(Color.White.copy(alpha = 0.06f))
    } else {
        Color.White.copy(alpha = 0.58f)
    }
}

@Composable
fun glassBorderColor(isDark: Boolean = isSystemInDarkTheme()): Color {
    return if (isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        Color.White.copy(alpha = 0.78f)
    }
}

@Composable
fun glassHighlightBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
    return if (isDark) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.22f),
                Color.White.copy(alpha = 0.05f),
                Color.Transparent,
            ),
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.92f),
                Color.White.copy(alpha = 0.38f),
                Color.White.copy(alpha = 0.10f),
            ),
        )
    }
}

@Composable
fun Modifier.liquidGlass(
    shape: Shape = GlassShapeMedium,
    elevation: Dp = 8.dp,
    isDark: Boolean = isSystemInDarkTheme(),
): Modifier {
    return this
        .shadow(elevation = elevation, shape = shape, clip = false, ambientColor = Color.Black.copy(alpha = 0.18f))
        .clip(shape)
        .background(glassFillColor(isDark))
        .background(glassHighlightBrush(isDark))
        .border(width = 1.dp, color = glassBorderColor(isDark), shape = shape)
}

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = GlassShapeMedium,
    elevation: Dp = 8.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.liquidGlass(shape = shape, elevation = elevation),
        content = content,
    )
}

@Composable
fun nexusAtmosphereBrush(isDark: Boolean = isSystemInDarkTheme()): Brush {
    val scheme = MaterialTheme.colorScheme
    return if (isDark) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF071214),
                scheme.background,
                Color(0xFF0E1A1C),
                Color(0xFF122022),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFDFF5F1),
                scheme.background,
                Color(0xFFF2FAF8),
                Color(0xFFE8F4F1),
            ),
        )
    }
}

private fun Color.compositeOverGlass(overlay: Color): Color {
    val a = overlay.alpha
    return Color(
        red = overlay.red * a + red * (1f - a),
        green = overlay.green * a + green * (1f - a),
        blue = overlay.blue * a + blue * (1f - a),
        alpha = (alpha + a * (1f - alpha)).coerceIn(0f, 1f),
    )
}
