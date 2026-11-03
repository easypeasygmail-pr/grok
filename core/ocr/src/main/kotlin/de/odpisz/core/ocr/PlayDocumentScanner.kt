package de.odpisz.core.ocr

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import androidx.activity.ComponentActivity
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Document Scanner. The scan UI is a Play services activity, so the app
 * does not take the camera permission itself. When Play services are missing
 * the call fails and the screen keeps the file and paste paths.
 */
@Singleton
class PlayDocumentScanner
    @Inject
    constructor() {
        suspend fun scanIntent(activity: ComponentActivity): IntentSender =
            suspendCancellableCoroutine { continuation ->
                val options =
                    GmsDocumentScannerOptions
                        .Builder()
                        .setGalleryImportAllowed(true)
                        .setPageLimit(MAX_PAGES)
                        .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                        .setScannerMode(SCANNER_MODE_FULL)
                        .build()
                GmsDocumentScanning
                    .getClient(options)
                    .getStartScanIntent(activity)
                    .addOnSuccessListener { sender ->
                        if (continuation.isActive) continuation.resume(sender)
                    }.addOnFailureListener { error ->
                        if (continuation.isActive) continuation.resumeWithException(error)
                    }
            }

        fun resultUri(
            resultCode: Int,
            data: Intent?,
        ): String? {
            if (resultCode != Activity.RESULT_OK) return null
            val result = GmsDocumentScanningResult.fromActivityResultIntent(data) ?: return null
            return result.pdf?.uri?.toString()
                ?: result.pages
                    ?.firstOrNull()
                    ?.imageUri
                    ?.toString()
        }

        private companion object {
            const val MAX_PAGES = 8
        }
    }
