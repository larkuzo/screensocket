package app.screensocket

import android.util.Log

fun ByteArray.asBitmapData(): IntArray {
    val buffer = IntArray(this.size / 3)

    for (i in 0 until (this.size / 3)) {
        val a = 0xFF shl 24
        val r = this[i * 3 + 0].toInt() shl 16
        val g = this[i * 3 + 1].toInt() shl 8
        val b = this[i * 3 + 2].toInt() shl 0

        buffer[i] = a + r + g + b
    }

    return buffer
}

@Suppress("NOTHING_TO_INLINE")
inline fun log(message: Any) = Log.d("screensocket", message.toString())
