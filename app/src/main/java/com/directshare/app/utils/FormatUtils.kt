package com.directshare.app.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object FormatUtils {

    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "0 B"
        if (bytes < 1024L) return "$bytes B"
        val unit = 1024.0
        val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceAtMost(4)
        val prefix = "KMGT"[exp - 1]
        val value = bytes / unit.pow(exp.toDouble())
        return String.format(Locale.US, "%.2f %sB", value, prefix)
    }

    fun formatSpeed(bytesPerSec: Long): String {
        if (bytesPerSec <= 0L) return "0 B/s"
        return "${formatBytes(bytesPerSec)}/s"
    }

    fun formatRemainingTime(seconds: Long?): String {
        if (seconds == null || seconds < 0L) return "Calculating..."
        if (seconds == 0L) return "< 1s"
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hrs > 0 -> String.format(Locale.US, "%dh %02dm %02ds", hrs, mins, secs)
            mins > 0 -> String.format(Locale.US, "%dm %02ds", mins, secs)
            else -> String.format(Locale.US, "%ds", secs)
        }
    }

    fun formatTimestamp(timestampMs: Long): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
        return sdf.format(Date(timestampMs))
    }
}
