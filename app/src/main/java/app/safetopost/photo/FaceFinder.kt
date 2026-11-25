package app.safetopost.photo

import android.graphics.Bitmap
import app.safetopost.model.FaceRegion
import app.safetopost.model.PixelRect
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** ML Kit face detection; the bundled model keeps everything on the phone. */
class FaceFinder {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .build()
    )

    suspend fun detect(bitmap: Bitmap): List<FaceRegion> = withContext(Dispatchers.IO) {
        val faces = Tasks.await(detector.process(InputImage.fromBitmap(bitmap, 0)))
        faces.mapIndexed { index, face ->
            val box = face.boundingBox
            FaceRegion(index, PixelRect(box.left, box.top, box.right, box.bottom))
        }
    }
}
