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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors
import com.appriyo.deulama.ui.theme.HeroTitleCompact

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    onGoToRegister: () -> Unit,
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

            Button(
                onClick = onLoginSuccess,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HangugBrandGradient, RoundedCornerShape(12.dp)),
            ) {
                Text("Log In", color = HangugColors.OnPrimary)
            }

            TextButton(
                onClick = onContinueWithoutAccount,
                modifier = Modifier.padding(top = 12.dp),
            ) {
                Text("Continue browsing without an account", color = HangugColors.TextSecondary)
            }

            TextButton(onClick = onGoToRegister) {
                Text("Don't have an account? Register", color = HangugColors.Secondary)
            }
        }
    }
}