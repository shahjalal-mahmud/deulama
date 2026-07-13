package com.appriyo.deulama.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appriyo.deulama.ui.theme.HangugBrandGradient
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Native equivalent of the web's `.btn-gradient` — a rose→gold linear
 * gradient CTA. Uses a plain clickable Box (not Material Button) so we
 * get full control over the gradient fill, which a themed Button's
 * containerColor can't express.
 *
 * Visual pass: adds a soft tinted shadow ("glow") that eases on press,
 * a slightly larger touch target, and a tiny arrow nudge for tactile
 * feedback. No behavioral changes — same params, same click contract.
 */
@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    showArrow: Boolean = true,
    gradient: Brush = HangugBrandGradient,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val active = enabled && !loading

    val scale = if (pressed) 0.97f else 1f
    val shadowElevation by animateDpAsState(
        targetValue = if (!active) 2.dp else if (pressed) 6.dp else 16.dp,
        animationSpec = spring(),
        label = "gradientButtonShadow",
    )
    val arrowOffset by animateDpAsState(
        targetValue = if (pressed) 3.dp else 0.dp,
        animationSpec = spring(),
        label = "gradientButtonArrow",
    )

    Row(
        modifier = modifier
            .height(58.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = shadowElevation,
                shape = RoundedCornerShape(18.dp),
                ambientColor = HangugColors.Primary.copy(alpha = 0.35f),
                spotColor = HangugColors.Primary.copy(alpha = 0.45f),
            )
            .clip(RoundedCornerShape(18.dp))
            .background(gradient)
            .alpha(if (active) 1f else 0.55f)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = active,
                onClick = onClick,
            ),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnimatedVisibility(visible = loading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = HangugColors.OnPrimary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.size(10.dp))
            }
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 0.3.sp),
            color = HangugColors.OnPrimary,
            fontWeight = FontWeight.SemiBold,
        )
        if (showArrow && !loading) {
            Spacer(Modifier.size(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = HangugColors.OnPrimary,
                modifier = Modifier.size(18.dp).offset(x = arrowOffset),
            )
        }
    }
}