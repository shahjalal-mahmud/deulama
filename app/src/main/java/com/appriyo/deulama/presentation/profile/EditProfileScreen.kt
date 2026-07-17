package com.appriyo.deulama.presentation.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.appriyo.deulama.BuildConfig
import com.appriyo.deulama.presentation.auth.AuthUiState
import com.appriyo.deulama.presentation.auth.AuthViewModel
import com.appriyo.deulama.presentation.components.ConnectionStatus
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.presentation.util.ImageUrls
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

/**
 * Edit-profile screen.
 *
 * - Avatar: `AsyncImage` showing the current cached avatar (from the
 *   auth session, full URL via [BuildConfig.API_BASE_URL]) OR the just-
 *   picked local URI, with a default person glyph fallback when no
 *   image is available. "Change photo" launches
 *   [ActivityResultContracts.PickVisualMedia] (image-only). "Remove"
 *   clears the pending upload.
 * - Name: pre-seeded from the auth user. Trim happens on submit, not
 *   while typing.
 * - Password change: three fields. Per api.md, all three are required
 *   together; the ViewModel short-circuits client-side partial sets
 *   so the user never sees a raw 422 dump for that case.
 * - Save: single button that hits the JSON path when no image is
 *   queued, or multipart otherwise.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    viewModel: EditProfileViewModel = koinViewModel(),
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val currentUser = (authState as? AuthUiState.SignedIn)?.user

    // Seed once on first composition.
    LaunchedEffect(currentUser) { viewModel.seedFromUser(currentUser) }

    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) viewModel.onImagePicked(uri)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack, enabled = !form.isSubmitting) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        }
    ) { innerPadding ->
        EditProfileScreenContent(
            form = form,
            currentImagePath = currentUser?.profileImage,
            currentEmail = currentUser?.email.orEmpty(),
            onNameChange = viewModel::onNameChanged,
            onCurrentPasswordChange = viewModel::onCurrentPasswordChanged,
            onNewPasswordChange = viewModel::onNewPasswordChanged,
            onConfirmPasswordChange = viewModel::onConfirmPasswordChanged,
            onPickPhoto = {
                photoPicker.launch(
                    PickVisualMediaRequest(
                        ActivityResultContracts.PickVisualMedia.ImageOnly,
                    ),
                )
            },
            onRemovePhoto = viewModel::onImageCleared,
            onDiscard = onBack,
            onSubmit = viewModel::submit,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        )
    }
}

@Composable
private fun EditProfileScreenContent(
    form: EditProfileFormState,
    currentImagePath: String?,
    currentEmail: String,
    onNameChange: (String) -> Unit,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onDiscard: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // Build the avatar model once per composition.
    val avatarModel: Any? = remember(form.imageBytes, currentImagePath) {
        form.imageBytes?.size?.let { form.imageBytes }
            ?: resolveAvatarUrl(currentImagePath)
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = HangugColors.Primary,
        focusedLabelColor = HangugColors.Primary,
        cursorColor = HangugColors.Primary,
    )

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top,
    ) {
        // ---- Avatar ----
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(HangugColors.SurfaceContainer)
                    .border(4.dp, HangugColors.SurfaceContainerLow, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarModel != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(avatarModel)
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
                    // The `image` field is never null per api.md (always
                    // returns at least default.png), so this branch should
                    // only ever fire while the auth state is still Loading.
                    Icon(
                        imageVector = Icons.Filled.Person,
                        contentDescription = "Default avatar",
                        tint = HangugColors.TextTertiary,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(HangugColors.Primary)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .clickable(enabled = !form.isSubmitting, onClick = onPickPhoto),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Change photo",
                    tint = HangugColors.OnPrimary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = currentEmail,
            style = MaterialTheme.typography.labelSmall,
            color = HangugColors.TextSecondary,
        )

        if (form.imageBytes != null) {
            Spacer(Modifier.height(6.dp))
            TextButton(onClick = onRemovePhoto, enabled = !form.isSubmitting) {
                Text("Remove new photo", color = HangugColors.Danger)
            }
        }
        if (form.imageError != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = form.imageError,
                color = HangugColors.Error,
                style = MaterialTheme.typography.labelSmall,
            )
        }

        Spacer(Modifier.height(28.dp))

        // ---- Basic info card ----
        SectionCard {
            SectionHeading(icon = Icons.Filled.Person, title = "Basic info")
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = form.name,
                onValueChange = onNameChange,
                label = { Text("Full name") },
                singleLine = true,
                isError = form.nameError != null,
                supportingText = form.nameError?.let { { Text(it, color = HangugColors.Error) } },
                enabled = !form.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(16.dp))

        // ---- Password card ----
        SectionCard {
            SectionHeading(icon = Icons.Filled.Lock, title = "Change password")
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Leave blank to keep your current password.",
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.TextSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp),
            )

            OutlinedTextField(
                value = form.currentPassword,
                onValueChange = onCurrentPasswordChange,
                label = { Text("Current password") },
                singleLine = true,
                isError = form.currentPasswordError != null,
                supportingText = form.currentPasswordError?.let {
                    { Text(it, color = HangugColors.Error) }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !form.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.newPassword,
                onValueChange = onNewPasswordChange,
                label = { Text("New password") },
                singleLine = true,
                isError = form.newPasswordError != null,
                supportingText = form.newPasswordError?.let {
                    { Text(it, color = HangugColors.Error) }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !form.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = form.confirmPassword,
                onValueChange = onConfirmPasswordChange,
                label = { Text("Confirm new password") },
                singleLine = true,
                isError = form.confirmPasswordError != null,
                supportingText = form.confirmPasswordError?.let {
                    { Text(it, color = HangugColors.Error) }
                },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !form.isSubmitting,
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // ---- Banner ----
        if (form.banner != null) {
            Spacer(Modifier.height(20.dp))
            // Treat the post-submit "Profile updated (name, ...)." message
            // as success; everything else is an error.
            val status = if (form.banner.startsWith("Profile updated")) {
                ConnectionStatus.CONNECTED
            } else {
                ConnectionStatus.ERROR
            }
            StatusBanner(
                status = status,
                message = form.banner,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(24.dp))

        // ---- Submit ----
        Button(
            onClick = onSubmit,
            enabled = !form.isSubmitting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HangugColors.PrimaryContainer,
                contentColor = HangugColors.OnPrimary,
            ),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (form.isSubmitting) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = HangugColors.OnPrimary,
                    modifier = Modifier.size(18.dp),
                )
            } else {
                Text("Save changes", color = HangugColors.OnPrimary, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Tapping the Avatar's camera badge picks a new photo; if the
        // user picks the same file again, we let them "undo" their
        // pending change via the explicit Remove button above. The
        // Discard button here leaves the screen unchanged.
        OutlinedButton(
            onClick = onDiscard,
            enabled = !form.isSubmitting,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HangugColors.Secondary),
            contentPadding = PaddingValues(vertical = 14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Discard changes", fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ---- Reusable sub-components ------------------------------------------------

@Composable
private fun SectionCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = HangugColors.SurfaceContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(18.dp), content = content)
    }
}

@Composable
private fun SectionHeading(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(HangugColors.Primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HangugColors.Primary,
                modifier = Modifier.size(14.dp),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = HangugColors.TextPrimary,
        )
    }
}

/**
 * Build the absolute URL for an avatar path returned by the API.
 *
 * Delegates to [ImageUrls.absolute] — which adds the
 * [BuildConfig.API_BASE_URL] prefix to relative server paths like
 * `uploads/profile/default.png` and passes absolute URLs through.
 */
private fun resolveAvatarUrl(path: String?): Any? = ImageUrls.absolute(path)