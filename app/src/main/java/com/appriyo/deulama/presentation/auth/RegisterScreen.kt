package com.appriyo.deulama.presentation.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors

@Composable
fun RegisterScreen(
    onRegisterSuccess: () -> Unit,
    onGoToLogin: () -> Unit,
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Create your account",
                style = MaterialTheme.typography.headlineMedium,
                color = HangugColors.TextPrimary,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Button(
                onClick = onRegisterSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HangugBrandGradient, RoundedCornerShape(12.dp)),
            ) {
                Text("Register", color = HangugColors.OnPrimary)
            }

            TextButton(onClick = onGoToLogin, modifier = Modifier.padding(top = 12.dp)) {
                Text("Already have an account? Log in", color = HangugColors.Secondary)
            }
        }
    }
}