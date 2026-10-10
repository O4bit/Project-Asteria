package space.o4bit.projectasteria.utils

import android.os.Build
import android.text.Html
import android.text.Spanned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration

object TextUtils {

    private val HTML_TAG_REGEX = Regex("<[^>]*>")
    private val BR_TAG_REGEX = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val P_CLOSE_TAG_REGEX = Regex("</p\\s*>", RegexOption.IGNORE_CASE)
    private val NUMERIC_ENTITY_REGEX = Regex("&#(\\d+);")
    private val HEX_ENTITY_REGEX = Regex("&#x([0-9a-fA-F]+);", RegexOption.IGNORE_CASE)

    private val LINK_REGEX = Regex(
        """<a\s+[^>]*?href\s*=\s*["']([^"']+)["'][^>]*>(.*?)</a>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )

    fun decodeHtmlEntities(text: String): String {
        var result = text
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&copy;", "\u00A9")
            .replace("&#169;", "\u00A9")

        result = NUMERIC_ENTITY_REGEX.replace(result) { match ->
            try {
                match.groupValues[1].toInt().toChar().toString()
            } catch (_: Exception) {
                match.value
            }
        }

        result = HEX_ENTITY_REGEX.replace(result) { match ->
            try {
                match.groupValues[1].toInt(16).toChar().toString()
            } catch (_: Exception) {
                match.value
            }
        }

        return result
    }

    fun stripHtml(html: String): String {
        if (html.isBlank()) return ""
        val normalized = html
            .replace(BR_TAG_REGEX, "\n")
            .replace(P_CLOSE_TAG_REGEX, "\n\n")
            .replace(HTML_TAG_REGEX, "")
        val decoded = decodeHtmlEntities(normalized)
        return decoded
            .replace(Regex("\n{3,}"), "\n\n")
            .trim()
    }

    fun htmlToSpanned(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html)
        }
    }

    fun htmlToAnnotatedString(
        html: String,
        prefix: String = "",
        linkColor: Color = Color(0xFF64B5F6)
    ): AnnotatedString {
        if (html.isBlank()) {
            return AnnotatedString(prefix)
        }

        val trimmedHtml = html.trim()
        val builder = AnnotatedString.Builder()

        if (prefix.isNotEmpty()) {
            builder.append(prefix)
        }

        val matches = LINK_REGEX.findAll(trimmedHtml).toList()
        if (matches.isEmpty()) {
            builder.append(stripHtml(trimmedHtml))
            return builder.toAnnotatedString()
        }

        var currentIndex = 0
        val linkStyle = TextLinkStyles(
            style = SpanStyle(
                color = linkColor,
                textDecoration = TextDecoration.Underline
            )
        )

        for (match in matches) {
            val plainBefore = trimmedHtml.substring(currentIndex, match.range.first)
            if (plainBefore.isNotEmpty()) {
                val cleanedPlain = plainBefore
                    .replace(BR_TAG_REGEX, "\n")
                    .replace(P_CLOSE_TAG_REGEX, "\n\n")
                    .replace(HTML_TAG_REGEX, "")
                builder.append(decodeHtmlEntities(cleanedPlain))
            }

            val url = match.groupValues[1].trim()
            val rawLinkText = match.groupValues[2]
            val cleanLinkText = decodeHtmlEntities(
                rawLinkText.replace(HTML_TAG_REGEX, "").trim()
            )

            if (cleanLinkText.isNotEmpty()) {
                val start = builder.length
                builder.append(cleanLinkText)
                val end = builder.length

                if (url.isNotEmpty()) {
                    builder.addLink(
                        LinkAnnotation.Url(
                            url = url,
                            styles = linkStyle
                        ),
                        start = start,
                        end = end
                    )
                    builder.addStringAnnotation(
                        tag = "URL",
                        annotation = url,
                        start = start,
                        end = end
                    )
                }
            }

            currentIndex = match.range.last + 1
        }

        if (currentIndex < trimmedHtml.length) {
            val plainAfter = trimmedHtml.substring(currentIndex)
            val cleanedPlain = plainAfter
                .replace(BR_TAG_REGEX, "\n")
                .replace(P_CLOSE_TAG_REGEX, "\n\n")
                .replace(HTML_TAG_REGEX, "")
            builder.append(decodeHtmlEntities(cleanedPlain))
        }

        return builder.toAnnotatedString()
    }
}
