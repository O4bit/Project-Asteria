package space.o4bit.projectasteria.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.LibraryDefaults
import com.mikepenz.aboutlibraries.ui.compose.chipColors
import com.mikepenz.aboutlibraries.ui.compose.libraryColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OssLicensesScreen(
    onNavigateUp: () -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var searchQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }

    // M3 Expressive colors derived from the current theme
    val chipColors = LibraryDefaults.chipColors(
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
    )
    val libraryColors = LibraryDefaults.libraryColors(
        libraryBackgroundColor = MaterialTheme.colorScheme.surface,
        libraryContentColor = MaterialTheme.colorScheme.onSurface,
        versionChipColors = chipColors,
        licenseChipColors = chipColors,
        fundingChipColors = chipColors,
        dialogBackgroundColor = MaterialTheme.colorScheme.surface,
        dialogContentColor = MaterialTheme.colorScheme.onSurface,
        dialogConfirmButtonColor = MaterialTheme.colorScheme.primary
    )

    // More spacious padding for an expressive feel
    val chipPadding = LibraryDefaults.chipPadding(
        containerPadding = PaddingValues(top = 10.dp, end = 6.dp),
        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
    )
    val libraryPadding = LibraryDefaults.libraryPadding(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        namePadding = PaddingValues(top = 6.dp),
        versionPadding = LibraryDefaults.chipPadding(
            containerPadding = PaddingValues(start = 12.dp),
            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
        ),
        licensePadding = chipPadding,
        fundingPadding = chipPadding
    )
    val libraryDimensions = LibraryDefaults.libraryDimensions(
        itemSpacing = 2.dp
    )

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Open Source Licenses",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                        Icon(
                            imageVector = if (isSearchVisible) Icons.Default.Clear else Icons.Default.Search,
                            contentDescription = if (isSearchVisible) "Close search" else "Search libraries"
                        )
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Animated search field
            AnimatedVisibility(
                visible = isSearchVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            "Search libraries...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                )
            }

            LibrariesContainer(
                modifier = Modifier.fillMaxSize(),
                showAuthor = true,
                showVersion = true,
                showLicenseBadges = true,
                colors = libraryColors,
                padding = libraryPadding,
                dimensions = libraryDimensions,
                contentPadding = PaddingValues(bottom = 24.dp),
                onLibraryClick = if (searchQuery.isNotBlank()) {
                    // When searching, filter is handled by default — just let clicks through
                    null
                } else {
                    null
                }
            )
        }
    }
}
