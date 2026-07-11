package com.appriyo.deulama.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

enum class ConnectionStatus { LOADING, CONNECTED, ERROR }

@Composable
fun StatusBanner(
    status: ConnectionStatus,
    message: String,
    modifier: Modifier = Modifier,
) {
    val (bg, dot) = when (status) {
        ConnectionStatus.LOADING -> HangugColors.SurfaceContainer to HangugColors.TextTertiary
        ConnectionStatus.CONNECTED -> HangugColors.TertiaryContainer.copy(alpha = 0.25f) to HangugColors.Tertiary
        ConnectionStatus.ERROR -> HangugColors.ErrorContainer.copy(alpha = 0.35f) to HangugColors.Error
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val emoji = when (status) {
            ConnectionStatus.LOADING -> "⏳"
            ConnectionStatus.CONNECTED -> "🟢"
            ConnectionStatus.ERROR -> "🔴"
        }
        Text(text = "$emoji  ", color = dot)
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = HangugColors.TextPrimary,
        )
    }
}