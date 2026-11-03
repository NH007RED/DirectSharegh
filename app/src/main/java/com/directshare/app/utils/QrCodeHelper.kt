package com.directshare.app.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

object QrCodeHelper {
    private const val QR_SCHEME_PREFIX = "directshare://pair?"

    fun encodePairingUri(sessionId: String, pairingCode: String): String {
        return "${QR_SCHEME_PREFIX}sessionId=$sessionId&code=$pairingCode"
    }

    fun parsePairingPayload(rawInput: String): Pair<String?, String?> {
        val trimmed = rawInput.trim()
        if (trimmed.startsWith(QR_SCHEME_PREFIX, ignoreCase = true)) {
            val query = trimmed.substringAfter("?")
            val params = query.split("&").associate {
                val parts = it.split("=", limit = 2)
                parts.getOrNull(0).orEmpty() to parts.getOrNull(1).orEmpty()
            }
            return params["sessionId"]?.takeIf { it.isNotBlank() } to
                params["code"]?.takeIf { it.isNotBlank() }
        }
        return if (trimmed.length == 6 && trimmed.all { it.isLetterOrDigit() }) {
            null to trimmed.uppercase()
        } else {
            trimmed.takeIf { it.isNotBlank() } to null
        }
    }

    fun generateQrBitmap(content: String, sizePx: Int = 512): Bitmap? {
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = QRCodeWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                sizePx,
                sizePx,
                hints
            )
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val foregroundCrimson = Color.parseColor("#FF1744")
            val backgroundDeepBlack = Color.parseColor("#050507")
            for (x in 0 until sizePx) {
                for (y in 0 until sizePx) {
                    bitmap.setPixel(
                        x,
                        y,
                        if (bitMatrix[x, y]) foregroundCrimson else backgroundDeepBlack
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            AppLogger.e("QrCodeHelper", "Failed to generate QR bitmap", e)
            null
        }
    }
}
