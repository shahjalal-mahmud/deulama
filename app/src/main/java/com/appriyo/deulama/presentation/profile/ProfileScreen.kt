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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
 * Profile screen — displays **every** field returned by
 * `GET /api/profile`:
 *
 * - `id`                   → "User #N"
 * - `name`                 → large title
 * - `email`                → labelSmall
 * - `image`                → circular avatar + raw-path caption
 * - `liked_count`          → stat card
 * - `watched_count`        → stat card
 * - `favorite_genres`      → AssistChip row (with empty-state placeholder)
 *
 * Data flow: [ProfileViewModel] calls [ProfileRepository.getProfile]
 * once on first composition and offers a Retry button on failure.
 * The screen renders values verbatim — no client-side recomputation,
 * scaling, or sorting (the server is the source of truth per api.md).
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

    Scaffold { innerPadding ->
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
                onOpenGenreStats = onOpenGenreStats,
                onLogout = onLogout,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
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
        Text(
            text = "You're signed out.",
            style = MaterialTheme.typography.titleMedium,
            color = HangugColors.TextPrimary,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
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
            style = MaterialTheme.typography.headlineMedium,
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
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
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
    onOpenGenreStats: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val avatarUrl = remember(profile.profileImage) { ImageUrls.absolute(profile.profileImage) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(8.dp))

        // ---- Avatar ----
        if (avatarUrl != null) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(avatarUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile avatar",
                contentScale = ContentScale.Crop,
                placeholder = painterResource(android.R.drawable.ic_menu_gallery),
                error = painterResource(android.R.drawable.ic_menu_report_image),
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .border(2.dp, HangugColors.Primary, CircleShape)
                    .background(HangugColors.SurfaceContainer, CircleShape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(HangugColors.SurfaceContainer, CircleShape),
            )
        }

        Spacer(Modifier.height(12.dp))

        // ---- Name ----
        Text(
            text = profile.fullName,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
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

        Spacer(Modifier.height(6.dp))

        // ---- User id ----
        Text(
            text = "User #${profile.userId}",
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextTertiary,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        // ---- Raw image path (debug aid, also a clear "show me what the
        //      server returned" line for the user) ----
        Surface(
            color = HangugColors.SurfaceContainerLow,
            shape = RoundedCornerShape(8.dp),
        ) {
            Text(
                text = "image: ${profile.profileImage}",
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.TextTertiary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ---- Stats row ----
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatCard(
                label = "Liked",
                value = profile.likedCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Watched",
                value = profile.watchedCount.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ---- Top genres ----
        SectionLabel(text = "Top genres")
        if (profile.favoriteGenres.isEmpty()) {
            Text(
                text = "No preferences yet — like or watch a few dramas to see your top genres here.",
                style = MaterialTheme.typography.bodyMedium,
                color = HangugColors.TextTertiary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 4.dp),
            ) {
                items(profile.favoriteGenres) { genre ->
                    AssistChip(
                        onClick = { /* display only */ },
                        label = { Text(genre) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = HangugColors.SurfaceContainer,
                            labelColor = HangugColors.TextPrimary,
                        ),
                        border = null,
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        // ---- Actions ----
        Button(
            onClick = onOpenEditProfile,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text("Edit Profile")
        }
        OutlinedButton(
            onClick = onOpenGenreStats,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
        ) {
            Text("Genre breakdown", color = HangugColors.Secondary)
        }
        TextButton(onClick = onLogout) {
            Text("Log out", color = HangugColors.Danger)
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---- Reusable sub-components ------------------------------------------------

/**
 * Small surface card showing a numeric stat. Used in pairs (Liked / Watched).
 */
@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = HangugColors.SurfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.SemiBold,
                color = HangugColors.Primary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.TextSecondary,
                letterSpacing = 1.2.sp,
            )
        }
    }
}

/**
 * Section title — uppercase, secondary-tinted. Mirrors the eyebrow
 * pattern used in the rest of the app (see [com.appriyo.deulama.presentation.components.SectionHeader]).
 */
@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = HangugColors.Secondary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp),
    )
}