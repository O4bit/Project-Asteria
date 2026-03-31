package space.o4bit.projectasteria.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AstronomyPicture(
    @param:Json(name = "date") val date: String,
    @param:Json(name = "explanation") val explanation: String,
    @param:Json(name = "hdurl") val hdUrl: String?,
    @param:Json(name = "media_type") val mediaType: String,
    @param:Json(name = "service_version") val serviceVersion: String? = null, // Optional - not present in mirror API
    @param:Json(name = "title") val title: String,
    @param:Json(name = "url") val url: String?,
    @param:Json(name = "copyright") val copyright: String? = null, // Optional copyright info
    @param:Json(name = "thumbnail") val thumbnail: String? = null // Optional thumbnail for videos
)

/**
 * Adds a short fact or description to the Astronomy Picture
 */
data class EnhancedAstronomyPicture(
    val astronomyPicture: AstronomyPicture,
    val shortFact: String,
    val notificationTitle: String,
    val notificationBody: String
)
