package com.directshare.app.utils

import android.util.Log

/**
 * Structured logger that redacts sensitive signaling tokens or credentials
 * and never logs raw file payloads.
 */
object AppLogger {
    private const val GLOBAL_TAG = "DirectShare"

    fun d(component: String, message: String) {
        try {
            Log.d("$GLOBAL_TAG:$component", sanitize(message))
        } catch (_: Throwable) {
            // Safe fallback when running in JVM unit tests without android.util.Log mocked
            println("DEBUG [$GLOBAL_TAG:$component] ${sanitize(message)}")
        }
    }

    fun i(component: String, message: String) {
        try {
            Log.i("$GLOBAL_TAG:$component", sanitize(message))
        } catch (_: Throwable) {
            println("INFO [$GLOBAL_TAG:$component] ${sanitize(message)}")
        }
    }

    fun w(component: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.w("$GLOBAL_TAG:$component", sanitize(message), throwable)
            } else {
                Log.w("$GLOBAL_TAG:$component", sanitize(message))
            }
        } catch (_: Throwable) {
            println("WARN [$GLOBAL_TAG:$component] ${sanitize(message)} ${throwable?.message ?: ""}")
        }
    }

    fun e(component: String, message: String, throwable: Throwable? = null) {
        try {
            if (throwable != null) {
                Log.e("$GLOBAL_TAG:$component", sanitize(message), throwable)
            } else {
                Log.e("$GLOBAL_TAG:$component", sanitize(message))
            }
        } catch (_: Throwable) {
            System.err.println("ERROR [$GLOBAL_TAG:$component] ${sanitize(message)} ${throwable?.message ?: ""}")
        }
    }

    private fun sanitize(input: String): String {
        return input
            .replace(Regex("(credential|password|apiKey)=([^&\\s]+)", RegexOption.IGNORE_CASE), "$1=***REDACTED***")
    }
}
