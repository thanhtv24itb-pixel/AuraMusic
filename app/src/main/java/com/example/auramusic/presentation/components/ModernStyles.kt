package com.example.auramusic.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.auramusic.presentation.theme.ShadowDark

@Composable
fun Modifier.glassmorphism(
    color: Color = Color.White.copy(alpha = 0.1f),
    cornerRadius: Dp = 24.dp
) = this.then(
    Modifier
        .background(color, RoundedCornerShape(cornerRadius))
        .border(0.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(cornerRadius))
)

@Composable
fun Modifier.neumorphic(
    elevation: Dp = 8.dp,
    cornerRadius: Dp = 24.dp,
    isDark: Boolean = isSystemInDarkTheme()
) = this.then(
    Modifier.shadow(
        elevation = elevation,
        shape = RoundedCornerShape(cornerRadius),
        ambientColor = if (isDark) ShadowDark else Color.Gray.copy(alpha = 0.2f),
        spotColor = if (isDark) ShadowDark else Color.Gray.copy(alpha = 0.3f)
    )
)
