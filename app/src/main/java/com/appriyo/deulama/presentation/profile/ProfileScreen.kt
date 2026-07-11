package com.appriyo.deulama.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
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
fun ProfileScreen(
    onOpenEditProfile: () -> Unit,
    onLogout: () -> Unit,
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
                text = "Profile",
                style = MaterialTheme.typography.displayLarge,
                color = HangugColors.TextPrimary,
                modifier = Modifier.padding(bottom = 24.dp),
            )
            Button(onClick = onOpenEditProfile, modifier = Modifier.padding(bottom = 12.dp)) {
                Text("Edit Profile")
            }
            TextButton(onClick = onLogout) {
                Text("Log out", color = HangugColors.Danger)
            }
        }
    }
}