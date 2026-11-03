package de.odpisz.core.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import de.odpisz.core.model.DocumentTextReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MlKitDocumentTextReader
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : DocumentTextReader {
        override suspend fun read(uri: String): String =
            withContext(Dispatchers.IO) {
                val parsed = Uri.parse(uri)
                val mime = context.contentResolver.getType(parsed).orEmpty()
                val text =
                    if (mime == PDF || uri.endsWith(".pdf", ignoreCase = true)) {
                        readPdf(parsed)
                    } else {
                        readImage(parsed)
                    }
                if (text.isBlank()) error("Nie udało się odczytać tekstu z pliku.")
                text
            }

        private suspend fun readImage(uri: Uri): String {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            return try {
                val image = InputImage.fromFilePath(context, uri)
                recognizer.process(image).await().text
            } finally {
                recognizer.close()
            }
        }

        private suspend fun readPdf(uri: Uri): String {
            val descriptor =
                context.contentResolver.openFileDescriptor(uri, "r")
                    ?: error("Nie można otworzyć PDF.")
            descriptor.use { parcel ->
                PdfRenderer(parcel).use { renderer ->
                    val builder = StringBuilder()
                    val pageCount = minOf(renderer.pageCount, MAX_PAGES)
                    for (index in 0 until pageCount) {
                        renderer.openPage(index).use { page ->
                            val bitmap =
                                Bitmap.createBitmap(
                                    page.width * SCALE,
                                    page.height * SCALE,
                                    Bitmap.Config.ARGB_8888,
                                )
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            builder.append(recognize(bitmap))
                            builder.append('\n')
                            bitmap.recycle()
                        }
                    }
                    return builder.toString()
                }
            }
        }

        private suspend fun recognize(bitmap: Bitmap): String {
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            return try {
                val image = InputImage.fromBitmap(bitmap, 0)
                recognizer.process(image).await().text
            } finally {
                recognizer.close()
            }
        }

        private companion object {
            const val PDF = "application/pdf"
            const val MAX_PAGES = 8
            const val SCALE = 2
        }
    }
