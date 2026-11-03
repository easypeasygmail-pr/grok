package de.odpisz.core.privacy

import de.odpisz.core.model.PiiKind
import de.odpisz.core.model.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PiiMaskerTest {
    private val masker = PiiMasker()
    private val profile =
        UserProfile(
            fullName = "Anna Kowalska",
            street = "Musterstraße 12",
            postcode = "10115",
            city = "Berlin",
            email = "anna@example.com",
            phone = "015112345678",
        )

    @Test
    fun masksIbanSteuerIdPhoneEmailAndPostcode() {
        val text =
            """
            IBAN DE89 3704 0044 0532 0130 00
            Steuer-ID 86095742719
            Tel +49 151 23456789
            Mail anna.kowalska@example.com
            PLZ 10115
            Az. 12/345/67890
            """.trimIndent()

        val result = masker.mask(text)

        assertTrue(result.tokens.any { it.kind == PiiKind.Iban })
        assertTrue(result.tokens.any { it.kind == PiiKind.SteuerId && it.original == "86095742719" })
        assertTrue(result.tokens.any { it.kind == PiiKind.Phone })
        assertTrue(result.tokens.any { it.kind == PiiKind.Email })
        assertTrue(result.tokens.any { it.kind == PiiKind.Postcode && it.original == "10115" })
        assertFalse(result.masked.contains("DE89"))
        assertFalse(result.masked.contains("86095742719"))
        assertFalse(result.masked.contains("example.com"))
        assertTrue(result.masked.contains("67890"))
        assertTrue(result.masked.contains("[IBAN_1]"))
        assertTrue(result.masked.contains("[STEUER_ID_1]"))
    }

    @Test
    fun masksProfileNameAndAddressAndRestores() {
        val text = "Anna Kowalska, Musterstraße 12, 10115 Berlin, dziękuje."
        val result = masker.mask(text, profile)

        assertTrue(result.tokens.any { it.kind == PiiKind.UserName })
        assertTrue(result.tokens.any { it.kind == PiiKind.UserAddress })
        assertFalse(result.masked.contains("Kowalska"))
        assertFalse(result.masked.contains("Musterstraße"))
        assertEquals(text, masker.restore(result.masked, result.tokens))
    }

    @Test
    fun rejectsRandomElevenDigitsThatAreNotASteuerId() {
        val result = masker.mask("Numer 12345678901 nie jest identyfikatorem.")
        assertTrue(result.tokens.none { it.kind == PiiKind.SteuerId })
        assertTrue(result.masked.contains("12345678901"))
    }

    @Test
    fun reusesTokenForTheSameValue() {
        val result = masker.mask("anna@example.com oraz anna@example.com")
        assertEquals(1, result.tokens.count { it.kind == PiiKind.Email })
        assertFalse(result.masked.contains("[EMAIL_2]"))
    }
}
