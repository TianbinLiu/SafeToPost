package app.safetopost.photo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
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
        val bytes = openPhoto(context, uri).use { it.readBytes() }
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

    /**
     * The system photo picker redacts location metadata unless the app has
     * ACCESS_MEDIA_LOCATION and asks for the original. Fall back to the
     * redacted copy when the permission is missing.
     */
    private fun openPhoto(context: Context, uri: Uri): java.io.InputStream {
        val resolver = context.contentResolver
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            try {
                val original = MediaStore.setRequireOriginal(uri)
                resolver.openInputStream(original)?.let { return it }
            } catch (t: Throwable) {
                // No permission or not a MediaStore URI: use the shared copy.
            }
        }
        return resolver.openInputStream(uri) ?: error("Could not open the selected photo")
    }

    private fun queryName(context: Context, uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}
