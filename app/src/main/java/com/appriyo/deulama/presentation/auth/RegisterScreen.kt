package com.appriyo.deulama.presentation.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.presentation.auth.components.AuthHeroPanel
import com.appriyo.deulama.presentation.auth.components.AuthPasswordField
import com.appriyo.deulama.presentation.auth.components.AuthTextField
import com.appriyo.deulama.presentation.components.ConnectionStatus
import com.appriyo.deulama.presentation.components.GradientButton
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit,
    viewModel: AuthViewModel = koinViewModel(),
) {
    val form by viewModel.registerForm.collectAsStateWithLifecycle()

    RegisterScreenContent(
        fullName = form.fullName,
        email = form.email,
        password = form.password,
        confirmation = form.confirmation,
        fullNameError = form.fullNameError,
        emailError = form.emailError,
        passwordError = form.passwordError,
        confirmationError = form.confirmationError,
        banner = form.banner,
        isSubmitting = form.isSubmitting,
        onNameChange = viewModel::onRegisterNameChanged,
        onEmailChange = viewModel::onRegisterEmailChanged,
        onPasswordChange = viewModel::onRegisterPasswordChanged,
        onConfirmationChange = viewModel::onRegisterConfirmationChanged,
        onSubmit = { viewModel.register(onRegisterSuccess) },
        onGoToLogin = onGoToLogin,
    )
}

@Composable
private fun RegisterScreenContent(
    fullName: String,
    email: String,
    password: String,
    confirmation: String,
    fullNameError: String?,
    emailError: String?,
    passwordError: String?,
    confirmationError: String?,
    banner: String?,
    isSubmitting: Boolean,
    onNameChange: (String) -> Unit,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onConfirmationChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    fun next() = focusManager.moveFocus(FocusDirection.Down)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HangugColors.BgElevated)
            .verticalScroll(rememberScrollState())
            .imePadding(),
    ) {
        AuthHeroPanel(
            eyebrow = "합류하기 · JOIN THE STORY",
            headline = "Discover a new\nworld of K-dramas",
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
            // Grabber handle — matches LoginScreen's sheet treatment so
            // the two auth flows read as one consistent system.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                androidx.compose.foundation.layout.Box(
                    modifier = Modifier
                        .size(width = 40.dp, height = 4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(HangugColors.OutlineVariant),
                )
            }

            Spacer(Modifier.size(20.dp))

            AuthTextField(
                label = "Full Name",
                value = fullName,
                onValueChange = onNameChange,
                placeholder = "Jane Doe",
                leadingIcon = Icons.Filled.Person,
                isError = fullNameError != null,
                errorText = fullNameError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { next() }),
            )

            Spacer(Modifier.size(16.dp))

            AuthTextField(
                label = "Email Address",
                value = email,
                onValueChange = onEmailChange,
                placeholder = "name@example.com",
                leadingIcon = Icons.Filled.Email,
                isError = emailError != null,
                errorText = emailError,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { next() }),
            )

            Spacer(Modifier.size(16.dp))

            AuthPasswordField(
                label = "Password",
                value = password,
                onValueChange = onPasswordChange,
                isError = passwordError != null,
                errorText = passwordError,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { next() }),
            )

            Spacer(Modifier.size(16.dp))

            AuthPasswordField(
                label = "Confirm Password",
                value = confirmation,
                onValueChange = onConfirmationChange,
                placeholder = "Repeat your password",
                isError = confirmationError != null,
                errorText = confirmationError,
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
                text = if (isSubmitting) "Creating account" else "Create Account",
                onClick = onSubmit,
                loading = isSubmitting,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.size(20.dp))

            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Already have an account? ",
                    color = HangugColors.TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                )
                TextButton(onClick = onGoToLogin, enabled = !isSubmitting, contentPadding = PaddingValues(0.dp)) {
                    Text("Log in", color = HangugColors.Primary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}