package com.example.myapplication.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun AnimateEntrance(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    durationMillis: Int = 400,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        visible = true
    }

    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            )
        ) + scaleIn(
            initialScale = 0.9f,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = FastOutSlowInEasing
            )
        ),
        exit = fadeOut(
            animationSpec = tween(
                durationMillis = durationMillis / 2,
                easing = FastOutSlowInEasing
            )
        ) + scaleOut(
            targetScale = 0.9f,
            animationSpec = tween(
                durationMillis = durationMillis / 2,
                easing = FastOutSlowInEasing
            )
        )
    ) {
        content()
    }
}