package space.o4bit.projectasteria.ui.components

import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import space.o4bit.projectasteria.util.toHexString
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FormatPaint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.data.preferences.BackgroundPreferencesRepository
import space.o4bit.projectasteria.data.preferences.NotificationPreferencesRepository
import space.o4bit.projectasteria.data.preferences.ThemePreferencesRepository
import space.o4bit.projectasteria.ui.components.settings.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onShowLicenses: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 3 })
    val tabs = listOf("Appearance", "General", "About")
    val iconsSelected = listOf(Icons.Filled.Star, Icons.Filled.Settings, Icons.Filled.Info)
    val iconsUnselected = listOf(Icons.Outlined.Star, Icons.Outlined.Settings, Icons.Outlined.Info)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            AsteriaBottomNavigation(
                tabs = tabs,
                selectedIcons = iconsSelected,
                unselectedIcons = iconsUnselected,
                selectedIndex = pagerState.currentPage,
                onTabSelected = { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { page ->
            when (page) {
                0 -> AppearanceTabContent()
                1 -> GeneralTabContent()
                2 -> AboutTabContent(onShowLicenses = onShowLicenses)
            }
        }
    }
}

@Composable
fun AppearanceTabContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val themePrefs = remember { ThemePreferencesRepository(context) }
    val bgPrefs = remember { BackgroundPreferencesRepository(context) }

    val followSystem by themePrefs.followSystem.collectAsState(initial = true)
    val isDarkMode by themePrefs.isDarkMode.collectAsState(initial = false)
    val pureBlack by themePrefs.pureBlack.collectAsState(initial = false)
    val customAccent by themePrefs.customAccent.collectAsState(initial = null)
    val customThemeColor by themePrefs.customThemeColor.collectAsState(initial = null)
    val dynamicColor by themePrefs.dynamicColor.collectAsState(initial = true)
    
    val currentBgName by bgPrefs.backgroundType.collectAsState(initial = BackgroundType.DEFAULT.name)
    val enableParallax by bgPrefs.enableParallax.collectAsState(initial = true)

    val supportsDynamicColor = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(title = "Theme Preferences")
        
        SectionCard {
            RichSettingsItem(
                title = "Follow System Theme",
                subtitle = "Automatically switch based on device settings",
                icon = Icons.Outlined.FormatPaint,
                action = {
                    Switch(
                        checked = followSystem,
                        onCheckedChange = { coroutineScope.launch { themePrefs.updateFollowSystem(it) } }
                    )
                }
            )
            AsteriaSettingsDivider()
            RichSettingsItem(
                title = "Dark Mode",
                subtitle = "Force dark elements across the app",
                icon = Icons.Outlined.DarkMode,
                action = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = { coroutineScope.launch { themePrefs.updateDarkMode(it) } },
                        enabled = !followSystem
                    )
                }
            )
            AsteriaSettingsDivider()
            RichSettingsItem(
                title = "Pure Black (AMOLED)",
                subtitle = "Use true black instead of dark gray for surfaces",
                icon = Icons.Outlined.Palette,
                action = {
                    Switch(
                        checked = pureBlack,
                        onCheckedChange = { coroutineScope.launch { themePrefs.updatePureBlack(it) } }
                    )
                }
            )
            if (supportsDynamicColor) {
                AsteriaSettingsDivider()
                RichSettingsItem(
                    title = "Dynamic Color",
                    subtitle = "Use Material You colors from your wallpaper",
                    icon = Icons.Outlined.FormatPaint,
                    action = {
                        Switch(
                            checked = dynamicColor,
                            onCheckedChange = { coroutineScope.launch { themePrefs.updateDynamicColor(it) } }
                        )
                    }
                )
            }
        }

        SectionTitle(title = "Accent Color")
        AccentColorSelector(
            selectedColorHex = customAccent ?: customThemeColor,
            onColorSelected = { color ->
                coroutineScope.launch {
                    if (color == null) {
                        themePrefs.updateCustomAccent(null)
                        themePrefs.updateCustomThemeColor(null)
                    } else {
                        // Convert Color to Hex String
                        val hex = color.toHexString()
                        themePrefs.updateCustomAccent(hex)
                        themePrefs.updateCustomThemeColor(hex)
                    }
                }
            },
            dynamicColorEnabled = dynamicColor && supportsDynamicColor
        )

        SectionTitle(title = "Animated Background")
        BackgroundSelector(
            selectedBackground = BackgroundType.fromName(currentBgName),
            onBackgroundSelected = { type ->
                coroutineScope.launch { bgPrefs.updateBackgroundType(type.name) }
            }
        )
        
        SectionCard {
            RichSettingsItem(
                title = "Parallax Effect",
                subtitle = "Tilt device to move the background pattern",
                icon = Icons.Outlined.Wallpaper,
                action = {
                    Switch(
                        checked = enableParallax,
                        onCheckedChange = { coroutineScope.launch { bgPrefs.updateEnableParallax(it) } }
                    )
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralTabContent() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val notificationPrefs = remember { NotificationPreferencesRepository(context) }
    
    val dailyNotificationsEnabled by notificationPrefs.dailyNotificationsEnabled.collectAsState(initial = true)
    val wifiOnlyPrefetch by notificationPrefs.wifiOnlyPrefetch.collectAsState(initial = false)
    val notificationHour by notificationPrefs.notificationHour.collectAsState(initial = 9)
    val notificationMinute by notificationPrefs.notificationMinute.collectAsState(initial = 0)
    
    var showTimePicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(title = "Notifications")
        
        SectionCard {
            RichSettingsItem(
                title = "Daily Space Notifications",
                subtitle = "Receive mission updates and astronomy alerts",
                icon = Icons.Filled.Star,
                action = {
                    Switch(
                        checked = dailyNotificationsEnabled,
                        onCheckedChange = { 
                            coroutineScope.launch { notificationPrefs.updateDailyNotificationsEnabled(it) }
                        }
                    )
                }
            )
            AsteriaSettingsDivider()
            RichSettingsItem(
                title = "Delivery Time",
                subtitle = String.format("Notifications arrive daily at %02d:%02d", notificationHour, notificationMinute),
                icon = Icons.Outlined.Build,
                action = {
                    FilledTonalButton(
                        onClick = { showTimePicker = true },
                        enabled = dailyNotificationsEnabled
                    ) {
                        Text("Change")
                    }
                }
            )
            AsteriaSettingsDivider()
            RichSettingsItem(
                title = "WiFi Only Data Fetch",
                subtitle = "Only download daily images over WiFi",
                icon = Icons.Filled.Star,
                action = {
                    Switch(
                        checked = wifiOnlyPrefetch,
                        onCheckedChange = { coroutineScope.launch { notificationPrefs.updateWifiOnlyPrefetch(it) } }
                    )
                }
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = androidx.compose.material3.rememberTimePickerState(
            initialHour = notificationHour,
            initialMinute = notificationMinute,
            is24Hour = true
        )

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    coroutineScope.launch {
                        notificationPrefs.updateNotificationTime(timePickerState.hour, timePickerState.minute)
                        if (dailyNotificationsEnabled) {
                            try {
                                space.o4bit.projectasteria.data.worker.DailySpaceWorker.schedule(context)
                            } catch (e: Exception) { }
                        }
                    }
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
            title = {
                Text(text = "Select Release Time")
            },
            text = {
                androidx.compose.material3.TimePicker(state = timePickerState)
            }
        )
    }
}

@Composable
fun AboutTabContent(onShowLicenses: () -> Unit) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SectionTitle(title = "Project Asteria")
        
        SectionCard {
            RichSettingsItem(
                title = "Version",
                subtitle = "4.0.0-Release"
            )
            AsteriaSettingsDivider()
            RichSettingsItem(
                title = "Developed By",
                subtitleContent = {
                    val annotatedString = buildAnnotatedString {
                        append("Vertronix-Software")
                        withStyle(style = SpanStyle(fontSize = 12.sp)) {
                            append("\n(Vertronix-System-subdevison)")
                        }
                    }
                    Text(
                        text = annotatedString,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            AsteriaSettingsDivider()
            RichSettingsItem(
                title = "Open Source Licenses",
                subtitle = "View third-party licenses",
                onClick = onShowLicenses
            )
        }
        
        SectionTitle(title = "Feedback")
        SectionCard {
            RichSettingsItem(
                title = "Report Issues to GitHub",
                subtitle = "Help us improve",
                action = {
                    Button(onClick = {
                        val githubIssueUrl = "https://github.com/O4bit/Project-Asteria/issues/new/choose"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubIssueUrl))
                        context.startActivity(intent)
                    }) {
                        Text("Report")
                    }
                }
            )
        }
    }
}
