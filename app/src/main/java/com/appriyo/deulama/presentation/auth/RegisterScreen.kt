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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.presentation.components.ConnectionStatus
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.ui.theme.HangugBrandGradient
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
                text = "Create your account",
                style = MaterialTheme.typography.headlineMedium,
                color = HangugColors.TextPrimary,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            OutlinedTextField(
                value = fullName,
                onValueChange = onNameChange,
                label = { Text("Full name") },
                singleLine = true,
                isError = fullNameError != null,
                supportingText = fullNameError?.let { { Text(it, color = HangugColors.Error) } },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(12.dp))

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

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = confirmation,
                onValueChange = onConfirmationChange,
                label = { Text("Confirm password") },
                singleLine = true,
                isError = confirmationError != null,
                supportingText = confirmationError?.let { { Text(it, color = HangugColors.Error) } },
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
                    Text("Register", color = HangugColors.OnPrimary)
                }
            }

            TextButton(
                onClick = onGoToLogin,
                enabled = !isSubmitting,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text(
                    "Already have an account? Log in",
                    color = HangugColors.Secondary,
                )
            }
        }
    }
}