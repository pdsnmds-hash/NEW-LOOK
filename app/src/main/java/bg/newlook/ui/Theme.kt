package bg.newlook.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.unit.*
import bg.newlook.R

@Composable fun english() = LocalConfiguration.current.locales[0].language == "en"

@Composable
fun l(bg: String, en: String): String {
    val context = LocalContext.current
    val id =
        context.resources.getIdentifier(
            "ui_" + bg.hashCode().toUInt().toString(16),
            "string",
            context.packageName,
        )
    return if (id != 0) stringResource(id) else if (english()) en else bg
}

val Indigo = Color(0xFF5546DB)
val Teal = Color(0xFF24756A)
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private val Manrope =
    FontFamily(
        Font(
            R.font.manrope,
            FontWeight.Normal,
            variationSettings = FontVariation.Settings(FontVariation.weight(400)),
        ),
        Font(
            R.font.manrope,
            FontWeight.Medium,
            variationSettings = FontVariation.Settings(FontVariation.weight(500)),
        ),
        Font(
            R.font.manrope,
            FontWeight.SemiBold,
            variationSettings = FontVariation.Settings(FontVariation.weight(600)),
        ),
        Font(
            R.font.manrope,
            FontWeight.Bold,
            variationSettings = FontVariation.Settings(FontVariation.weight(700)),
        ),
    )

private fun type(
    size: Int,
    height: Int,
    weight: FontWeight = FontWeight.Normal,
    spacing: Float = 0f,
) =
    TextStyle(
        fontFamily = Manrope,
        fontSize = size.sp,
        lineHeight = height.sp,
        fontWeight = weight,
        letterSpacing = spacing.sp,
    )

private val LookTypography =
    Typography(
        displayLarge = type(52, 58, FontWeight.SemiBold, -2f),
        displayMedium = type(44, 50, FontWeight.SemiBold, -1.7f),
        displaySmall = type(36, 43, FontWeight.SemiBold, -1.4f),
        headlineLarge = type(32, 39, FontWeight.SemiBold, -1.1f),
        headlineMedium = type(28, 35, FontWeight.SemiBold, -.8f),
        headlineSmall = type(24, 31, FontWeight.SemiBold, -.6f),
        titleLarge = type(21, 28, FontWeight.SemiBold, -.4f),
        titleMedium = type(16, 23, FontWeight.SemiBold, -.2f),
        titleSmall = type(14, 21, FontWeight.SemiBold),
        bodyLarge = type(16, 25),
        bodyMedium = type(14, 22),
        bodySmall = type(12, 18),
        labelLarge = type(14, 20, FontWeight.SemiBold),
        labelMedium = type(12, 18, FontWeight.SemiBold),
        labelSmall = type(10, 15, FontWeight.SemiBold, .3f),
    )

/** Reduce visual density on narrow phones while preserving the user's system font scale. */
private fun Typography.forWidth(width: Int): Typography {
    val factor = (.88f + .12f * ((width - 320) / 91f).coerceIn(0f, 1f))
    fun fit(style: TextStyle) =
        style.copy(
            fontSize = (style.fontSize.value * factor).coerceAtLeast(10f).sp,
            lineHeight = (style.lineHeight.value * factor).coerceAtLeast(14f).sp,
            letterSpacing = (style.letterSpacing.value * factor).sp,
        )
    return copy(
        displayLarge = fit(displayLarge),
        displayMedium = fit(displayMedium),
        displaySmall = fit(displaySmall),
        headlineLarge = fit(headlineLarge),
        headlineMedium = fit(headlineMedium),
        headlineSmall = fit(headlineSmall),
        titleLarge = fit(titleLarge),
        titleMedium = fit(titleMedium),
        titleSmall = fit(titleSmall),
        bodyLarge = fit(bodyLarge),
        bodyMedium = fit(bodyMedium),
        bodySmall = fit(bodySmall),
        labelLarge = fit(labelLarge),
        labelMedium = fit(labelMedium),
        labelSmall = fit(labelSmall),
    )
}

@Composable
fun contentGutter() =
    when {
        LocalConfiguration.current.screenWidthDp <= 340 -> 16.dp
        LocalConfiguration.current.screenWidthDp < 400 -> 20.dp
        else -> 24.dp
    }

@Composable
fun LookTheme(mode: String, content: @Composable () -> Unit) {
    val width = LocalConfiguration.current.screenWidthDp
    val typography = remember(width) { LookTypography.forWidth(width) }
    val dark = mode == "dark" || (mode == "system" && isSystemInDarkTheme())
    val colors =
        if (dark)
            darkColorScheme(
                primary = Color(0xFFBEB3FF),
                onPrimary = Color(0xFF241B56),
                primaryContainer = Color(0xFF332A59),
                onPrimaryContainer = Color(0xFFE5DEFF),
                secondary = Color(0xFF92D8C5),
                onSecondary = Color(0xFF123D34),
                secondaryContainer = Color(0xFF203C37),
                onSecondaryContainer = Color(0xFFB9E6D9),
                background = Color(0xFF10131B),
                onBackground = Color(0xFFF0F0F6),
                surface = Color(0xFF1B1F2A),
                onSurface = Color(0xFFF0F0F6),
                surfaceVariant = Color(0xFF292D3B),
                onSurfaceVariant = Color(0xFFA9B0C3),
                outline = Color(0xFF6B7286),
                outlineVariant = Color(0xFF303644),
                surfaceContainerLowest = Color(0xFF141821),
                surfaceContainerLow = Color(0xFF1B1F2A),
                surfaceContainer = Color(0xFF202532),
                surfaceContainerHigh = Color(0xFF292E3C),
                surfaceContainerHighest = Color(0xFF303645),
            )
        else
            lightColorScheme(
                primary = Indigo,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEEEAFF),
                onPrimaryContainer = Color(0xFF3D3188),
                secondary = Teal,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFE3F2EC),
                onSecondaryContainer = Color(0xFF205449),
                background = Color(0xFFF8F9FD),
                onBackground = Color(0xFF202535),
                surface = Color.White,
                onSurface = Color(0xFF202535),
                surfaceVariant = Color(0xFFF0F1F7),
                onSurfaceVariant = Color(0xFF687186),
                outline = Color(0xFF8990A1),
                outlineVariant = Color(0xFFE7E9F1),
                surfaceContainerLowest = Color.White,
                surfaceContainerLow = Color(0xFFF4F5FA),
                surfaceContainer = Color(0xFFF0F1F7),
                surfaceContainerHigh = Color(0xFFEAECF4),
                surfaceContainerHighest = Color(0xFFE4E7F0),
            )
    MaterialTheme(
        colorScheme = colors,
        typography = typography,
        shapes =
            Shapes(
                extraSmall = RoundedCornerShape(8.dp),
                small = RoundedCornerShape(12.dp),
                medium = RoundedCornerShape(18.dp),
                large = RoundedCornerShape(24.dp),
                extraLarge = RoundedCornerShape(32.dp),
            ),
        content = content,
    )
}
