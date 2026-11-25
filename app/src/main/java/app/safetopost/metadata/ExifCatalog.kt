package app.safetopost.metadata

import androidx.exifinterface.media.ExifInterface
import app.safetopost.model.LeakCategory
import app.safetopost.model.Risk

data class TagSpec(
    val tag: String,
    val label: String,
    val category: LeakCategory,
    val risk: Risk
)

/** Which tags the app can explain, and how much each one matters. */
object ExifCatalog {

    val specs: List<TagSpec> = listOf(
        TagSpec(ExifInterface.TAG_GPS_LATITUDE, "GPS latitude", LeakCategory.LOCATION, Risk.HIGH),
        TagSpec(ExifInterface.TAG_GPS_LONGITUDE, "GPS longitude", LeakCategory.LOCATION, Risk.HIGH),
        TagSpec(ExifInterface.TAG_GPS_ALTITUDE, "GPS altitude", LeakCategory.LOCATION, Risk.MEDIUM),
        TagSpec(ExifInterface.TAG_DATETIME_ORIGINAL, "Capture time", LeakCategory.TIME, Risk.MEDIUM),
        TagSpec(ExifInterface.TAG_DATETIME, "File date/time", LeakCategory.TIME, Risk.LOW),
        TagSpec(ExifInterface.TAG_MAKE, "Device make", LeakCategory.DEVICE, Risk.LOW),
        TagSpec(ExifInterface.TAG_MODEL, "Device model", LeakCategory.DEVICE, Risk.LOW),
        TagSpec(ExifInterface.TAG_SOFTWARE, "Software", LeakCategory.DEVICE, Risk.LOW),
        TagSpec(ExifInterface.TAG_LENS_MODEL, "Lens", LeakCategory.DEVICE, Risk.LOW),
        TagSpec(ExifInterface.TAG_FOCAL_LENGTH, "Focal length", LeakCategory.DEVICE, Risk.LOW),
        TagSpec(ExifInterface.TAG_IMAGE_DESCRIPTION, "Description", LeakCategory.IDENTITY, Risk.MEDIUM),
        TagSpec(ExifInterface.TAG_ARTIST, "Artist", LeakCategory.IDENTITY, Risk.HIGH),
        TagSpec(ExifInterface.TAG_COPYRIGHT, "Copyright", LeakCategory.IDENTITY, Risk.LOW),
        TagSpec(ExifInterface.TAG_ORIENTATION, "Orientation", LeakCategory.DEVICE, Risk.LOW)
    )

    fun specFor(tag: String): TagSpec? = specs.firstOrNull { it.tag == tag }
}
