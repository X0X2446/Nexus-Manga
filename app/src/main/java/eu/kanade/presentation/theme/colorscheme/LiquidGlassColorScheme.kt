package eu.kanade.presentation.theme.colorscheme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Liquid-glass inspired palette for Nexus.
 * Soft translucent surfaces, cool neutrals, restrained cyan accent.
 */
internal object LiquidGlassColorScheme : BaseColorScheme() {

    override val darkScheme = darkColorScheme(
        primary = Color(0xFF7EE0D6),
        onPrimary = Color(0xFF003731),
        primaryContainer = Color(0xFF1A4F4A),
        onPrimaryContainer = Color(0xFFB8FFF5),
        inversePrimary = Color(0xFF006A62),
        secondary = Color(0xFFB4C8C5),
        onSecondary = Color(0xFF1F3331),
        secondaryContainer = Color(0xFF354B48),
        onSecondaryContainer = Color(0xFFD0E4E1),
        tertiary = Color(0xFFC3C2FF),
        onTertiary = Color(0xFF2A2A60),
        tertiaryContainer = Color(0xFF414178),
        onTertiaryContainer = Color(0xFFE2DFFF),
        background = Color(0xFF0B1214),
        onBackground = Color(0xFFE1E8E7),
        surface = Color(0xFF0F1719),
        onSurface = Color(0xFFE1E8E7),
        surfaceVariant = Color(0xFF1A2628),
        onSurfaceVariant = Color(0xFFB8C7C5),
        surfaceTint = Color(0xFF7EE0D6),
        inverseSurface = Color(0xFFE1E8E7),
        inverseOnSurface = Color(0xFF0F1719),
        error = Color(0xFFFFB4AB),
        onError = Color(0xFF690005),
        errorContainer = Color(0xFF93000A),
        onErrorContainer = Color(0xFFFFDAD6),
        outline = Color(0xFF82908E),
        outlineVariant = Color(0xFF394846),
        surfaceContainerLowest = Color(0xFF080E10),
        surfaceContainerLow = Color(0xFF131B1D),
        surfaceContainer = Color(0xFF172022),
        surfaceContainerHigh = Color(0xFF212B2D),
        surfaceContainerHighest = Color(0xFF2C3638),
    )

    override val lightScheme = lightColorScheme(
        primary = Color(0xFF006A62),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFF9DF2E8),
        onPrimaryContainer = Color(0xFF00201D),
        inversePrimary = Color(0xFF7EE0D6),
        secondary = Color(0xFF4A6360),
        onSecondary = Color(0xFFFFFFFF),
        secondaryContainer = Color(0xFFCCE8E4),
        onSecondaryContainer = Color(0xFF051F1D),
        tertiary = Color(0xFF59607A),
        onTertiary = Color(0xFFFFFFFF),
        tertiaryContainer = Color(0xFFDDE1FF),
        onTertiaryContainer = Color(0xFF151B33),
        background = Color(0xFFF4FBF9),
        onBackground = Color(0xFF161D1C),
        surface = Color(0xFFF4FBF9),
        onSurface = Color(0xFF161D1C),
        surfaceVariant = Color(0xFFDAE5E3),
        onSurfaceVariant = Color(0xFF3F4947),
        surfaceTint = Color(0xFF006A62),
        inverseSurface = Color(0xFF2B3231),
        inverseOnSurface = Color(0xFFECF2F0),
        error = Color(0xFFBA1A1A),
        onError = Color(0xFFFFFFFF),
        errorContainer = Color(0xFFFFDAD6),
        onErrorContainer = Color(0xFF410002),
        outline = Color(0xFF6F7977),
        outlineVariant = Color(0xFFBEC9C7),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFEEF5F3),
        surfaceContainer = Color(0xFFE8F0EE),
        surfaceContainerHigh = Color(0xFFE2EBE9),
        surfaceContainerHighest = Color(0xFFDCE5E3),
    )
}
