package space.o4bit.projectasteria.utils

import android.os.Build
import android.text.Html
import android.text.Spanned

/**
 * Utility functions for text processing
 */
object TextUtils {
    
    /**
     * Strips HTML tags from a string and returns plain text
     * @param html The HTML string to process
     * @return Plain text without HTML tags
     */
    fun stripHtml(html: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString().trim()
        }
    }
    
    /**
     * Converts HTML to Spanned for display in Text composables
     * @param html The HTML string to convert
     * @return Spanned text with formatting preserved
     */
    fun htmlToSpanned(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }
}
