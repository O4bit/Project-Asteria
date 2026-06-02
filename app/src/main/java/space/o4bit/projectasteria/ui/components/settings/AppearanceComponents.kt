package space.o4bit.projectasteria.ui.components.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import space.o4bit.projectasteria.ui.components.BackgroundType
import space.o4bit.projectasteria.util.darken
import space.o4bit.projectasteria.util.toColorOrNull

@Composable
fun ModernIconOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.height(88.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=0.3f)
        ),
        onClick = onClick,
        enabled = enabled
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
                .semantics(mergeDescendants = true) {
                    role = Role.RadioButton
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp).let { if (!enabled) it.alpha(0.5f) else it }
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.fontSize * 1.2,
                modifier = Modifier.let { if (!enabled) it.alpha(0.5f) else it }
            )
        }
    }
}


@Composable
fun CompactOptionCard(
    selected: Boolean,
    onClick: () -> Unit,
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(14.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha=0.3f)
        ),
        onClick = onClick,
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .semantics(mergeDescendants = true) { role = Role.RadioButton },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp).let { if (!enabled) it.alpha(0.5f) else it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.let { if (!enabled) it.alpha(0.5f) else it }
            )
        }
    }
}

val THEME_PRESET_COLORS = listOf(
    Color(0xFF6750A4), Color(0xFF386641), Color(0xFF0061A4), Color(0xFF8E24AA),
    Color(0xFFEF6C00), Color(0xFF00897B), Color(0xFFD81B60), Color(0xFF5C6BC0),
    Color(0xFF43A047), Color(0xFFFF7043), Color(0xFF1DE9B6), Color(0xFFFFC400),
    Color(0xFF00B8D4), Color(0xFFBA68C8)
)

@Composable
fun AccentColorSelector(
    selectedColorHex: String?,
    onColorSelected: (Color?) -> Unit,
    dynamicColorEnabled: Boolean
) {
    val columns = 7
    val selectedArgb = selectedColorHex.toColorOrNull()?.toArgb()
    val isEnabled = !dynamicColorEnabled

    SectionCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            THEME_PRESET_COLORS.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { preset ->
                        val isSelected = selectedArgb != null && preset.toArgb() == selectedArgb
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) preset.darken(0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .background(preset.copy(alpha = if (isEnabled) 1f else 0.5f), RoundedCornerShape(12.dp))
                                .clickable(enabled = isEnabled) { if (isEnabled) onColorSelected(preset) }
                        )
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }

            CompactOptionCard(
                selected = selectedArgb == null,
                onClick = { if (isEnabled) onColorSelected(null) },
                icon = Icons.Outlined.Close,
                label = "Default Tone",
                modifier = Modifier.fillMaxWidth(),
                enabled = isEnabled
            )
        }
    }
}

@Composable
fun BackgroundSelector(
    selectedBackground: BackgroundType,
    onBackgroundSelected: (BackgroundType) -> Unit
) {
    val columns = 4

    SectionCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            BackgroundType.entries.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { bgType ->
                        ModernIconOptionCard(
                            selected = selectedBackground == bgType,
                            onClick = { onBackgroundSelected(bgType) },
                            icon = getBackgroundIcon(bgType),
                            label = bgType.displayName,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(columns - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

private fun getBackgroundIcon(type: BackgroundType): ImageVector = when (type) {
    BackgroundType.STARRY    -> Icons.Outlined.Star
    BackgroundType.SPACE     -> Icons.Outlined.NightsStay
    BackgroundType.CIRCLES   -> Icons.Outlined.Circle
    BackgroundType.RINGS     -> Icons.Outlined.RadioButtonUnchecked
    BackgroundType.MESH      -> Icons.Outlined.Grid3x3
    BackgroundType.SHAPES    -> Icons.Outlined.Pentagon
    BackgroundType.GRID      -> Icons.Outlined.Apps
    BackgroundType.PARTICLES -> Icons.Outlined.BubbleChart
    BackgroundType.NONE      -> Icons.Outlined.VisibilityOff
}
