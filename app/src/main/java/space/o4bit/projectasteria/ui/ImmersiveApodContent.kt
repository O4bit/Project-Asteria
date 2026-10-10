package space.o4bit.projectasteria.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import space.o4bit.projectasteria.R
import space.o4bit.projectasteria.data.local.LaunchEntity
import space.o4bit.projectasteria.data.model.EnhancedAstronomyPicture
import space.o4bit.projectasteria.ui.components.AstronomyPictureCard
import space.o4bit.projectasteria.ui.components.PinnedLaunchCard
import space.o4bit.projectasteria.ui.viewmodels.ApodUiState
import space.o4bit.projectasteria.utils.Constants
import space.o4bit.projectasteria.utils.TextUtils

/**
 * Immersive APOD reading experience where the reading content card smoothly
 * slides up and over the APOD picture card, with natural parallax and zero gap.
 */
@Composable
fun ImmersiveApodContent(
    state: ApodUiState.Success,
    onCardClick: () -> Unit,
    onToggleFavorite: (EnhancedAstronomyPicture) -> Unit,
    pinnedLaunches: List<LaunchEntity> = emptyList(),
    onLaunchClick: (String) -> Unit = {},
    onUnpinLaunch: (String) -> Unit = {},
    autoRemoveSetting: Boolean = false,
    neverAskSetting: Boolean = false,
    onUpdateAutoRemove: (Boolean) -> Unit = {},
    onUpdateNeverAsk: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val onSurface = MaterialTheme.colorScheme.onSurface
    val picture = state.picture

    // Parallax translation so the picture card glides slower than scroll,
    // allowing the reading card to slide cleanly up and over it without disappearing early.
    val maxParallaxPx = with(density) { 220.dp.toPx() }
    val cardTranslationY = (scrollState.value * 0.4f).coerceAtMost(maxParallaxPx)
    val expandScrollTargetPx = with(density) { 460.dp.toPx() }.toInt()

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            // ── 0. Pinned Launches Carousel/List ─────────────────────────────
            if (pinnedLaunches.isNotEmpty()) {
                Column {
                    pinnedLaunches.forEach { launch ->
                        PinnedLaunchCard(
                            launch = launch,
                            onLaunchClick = { onLaunchClick(launch.id) },
                            onUnpinClick = { onUnpinLaunch(launch.id) },
                            autoRemoveSetting = autoRemoveSetting,
                            neverAskSetting = neverAskSetting,
                            onUpdateAutoRemove = onUpdateAutoRemove,
                            onUpdateNeverAsk = onUpdateNeverAsk
                        )
                    }
                }
            }

            // ── 1. APOD Picture Card (Moves with parallax so reading card slides over it)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = cardTranslationY
                    }
            ) {
                AstronomyPictureCard(
                    enhancedPicture = picture,
                    onCardClick = onCardClick,
                    onToggleFavorite = { onToggleFavorite(picture) }
                )
            }

            // ── 2. Full Reading Content Section (Slides up and covers the picture card)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-10).dp)
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
                shadowElevation = 10.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Tap handle to expand/scroll
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) {
                                if (scrollState.value < expandScrollTargetPx / 2) {
                                    scope.launch { scrollState.animateScrollTo(expandScrollTargetPx) }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Drag handle pill
                        Box(
                            modifier = Modifier
                                .width(38.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                        )

                        Spacer(Modifier.height(14.dp))

                        // Category Pill + Date
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Text(
                                    text = "ABOUT THIS IMAGE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    letterSpacing = 1.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }

                            Text(
                                text = picture.astronomyPicture.date,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Title
                    Text(
                        text = picture.astronomyPicture.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = onSurface
                    )

                    // Image Credit (with clickable links)
                    if (!picture.astronomyPicture.copyright.isNullOrBlank()) {
                        Spacer(Modifier.height(6.dp))
                        val linkColor = MaterialTheme.colorScheme.primary
                        val creditText = remember(picture.astronomyPicture.copyright, linkColor) {
                            TextUtils.htmlToAnnotatedString(
                                html = picture.astronomyPicture.copyright,
                                prefix = "Image Credit: ",
                                linkColor = linkColor
                            )
                        }
                        Text(
                            text = creditText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // Divider line
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        thickness = 1.dp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Full Explanation Body
                    val linkColor = MaterialTheme.colorScheme.primary
                    val explanationText = remember(picture.astronomyPicture.explanation, linkColor) {
                        TextUtils.htmlToAnnotatedString(
                            html = picture.astronomyPicture.explanation,
                            linkColor = linkColor
                        )
                    }

                    SelectionContainer {
                        Text(
                            text = explanationText,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.5.sp,
                                lineHeight = 27.sp,
                                letterSpacing = 0.2.sp
                            ),
                            color = onSurface,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Join Discord Server Pill ──────────────────────────
                    val context = LocalContext.current
                    Surface(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Constants.DISCORD_INVITE_URL))
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(percent = 50),
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_discord),
                                contentDescription = "Discord",
                                modifier = Modifier.size(22.dp),
                                tint = Color.Unspecified
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Join the discord server to talk about project asteria and get important announcements!",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Return hint
                    Text(
                        text = "↑ Scroll up to return",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom space
            Spacer(Modifier.height(80.dp))
        }
    }
}
