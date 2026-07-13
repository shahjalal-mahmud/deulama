package com.appriyo.deulama.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.presentation.auth.components.AuthHeroPanel
import com.appriyo.deulama.presentation.auth.components.AuthPasswordField
import com.appriyo.deulama.presentation.auth.components.AuthTextField
import com.appriyo.deulama.presentation.auth.components.SocialAuthRow
import com.appriyo.deulama.presentation.components.ConnectionStatus
import com.appriyo.deulama.presentation.components.GradientButton
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onGoToRegister: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val form by viewModel.loginForm.collectAsStateWithLifecycle()

    LoginScreenContent(
        email = form.email,
        password = form.password,
        emailError = form.emailError,
        passwordError = form.passwordError,
        banner = form.banner,
        isSubmitting = form.isSubmitting,
        onEmailChange = viewModel::onLoginEmailChanged,
        onPasswordChange = viewModel::onLoginPasswordChanged,
        onSubmit = { viewModel.login(onLoginSuccess) },
        onContinueWithoutAccount = onContinueWithoutAccount,
        onGoToRegister = onGoToRegister,
    )
}

@Composable
private fun LoginScreenContent(
    email: String,
    password: String,
    emailError: String?,
    passwordError: String?,
    banner: String?,
    isSubmitting: Boolean,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onGoToRegister: () -> Unit,
) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HangugColors.BgElevated)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        AuthHeroPanel(
            eyebrow = "돌아오신 것을 환영합니다 · WELCOME BACK",
            headline = "Continue your\ncinematic journey",
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-28).dp)
                .shadow(
                    elevation = 20.dp,
                    shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                    ambientColor = HangugColors.Primary.copy(alpha = 0.12f),
                    spotColor = HangugColors.Primary.copy(alpha = 0.12f),
                )
                .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .background(HangugColors.BgElevated)
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 14.dp, bottom = 20.dp),
        ) {
            // Grabber handle — reinforces the "sheet rising over the
            // hero" motif and reads as a deliberate, modern card edge.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(HangugColors.OutlineVariant),
                )
            }

            Spacer(Modifier.size(20.dp))

            SocialAuthRow()

            Spacer(Modifier.size(24.dp))
            AuthDivider()
            Spacer(Modifier.size(24.dp))

            AuthTextField(
                label = "Email Address",
                value = email,
                onValueChange = onEmailChange,
                placeholder = "name@example.com",
                leadingIcon = Icons.Filled.Email,
                isError = emailError != null,
                errorText = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
            )

            Spacer(Modifier.size(16.dp))

            AuthPasswordField(
                label = "Password",
                value = password,
                onValueChange = onPasswordChange,
                isError = passwordError != null,
                errorText = passwordError,
                showForgotLink = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    focusManager.clearFocus()
                    onSubmit()
                }),
            )

            AnimatedVisibility(
                visible = banner != null,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Column {
                    Spacer(Modifier.size(16.dp))
                    StatusBanner(
                        status = ConnectionStatus.ERROR,
                        message = banner.orEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            Spacer(Modifier.size(28.dp))

            GradientButton(
                text = if (isSubmitting) "Signing in" else "Get Started",
                onClick = onSubmit,
                loading = isSubmitting,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.size(16.dp))

            TextButton(
                onClick = onContinueWithoutAccount,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue browsing without an account", color = HangugColors.TextSecondary)
            }

            Spacer(Modifier.size(4.dp))

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Don't have an account? ",
                    color = HangugColors.TextSecondary,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onGoToRegister, enabled = !isSubmitting, contentPadding = PaddingValues(0.dp)) {
                    Text("Create account", color = HangugColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/**
 * Divider with a soft fading gradient line either side of a pill-shaped
 * label badge, in place of the previous flat 1dp rule.
 */
@Composable
private fun AuthDivider() {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f)
                .size(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(HangugColors.OutlineVariant.copy(alpha = 0f), HangugColors.OutlineVariant),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .padding(horizontal = 10.dp)
                .clip(RoundedCornerShape(50))
                .background(HangugColors.BgElevated2)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            Text(
                text = "OR CONTINUE WITH EMAIL",
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                color = HangugColors.TextTertiary,
                textAlign = TextAlign.Center,
            )
        }
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .weight(1f)
                .size(1.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(HangugColors.OutlineVariant, HangugColors.OutlineVariant.copy(alpha = 0f)),
                    ),
                ),
        )
    }
}