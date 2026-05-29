package com.example.auramusic.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun RotatingVinyl(
    imageUrl: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "vinylRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    // Smoothly stop/start rotation could be more complex, but simple rotation for now
    val currentRotation = if (isPlaying) rotation else 0f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .neumorphic(elevation = 20.dp, cornerRadius = 200.dp)
            .graphicsLayer { rotationZ = currentRotation },
        contentAlignment = Alignment.Center
    ) {
        // Outer Vinyl Circle
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = CircleShape,
            color = Color.Black,
            border = BorderStroke(8.dp, Color(0xFF1A1A1A))
        ) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .clip(CircleShape)
            )
        }
        // Center hole
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.8f),
            border = BorderStroke(2.dp, Color.Gray)
        ) {}
    }
}
