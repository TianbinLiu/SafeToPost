package app.safetopost.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns

data class LoadedPhoto(
    val bitmap: Bitmap,
    val bytes: ByteArray,
    val name: String,
    val mimeType: String?
)

object PhotoFiles {

    fun load(context: Context, uri: Uri): LoadedPhoto {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() }
            ?: error("Could not open the selected photo")
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)

        var sample = 1
        while (bounds.outWidth / sample > 2048 || bounds.outHeight / sample > 2048) sample *= 2

        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            ?: error("Could not decode the selected photo")

        return LoadedPhoto(
            bitmap = bitmap,
            bytes = bytes,
            name = queryName(context, uri) ?: "photo",
            mimeType = resolver.getType(uri)
        )
    }

    private fun queryName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
