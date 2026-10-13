package space.o4bit.projectasteria.data.model.solarsystem

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BodiesResponse(
    val bodies: List<CelestialBodyDto> = emptyList()
)

@JsonClass(generateAdapter = true)
data class CelestialBodyDto(
    val id: String? = null,
    val name: String? = null,
    val englishName: String? = null,
    val isPlanet: Boolean? = false,
    val moons: List<MoonRefDto>? = null,
    val semimajorAxis: Double? = null,
    val perihelion: Double? = null,
    val aphelion: Double? = null,
    val eccentricity: Double? = null,
    val inclination: Double? = null,
    val mass: MassDto? = null,
    val vol: VolDto? = null,
    val density: Double? = null,
    val gravity: Double? = null,
    val escape: Double? = null,
    val meanRadius: Double? = null,
    val equaRadius: Double? = null,
    val polarRadius: Double? = null,
    val flattening: Double? = null,
    val dimension: String? = null,
    val sideralOrbit: Double? = null,
    val sideralRotation: Double? = null,
    val aroundPlanet: AroundPlanetDto? = null,
    val discoveredBy: String? = null,
    val discoveryDate: String? = null,
    val alternativeName: String? = null,
    val axialTilt: Double? = null,
    val avgTemp: Double? = null,
    val mainAnomaly: Double? = null,
    val argPeriapsis: Double? = null,
    val nodeLon: Double? = null,
    val bodyType: String? = null,
    val constellation: String? = null,
    val description: String? = null,
    val spectralType: String? = null,
    val distanceLightYears: Double? = null
)

@JsonClass(generateAdapter = true)
data class MoonRefDto(
    val moon: String? = null,
    val rel: String? = null
)

@JsonClass(generateAdapter = true)
data class AroundPlanetDto(
    val planet: String? = null,
    val rel: String? = null
)

@JsonClass(generateAdapter = true)
data class MassDto(
    val massValue: Double? = null,
    val massExponent: Int? = null
)

@JsonClass(generateAdapter = true)
data class VolDto(
    val volValue: Double? = null,
    val volExponent: Int? = null
)
