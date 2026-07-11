package com.appriyo.deulama.presentation.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

@Composable
fun DiscoverScreen(
    onOpenDramaDetails: (Int) -> Unit,
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
                text = "Discover",
                style = MaterialTheme.typography.displayLarge,
                color = HangugColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "Swipe deck lives here — Phase 4",
                style = MaterialTheme.typography.bodyMedium,
                color = HangugColors.TextSecondary,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            OutlinedButton(onClick = { onOpenDramaDetails(1) }) {
                Text("View a drama's details")
            }
        }
    }
}