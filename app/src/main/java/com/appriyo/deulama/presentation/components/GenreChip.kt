package com.appriyo.deulama.presentation.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Native FilterChip standing in for the web's pill buttons — same
 * accent-on-select behavior, but using the platform's real chip
 * component so it gets ripple/focus/talkback for free.
 */
@Composable
fun GenreChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        modifier = modifier.padding(end = 8.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = HangugColors.SurfaceContainer,
            labelColor = HangugColors.TextSecondary,
            selectedContainerColor = HangugColors.Primary,
            selectedLabelColor = HangugColors.OnPrimary,
        ),
        border = null,
    )
}