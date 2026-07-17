package com.appriyo.deulama.presentation.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.appriyo.deulama.domain.model.Drama
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors
import com.appriyo.deulama.ui.theme.HangugGlassOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/* ---- Card geometry / tuning knobs (single source of truth) ---- */

const val CARD_WIDTH_DP = 300
const val CARD_HEIGHT_DP = 440
private const val MAX_ROTATION_DEG = 18f
private const val DISMISS_DISTANCE_FRACTION = 0.30f
private const val FLING_VELOCITY_PX_PER_SEC = 1200f
private const val VERTICAL_DAMPING = 0.10f

/**
 * Screen-level handle to the deck's animation state. Both swipe gestures and
 * action buttons drive the *same* [Animatable]s so a button press animates
 * identically to a real swipe. `Modifier.draggable` gives us fling velocity
 * at drag-end via `onDragStopped(velocity)` — the genuine VelocityTracker path.
 */
class DeckAnimationController internal constructor(
    internal val offsetX: Animatable<Float, AnimationVector1D>,
    internal val offsetY: Animatable<Float, AnimationVector1D>,
    internal val scope: CoroutineScope,
    internal val offscreenX: Float,
    internal val dismissDistance: Float,
    internal val cardWidthPx: Float,
) {
    fun triggerFlyOff(action: DeckAction, onDismiss: () -> Unit) {
        scope.launch {
            when (action) {
                DeckAction.Like, DeckAction.Favorite, DeckAction.Dislike -> {
                    val targetX = when (action) {
                        DeckAction.Like, DeckAction.Favorite -> +offscreenX
                        DeckAction.Dislike -> -offscreenX
                        DeckAction.WatchLater, DeckAction.Watched -> 0f
                    }
                    offsetY.animateTo(offsetY.value + 40f, tween(280))
                    offsetX.animateTo(targetX, tween(280))
                }
                DeckAction.WatchLater, DeckAction.Watched -> {
                    offsetX.snapTo(0f)
                    offsetY.animateTo(-1200f, tween(320))
                }
            }
            onDismiss()
        }
    }

    internal fun snapBack() {
        scope.launch {
            offsetX.animateTo(0f, spring())
            offsetY.animateTo(0f, spring())
        }
    }
}

@Composable
fun rememberDeckController(): DeckAnimationController {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    return remember(configuration.screenWidthDp, density) {
        val cardWidthPx = with(density) { CARD_WIDTH_DP.dp.toPx() }
        val offscreenX = with(density) { (configuration.screenWidthDp + 120).dp.toPx() }
        DeckAnimationController(
            offsetX = offsetX,
            offsetY = offsetY,
            scope = scope,
            offscreenX = offscreenX,
            dismissDistance = cardWidthPx * DISMISS_DISTANCE_FRACTION,
            cardWidthPx = cardWidthPx,
        )
    }
}

/* ============================ DECK ROOT ============================== */

@Composable
fun SwipeDeck(
    activeDrama: Drama?,
    behindDramas: List<Drama>,
    onDismiss: (DeckAction, Drama) -> Unit,
    controller: DeckAnimationController,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        behindDramas.forEachIndexed { index, drama ->
            BackingCard(
                drama = drama,
                depth = index + 1,
                modifier = Modifier.matchParentSize(),
            )
        }

        if (activeDrama != null) {
            LaunchedEffect(activeDrama.dramaId) {
                controller.offsetX.snapTo(0f)
                controller.offsetY.snapTo(0f)
            }
            ActiveSwipeCard(
                drama = activeDrama,
                controller = controller,
                onDismiss = onDismiss,
            )
        }
    }
}

/* ============================ ACTIVE CARD ============================ */

@Composable
private fun ActiveSwipeCard(
    drama: Drama,
    controller: DeckAnimationController,
    onDismiss: (DeckAction, Drama) -> Unit,
) {
    val rotation =
        (controller.offsetX.value / controller.cardWidthPx) * MAX_ROTATION_DEG
    val likeProgress =
        (controller.offsetX.value / controller.dismissDistance).coerceIn(0f, 1f)
    val dislikeProgress =
        (-controller.offsetX.value / controller.dismissDistance).coerceIn(0f, 1f)

    val draggableState = rememberDraggableState { delta ->
        controller.scope.launch {
            controller.offsetX.snapTo(controller.offsetX.value + delta)
            controller.offsetY.snapTo(
                controller.offsetY.value + delta * VERTICAL_DAMPING,
            )
        }
    }

    Box(
        modifier = Modifier
            .size(CARD_WIDTH_DP.dp, CARD_HEIGHT_DP.dp)
            .graphicsLayer(
                translationX = controller.offsetX.value,
                translationY = controller.offsetY.value,
                rotationZ = rotation,
            )
            .draggable(
                state = draggableState,
                orientation = Orientation.Horizontal,
                onDragStopped = { velocity ->
                    val distance = controller.offsetX.value
                    val flingLeft = velocity <= -FLING_VELOCITY_PX_PER_SEC
                    val flingRight = velocity >= FLING_VELOCITY_PX_PER_SEC
                    val distanceLeft = distance <= -controller.dismissDistance
                    val distanceRight = distance >= controller.dismissDistance
                    when {
                        flingRight || distanceRight -> controller.scope.launch {
                            animateOffscreen(controller, +controller.offscreenX)
                            onDismiss(DeckAction.Like, drama)
                        }
                        flingLeft || distanceLeft -> controller.scope.launch {
                            animateOffscreen(controller, -controller.offscreenX)
                            onDismiss(DeckAction.Dislike, drama)
                        }
                        else -> controller.snapBack()
                    }
                },
            ),
    ) {
        SwipeCardFace(
            drama = drama,
            likeProgress = likeProgress,
            dislikeProgress = dislikeProgress,
        )
    }
}

/* ============================ BACKING CARD =========================== */

@Composable
private fun BackingCard(
    drama: Drama,
    depth: Int,
    modifier: Modifier = Modifier,
) {
    val scale = 1f - (depth * 0.06f)
    val alpha = 1f - (depth * 0.18f)
    Box(
        modifier = modifier
            .padding(top = (depth * 14).dp)
            .size(
                (CARD_WIDTH_DP - depth * 8).dp,
                (CARD_HEIGHT_DP - depth * 8).dp,
            )
            .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha),
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(HangugBrandGradient)) {
                if (!drama.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = drama.posterUrl,
                        contentDescription = drama.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = HangugColors.TextPrimary.copy(alpha = 0.45f),
                            modifier = Modifier.size(48.dp),
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xAA0B0708)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY,
                            ),
                        ),
                )
            }
        }
    }
}

/* ============================== FRONT ================================ */

@Composable
fun SwipeCardFace(
    drama: Drama,
    likeProgress: Float,
    dislikeProgress: Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.fillMaxSize().background(HangugBrandGradient)) {
                if (!drama.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = drama.posterUrl,
                        contentDescription = drama.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = HangugColors.TextPrimary.copy(alpha = 0.45f),
                            modifier = Modifier.size(72.dp),
                        )
                    }
                }
                Box(modifier = Modifier.fillMaxSize().background(HangugGlassOverlay))
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                ) {
                    if (drama.imdbRating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = HangugColors.Secondary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(
                                text = "%.1f".format(drama.imdbRating),
                                style = MaterialTheme.typography.labelMedium,
                                color = HangugColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }

                    Column {
                        Text(
                            text = drama.title,
                            style = MaterialTheme.typography.headlineSmall,
                            color = HangugColors.TextPrimary,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = buildString {
                                append(drama.releaseYear)
                                if (drama.genres.isNotEmpty()) {
                                    append(" · ")
                                    append(drama.genres.take(2).joinToString(", "))
                                }
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = HangugColors.TextPrimary.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Spacer(Modifier.size(10.dp))
                        if (drama.storyline.isNotBlank()) {
                            Text(
                                text = drama.storyline,
                                style = MaterialTheme.typography.bodySmall,
                                color = HangugColors.TextPrimary.copy(alpha = 0.75f),
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // LIKE / NOPE stamps.
        if (likeProgress > 0f) {
            StampBadge(
                text = "LIKE",
                color = HangugColors.Tertiary,
                rotation = -18f,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 36.dp, start = 24.dp)
                    .alpha(likeProgress),
            )
        }
        if (dislikeProgress > 0f) {
            StampBadge(
                text = "NOPE",
                color = HangugColors.Danger,
                rotation = 18f,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 36.dp, end = 24.dp)
                    .alpha(dislikeProgress),
            )
        }
    }
}

@Composable
private fun StampBadge(
    text: String,
    color: Color,
    rotation: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
            .graphicsLayer(rotationZ = rotation),
    ) {
        Text(
            text = text,
            color = color,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 28.sp,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/* ============================ ANIM HELPERS =========================== */

private suspend fun animateOffscreen(
    controller: DeckAnimationController,
    offscreenX: Float,
) {
    controller.offsetY.animateTo(
        controller.offsetY.value + 40f,
        tween(durationMillis = 280),
    )
    controller.offsetX.animateTo(
        offscreenX,
        tween(durationMillis = 280),
    )
}
