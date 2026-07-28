package space.o4bit.projectasteria.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AstronomyPicture(
    @param:Json(name = "date") val date: String,
    @param:Json(name = "explanation") val explanation: String,
    @param:Json(name = "hdurl") val hdUrl: String?,
    @param:Json(name = "media_type") val mediaType: String,
    @param:Json(name = "service_version") val serviceVersion: String? = null,
    @param:Json(name = "title") val title: String,
    @param:Json(name = "url") val url: String?,
    @param:Json(name = "copyright") val copyright: String? = null,
    @param:Json(name = "thumbnail") val thumbnail: String? = null
)

data class EnhancedAstronomyPicture(
    val astronomyPicture: AstronomyPicture,
    val shortFact: String,
    val notificationTitle: String,
    val notificationBody: String,
    val isFavorite: Boolean = false
)
