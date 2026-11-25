package app.safetopost.photo

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import app.safetopost.model.FaceRegion

/** Mosaic-style blur over the faces the user selected. */
object FaceBlur {

    fun apply(source: Bitmap, regions: List<FaceRegion>): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)
        val paint = Paint().apply { isAntiAlias = true }
        val path = Path()

        for (region in regions.filter { it.selected }) {
            val rect = region.rect
            val pad = (rect.width / 10).coerceAtLeast(4)
            val left = (rect.left - pad).coerceIn(0, result.width - 2)
            val top = (rect.top - pad).coerceIn(0, result.height - 2)
            val right = (rect.right + pad).coerceIn(left + 2, result.width)
            val bottom = (rect.bottom + pad).coerceIn(top + 2, result.height)
            val width = right - left
            val height = bottom - top

            val patch = Bitmap.createBitmap(result, left, top, width, height)
            val small = Bitmap.createScaledBitmap(
                patch,
                (width / 18).coerceAtLeast(2),
                (height / 18).coerceAtLeast(2),
                false
            )
            val mosaic = Bitmap.createScaledBitmap(small, width, height, false)

            path.reset()
            path.addRoundRect(
                RectF(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat()),
                width * 0.18f,
                height * 0.18f,
                Path.Direction.CW
            )
            canvas.save()
            canvas.clipPath(path)
            canvas.drawBitmap(mosaic, left.toFloat(), top.toFloat(), paint)
            canvas.restore()
        }
        return result
    }
}
