package com.example.myapplication

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myapplication.ui.theme.ZenTaskTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install the Android 12+ splash screen
        val splashScreen = installSplashScreen()

        super.onCreate(savedInstanceState)

        // Track when the Compose app reports it's ready
        var isAppReady = false

        // Keep splash on screen until app is ready
        splashScreen.setKeepOnScreenCondition { !isAppReady }

        // Customize the exit animation: zoom-in + fade-out on the splash icon
        splashScreen.setOnExitAnimationListener { provider ->
            val iconView = provider.iconView ?: run {
                provider.remove()
                return@setOnExitAnimationListener
            }

            val scaleX = ObjectAnimator.ofFloat(iconView, View.SCALE_X, 1f, 3f)
            val scaleY = ObjectAnimator.ofFloat(iconView, View.SCALE_Y, 1f, 3f)
            val fadeOut = ObjectAnimator.ofFloat(iconView, View.ALPHA, 1f, 0f)

            AnimatorSet().apply {
                playTogether(scaleX, scaleY, fadeOut)
                interpolator = AnticipateInterpolator()
                duration = 500L
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        // Remove the splash screen once the animation completes
                        provider.remove()
                    }
                })
                start()
            }
        }

        setContent {
            ZenTaskTheme {
                ZenTaskApp(
                    onAppReady = {
                        // Called from Compose when the main UI is ready to show
                        isAppReady = true
                    }
                )
            }
        }
    }
}