package app.safetopost.model

enum class LeakCategory { LOCATION, IDENTITY, DEVICE, TIME, SOCIAL, OTHER }

enum class AttributeSource { EXIF, JPEG_SEGMENT, INFERRED }

enum class Risk { LOW, MEDIUM, HIGH }

enum class CaptionKind { PHONE, EMAIL, ADDRESS, DATE, SCHOOL_OR_WORK, HANDLE, LINK }

enum class RiskLevel { LOW, MEDIUM, HIGH }

enum class RiskSource { METADATA, CAPTION }

data class PixelRect(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width: Int get() = (right - left).coerceAtLeast(0)
    val height: Int get() = (bottom - top).coerceAtLeast(0)
}

data class FaceRegion(val id: Int, val rect: PixelRect, val selected: Boolean = false)

data class MetadataEntry(
    val tag: String,
    val label: String,
    val value: String,
    val source: AttributeSource,
    val category: LeakCategory,
    val risk: Risk
)

data class PhotoMetadata(
    val entries: List<MetadataEntry> = emptyList(),
    val hasGps: Boolean = false,
    val captureTime: String? = null,
    val width: Int = 0,
    val height: Int = 0,
    val hasThumbnail: Boolean = false
)

data class CaptionFinding(val kind: CaptionKind, val range: IntRange, val matched: String)

data class RiskFactor(val source: RiskSource, val weight: Int, val explanation: String)

data class RiskScore(val total: Int = 0, val level: RiskLevel = RiskLevel.LOW, val factors: List<RiskFactor> = emptyList())
