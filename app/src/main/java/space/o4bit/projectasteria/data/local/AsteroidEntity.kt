package space.o4bit.projectasteria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asteroids")
data class AsteroidEntity(
    @PrimaryKey val id: String,
    val name: String,
    val nasaJplUrl: String,
    val absoluteMagnitudeH: Double,
    val estimatedDiameterMinKm: Double,
    val estimatedDiameterMaxKm: Double,
    val isPotentiallyHazardous: Boolean,
    val isSentryObject: Boolean,
    
    // Nearest approach data
    val closeApproachDate: String,
    val relativeVelocityKms: String,
    val relativeVelocityKmh: String,
    val missDistanceKm: Double, // Double for sorting
    val missDistanceLunar: String
)
