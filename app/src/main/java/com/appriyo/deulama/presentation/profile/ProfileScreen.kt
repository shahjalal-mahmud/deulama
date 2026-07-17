package com.appriyo.deulama.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.appriyo.deulama.domain.model.UserProfile
import com.appriyo.deulama.presentation.components.ConnectionStatus
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.presentation.util.ImageUrls
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/**
 * Profile screen — displays the fields returned by `GET /api/profile`:
 *
 * - `name`                 → large title on the gradient header
 * - `email`                → subtitle under the name
 * - `image`                → circular avatar; falls back to a default
 *                             person glyph if no image is set or the
 *                             remote image fails to load
 * - `liked_count`          → stat card
 * - `watched_count`        → stat card
 * - `favorite_genres`      → chip row (with empty-state copy)
 *
 * Data flow: [ProfileViewModel] calls [ProfileRepository.getProfile]
 * once on first composition and offers a Retry button on failure.
 * The screen renders values verbatim — no client-side recomputation,
 * scaling, or sorting (the server is the source of truth per api.md).
 *
 * Note: the numeric user id and the raw image path are intentionally
 * not rendered — they were debug artifacts, not user-facing info. The
 * "Genre breakdown" entry point has also been removed from this
 * screen; [onOpenGenreStats] is kept as a parameter only so existing
 * call sites (e.g. the nav graph) don't need to change, but it is no
 * longer wired to anything here.
 */
@Composable
fun ProfileScreen(
    onOpenEditProfile: () -> Unit,
    onOpenGenreStats: () -> Unit,
    onLogout: () -> Unit,
    viewModel: ProfileViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Re-fetch every time the screen re-enters composition so any
    // updates made on the EditProfile screen show up on return.
    LaunchedEffect(Unit) { viewModel.refresh() }

    Scaffold(containerColor = MaterialTheme.colorScheme.background) { innerPadding ->
        when (val s = state) {
            is ProfileUiState.Loading -> ProfileLoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            is ProfileUiState.SignedOut -> ProfileSignedOutContent(
                onLogout = onLogout,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
            )

            is ProfileUiState.Error -> ProfileErrorContent(
                message = s.message,
                onRetry = viewModel::refresh,
                onLogout = onLogout,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            )

            is ProfileUiState.Success -> ProfileContent(
                profile = s.profile,
                onOpenEditProfile = onOpenEditProfile,
                onLogout = onLogout,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState()),
            )
        }
    }
}

// ---- Loading ----------------------------------------------------------------

@Composable
private fun ProfileLoadingContent(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = HangugColors.Primary)
    }
}

// ---- Signed-out -------------------------------------------------------------

@Composable
private fun ProfileSignedOutContent(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(HangugColors.Primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = HangugColors.Primary,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            text = "You're signed out.",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = HangugColors.TextPrimary,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onLogout,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Log in")
        }
    }
}

// ---- Error ------------------------------------------------------------------

@Composable
private fun ProfileErrorContent(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Couldn't load profile",
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = HangugColors.TextPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        StatusBanner(
            status = ConnectionStatus.ERROR,
            message = message,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Retry")
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onLogout) {
            Text("Log out", color = HangugColors.Danger)
        }
    }
}

// ---- Success ----------------------------------------------------------------

@Composable
private fun ProfileContent(
    profile: UserProfile,
    onOpenEditProfile: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val avatarUrl = remember(profile.profileImage) { ImageUrls.absolute(profile.profileImage) }

    Column(modifier = modifier) {
        // ---- Gradient header ----
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(
                    Brush.linearGradient(
                        listOf(HangugColors.Primary, HangugColors.Secondary),
                    ),
                    shape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-56).dp)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // ---- Avatar ----
            ProfileAvatar(context = context, avatarUrl = avatarUrl, size = 112.dp)

            Spacer(Modifier.height(14.dp))

            // ---- Name ----
            Text(
                text = profile.fullName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = HangugColors.TextPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(4.dp))

            // ---- Email ----
            Text(
                text = profile.email,
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.TextSecondary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            // ---- Stats row ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StatCard(
                    icon = Icons.Filled.Favorite,
                    label = "Liked",
                    value = profile.likedCount.toString(),
                    accentColor = HangugColors.Danger,
                    modifier = Modifier.weight(1f),
                )
                StatCard(
                    icon = Icons.Filled.CheckCircle,
                    label = "Watched",
                    value = profile.watchedCount.toString(),
                    accentColor = HangugColors.Tertiary,
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(Modifier.height(28.dp))

            // ---- Top genres ----
            SectionLabel(icon = Icons.Filled.Style, text = "Top genres")
            if (profile.favoriteGenres.isEmpty()) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainerLow),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    Text(
                        text = "No preferences yet — like or watch a few dramas to see your top genres here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HangugColors.TextTertiary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp),
                ) {
                    items(profile.favoriteGenres) { genre ->
                        GenrePill(genre)
                    }
                }
            }

            Spacer(Modifier.height(32.dp))

            // ---- Actions ----
            Button(
                onClick = onOpenEditProfile,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HangugColors.Primary,
                    contentColor = HangugColors.OnPrimary,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Edit Profile", fontWeight = FontWeight.SemiBold)
            }

            OutlinedButton(
                onClick = onLogout,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = HangugColors.Danger),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text("Log out", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ---- Reusable sub-components ------------------------------------------------

/**
 * Circular avatar with a themed border. Shows the remote image when
 * available, and otherwise (no image set, or the load fails) falls
 * back to a default person glyph on a tinted background — there is no
 * "broken image" state visible to the user.
 */
@Composable
private fun ProfileAvatar(
    context: android.content.Context,
    avatarUrl: String?,
    size: androidx.compose.ui.unit.Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(HangugColors.SurfaceContainer)
            .border(4.dp, HangugColors.SurfaceContainerLow, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (avatarUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile avatar",
                contentScale = ContentScale.Crop,
                placeholder = rememberVectorPainter(Icons.Filled.Person),
                error = rememberVectorPainter(Icons.Filled.Person),
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Default avatar",
                tint = HangugColors.TextTertiary,
                modifier = Modifier.size(size / 2),
            )
        }
    }
}

/**
 * Stat card with a leading icon badge, used in pairs (Liked / Watched).
 */
@Composable
private fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    accentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = HangugColors.TextPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.TextSecondary,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

/**
 * Pill-shaped genre tag. Display-only, mirrors the visual language of
 * chips used elsewhere in the app but without the AssistChip's ripple
 * affordance implying it's actionable.
 */
@Composable
private fun GenrePill(genre: String) {
    Surface(
        color = HangugColors.Primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(50),
    ) {
        Text(
            text = genre,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = HangugColors.Primary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

/**
 * Section title with a small leading icon — uppercase, secondary-tinted.
 */
@Composable
private fun SectionLabel(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = HangugColors.Secondary,
            modifier = Modifier.size(16.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.Secondary,
            letterSpacing = 1.sp,
        )
    }
}