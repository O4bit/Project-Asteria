package space.o4bit.projectasteria.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URI
import java.util.Locale

object VideoEmbedUtils {

    fun normalizeUrl(rawUrl: String?): String {
        val cleaned = rawUrl?.replace("&amp;", "&")?.trim() ?: return ""
        if (cleaned.isEmpty()) return ""
        return when {
            cleaned.startsWith("//") -> "https:$cleaned"
            cleaned.startsWith("http://", ignoreCase = true) -> "https://${cleaned.substring(7)}"
            cleaned.startsWith("https://", ignoreCase = true) -> cleaned
            cleaned.startsWith("/") -> "https://science.nasa.gov$cleaned"
            cleaned.startsWith("ap", ignoreCase = true) && cleaned.contains(".html", ignoreCase = true) ->
                "https://apod.nasa.gov/apod/$cleaned"
            cleaned.startsWith("image/", ignoreCase = true) -> "https://apod.nasa.gov/apod/$cleaned"
            !cleaned.contains("://") -> "https://$cleaned"
            else -> cleaned
        }
    }

    fun extractMediaUrlFromHtml(html: String?): String? {
        val content = html?.takeIf { !it.isBlank() } ?: return null
        val patterns = listOf(
            Regex("<source[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("<iframe[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("<embed[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("<video[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE),
            Regex("<a[^>]+href=[\"']([^\"']+\\.(?:mp4|webm|mov|m4v|m3u8)(?:\\?[^\"']*)?)[\"']", RegexOption.IGNORE_CASE),
            Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
        )
        for (regex in patterns) {
            val match = regex.find(content)?.groupValues?.getOrNull(1)
            val normalized = normalizeUrl(match)
            if (!normalized.isBlank()) {
                return normalized
            }
        }
        return null
    }

    fun dateToApodCode(date: String): String? {
        val parts = date.trim().split("-")
        if (parts.size != 3) return null
        val year = parts[0]
        val month = parts[1].padStart(2, '0')
        val day = parts[2].padStart(2, '0')
        if (year.length != 4) return null
        return year.substring(2, 4) + month + day
    }

    fun extractYouTubeVideoId(url: String?): String? {
        val normalized = normalizeUrl(url).takeIf { it.isNotEmpty() } ?: return null
        val patterns = listOf(
            Regex("(?:youtube(?:-nocookie)?\\.com/(?:embed/|v/|shorts/|live/|e/|watch\\?(?:.*&)?v=)|youtu\\.be/)([A-Za-z0-9_-]{11})", RegexOption.IGNORE_CASE),
            Regex("[?&]v=([A-Za-z0-9_-]{11})")
        )
        for (regex in patterns) {
            val match = regex.find(normalized)
            if (match != null) {
                return match.groupValues[1]
            }
        }
        return null
    }

    fun extractYouTubeStartTime(url: String?): Int? {
        val normalized = normalizeUrl(url).takeIf { it.isNotEmpty() } ?: return null
        val raw = Regex("[?&](?:start|t)=([^&#]+)", RegexOption.IGNORE_CASE)
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null

        raw.toIntOrNull()?.let { return it }
        if (raw.endsWith("s", ignoreCase = true)) {
            raw.dropLast(1).toIntOrNull()?.let { return it }
        }
        val hmsMatch = Regex("(?:(\\d+)h)?(?:(\\d+)m)?(?:(\\d+)s?)?", RegexOption.IGNORE_CASE)
            .matchEntire(raw)
        if (hmsMatch != null) {
            val hours = hmsMatch.groupValues[1].toIntOrNull() ?: 0
            val minutes = hmsMatch.groupValues[2].toIntOrNull() ?: 0
            val seconds = hmsMatch.groupValues[3].toIntOrNull() ?: 0
            val total = hours * 3600 + minutes * 60 + seconds
            if (total > 0) return total
        }
        return null
    }

    fun extractVimeoVideoId(url: String?): String? {
        val normalized = normalizeUrl(url).takeIf { it.isNotEmpty() } ?: return null
        val regex = Regex("(?:player\\.vimeo\\.com/video/|vimeo\\.com/(?:channels/[^/]+/|groups/[^/]+/videos/|manage/videos/|event/|video/)?)(\\d+)", RegexOption.IGNORE_CASE)
        return regex.find(normalized)?.groupValues?.getOrNull(1)
    }

    fun extractVimeoPrivacyHash(url: String?): String? {
        val normalized = normalizeUrl(url).takeIf { it.isNotEmpty() } ?: return null
        val queryHash = Regex("[?&]h=([A-Za-z0-9]+)")
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
        if (!queryHash.isNullOrBlank()) {
            return queryHash
        }
        val pathHash = Regex("vimeo\\.com/\\d+/([A-Za-z0-9]+)", RegexOption.IGNORE_CASE)
            .find(normalized)
            ?.groupValues
            ?.getOrNull(1)
        return pathHash?.takeIf { !it.isBlank() }
    }

    fun extractVideoThumbnailUrl(url: String?): String? {
        val normalized = normalizeUrl(url).takeIf { it.isNotEmpty() } ?: return null
        extractYouTubeVideoId(normalized)?.let {
            return "https://img.youtube.com/vi/$it/hqdefault.jpg"
        }
        extractVimeoVideoId(normalized)?.let {
            return "https://vumbnail.com/$it.jpg"
        }
        return null
    }

    fun isDirectVideoUrl(url: String?): Boolean {
        val normalized = normalizeUrl(url).takeIf { it.isNotEmpty() } ?: return false
        val path = try {
            URI(normalized).path?.lowercase(Locale.ROOT)
                ?: normalized.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        } catch (_: Exception) {
            normalized.substringBefore('?').substringBefore('#').lowercase(Locale.ROOT)
        }
        val extensions = listOf(".mp4", ".m4v", ".webm", ".ogv", ".ogg", ".mov", ".m3u8", ".avi", ".ts")
        return extensions.any { path.endsWith(it) }
    }

    fun formatEmbedUrl(url: String?): String {
        val normalized = normalizeUrl(url)
        if (normalized.isEmpty()) return ""
        extractYouTubeVideoId(normalized)?.let { id ->
            val start = extractYouTubeStartTime(normalized)
            val base = "https://www.youtube-nocookie.com/embed/$id?autoplay=1&mute=0&playsinline=1&rel=0&enablejsapi=1&fs=1"
            return if (start != null && start > 0) "$base&start=$start" else base
        }
        extractVimeoVideoId(normalized)?.let { id ->
            val hash = extractVimeoPrivacyHash(normalized)
            val base = "https://player.vimeo.com/video/$id?autoplay=1&title=0&byline=0&portrait=0&playsinline=1"
            return if (!hash.isNullOrBlank()) "$base&h=$hash" else base
        }
        return normalized
    }

    fun buildEmbedHtml(embedUrl: String): String {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <meta name="referrer" content="strict-origin-when-cross-origin">
            <style>
                * { box-sizing: border-box; margin: 0; padding: 0; }
                html, body {
                    width: 100%;
                    height: 100%;
                    background: #000000;
                    overflow: hidden;
                }
                .embed-wrapper {
                    position: relative;
                    width: 100%;
                    height: 100%;
                    background: #000000;
                }
                iframe {
                    position: absolute;
                    top: 0;
                    left: 0;
                    width: 100%;
                    height: 100%;
                    border: 0;
                }
            </style>
        </head>
        <body>
            <div class="embed-wrapper">
                <iframe
                    src="$embedUrl"
                    frameborder="0"
                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture; web-share; fullscreen"
                    referrerpolicy="strict-origin-when-cross-origin"
                    allowfullscreen="true">
                </iframe>
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    fun openVideoInExternalApp(context: Context, videoUrl: String?) {
        val normalized = normalizeUrl(videoUrl).takeIf { it.isNotEmpty() } ?: return
        val targetUrl = extractYouTubeVideoId(normalized)?.let {
            "https://www.youtube.com/watch?v=$it"
        } ?: extractVimeoVideoId(normalized)?.let {
            "https://vimeo.com/$it"
        } ?: normalized
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (_: Exception) {
        }
    }
}
