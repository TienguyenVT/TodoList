package com.example.myapplication.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.myapplication.ui.theme.NeumorphicColors

/**
 * Shimmer effect for skeleton loading placeholders.
 * Shows a subtle animation to indicate content is loading.
 */
@Composable
fun ShimmerEffect(
    modifier: Modifier = Modifier,
    baseColor: Color = NeumorphicColors.surface,
    highlightColor: Color = NeumorphicColors.background.copy(alpha = 0.6f)
) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )

    Box(
        modifier = modifier
            .background(baseColor.copy(alpha = alpha))
    )
}

/**
 * Skeleton placeholder for a single task card.
 */
@Composable
fun SkeletonTaskCard(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        ShimmerEffect(modifier = Modifier.fillMaxSize())
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Title placeholder
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .clip(RoundedCornerShape(4.dp)),
                highlightColor = NeumorphicColors.textSecondary.copy(alpha = 0.2f)
            )
            
            // Subtitle placeholder
            ShimmerEffect(
                modifier = Modifier
                    .fillMaxWidth(0.4f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp)),
                highlightColor = NeumorphicColors.textSecondary.copy(alpha = 0.15f)
            )
        }
    }
}

/**
 * Skeleton placeholder for a Kanban column.
 */
@Composable
fun SkeletonKanbanColumn(
    itemCount: Int = 3,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(NeumorphicColors.surface)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Header placeholder
        ShimmerEffect(
            modifier = Modifier
                .fillMaxWidth(0.5f)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Task placeholders in grid-like pattern
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(minOf(itemCount, 3)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                ) {
                    ShimmerEffect(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

/**
 * Full screen skeleton for KanbanBoard.
 */
@Composable
fun SkeletonKanbanScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            SkeletonKanbanColumn(
                itemCount = 3,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * Full screen skeleton for CalendarScreen task list.
 */
@Composable
fun SkeletonTaskList(
    itemCount: Int = 5,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(itemCount) {
            SkeletonTaskCard()
        }
    }
}
