package app.safetopost.metadata

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import app.safetopost.model.AttributeSource
import app.safetopost.model.LeakCategory
import app.safetopost.model.MetadataEntry
import app.safetopost.model.PhotoMetadata
import app.safetopost.model.Risk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Parses EXIF and JPEG segments into the list the UI explains. */
class MetadataReader(private val context: Context) {

    suspend fun read(bytes: ByteArray, segments: List<JpegSegment>): PhotoMetadata =
        withContext(Dispatchers.IO) {
            val temp = File.createTempFile("safetopost", ".jpg", context.cacheDir)
            try {
                temp.writeBytes(bytes)
                val exif = ExifInterface(temp.absolutePath)
                val entries = mutableListOf<MetadataEntry>()

                for (spec in ExifCatalog.specs) {
                    // GPS is parsed from the APP1/TIFF block below: the platform reader
                    // returns zeros for some camera files, so we do not trust it there.
                    if (spec.tag == ExifInterface.TAG_GPS_LATITUDE ||
                        spec.tag == ExifInterface.TAG_GPS_LONGITUDE
                    ) continue

                    val raw = exif.getAttribute(spec.tag) ?: continue
                    entries += MetadataEntry(
                        tag = spec.tag,
                        label = spec.label,
                        value = ValueFormat.short(raw),
                        source = AttributeSource.EXIF,
                        category = spec.category,
                        risk = spec.risk
                    )
                }

                val gps = ExifGpsParser.parse(bytes, segments)
                if (gps != null) {
                    entries += MetadataEntry(
                        tag = ExifInterface.TAG_GPS_LATITUDE,
                        label = "GPS latitude",
                        value = gps.latitude,
                        source = AttributeSource.EXIF,
                        category = LeakCategory.LOCATION,
                        risk = Risk.HIGH
                    )
                    entries += MetadataEntry(
                        tag = ExifInterface.TAG_GPS_LONGITUDE,
                        label = "GPS longitude",
                        value = gps.longitude,
                        source = AttributeSource.EXIF,
                        category = LeakCategory.LOCATION,
                        risk = Risk.HIGH
                    )
                }

                val hasThumbnail = runCatching { exif.thumbnailBytes?.isNotEmpty() == true }.getOrDefault(false)
                if (hasThumbnail) {
                    entries += MetadataEntry(
                        tag = "thumbnail",
                        label = "Embedded thumbnail",
                        value = "present",
                        source = AttributeSource.EXIF,
                        category = LeakCategory.IDENTITY,
                        risk = Risk.MEDIUM
                    )
                }

                val exifBlocks = segments.count { it.kind == SegmentKind.APP1 }
                if (exifBlocks > 0) {
                    entries += MetadataEntry(
                        tag = "app1",
                        label = "EXIF block",
                        value = "$exifBlocks block(s)",
                        source = AttributeSource.JPEG_SEGMENT,
                        category = LeakCategory.DEVICE,
                        risk = Risk.LOW
                    )
                }
                if (segments.any { it.kind == SegmentKind.COMMENT }) {
                    entries += MetadataEntry(
                        tag = "comment",
                        label = "JPEG comment",
                        value = "present",
                        source = AttributeSource.JPEG_SEGMENT,
                        category = LeakCategory.IDENTITY,
                        risk = Risk.LOW
                    )
                }

                PhotoMetadata(
                    entries = entries,
                    hasGps = gps != null,
                    captureTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL),
                    hasThumbnail = hasThumbnail
                )
            } catch (t: Throwable) {
                PhotoMetadata()
            } finally {
                temp.delete()
            }
        }
}
