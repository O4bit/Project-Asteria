package space.o4bit.projectasteria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import space.o4bit.projectasteria.data.model.AstronomyPicture
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture

@Entity(tableName = "apods")
data class ApodEntity(
    @PrimaryKey
    val date: String,
    val explanation: String,
    val hdUrl: String?,
    val mediaType: String,
    val serviceVersion: String?,
    val title: String,
    val url: String?,
    val copyright: String?,
    val thumbnail: String?,
    val shortFact: String,
    val notificationTitle: String,
    val notificationBody: String,
    val isFavorite: Boolean = false
) {
    fun toEnhancedAstronomyPicture(): EnhancedAstronomyPicture {
        return EnhancedAstronomyPicture(
            astronomyPicture = AstronomyPicture(
                date = date,
                explanation = explanation,
                hdUrl = hdUrl,
                mediaType = mediaType,
                serviceVersion = serviceVersion,
                title = title,
                url = url,
                copyright = copyright,
                thumbnail = thumbnail
            ),
            shortFact = shortFact,
            notificationTitle = notificationTitle,
            notificationBody = notificationBody,
            isFavorite = isFavorite
        )
    }

    companion object {
        fun from(enhanced: EnhancedAstronomyPicture): ApodEntity {
            return ApodEntity(
                date = enhanced.astronomyPicture.date,
                explanation = enhanced.astronomyPicture.explanation,
                hdUrl = enhanced.astronomyPicture.hdUrl,
                mediaType = enhanced.astronomyPicture.mediaType,
                serviceVersion = enhanced.astronomyPicture.serviceVersion,
                title = enhanced.astronomyPicture.title,
                url = enhanced.astronomyPicture.url,
                copyright = enhanced.astronomyPicture.copyright,
                thumbnail = enhanced.astronomyPicture.thumbnail,
                shortFact = enhanced.shortFact,
                notificationTitle = enhanced.notificationTitle,
                notificationBody = enhanced.notificationBody,
                isFavorite = enhanced.isFavorite
            )
        }
    }
}

