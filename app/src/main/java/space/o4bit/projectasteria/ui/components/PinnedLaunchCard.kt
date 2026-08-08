package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import space.o4bit.projectasteria.data.local.LaunchEntity

/**
 * Pinned Launch Card displayed on the APOD Home tab.
 *
 * Provides real-time countdown, mission details, and automatic post-launch completion dialog.
 */
@Composable
fun PinnedLaunchCard(
    launch: LaunchEntity,
    onLaunchClick: () -> Unit,
    onUnpinClick: () -> Unit,
    autoRemoveSetting: Boolean,
    neverAskSetting: Boolean,
    onUpdateAutoRemove: (Boolean) -> Unit,
    onUpdateNeverAsk: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val isCompleted = launch.statusName.equals("In Flight", ignoreCase = true) ||
            launch.statusName.equals("Success", ignoreCase = true) ||
            (launch.netMillis > 0 && now >= launch.netMillis + 15 * 60 * 1000L)

    var showRemovalDialog by remember { mutableStateOf(false) }
    var showConfirmUnpinDialog by remember { mutableStateOf(false) }
    var dontAskAgainChecked by remember { mutableStateOf(false) }
    // Tracks whether we've already shown the post-launch prompt so that
    // dismissing it ("Keep Pinned") doesn't instantly re-trigger the dialog
    // on the very next recomposition.
    var hasPromptedForRemoval by remember { mutableStateOf(false) }

    // Handle post-launch state changes exactly once per transition.
    LaunchedEffect(isCompleted, autoRemoveSetting) {
        if (!isCompleted) return@LaunchedEffect
        if (autoRemoveSetting) {
            onUnpinClick()
        } else if (!neverAskSetting && !hasPromptedForRemoval) {
            showRemovalDialog = true
            hasPromptedForRemoval = true
        }
    }

    if (showRemovalDialog) {
        AlertDialog(
            onDismissRequest = { showRemovalDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.RocketLaunch,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Launch Completed!",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        text = "The mission \"${launch.name}\" has launched. Would you like to remove it from your pinned home list?"
                    )
                    Spacer(Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { dontAskAgainChecked = !dontAskAgainChecked }
                    ) {
                        Checkbox(
                            checked = dontAskAgainChecked,
                            onCheckedChange = { dontAskAgainChecked = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Don't ask again (toggle in settings)",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (dontAskAgainChecked) {
                            onUpdateAutoRemove(true)
                            onUpdateNeverAsk(true)
                        }
                        showRemovalDialog = false
                        onUnpinClick()
                    }
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        if (dontAskAgainChecked) {
                            onUpdateNeverAsk(true)
                        }
                        showRemovalDialog = false
                    }
                ) {
                    Text("Keep Pinned")
                }
            }
        )
    }

    if (showConfirmUnpinDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmUnpinDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = {
                Text(
                    text = "Unpin Launch?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text("Are you sure you want to remove \"${launch.name}\" from your pinned home list?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmUnpinDialog = false
                        onUnpinClick()
                    }
                ) {
                    Text("Unpin")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showConfirmUnpinDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable(onClick = onLaunchClick),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.RocketLaunch,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = "Pinned",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "PINNED LAUNCH",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 0.8.sp
                    )
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = launch.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${launch.providerName} • ${launch.statusName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(
                onClick = { showConfirmUnpinDialog = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Unpin launch",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
