package space.o4bit.projectasteria.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import space.o4bit.projectasteria.data.model.SortDirection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortFilterHeader(
    sortLabel: String,
    sortDirection: SortDirection,
    onOpenSortSheet: () -> Unit,
    onToggleDirection: () -> Unit,
    modifier: Modifier = Modifier,
    hazardousFilterSelected: Boolean? = null,
    onToggleHazardousFilter: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = true,
                onClick = onOpenSortSheet,
                label = { Text("Sort: $sortLabel") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Change Sort Option",
                        modifier = Modifier.size(16.dp)
                    )
                }
            )

            if (hazardousFilterSelected != null && onToggleHazardousFilter != null) {
                FilterChip(
                    selected = hazardousFilterSelected,
                    onClick = onToggleHazardousFilter,
                    label = { Text("Hazardous Only") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Filter Hazardous Asteroids",
                            modifier = Modifier.size(16.dp),
                            tint = if (hazardousFilterSelected) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }
        }

        IconButton(
            onClick = onToggleDirection,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = if (sortDirection == SortDirection.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                contentDescription = if (sortDirection == SortDirection.ASCENDING) "Ascending Order" else "Descending Order",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SortOptionBottomSheet(
    options: List<T>,
    selectedOption: T,
    getOptionLabel: (T) -> String,
    onOptionSelected: (T) -> Unit,
    onDismissRequest: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, top = 8.dp, start = 16.dp, end = 16.dp)
        ) {
            Text(
                text = "Sort By",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
            options.forEach { option ->
                val isSelected = option == selectedOption
                ListItem(
                    headlineContent = {
                        Text(
                            text = getOptionLabel(option),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    leadingContent = {
                        RadioButton(
                            selected = isSelected,
                            onClick = {
                                onOptionSelected(option)
                                onDismissRequest()
                            }
                        )
                    },
                    modifier = Modifier.clickable {
                        onOptionSelected(option)
                        onDismissRequest()
                    }
                )
            }
        }
    }
}
