package space.o4bit.projectasteria.ui.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import space.o4bit.projectasteria.data.model.AstronomyPicture
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.data.repository.SpaceRepository
import space.o4bit.projectasteria.ui.components.ApodDetailScreen
import space.o4bit.projectasteria.ui.components.ExplanationDetailScreen
import space.o4bit.projectasteria.ui.components.FullscreenImageViewer
import space.o4bit.projectasteria.ui.components.HistoryScreen
import space.o4bit.projectasteria.ui.components.LaunchDetailScreen
import space.o4bit.projectasteria.ui.components.OssLicensesScreen
import space.o4bit.projectasteria.ui.components.SettingsScreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SLIDE_MS = 350
private const val FADE_MS  = 300

/**
 * Root navigation graph with SharedTransitionLayout support.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun AsteriaNavGraph(
    navController: NavHostController,
    spaceRepository: SpaceRepository,
    /** Returns the currently loaded APOD picture from the ViewModel; null until loaded. */
    currentApodPicture: () -> EnhancedAstronomyPicture?,
    onRequestNotificationPermission: () -> Unit = {},
    mainContent: @Composable () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val animationsDisabled = remember(context) {
        try {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE,
                1f
            ) == 0f
        } catch (_: Exception) { false }
    }

    val slideMs = if (animationsDisabled) 0 else SLIDE_MS
    val fadeMs = if (animationsDisabled) 0 else FADE_MS

    SharedTransitionLayout {
        CompositionLocalProvider(LocalSharedTransitionScope provides this) {
            NavHost(
                navController = navController,
                startDestination = "main",
                enterTransition = {
                    fadeIn(tween(fadeMs)) +
                        slideInHorizontally(tween(slideMs)) { (it * 0.1f).toInt() }
                },
                exitTransition = {
                    fadeOut(tween(fadeMs)) +
                        slideOutHorizontally(tween(slideMs)) { -(it * 0.1f).toInt() }
                },
                popEnterTransition = {
                    fadeIn(tween(fadeMs)) +
                        slideInHorizontally(tween(slideMs)) { -(it * 0.1f).toInt() }
                },
                popExitTransition = {
                    fadeOut(tween(fadeMs)) +
                        slideOutHorizontally(tween(slideMs)) { (it * 0.1f).toInt() }
                }
            ) {

                // ── Main (HorizontalPager with 4 tabs) ──────────────────────────────
                composable("main") {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        mainContent()
                    }
                }

                // ── Settings ─────────────────────────────────────────────────────────
                composable("settings") {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onShowLicenses = { navController.navigate("licenses") },
                            onRequestNotificationPermission = onRequestNotificationPermission
                        )
                    }
                }

                // ── History ───────────────────────────────────────────────────────────
                composable("history") {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        HistoryScreen(
                            repository = spaceRepository,
                            onNavigateUp = { navController.popBackStack() },
                            onApodClick = { enhanced ->
                                navController.navigate(
                                    "apod_detail/${enhanced.astronomyPicture.date}"
                                )
                            }
                        )
                    }
                }

                // ── Fullscreen image viewer (slide up from bottom) ────────────────────
                composable(
                    route = "fullscreen?date={date}",
                    arguments = listOf(
                        navArgument("date") {
                            type = NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    ),
                    deepLinks = listOf(
                        navDeepLink { uriPattern = "asteria://image?date={date}" },
                        navDeepLink { uriPattern = "asteria://image" }
                    ),
                    enterTransition = {
                        fadeIn(tween(FADE_MS)) +
                            slideInVertically(tween(SLIDE_MS)) { (it * 0.25f).toInt() }
                    },
                    exitTransition = { fadeOut(tween(FADE_MS)) },
                    popExitTransition = {
                        fadeOut(tween(FADE_MS)) +
                            slideOutVertically(tween(SLIDE_MS)) { (it * 0.25f).toInt() }
                    }
                ) { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("date")

                    val picture by produceState<AstronomyPicture?>(
                        initialValue = currentApodPicture()?.astronomyPicture
                    ) {
                        if (value == null) {
                            val target = date
                                ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            value = spaceRepository.getApodByDate(target)?.astronomyPicture
                        }
                    }

                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        picture?.let {
                            FullscreenImageViewer(
                                astronomyPicture = it,
                                onBackPressed = { navController.popBackStack() }
                            )
                        }
                    }
                }

                // ── Explanation detail (slide up from bottom) ─────────────────────────
                composable(
                    route = "explanation/{date}",
                    arguments = listOf(navArgument("date") { type = NavType.StringType }),
                    enterTransition = {
                        fadeIn(tween(FADE_MS)) +
                            slideInVertically(tween(SLIDE_MS)) { (it * 0.25f).toInt() }
                    },
                    exitTransition = { fadeOut(tween(FADE_MS)) },
                    popExitTransition = {
                        fadeOut(tween(FADE_MS)) +
                            slideOutVertically(tween(SLIDE_MS)) { (it * 0.25f).toInt() }
                    }
                ) { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("date") ?: return@composable

                    val picture by produceState<AstronomyPicture?>(
                        initialValue = currentApodPicture()?.astronomyPicture
                    ) {
                        if (value == null) {
                            value = spaceRepository.getApodByDate(date)?.astronomyPicture
                        }
                    }

                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        picture?.let {
                            ExplanationDetailScreen(
                                astronomyPicture = it,
                                onBackPressed = { navController.popBackStack() }
                            )
                        }
                    }
                }

                // ── OSS Licenses ──────────────────────────────────────────────────────
                composable("licenses") {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        OssLicensesScreen(onNavigateUp = { navController.popBackStack() })
                    }
                }

                // ── Launch detail (existing) ──────────────────────────────────────────
                composable(
                    route = "launch_detail/{launchId}",
                    arguments = listOf(navArgument("launchId") { type = NavType.StringType })
                ) { backStackEntry ->
                    val launchId = backStackEntry.arguments?.getString("launchId")
                        ?: return@composable
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        LaunchDetailScreen(
                            launchId = launchId,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }

                // ── APOD detail (existing) ────────────────────────────────────────────
                composable(
                    route = "apod_detail/{date}",
                    arguments = listOf(navArgument("date") { type = NavType.StringType })
                ) { backStackEntry ->
                    val date = backStackEntry.arguments?.getString("date") ?: return@composable
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        ApodDetailScreen(
                            date = date,
                            repository = spaceRepository,
                            onBackClick = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
