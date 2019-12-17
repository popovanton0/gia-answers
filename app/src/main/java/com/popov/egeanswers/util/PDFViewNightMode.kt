package com.popov.egeanswers.util

import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import com.github.barteksc.pdfviewer.PDFView

fun PDFView.nightMode(nightMode: Boolean) {
    val paintField = PDFView::class.java.getDeclaredField("paint").apply { isAccessible = true }
    val paint = paintField.get(this) as Paint

    if (nightMode) {
        val colorMatrixInverted = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
        val filter = ColorMatrixColorFilter(colorMatrixInverted)
        paint.colorFilter = filter
    } else {
        paint.colorFilter = null
    }
    paintField.set(this, paint)
}