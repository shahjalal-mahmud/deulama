package com.appriyo.deulama.presentation.auth.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Visual pass only — same composable signature, no click wiring added
 * or removed (matches the original, which is presentational only).
 * Each provider now gets a small circular badge and a soft card
 * elevation instead of a flat bordered box.
 */
@Composable
fun SocialAuthRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SocialButton(badge = "G", text = "Google", modifier = Modifier.weight(1f))
        SocialButton(badge = "", text = "Apple", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SocialButton(badge: String, text: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(54.dp)
            .shadow(
                elevation = 3.dp,
                shape = RoundedCornerShape(15.dp),
                ambientColor = HangugColors.OutlineVariant,
                spotColor = HangugColors.OutlineVariant,
            )
            .clip(RoundedCornerShape(15.dp))
            .background(HangugColors.BgElevated2)
            .border(1.dp, HangugColors.OutlineVariant, RoundedCornerShape(15.dp)),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(HangugColors.BgElevated),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = badge,
                color = HangugColors.TextPrimary,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.size(10.dp))
        Text(
            text = text,
            color = HangugColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}