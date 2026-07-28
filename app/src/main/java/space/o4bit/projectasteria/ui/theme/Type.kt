package space.o4bit.projectasteria.ui.theme

import androidx.compose.material3.Typography

/**
 * Application typography.
 *
 * Currently uses the Material 3 system defaults (Roboto / system font).
 *
 * To give the app a distinctive identity, bundle `SpaceGrotesk-Variable.ttf`
 * (SIL Open Font Licence — F-Droid safe) under `res/font/`, then replace
 * the display/headline styles with:
 *
 *   val SpaceGrotesk = FontFamily(Font(R.font.space_grotesk_variable))
 *   displayLarge  = TextStyle(fontFamily = SpaceGrotesk, …)
 *   headlineLarge = TextStyle(fontFamily = SpaceGrotesk, …)
 *   … etc.
 *
 * Body, label, and title styles can remain on the system font for readability.
 */
val Typography = Typography()
