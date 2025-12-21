package com.example.myapplication.utils

import android.util.Log

/**
 * Utility class for performance debugging and logging.
 * Use Logcat filter: "PerfDebug" to see all performance logs.
 */
object PerfLogger {
    private const val TAG = "PerfDebug"
    private var lastActionTime = 0L
    private var actionCount = 0

    /**
     * Log a user action with file, function, and action description.
     * Automatically calculates delay since the last action.
     */
    fun logAction(file: String, function: String, action: String) {
        val now = System.currentTimeMillis()
        val delay = if (lastActionTime > 0) now - lastActionTime else 0
        actionCount++

        Log.d(TAG, """
            |=== ACTION #$actionCount ===
            |📍 Location: $file :: $function
            |🎯 Action: $action
            |⏱️ Delay since last action: ${delay}ms
            |🕐 Timestamp: $now
        """.trimMargin())

        lastActionTime = now
    }

    /**
     * Log a render event with the number of items rendered.
     */
    fun logRender(file: String, function: String, itemCount: Int) {
        Log.d(TAG, """
            |--- RENDER ---
            |📍 Location: $file :: $function
            |📦 Items rendered: $itemCount
            |🕐 Timestamp: ${System.currentTimeMillis()}
        """.trimMargin())
    }

    /**
     * Log a recomposition event.
     */
    fun logRecomposition(file: String, composable: String) {
        Log.d(TAG, "♻️ RECOMPOSE: $file :: $composable @ ${System.currentTimeMillis()}")
    }

    /**
     * Reset the action counter and last action time.
     * Useful for testing or when starting a new session.
     */
    fun reset() {
        lastActionTime = 0L
        actionCount = 0
    }
}
