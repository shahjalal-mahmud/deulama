package com.appriyo.deulama.presentation.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.appriyo.deulama.R
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Visual pass: swapped the flat letter badges ("G" / blank) for the
 * real, full-color provider logos so the row reads as a proper,
 * trustworthy sign-in row rather than a placeholder mockup.
 *
 * Requires two vector drawables in res/drawable:
 *   - ic_google_logo.xml  (4-color Google "G")
 *   - ic_apple_logo.xml   (Apple glyph)
 */
@Composable
fun SocialAuthRow(modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SocialButton(
            iconRes = R.drawable.ic_google_logo,
            text = "Google",
            modifier = Modifier.weight(1f),
        )
        SocialButton(
            iconRes = R.drawable.ic_apple_logo,
            text = "Apple",
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SocialButton(iconRes: Int, text: String, modifier: Modifier = Modifier) {
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
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = "$text logo",
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = text,
            color = HangugColors.TextPrimary,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}