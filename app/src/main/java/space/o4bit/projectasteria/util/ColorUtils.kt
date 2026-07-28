package space.o4bit.projectasteria.util

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.toColorInt

fun Color.isDarkBackground(): Boolean = luminance() < 0.5f

fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

fun Color.lighten(factor: Float): Color {
    return Color(
        red = red + (1f - red) * factor,
        green = green + (1f - green) * factor,
        blue = blue + (1f - blue) * factor,
        alpha = alpha
    )
}

fun Color.darken(factor: Float): Color {
    return Color(
        red = red * (1f - factor),
        green = green * (1f - factor),
        blue = blue * (1f - factor),
        alpha = alpha
    )
}

fun Color.toHexString(includeAlpha: Boolean = false): String {
    val argb = toArgb()
    return if (includeAlpha) {
        String.format(java.util.Locale.US, "#%08X", argb)
    } else {
        String.format(java.util.Locale.US, "#%06X", argb and 0xFFFFFF)
    }
}

fun String?.toColorOrNull(): Color? {
    val value = this?.trim().orEmpty()
    if (value.isEmpty()) return null
    return runCatching {
        val hexValue = if (value.startsWith("#")) value else "#$value"
        Color(hexValue.toColorInt())
    }.getOrNull()
}
