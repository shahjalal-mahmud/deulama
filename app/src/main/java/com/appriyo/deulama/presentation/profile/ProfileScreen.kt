package com.appriyo.deulama.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.appriyo.deulama.presentation.auth.AuthUiState
import com.appriyo.deulama.presentation.auth.AuthViewModel
import com.appriyo.deulama.ui.theme.HangugColors
import org.koin.androidx.compose.koinViewModel

@Composable
fun ProfileScreen(
    onOpenEditProfile: () -> Unit,
    onOpenGenreStats: () -> Unit,
    onLogout: () -> Unit,
    authViewModel: AuthViewModel = koinViewModel(),
) {
    val authState by authViewModel.state.collectAsStateWithLifecycle()
    val user = (authState as? AuthUiState.SignedIn)?.user

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
                modifier = Modifier.padding(bottom = 8.dp),
            )

            if (user != null) {
                Text(
                    text = user.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    color = HangugColors.TextPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.labelSmall,
                    color = HangugColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            } else {
                Spacer(Modifier.height(24.dp))
            }

            Button(
                onClick = onOpenEditProfile,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text("Edit Profile")
            }
            OutlinedButton(
                onClick = onOpenGenreStats,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
            ) {
                Text("Genre breakdown", color = HangugColors.Secondary)
            }
            TextButton(onClick = onLogout) {
                Text("Log out", color = HangugColors.Danger)
            }
        }
    }
}
