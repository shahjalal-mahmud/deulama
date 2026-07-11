package com.appriyo.deulama.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.presentation.components.StatusBanner
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onOpenDiscover: () -> Unit,
    onOpenDramaDetails: (Int) -> Unit,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()

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
                text = "Home",
                style = MaterialTheme.typography.displayLarge,
                color = HangugColors.TextPrimary,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(
                text = "지금 인기 · TRENDING NOW",
                style = MaterialTheme.typography.labelSmall,
                color = HangugColors.Secondary,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            StatusBanner(
                status = uiState.status,
                message = uiState.message,
                modifier = Modifier.padding(bottom = 32.dp),
            )

            Button(onClick = onOpenDiscover, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("Go to Discover")
            }

            OutlinedButton(onClick = { onOpenDramaDetails(1) }) {
                Text("Open a sample drama's details")
            }
        }
    }
}