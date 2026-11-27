package app.safetopost

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.safetopost.caption.CaptionScanner
import app.safetopost.metadata.JpegInspector
import app.safetopost.metadata.MetadataReader
import app.safetopost.photo.FaceBlur
import app.safetopost.photo.FaceFinder
import app.safetopost.photo.GalleryExport
import app.safetopost.photo.MetadataStripper
import app.safetopost.photo.PhotoFiles
import app.safetopost.risk.RiskScorer
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanViewModel(application: Application) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(ScanState())
    val state: StateFlow<ScanState> = _state.asStateFlow()

    private val captionScanner = CaptionScanner()
    private val metadataReader = MetadataReader(application)
    private val faceFinder = FaceFinder()
    private val busy = AtomicBoolean(false)

    fun load(uri: Uri) {
        if (!busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            _state.update { it.copy(phase = ScanPhase.SCANNING) }
            try {
                val photo = withContext(Dispatchers.IO) { PhotoFiles.load(getApplication(), uri) }
                val metadata = withContext(Dispatchers.IO) {
                    val segments = JpegInspector.inspect(photo.bytes)
                    metadataReader.read(photo.bytes, segments).copy(
                        width = photo.bitmap.width,
                        height = photo.bitmap.height
                    )
                }
                val faces = faceFinder.detect(photo.bitmap)
                val findings = captionScanner.scan(_state.value.caption)
                val score = RiskScorer.score(metadata, findings)
                _state.value = ScanState(
                    phase = ScanPhase.READY,
                    photo = photo,
                    metadata = metadata,
                    faces = faces,
                    caption = _state.value.caption,
                    findings = findings,
                    score = score
                )
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        phase = ScanPhase.IDLE,
                        message = UiMessage(
                            if (t is SecurityException) "Android did not grant access to that photo. Pick it from the gallery instead."
                            else t.message ?: "Could not read the photo"
                        )
                    )
                }
            } finally {
                busy.set(false)
            }
        }
    }

    fun updateCaption(text: String) {
        val current = _state.value
        val findings = captionScanner.scan(text)
        val score = RiskScorer.score(current.metadata, findings)
        _state.update { it.copy(caption = text, findings = findings, score = score) }
    }

    fun toggleFace(id: Int) {
        _state.update { state ->
            state.copy(
                faces = state.faces.map { face ->
                    if (face.id == id) face.copy(selected = !face.selected) else face
                }
            )
        }
    }

    fun export() {
        val current = _state.value
        val photo = current.photo ?: return
        if (!busy.compareAndSet(false, true)) return
        viewModelScope.launch {
            _state.update { it.copy(phase = ScanPhase.EXPORTING) }
            try {
                val bytes = withContext(Dispatchers.Default) {
                    val blurred = FaceBlur.apply(photo.bitmap, current.faces)
                    MetadataStripper.strip(blurred)
                }
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: error("Could not encode the cleaned photo")
                val uri = withContext(Dispatchers.IO) {
                    GalleryExport.save(getApplication(), bitmap, photo.name)
                }
                val removed = current.metadata.entries.size
                val blurred = current.faces.count { it.selected }
                _state.update {
                    it.copy(
                        phase = ScanPhase.READY,
                        message = UiMessage(
                            if (uri != null) "Saved to Pictures/Safe to Post · removed $removed metadata fields, blurred $blurred face(s)"
                            else "Could not save the clean copy"
                        ),
                        lastExport = uri
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        phase = ScanPhase.READY,
                        message = UiMessage(t.message ?: "Could not export the photo")
                    )
                }
            } finally {
                busy.set(false)
            }
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
