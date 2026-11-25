package app.safetopost.photo

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream

/** Re-encodes the bitmap so the exported copy carries no EXIF/APP segments. */
object MetadataStripper {

    fun strip(bitmap: Bitmap, quality: Int = 95): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        return out.toByteArray()
    }
}
