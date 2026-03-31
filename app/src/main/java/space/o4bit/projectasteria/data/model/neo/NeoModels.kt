package space.o4bit.projectasteria.data.model.neo

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NeoWsResponse(
    @Json(name = "element_count") val elementCount: Int,
    @Json(name = "near_earth_objects") val nearEarthObjects: Map<String, List<Asteroid>>
)

@JsonClass(generateAdapter = true)
data class Asteroid(
    val id: String,
    val name: String,
    @Json(name = "nasa_jpl_url") val nasaJplUrl: String,
    @Json(name = "absolute_magnitude_h") val absoluteMagnitudeH: Double,
    @Json(name = "estimated_diameter") val estimatedDiameter: EstimatedDiameter,
    @Json(name = "is_potentially_hazardous_asteroid") val isPotentiallyHazardous: Boolean,
    @Json(name = "close_approach_data") val closeApproachData: List<CloseApproachData>,
    @Json(name = "is_sentry_object") val isSentryObject: Boolean
)

@JsonClass(generateAdapter = true)
data class EstimatedDiameter(
    val kilometers: DiameterRange,
    val meters: DiameterRange
)

@JsonClass(generateAdapter = true)
data class DiameterRange(
    @Json(name = "estimated_diameter_min") val min: Double,
    @Json(name = "estimated_diameter_max") val max: Double
)

@JsonClass(generateAdapter = true)
data class CloseApproachData(
    @Json(name = "close_approach_date") val closeApproachDate: String,
    @Json(name = "relative_velocity") val relativeVelocity: RelativeVelocity,
    @Json(name = "miss_distance") val missDistance: MissDistance
)

@JsonClass(generateAdapter = true)
data class RelativeVelocity(
    @Json(name = "kilometers_per_second") val kilometersPerSecond: String,
    @Json(name = "kilometers_per_hour") val kilometersPerHour: String
)

@JsonClass(generateAdapter = true)
data class MissDistance(
    val astronomical: String,
    val lunar: String,
    val kilometers: String,
    val miles: String
)
