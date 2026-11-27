package app.safetopost

import android.graphics.Bitmap
import android.net.Uri
import app.safetopost.model.PhotoMetadata
import app.safetopost.model.RiskScore

enum class BatchPhase { SCANNING, READY, EXPORTING }

data class BatchItem(
    val index: Int,
    val uri: Uri,
    val name: String,
    val metadata: PhotoMetadata,
    val score: RiskScore,
    val thumbnail: Bitmap? = null,
    val selected: Boolean = true,
    val cleaned: Boolean = false
)

data class BatchState(
    val phase: BatchPhase = BatchPhase.READY,
    val items: List<BatchItem> = emptyList(),
    val progress: Int = 0,
    val total: Int = 0,
    val message: String? = null
)
