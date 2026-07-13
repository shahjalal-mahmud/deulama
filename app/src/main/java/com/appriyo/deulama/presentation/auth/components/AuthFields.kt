package com.appriyo.deulama.presentation.auth.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.appriyo.deulama.ui.theme.HangugColors

/**
 * Text field matching the web's AuthInput: uppercase label above a
 * rounded, elevated field. Built on BasicTextField so the focus/error
 * border glow can be hand-tuned instead of fighting OutlinedTextField's
 * built-in chrome.
 *
 * Fix: the placeholder and the real input text now share the exact
 * same padding/box alignment, so the placeholder no longer floats
 * above or below the typed text — both sit centered in the field.
 */
@Composable
fun AuthTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    trailing: @Composable (() -> Unit)? = null,
    labelTrailing: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()

    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> HangugColors.Primary
            focused -> HangugColors.Primary
            else -> HangugColors.OutlineVariant
        },
        animationSpec = tween(180),
        label = "authFieldBorder",
    )
    val borderWidth = if (focused || isError) 1.6.dp else 1.dp
    val iconTint by animateColorAsState(
        targetValue = if (focused || isError) HangugColors.Primary else HangugColors.TextTertiary,
        animationSpec = tween(180),
        label = "authFieldIconTint",
    )
    val iconBg by animateColorAsState(
        targetValue = if (focused || isError) HangugColors.Primary.copy(alpha = 0.14f) else HangugColors.BgElevated,
        animationSpec = tween(180),
        label = "authFieldIconBg",
    )
    val fieldElevation by animateColorAsState(
        targetValue = HangugColors.Primary,
        animationSpec = tween(180),
        label = "authFieldElevationTint",
    )

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.4.sp),
                color = HangugColors.TextSecondary,
                fontWeight = FontWeight.SemiBold,
            )
            labelTrailing?.invoke()
        }
        Spacer(Modifier.size(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = if (focused) 8.dp else 0.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = fieldElevation.copy(alpha = 0.25f),
                    spotColor = fieldElevation.copy(alpha = 0.25f),
                )
                .clip(RoundedCornerShape(16.dp))
                .background(HangugColors.BgElevated2)
                .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.size(10.dp))
            }
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = HangugColors.TextTertiary,
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = singleLine,
                    textStyle = LocalTextStyle.current.copy(
                        color = HangugColors.TextPrimary,
                        fontSize = 16.sp,
                    ),
                    cursorBrush = SolidColor(HangugColors.Primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    interactionSource = interactionSource,
                )
            }
            trailing?.invoke()
        }

        if (isError && errorText != null) {
            Spacer(Modifier.size(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = HangugColors.Primary,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.labelSmall,
                    color = HangugColors.Primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
    }
}

/** Password variant of [AuthTextField] with a built-in visibility toggle. */
@Composable
fun AuthPasswordField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "••••••••",
    isError: Boolean = false,
    errorText: String? = null,
    showForgotLink: Boolean = false,
    onForgotClick: (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    var visible by remember { mutableStateOf(false) }

    AuthTextField(
        label = label,
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = placeholder,
        leadingIcon = Icons.Filled.Lock,
        isError = isError,
        errorText = errorText,
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        labelTrailing = if (showForgotLink) {
            {
                TextButton(onClick = { onForgotClick?.invoke() }, contentPadding = PaddingValues(0.dp)) {
                    Text(
                        text = "Forgot?",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                        color = HangugColors.Primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        } else null,
        trailing = {
            IconButton(onClick = { visible = !visible }, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = if (visible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (visible) "Hide password" else "Show password",
                    tint = HangugColors.TextTertiary,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
    )
}