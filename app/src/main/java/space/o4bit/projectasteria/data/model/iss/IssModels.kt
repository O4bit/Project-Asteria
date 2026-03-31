package space.o4bit.projectasteria.data.model.iss

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class IssPosition(
    val name: String,
    val id: Int,
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val velocity: Double,
    val visibility: String,
    val footprint: Double,
    val timestamp: Long,
    val units: String
)
