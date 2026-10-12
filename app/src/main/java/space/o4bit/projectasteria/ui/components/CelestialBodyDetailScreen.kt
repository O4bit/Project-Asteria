package space.o4bit.projectasteria.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import space.o4bit.projectasteria.data.local.CelestialBodyEntity
import space.o4bit.projectasteria.data.repository.SolarSystemRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CelestialBodyDetailScreen(
    bodyId: String,
    repository: SolarSystemRepository,
    onBackClick: () -> Unit
) {
    val loadState by produceState<Pair<Boolean, CelestialBodyEntity?>>(initialValue = Pair(true, null), bodyId) {
        val loaded = repository.getBodyById(bodyId)
        value = Pair(false, loaded)
    }
    val isLoading = loadState.first
    val celestialBody = loadState.second

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = celestialBody?.englishName ?: "Celestial Object",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (celestialBody == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Celestial object not found",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    androidx.compose.material3.OutlinedButton(onClick = onBackClick) {
                        Text("Go Back")
                    }
                }
            }
        } else {
            val body = celestialBody
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = body.englishName,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (body.name.isNotBlank() && !body.name.equals(body.englishName, ignoreCase = true)) {
                                    Text(
                                        text = body.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (body.alternativeName.isNotBlank()) {
                                    Text(
                                        text = "Designation: ${body.alternativeName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                            Surface(
                                color = when (body.bodyType.lowercase()) {
                                    "planet" -> MaterialTheme.colorScheme.primaryContainer
                                    "dwarf planet" -> MaterialTheme.colorScheme.secondaryContainer
                                    "moon" -> MaterialTheme.colorScheme.tertiaryContainer
                                    "star" -> MaterialTheme.colorScheme.errorContainer
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = body.bodyType,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = when (body.bodyType.lowercase()) {
                                        "planet" -> MaterialTheme.colorScheme.onPrimaryContainer
                                        "dwarf planet" -> MaterialTheme.colorScheme.onSecondaryContainer
                                        "moon" -> MaterialTheme.colorScheme.onTertiaryContainer
                                        "star" -> MaterialTheme.colorScheme.onErrorContainer
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        if (!body.aroundPlanetName.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Natural satellite orbiting ${body.aroundPlanetName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Physical Characteristics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        if (body.meanRadius > 0.0) {
                            DetailRow(label = "Mean Radius", value = "${formatValue(body.meanRadius)} km")
                        }
                        if (body.equaRadius > 0.0) {
                            DetailRow(label = "Equatorial Radius", value = "${formatValue(body.equaRadius)} km")
                        }
                        if (body.polarRadius > 0.0) {
                            DetailRow(label = "Polar Radius", value = "${formatValue(body.polarRadius)} km")
                        }
                        if (body.massValue != null && body.massExponent != null) {
                            DetailRow(label = "Mass", value = "${body.massValue} × 10^${body.massExponent} kg")
                        }
                        if (body.density > 0.0) {
                            DetailRow(label = "Density", value = "${body.density} g/cm³")
                        }
                        if (body.gravity > 0.0) {
                            DetailRow(label = "Surface Gravity", value = "${body.gravity} m/s²")
                        }
                        if (body.escape > 0.0) {
                            DetailRow(label = "Escape Velocity", value = "${formatValue(body.escape)} m/s")
                        }
                        if (body.flattening > 0.0) {
                            DetailRow(label = "Flattening Factor", value = "${body.flattening}")
                        }
                        if (body.dimension.isNotBlank()) {
                            DetailRow(label = "Dimensions", value = "${body.dimension} km")
                        }
                        if (body.avgTemp > 0.0) {
                            val celsius = body.avgTemp - 273.15
                            DetailRow(label = "Mean Temperature", value = "${body.avgTemp} K (${"%.1f".format(celsius)} °C)")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Orbital Mechanics",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        if (body.semimajorAxis > 0.0) {
                            val au = body.semimajorAxis / 149597870.7
                            DetailRow(label = "Semi-major Axis", value = "${formatValue(body.semimajorAxis)} km (${"%.3f".format(au)} AU)")
                        }
                        if (body.perihelion > 0.0) {
                            DetailRow(label = "Perihelion", value = "${formatValue(body.perihelion)} km")
                        }
                        if (body.aphelion > 0.0) {
                            DetailRow(label = "Aphelion", value = "${formatValue(body.aphelion)} km")
                        }
                        if (body.eccentricity > 0.0) {
                            DetailRow(label = "Eccentricity", value = "${body.eccentricity}")
                        }
                        if (body.inclination > 0.0) {
                            DetailRow(label = "Orbital Inclination", value = "${body.inclination}°")
                        }
                        if (body.axialTilt != 0.0) {
                            DetailRow(label = "Axial Tilt", value = "${body.axialTilt}°")
                        }
                        if (body.sideralOrbit != 0.0) {
                            val days = body.sideralOrbit
                            val orbitFormatted = if (kotlin.math.abs(days) >= 365.25) {
                                "${"%.2f".format(days / 365.256)} Earth years (${formatValue(days)} days)"
                            } else {
                                "${formatValue(days)} days"
                            }
                            DetailRow(label = "Sidereal Orbit Period", value = orbitFormatted)
                        }
                        if (body.sideralRotation != 0.0) {
                            DetailRow(label = "Rotation Period", value = "${formatValue(body.sideralRotation)} hours")
                        }
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Discovery & History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow(
                            label = "Discovered By",
                            value = if (body.discoveredBy.isNotBlank()) body.discoveredBy else "Known since antiquity"
                        )
                        DetailRow(
                            label = "Discovery Date",
                            value = if (body.discoveryDate.isNotBlank()) body.discoveryDate else "Prehistoric antiquity"
                        )
                        DetailRow(label = "Classification", value = body.bodyType)
                        if (body.isPlanet) {
                            DetailRow(label = "Major Planet Status", value = "Yes (IAU Recognized)")
                        }
                    }
                }

                if (body.moonCount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Moons & Satellites",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            DetailRow(label = "Total Known Satellites", value = "${body.moonCount}")
                            if (body.moonsSummary.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Notable Moons",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = body.moonsSummary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatValue(value: Double): String {
    return if (kotlin.math.abs(value) >= 1000.0) {
        "%,.0f".format(value)
    } else {
        "%.2f".format(value)
    }
}
