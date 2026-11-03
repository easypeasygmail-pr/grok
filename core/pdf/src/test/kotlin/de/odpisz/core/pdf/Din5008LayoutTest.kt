package de.odpisz.core.pdf

import de.odpisz.core.model.Din5008Letter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Din5008LayoutTest {
    @Test
    fun placesAddressWindowSubjectAndBody() {
        val pages =
            Din5008Layout.layout(
                Din5008Letter(
                    senderName = "Anna Kowalska",
                    senderStreet = "Musterstraße 12",
                    senderPostcodeCity = "10115 Berlin",
                    recipientLines = listOf("Finanzamt Berlin Mitte"),
                    placeAndDate = "Berlin, den 03.11.2026",
                    aktenzeichen = "12/345/67890",
                    subject = "Bitte um Fristverlängerung",
                    body = "Sehr geehrte Damen und Herren,\n\nhiermit bitte ich um Verlängerung der Frist.",
                ),
            )
        val lines = pages.first().lines
        val sender = lines.first { it.text == "Anna Kowalska" }
        assertEquals(Din5008Layout.ADDRESS_LEFT_MM, sender.xMm)
        assertEquals(Din5008Layout.ADDRESS_TOP_MM, sender.yMm)
        assertTrue(lines.any { it.text.startsWith("Betreff:") })
        assertTrue(lines.any { it.text.contains("Verlängerung") })
        assertTrue(lines.any { it.text.contains("12/345/67890") })
        assertEquals(listOf(105f, 210f), Din5008Layout.foldMarksMm)
        lines.filter { it.xMm == Din5008Layout.LEFT_MM }.forEach { line ->
            assertTrue(line.xMm + Din5008Layout.RIGHT_MM < Din5008Layout.PAGE_WIDTH_MM)
        }
    }
}
