package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Air
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun WindIndicator(
    windSpeed: Float,
    modifier: Modifier = Modifier
) {
    val isEast = windSpeed >= 0
    val magnitude = abs(windSpeed)
    val animatedMagnitude by animateFloatAsState(targetValue = magnitude, label = "wind_mag")

    val (desc, tagColor) = when {
        magnitude < 10f -> "Calm" to Color(0xFF94A3B8)
        magnitude < 30f -> "Light Breeze" to Color(0xFF38BDF8)
        magnitude < 60f -> "Moderate Wind" to Color(0xFFFBBF24)
        else -> "Gale Force" to Color(0xFFF43F5E)
    }

    Box(
        modifier = modifier
            .testTag("wind_indicator")
            .background(Color(0xE60F172A), RoundedCornerShape(12.dp))
            .border(1.dp, tagColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = "Wind Gauge",
                tint = tagColor,
                modifier = Modifier.size(20.dp)
            )

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "WIND",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = desc,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = tagColor
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isEast) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Blowing East",
                            tint = tagColor,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Blowing West",
                            tint = tagColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Text(
                        text = "${String.format("%.1f", animatedMagnitude)} m/s ${if (isEast) "E" else "W"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
