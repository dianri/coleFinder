package es.colefinder.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun createNumberedMarkerBitmap(
    context: Context,
    number: String,
    color: Color
): BitmapDescriptor {
    val size = 100 // Size of the marker in pixels
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    // Paint for the circle background
    val bgPaint = Paint().apply {
        isAntiAlias = true
        this.color = color.toArgb()
        style = Paint.Style.FILL
    }

    // Paint for the border/stroke
    val borderPaint = Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }

    // Paint for the number text
    val textPaint = Paint().apply {
        isAntiAlias = true
        this.color = android.graphics.Color.WHITE
        textSize = 40f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }

    // Draw background circle
    val radius = size / 2f
    canvas.drawCircle(radius, radius, radius - 4f, bgPaint)
    canvas.drawCircle(radius, radius, radius - 4f, borderPaint)

    // Draw centered text
    val bounds = Rect()
    textPaint.getTextBounds(number, 0, number.length, bounds)
    val x = radius
    val y = radius + (bounds.height() / 2f)
    canvas.drawText(number, x, y, textPaint)

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}
