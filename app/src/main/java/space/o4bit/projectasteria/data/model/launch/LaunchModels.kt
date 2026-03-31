package space.o4bit.projectasteria.data.model.launch

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LaunchResponse(
    val count: Int,
    val results: List<Launch>
)

@JsonClass(generateAdapter = true)
data class Launch(
    val id: String,
    val name: String,
    val status: LaunchStatus,
    val net: String,
    @Json(name = "launch_service_provider") val launchServiceProvider: LaunchServiceProvider?,
    val pad: Pad?,
    val mission: Mission?,
    val image: String?
)

@JsonClass(generateAdapter = true)
data class LaunchStatus(
    val id: Int,
    val name: String,
    val abbrev: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class LaunchServiceProvider(
    val id: Int,
    val name: String,
    val type: String?
)

@JsonClass(generateAdapter = true)
data class Pad(
    val id: Int,
    val name: String,
    val location: Location?
)

@JsonClass(generateAdapter = true)
data class Location(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class Mission(
    val id: Int,
    val name: String,
    val description: String?,
    val type: String?
)
