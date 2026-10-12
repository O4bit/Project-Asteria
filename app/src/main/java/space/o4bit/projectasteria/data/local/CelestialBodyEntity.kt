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
    val moonsSummary: String
) {
    companion object {
        fun fromDto(dto: CelestialBodyDto): CelestialBodyEntity {
            val resolvedId = dto.id ?: dto.name ?: dto.englishName ?: java.util.UUID.randomUUID().toString()
            val english = if (!dto.englishName.isNullOrBlank()) dto.englishName else (dto.name ?: resolvedId)
            val type = when {
                !dto.bodyType.isNullOrBlank() -> dto.bodyType
                dto.isPlanet == true -> "Planet"
                dto.aroundPlanet != null -> "Moon"
                else -> "Asteroid"
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
                null -> null
                else -> rawAround.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }
            return CelestialBodyEntity(
                id = resolvedId,
                name = dto.name ?: english,
                englishName = english,
                isPlanet = dto.isPlanet ?: (type.equals("Planet", ignoreCase = true)),
                bodyType = type,
                semimajorAxis = dto.semimajorAxis ?: 0.0,
                perihelion = dto.perihelion ?: 0.0,
                aphelion = dto.aphelion ?: 0.0,
                eccentricity = dto.eccentricity ?: 0.0,
                inclination = dto.inclination ?: 0.0,
                massValue = dto.mass?.massValue,
                massExponent = dto.mass?.massExponent,
                volValue = dto.vol?.volValue,
                volExponent = dto.vol?.volExponent,
                density = dto.density ?: 0.0,
                gravity = dto.gravity ?: 0.0,
                escape = dto.escape ?: 0.0,
                meanRadius = dto.meanRadius ?: 0.0,
                equaRadius = dto.equaRadius ?: 0.0,
                polarRadius = dto.polarRadius ?: 0.0,
                flattening = dto.flattening ?: 0.0,
                dimension = dto.dimension ?: "",
                sideralOrbit = dto.sideralOrbit ?: 0.0,
                sideralRotation = dto.sideralRotation ?: 0.0,
                aroundPlanetName = aroundPlanetFormatted,
                discoveredBy = dto.discoveredBy ?: "",
                discoveryDate = dto.discoveryDate ?: "",
                alternativeName = dto.alternativeName ?: "",
                axialTilt = dto.axialTilt ?: 0.0,
                avgTemp = dto.avgTemp ?: 0.0,
                moonCount = moonsList.size,
                moonsSummary = moonsList.joinToString(", ")
            )
        }
    }
}
