package de.odpisz.core.pdf

import de.odpisz.core.model.Din5008Letter

/**
 * DIN 5008 Form B, approximate millimetre positions.
 * Address window starts 20 mm from the left and 45 mm from the top.
 * Body text uses a 25 mm left margin and a 20 mm right margin.
 */
object Din5008Layout {
    const val PAGE_WIDTH_MM = 210f
    const val PAGE_HEIGHT_MM = 297f
    const val LEFT_MM = 25f
    const val RIGHT_MM = 20f
    const val ADDRESS_LEFT_MM = 20f
    const val ADDRESS_TOP_MM = 45f
    private const val BODY_TOP_MM = 125f
    private const val BOTTOM_MM = 270f
    private const val LINE_MM = 6f
    private const val BODY_CHARS = 78
    private const val PT_PER_MM = 72f / 25.4f

    val foldMarksMm: List<Float> = listOf(105f, 210f)

    data class TextLine(
        val text: String,
        val xMm: Float,
        val yMm: Float,
        val sizePt: Float,
    )

    data class Page(
        val lines: List<TextLine>,
    )

    fun layout(letter: Din5008Letter): List<Page> {
        val pages = mutableListOf<MutableList<TextLine>>()
        var lines = mutableListOf<TextLine>()
        pages += lines
        lines += TextLine(letter.senderName.ifBlank { "Absender" }, ADDRESS_LEFT_MM, ADDRESS_TOP_MM, 8f)
        var addressY = ADDRESS_TOP_MM + 8f
        if (letter.senderStreet.isNotBlank()) {
            lines += TextLine(letter.senderStreet, ADDRESS_LEFT_MM, addressY, 11f)
            addressY += LINE_MM
        }
        if (letter.senderPostcodeCity.isNotBlank()) {
            lines += TextLine(letter.senderPostcodeCity, ADDRESS_LEFT_MM, addressY, 11f)
        }
        letter.recipientLines.take(RECIPIENT_LINES).forEachIndexed { index, line ->
            lines += TextLine(line, ADDRESS_LEFT_MM, ADDRESS_TOP_MM + 16f + index * LINE_MM, 11f)
        }
        lines += TextLine(letter.placeAndDate, 120f, 98f, 11f)
        letter.aktenzeichen?.let { fileNumber ->
            lines += TextLine("Ihr Zeichen: $fileNumber", LEFT_MM, 108f, 11f)
        }
        lines += TextLine("Betreff: ${letter.subject}", LEFT_MM, 116f, 12f)

        var y = BODY_TOP_MM
        wrap(letter.body).forEach { row ->
            if (y > BOTTOM_MM) {
                lines = mutableListOf()
                pages += lines
                y = ADDRESS_TOP_MM
            }
            lines += TextLine(row, LEFT_MM, y, 11f)
            y += LINE_MM
        }
        return pages.map { Page(it) }
    }

    fun mmToPt(mm: Float): Float = mm * PT_PER_MM

    private fun wrap(text: String): List<String> =
        text
            .lineSequence()
            .flatMap { line ->
                if (line.length <= BODY_CHARS) listOf(line) else line.chunked(BODY_CHARS)
            }.toList()

    private const val RECIPIENT_LINES = 6
}
