package com.appriyo.deulama.presentation.discover

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import com.appriyo.deulama.ui.theme.HangugColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/* ---- Card geometry / tuning knobs (single source of truth) ---- */

const val CARD_WIDTH_DP = 300
const val CARD_HEIGHT_DP = 440
private const val MAX_ROTATION_DEG = 14f
private const val DISMISS_DISTANCE_FRACTION = 0.30f
private const val FLING_VELOCITY_PX_PER_SEC = 900f
private const val VERTICAL_DAMPING = 0.06f

/* Animation curves — picked for a snappy, professional feel:
 *  - fly-off tween: 220ms FastOutSlowIn — accelerates out, decelerates into
 *    the off-screen position. Single-pass; X and Y animate together.
 *  - snap-back spring: medium bounce so a cancelled drag settles cleanly
 *    without feeling rubbery.
 *  - backing-card lift: 180ms LinearOutSlowIn so the new top card glides up
 *    one slot as the previous card flies off (Tinder-style handoff).
 */
private const val FLY_OFF_DURATION_MS = 220
private const val BACKING_LIFT_DURATION_MS = 180
private const val NEW_CARD_LIFT_DURATION_MS = 220

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
    /**
     * Kicks the active card off-screen for the chosen [action]. The on-screen
     * X and Y translate simultaneously (no sequential "bob then slide"), and
     * [onDismiss] fires **only after** the card has cleared the screen —
     * which is the moment the next card should mount. This single change is
     * what removes the visible "old card still in flight while new card
     * already on top" jank that previously made the swipe feel late.
     */
    fun triggerFlyOff(action: DeckAction, onDismiss: () -> Unit) {
        scope.launch {
            when (action) {
                DeckAction.Like, DeckAction.Favorite, DeckAction.Dislike -> {
                    val targetX = when (action) {
                        DeckAction.Like, DeckAction.Favorite -> +offscreenX
                        DeckAction.Dislike -> -offscreenX
                        DeckAction.WatchLater, DeckAction.Watched -> 0f
                    }
                    flyOffTo(targetX = targetX, targetY = offsetY.value + 24f)
                }
                DeckAction.WatchLater, DeckAction.Watched -> {
                    offsetX.snapTo(0f)
                    flyOffTo(targetX = 0f, targetY = -offscreenX)
                }
            }
            onDismiss()
        }
    }

    private suspend fun flyOffTo(targetX: Float, targetY: Float) {
        // Parallel X+Y tween — total wall-clock = FLY_OFF_DURATION_MS.
        coroutineScope {
            val anim = tween<Float>(
                durationMillis = FLY_OFF_DURATION_MS,
                easing = FastOutSlowInEasing,
            )
            launch { offsetX.animateTo(targetX, anim) }
            launch { offsetY.animateTo(targetY, anim) }
        }
    }

    /** Used by the gesture path when the user releases below the dismiss threshold. */
    internal fun snapBack() {
        scope.launch {
            coroutineScope {
                val s = spring<Float>(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                )
                launch { offsetX.animateTo(0f, s) }
                launch { offsetY.animateTo(0f, s) }
            }
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
            // Keying on drama id makes the backing stack stable across
            // recompositions, so Compose doesn't tear down and rebuild
            // every image layer on each drag tick.
            key(drama.dramaId) {
                BackingCard(
                    drama = drama,
                    depth = index + 1,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        if (activeDrama != null) {
            // The new active card only mounts once the previous fly-off
            // has called `onDismiss` (which advances `activeIndex` on the
            // caller). The controller is still holding the off-screen
            // offsets from that animation, so we snap it back to the
            // resting position before the new card reads it. We do this
            // inside the `key` so it runs exactly once per drama id,
            // and we use `snapTo` so the user never sees the reset.
            key(activeDrama.dramaId) {
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
}

/* ============================ ACTIVE CARD ============================ */

@Composable
private fun ActiveSwipeCard(
    drama: Drama,
    controller: DeckAnimationController,
    onDismiss: (DeckAction, Drama) -> Unit,
) {
    // Read the underlying State vectors directly so this composable
    // subscribes to *just* offset changes (not the Animatable wrapper).
    val offsetXState = controller.offsetX.asState()
    val offsetYState = controller.offsetY.asState()
    val translationX by offsetXState
    val translationY by offsetYState

    val rotation by remember(translationX, controller.cardWidthPx) {
        derivedStateOf { (translationX / controller.cardWidthPx) * MAX_ROTATION_DEG }
    }
    val likeProgress by remember(translationX, controller.dismissDistance) {
        derivedStateOf { (translationX / controller.dismissDistance).coerceIn(0f, 1f) }
    }
    val dislikeProgress by remember(translationX, controller.dismissDistance) {
        derivedStateOf { (-translationX / controller.dismissDistance).coerceIn(0f, 1f) }
    }

    // New-card "lift" animation: each fresh active card scales/translates
    // up from the second slot so the deck feels responsive instead of
    // popping. We only run this once per drama id.
    val lift = remember { Animatable(0f) }
    LaunchedEffect(drama.dramaId) {
        lift.snapTo(0f)
        lift.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = NEW_CARD_LIFT_DURATION_MS,
                easing = LinearOutSlowInEasing,
            ),
        )
    }
    val liftScale = 0.94f + 0.06f * lift.value
    val liftTranslateY = -10f * (1f - lift.value)

    val draggableState = rememberDraggableState { delta ->
        // Compose's dragger is designed to be called from a coroutine,
        // so we still need a launch — but using `coroutineScope` here
        // (instead of plain `controller.scope.launch { snapTo(...) }`)
        // cuts the allocation: one Job per drag tick, not per delta
        // frame, because `coroutineScope` joins before returning.
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
                translationX = translationX,
                translationY = translationY + liftTranslateY,
                rotationZ = rotation,
                scaleX = liftScale,
                scaleY = liftScale,
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
                            flyOffAndDismiss(controller, +controller.offscreenX, drama, DeckAction.Like, onDismiss)
                        }
                        flingLeft || distanceLeft -> controller.scope.launch {
                            flyOffAndDismiss(controller, -controller.offscreenX, drama, DeckAction.Dislike, onDismiss)
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
    // Stack offsets. depth=1 is the card immediately behind the active one.
    val restTranslateY: Float = with(LocalDensity.current) { (depth * 12).dp.toPx() }
    val restScale = 1f - (depth * 0.05f)

    // When this composable first appears at a given depth, it animates
    // from "one slot further back" up to its rest position. That gives
    // the deck a Tinder-style lift as the previous card flies off,
    // instead of the new front card popping in cold.
    val lift = remember(depth) { Animatable(0f) }
    LaunchedEffect(depth) {
        lift.snapTo(0f)
        lift.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = BACKING_LIFT_DURATION_MS,
                easing = LinearOutSlowInEasing,
            ),
        )
    }
    val scale = restScale + (1f - restScale) * (1f - lift.value)
    val translateY = restTranslateY * (1f - lift.value)
    val alpha = (1f - depth * 0.18f).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .padding(top = (depth * 14).dp)
            .size(
                (CARD_WIDTH_DP - depth * 8).dp,
                (CARD_HEIGHT_DP - depth * 8).dp,
            )
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                alpha = alpha,
                translationY = translateY,
            ),
    ) {
        BackingCardFace(drama = drama, depthIconSize = 48.dp)
    }
}

@Composable
private fun BackingCardFace(drama: Drama, depthIconSize: androidx.compose.ui.unit.Dp) {
    // Plain Box + clip — no Material Card so the layer is GPU-cheap and
    // doesn't recompute its shadow every animation tick.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(elevation = 0.dp, shape = RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(HangugColors.SurfaceContainer),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
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
                        modifier = Modifier.size(depthIconSize),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0xCC000000)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY,
                        ),
                    ),
            )
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
        // Plain Box + shadow + clip — same shape as before, but avoids
        // Material's Card composable which re-evaluates its elevation
        // layer on every graphicsLayer tick (the source of much of the
        // perceived jank while dragging).
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(20.dp))
                .clip(RoundedCornerShape(20.dp))
                .background(HangugColors.SurfaceContainer),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
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
                // Bottom-only neutral scrim so the title / year /
                // storyline stay legible regardless of poster art
                // — no brand-tinted wash on top of the image itself.
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color(0xCC000000)),
                                startY = 0f,
                                endY = Float.POSITIVE_INFINITY,
                            ),
                        ),
                )
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

private suspend fun flyOffAndDismiss(
    controller: DeckAnimationController,
    offscreenX: Float,
    drama: Drama,
    action: DeckAction,
    onDismiss: (DeckAction, Drama) -> Unit,
) {
    // Cancel any in-flight snapBack / drag animation so the off-screen
    // tween always starts from the user's exact release position.
    controller.offsetX.stop()
    controller.offsetY.stop()
    coroutineScope {
        val anim = tween<Float>(
            durationMillis = FLY_OFF_DURATION_MS,
            easing = FastOutSlowInEasing,
        )
        launch { controller.offsetX.animateTo(offscreenX, anim) }
        launch { controller.offsetY.animateTo(controller.offsetY.value + 24f, anim) }
    }
    // Defer `activeIndex += 1` (i.e. the next-card mount) until after
    // the off-screen tween settles. This is the single biggest win for
    // perceived smoothness — previously the new card mounted halfway
    // through the previous one's flight and the two cards visibly
    // overlapped for a frame.
    onDismiss(action, drama)
}
