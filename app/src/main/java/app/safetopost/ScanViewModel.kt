package app.safetopost

import android.app.Application
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.safetopost.caption.CaptionCleaner
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

    private val _batch = MutableStateFlow(BatchState())
    val batch: StateFlow<BatchState> = _batch.asStateFlow()
    private val batchBusy = AtomicBoolean(false)

    private val captionScanner = CaptionScanner()
    private val metadataReader = MetadataReader(application)
    private val faceFinder = FaceFinder()
    private val busy = AtomicBoolean(false)

    fun loadBatch(uris: List<Uri>) {
        val selected = uris.distinct().take(12)
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _batch.value = BatchState(phase = BatchPhase.SCANNING, total = selected.size)
            val items = mutableListOf<BatchItem>()
            selected.forEachIndexed { index, uri ->
                try {
                    val name = withContext(Dispatchers.IO) {
                        PhotoFiles.displayName(getApplication(), uri)
                    } ?: "Photo ${index + 1}"
                    val bytes = withContext(Dispatchers.IO) {
                        PhotoFiles.bytes(getApplication(), uri)
                    }
                    val segments = JpegInspector.inspect(bytes)
                    val metadata = metadataReader.read(bytes, segments)
                    val score = RiskScorer.score(metadata, emptyList())
                    val thumbnail = runCatching {
                        withContext(Dispatchers.IO) {
                            PhotoFiles.thumbnail(getApplication(), uri)
                        }
                    }.getOrNull()
                    items += BatchItem(index, uri, name, metadata, score, thumbnail = thumbnail)
                } catch (t: Throwable) {
                    // Skip a photo that cannot be read; the rest of the batch continues.
                }
                _batch.update { it.copy(items = items.toList(), progress = index + 1) }
            }
            _batch.update { it.copy(phase = BatchPhase.READY, items = items) }
        }
    }

    fun toggleBatchItem(index: Int) {
        _batch.update { state ->
            state.copy(
                items = state.items.map { item ->
                    if (item.index == index && !item.cleaned) item.copy(selected = !item.selected) else item
                }
            )
        }
    }

    fun cleanBatch() {
        val current = _batch.value
        val targets = current.items.filter { it.selected && !it.cleaned }
        if (targets.isEmpty() || current.phase != BatchPhase.READY) return
        if (!batchBusy.compareAndSet(false, true)) return
        viewModelScope.launch {
            _batch.update { it.copy(phase = BatchPhase.EXPORTING, progress = 0, total = targets.size) }
            var cleaned = 0
            targets.forEachIndexed { position, item ->
                if (cleanItem(item)) {
                    cleaned++
                    markCleaned(item.index)
                }
                _batch.update { it.copy(progress = position + 1) }
            }
            _batch.update { it.copy(phase = BatchPhase.READY) }
            _state.update {
                it.copy(message = UiMessage("Batch cleaned: $cleaned of ${targets.size} selected photos"))
            }
            batchBusy.set(false)
        }
    }

    fun cleanBatchItem(index: Int) {
        val current = _batch.value
        val item = current.items.firstOrNull { it.index == index } ?: return
        if (item.cleaned || current.phase != BatchPhase.READY) return
        if (!batchBusy.compareAndSet(false, true)) return
        viewModelScope.launch {
            _batch.update { it.copy(phase = BatchPhase.EXPORTING, progress = 0, total = 1) }
            val ok = cleanItem(item)
            if (ok) markCleaned(item.index)
            _batch.update { it.copy(phase = BatchPhase.READY) }
            _state.update {
                it.copy(message = UiMessage(if (ok) "Cleaned ${item.name}" else "Could not clean ${item.name}"))
            }
            batchBusy.set(false)
        }
    }

    private fun markCleaned(index: Int) {
        _batch.update { state ->
            state.copy(items = state.items.map { if (it.index == index) it.copy(cleaned = true) else it })
        }
    }

    private suspend fun cleanItem(item: BatchItem): Boolean {
        return try {
            val photo = withContext(Dispatchers.IO) { PhotoFiles.load(getApplication(), item.uri) }
            val bytes = withContext(Dispatchers.Default) { MetadataStripper.strip(photo.bitmap) }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return false
            withContext(Dispatchers.IO) { GalleryExport.save(getApplication(), bitmap, photo.name) } != null
        } catch (t: Throwable) {
            false
        }
    }

    fun clearBatch() {
        _batch.value = BatchState()
    }

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
        _state.update { it.copy(caption = text, findings = findings, score = score, previousCaption = null) }
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
                            else "Could not save the clean copy",
                            action = if (uri != null) UiAction.SHARE else null
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

    fun cleanCaption() {
        val current = _state.value
        if (current.findings.isEmpty()) return
        val cleaned = CaptionCleaner.clean(current.caption, current.findings)
        val findings = captionScanner.scan(cleaned)
        val score = RiskScorer.score(current.metadata, findings)
        _state.update {
            it.copy(
                previousCaption = it.caption,
                caption = cleaned,
                findings = findings,
                score = score,
                message = UiMessage(
                    "Caption cleaned · ${current.findings.size} detail(s) replaced",
                    action = UiAction.UNDO_CAPTION
                )
            )
        }
    }

    fun undoCaptionClean() {
        val previous = _state.value.previousCaption ?: return
        val findings = captionScanner.scan(previous)
        val score = RiskScorer.score(_state.value.metadata, findings)
        _state.update {
            it.copy(
                previousCaption = null,
                caption = previous,
                findings = findings,
                score = score,
                message = UiMessage("Caption restored")
            )
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
