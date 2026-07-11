package com.appriyo.deulama.presentation.details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DramaDetailsScreen(
    dramaId: Int,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Drama Details") },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("← Back") }
                },
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "Drama #$dramaId",
                style = MaterialTheme.typography.displayLarge,
                color = HangugColors.TextPrimary,
            )
            Text(
                text = "Synopsis, cast, similar dramas go here — Phase 3",
                style = MaterialTheme.typography.bodyMedium,
                color = HangugColors.TextSecondary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}