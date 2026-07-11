package com.appriyo.deulama.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.presentation.components.ConnectionStatus
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors
import com.appriyo.deulama.ui.theme.HeroTitleCompact
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
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(Modifier.height(24.dp))

            Text(
                text = "한",
                style = HeroTitleCompact,
                color = HangugColors.Primary,
            )
            Text(
                text = "Sign in",
                style = MaterialTheme.typography.headlineMedium,
                color = HangugColors.TextPrimary,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
            Text(
                text = "지금 인기 · TRENDING NOW",
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.Secondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                singleLine = true,
                isError = emailError != null,
                supportingText = emailError?.let { { Text(it, color = HangugColors.Error) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                singleLine = true,
                isError = passwordError != null,
                supportingText = passwordError?.let { { Text(it, color = HangugColors.Error) } },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )

            if (banner != null) {
                Spacer(Modifier.height(16.dp))
                StatusBanner(
                    status = ConnectionStatus.ERROR,
                    message = banner,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onSubmit,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = HangugColors.PrimaryContainer,
                    contentColor = HangugColors.OnPrimary,
                ),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HangugBrandGradient, RoundedCornerShape(12.dp)),
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        color = HangugColors.OnPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text("Log In", color = HangugColors.OnPrimary)
                }
            }

            TextButton(
                onClick = onContinueWithoutAccount,
                enabled = !isSubmitting,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    "Continue browsing without an account",
                    color = HangugColors.TextSecondary,
                )
            }

            TextButton(
                onClick = onGoToRegister,
                enabled = !isSubmitting,
            ) {
                Text(
                    "Don't have an account? Register",
                    color = HangugColors.Secondary,
                )
            }
        }
    }
}