package app.safetopost

import android.net.Uri
import app.safetopost.model.CaptionFinding
import app.safetopost.model.FaceRegion
import app.safetopost.model.PhotoMetadata
import app.safetopost.model.RiskScore
import app.safetopost.photo.LoadedPhoto

enum class ScanPhase { IDLE, SCANNING, READY, EXPORTING }

enum class UiAction { SHARE, UNDO_CAPTION }

data class UiMessage(
    val text: String,
    val action: UiAction? = null,
    val id: Long = System.nanoTime()
)

data class ScanState(
    val phase: ScanPhase = ScanPhase.IDLE,
    val photo: LoadedPhoto? = null,
    val metadata: PhotoMetadata = PhotoMetadata(),
    val faces: List<FaceRegion> = emptyList(),
    val caption: String = "",
    val findings: List<CaptionFinding> = emptyList(),
    val score: RiskScore = RiskScore(),
    val message: UiMessage? = null,
    val lastExport: Uri? = null,
    val previousCaption: String? = null
)
