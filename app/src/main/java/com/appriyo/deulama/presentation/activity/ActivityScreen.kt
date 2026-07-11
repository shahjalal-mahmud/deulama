package com.appriyo.deulama.presentation.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

@Composable
fun ActivityScreen(
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
                text = "Activity",
                style = MaterialTheme.typography.displayLarge,
                color = HangugColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "Protected route — reverse-chronological timeline goes here (Phase 8)",
                style = MaterialTheme.typography.bodyMedium,
                color = HangugColors.TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            TextButton(onClick = onGoToLogin) {
                Text("Simulate: sign out / not signed in", color = HangugColors.Secondary)
            }
        }
    }
}