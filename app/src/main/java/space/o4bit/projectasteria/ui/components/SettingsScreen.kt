package space.o4bit.projectasteria.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.preferences.ThemePreferencesRepository
import androidx.core.net.toUri

/**
 * Simplified Settings screen for the app
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    notificationsEnabled: Boolean,
    onNotificationsToggled: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    val themePreferences = remember { ThemePreferencesRepository(context) }
                    val followSystem by themePreferences.followSystem.collectAsState(initial = true)
                    val isDarkMode by themePreferences.isDarkMode.collectAsState(initial = false)
                    val coroutineScope = rememberCoroutineScope()

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Follow System Theme",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Automatically switch between light and dark themes based on your device settings",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = followSystem,
                            onCheckedChange = { follow ->
                                coroutineScope.launch {
                                    themePreferences.updateFollowSystem(follow)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }

                    if (!followSystem) {
                        Spacer(modifier = Modifier.height(16.dp))

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            thickness = 1.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = if (isDarkMode) R.drawable.brightness2 else R.drawable.brightness7
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (isDarkMode) "Dark Mode" else "Light Mode",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = "Choose between light and dark appearance",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { dark ->
                                    coroutineScope.launch {
                                        themePreferences.updateDarkMode(dark)
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            )
                        }
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Daily Space Discoveries",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Text(
                                text = "Receive daily notifications with new astronomy pictures and space facts",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = onNotificationsToggled,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.primary,
                                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                }
            }

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.elevatedCardElevation(
                    defaultElevation = 2.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "About",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        thickness = 1.dp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Project Asteria",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "Version 1.0",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Discover space right from your phone with astronomy pictures from NASA's APOD API.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    FilledTonalButton(
                        onClick = {
                            val githubIssueUrl = "https://github.com/O4bit/Project-Asteria/issues/new/choose"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubIssueUrl))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Report Issues to GitHub")
                    }
                }
            }

            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = Color(0xFF1A0B2E) // Deep space purple
                ),
                border = BorderStroke(
                    width = 2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF6B46C1), // Bright purple
                            Color(0xFF9333EA), // Vibrant purple
                            Color(0xFF8B5CF6)  // Light purple
                        )
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF2D1B4E).copy(alpha = 0.8f), // Dark purple center
                                    Color(0xFF1A0B2E).copy(alpha = 0.9f), // Deep space purple
                                    Color(0xFF0F0A1A) // Almost black space
                                ),
                                radius = 800f
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Team P.U.L.S.E.D",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color(0xFFE9D5FF) // Light purple text
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        HorizontalDivider(
                            color = Color(0xFF8B5CF6).copy(alpha = 0.6f), // Purple divider
                            thickness = 2.dp
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        TeamMemberItem(
                            name = "O4bit",
                            role = "Lead developer"
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Special thanks",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFFE9D5FF) // Light purple text
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "My brother, for being my inspiration and support 💜",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDDD6FE) // Light purple-gray text
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "🚀 NASA Astronomy Picture of the Day (APOD) API",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFDDD6FE) // Light purple-gray text
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "For providing incredible imagery of our universe ✨",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFBFBFE0) // Muted purple-gray text
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TeamMemberItem(
    modifier: Modifier = Modifier,
    name: String,
    role: String
) {
    val context = LocalContext.current
    val uri = "https://o4bit.space".toUri()
    val intent = Intent(Intent.ACTION_VIEW, uri)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color(0xFF3B1E6B).copy(alpha = 0.6f), // Purple glow
                        Color(0xFF2D1B4E).copy(alpha = 0.4f), // Deep purple
                    )
                )
            )
            .clickable { context.startActivity(intent) }
            .padding(12.dp)
    ) {
        Surface(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            color = Color(0xFF6B46C1), // Purple background
            border = BorderStroke(
                width = 3.dp, 
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF9333EA), // Bright purple
                        Color(0xFFE879F9), // Pink-purple
                        Color(0xFF8B5CF6)  // Light purple
                    )
                )
            )
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data("https://o4bit.space/assets/og/ezgif-7-2eaf8a8b05.gif")
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile picture of $name",
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = Color(0xFFE9D5FF), // Light purple text
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = role,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFFDDD6FE), // Light purple-gray text
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
