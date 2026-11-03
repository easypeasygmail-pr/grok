package de.odpisz.core.pdf

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import dagger.hilt.android.qualifiers.ApplicationContext
import de.odpisz.core.model.Din5008Letter
import de.odpisz.core.model.LetterPdfWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidLetterPdfWriter
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : LetterPdfWriter {
        override suspend fun write(letter: Din5008Letter): String =
            withContext(Dispatchers.IO) {
                val pages = Din5008Layout.layout(letter)
                val document = PdfDocument()
                pages.forEachIndexed { index, page ->
                    val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH_PT, PAGE_HEIGHT_PT, index + 1).create()
                    val pdfPage = document.startPage(info)
                    val canvas = pdfPage.canvas
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
                    page.lines.forEach { line ->
                        paint.textSize = line.sizePt
                        canvas.drawText(
                            line.text,
                            Din5008Layout.mmToPt(line.xMm),
                            Din5008Layout.mmToPt(line.yMm),
                            paint,
                        )
                    }
                    paint.strokeWidth = 1f
                    Din5008Layout.foldMarksMm.forEach { mark ->
                        val y = Din5008Layout.mmToPt(mark)
                        canvas.drawLine(0f, y, Din5008Layout.mmToPt(FOLD_MARK_MM), y, paint)
                    }
                    document.finishPage(pdfPage)
                }
                val directory = File(context.filesDir, "letters").apply { mkdirs() }
                val file = File(directory, "odpisz-${System.currentTimeMillis()}.pdf")
                FileOutputStream(file).use { document.writeTo(it) }
                document.close()
                file.absolutePath
            }

        private companion object {
            const val PAGE_WIDTH_PT = 595
            const val PAGE_HEIGHT_PT = 842
            const val FOLD_MARK_MM = 5f
        }
    }
