package com.roposo.creation.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect

/**
 *  @author janika on 17/11/17.
 */

fun Bitmap?.getScaledBitmap(width: Int, height: Int): Bitmap? {
    if (this == null || width <= 0 || height <= 0) {
        return null
    }
    if (!isRecycled) {
        val result = Bitmap.createBitmap(width, height, config)
        val canvas = Canvas(result)
        val scaleX = width.toFloat() / getWidth().toFloat()
        val scaleY = height.toFloat() / getHeight().toFloat()
        val scale = if (scaleX > scaleY) scaleX else scaleY
        val w = (getWidth() * scale).toInt()
        val h = (getHeight() * scale).toInt()
        val srcRect = Rect(0, 0, getWidth(), getHeight())
        val destRect = Rect((width - w) / 2, (height - h) / 2, w, h)
        canvas.drawBitmap(this, srcRect, destRect, null)
        recycle()
        return result
    }

    return null
}