package space.o4bit.projectasteria.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import space.o4bit.projectasteria.data.model.solarsystem.CelestialBodyDto

@Entity(tableName = "celestial_bodies")
data class CelestialBodyEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val englishName: String,
    val isPlanet: Boolean,
    val bodyType: String,
    val semimajorAxis: Double,
    val perihelion: Double,
    val aphelion: Double,
    val eccentricity: Double,
    val inclination: Double,
    val massValue: Double?,
    val massExponent: Int?,
    val volValue: Double?,
    val volExponent: Int?,
    val density: Double,
    val gravity: Double,
    val escape: Double,
    val meanRadius: Double,
    val equaRadius: Double,
    val polarRadius: Double,
    val flattening: Double,
    val dimension: String,
    val sideralOrbit: Double,
    val sideralRotation: Double,
    val aroundPlanetName: String?,
    val discoveredBy: String,
    val discoveryDate: String,
    val alternativeName: String,
    val axialTilt: Double,
    val avgTemp: Double,
    val moonCount: Int,
    val moonsSummary: String,
    val constellation: String = "",
    val description: String = "",
    val spectralType: String = "",
    val distanceLightYears: Double = 0.0
) {
    companion object {
        fun fromDto(dto: CelestialBodyDto, fallback: CelestialBodyEntity? = null): CelestialBodyEntity {
            val resolvedId = dto.id ?: dto.name ?: dto.englishName ?: java.util.UUID.randomUUID().toString()
            val english = if (!dto.englishName.isNullOrBlank()) dto.englishName else (dto.name ?: resolvedId)
            val type = when {
                !dto.bodyType.isNullOrBlank() -> dto.bodyType
                dto.isPlanet == true -> "Planet"
                dto.aroundPlanet != null -> "Moon"
                else -> fallback?.bodyType ?: "Asteroid"
            }
            val moonsList = dto.moons?.mapNotNull { it.moon } ?: emptyList()
            val rawAround = dto.aroundPlanet?.planet
            val aroundPlanetFormatted = when (rawAround?.lowercase()) {
                "terre" -> "Earth"
                "mercure" -> "Mercury"
                "venus" -> "Venus"
                "mars" -> "Mars"
                "jupiter" -> "Jupiter"
                "saturne" -> "Saturn"
                "uranus" -> "Uranus"
                "neptune" -> "Neptune"
                "pluton" -> "Pluto"
                null -> fallback?.aroundPlanetName
                else -> rawAround.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            val resolvedConstellation = dto.constellation?.takeIf { it.isNotBlank() } ?: fallback?.constellation ?: ""
            val resolvedDescription = dto.description?.takeIf { it.isNotBlank() } ?: fallback?.description ?: ""
            val resolvedSpectralType = dto.spectralType?.takeIf { it.isNotBlank() } ?: fallback?.spectralType ?: ""
            val resolvedDistance = dto.distanceLightYears?.takeIf { it > 0.0 } ?: fallback?.distanceLightYears ?: 0.0
            return CelestialBodyEntity(
                id = resolvedId,
                name = dto.name ?: fallback?.name ?: english,
                englishName = english,
                isPlanet = dto.isPlanet ?: fallback?.isPlanet ?: (type.equals("Planet", ignoreCase = true)),
                bodyType = type,
                semimajorAxis = dto.semimajorAxis ?: fallback?.semimajorAxis ?: 0.0,
                perihelion = dto.perihelion ?: fallback?.perihelion ?: 0.0,
                aphelion = dto.aphelion ?: fallback?.aphelion ?: 0.0,
                eccentricity = dto.eccentricity ?: fallback?.eccentricity ?: 0.0,
                inclination = dto.inclination ?: fallback?.inclination ?: 0.0,
                massValue = dto.mass?.massValue ?: fallback?.massValue,
                massExponent = dto.mass?.massExponent ?: fallback?.massExponent,
                volValue = dto.vol?.volValue ?: fallback?.volValue,
                volExponent = dto.vol?.volExponent ?: fallback?.volExponent,
                density = dto.density ?: fallback?.density ?: 0.0,
                gravity = dto.gravity ?: fallback?.gravity ?: 0.0,
                escape = dto.escape ?: fallback?.escape ?: 0.0,
                meanRadius = dto.meanRadius ?: fallback?.meanRadius ?: 0.0,
                equaRadius = dto.equaRadius ?: fallback?.equaRadius ?: 0.0,
                polarRadius = dto.polarRadius ?: fallback?.polarRadius ?: 0.0,
                flattening = dto.flattening ?: fallback?.flattening ?: 0.0,
                dimension = dto.dimension ?: fallback?.dimension ?: "",
                sideralOrbit = dto.sideralOrbit ?: fallback?.sideralOrbit ?: 0.0,
                sideralRotation = dto.sideralRotation ?: fallback?.sideralRotation ?: 0.0,
                aroundPlanetName = aroundPlanetFormatted,
                discoveredBy = dto.discoveredBy ?: fallback?.discoveredBy ?: "",
                discoveryDate = dto.discoveryDate ?: fallback?.discoveryDate ?: "",
                alternativeName = dto.alternativeName ?: fallback?.alternativeName ?: "",
                axialTilt = dto.axialTilt ?: fallback?.axialTilt ?: 0.0,
                avgTemp = dto.avgTemp ?: fallback?.avgTemp ?: 0.0,
                moonCount = if (moonsList.isNotEmpty()) moonsList.size else (fallback?.moonCount ?: 0),
                moonsSummary = if (moonsList.isNotEmpty()) moonsList.joinToString(", ") else (fallback?.moonsSummary ?: ""),
                constellation = resolvedConstellation,
                description = resolvedDescription,
                spectralType = resolvedSpectralType,
                distanceLightYears = resolvedDistance
            )
        }
    }
}
