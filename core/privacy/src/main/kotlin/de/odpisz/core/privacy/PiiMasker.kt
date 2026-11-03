package de.odpisz.core.privacy

import de.odpisz.core.model.PiiKind
import de.odpisz.core.model.PiiToken
import de.odpisz.core.model.UserProfile

data class MaskResult(
    val masked: String,
    val tokens: List<PiiToken>,
)

/**
 * Best-effort, on-device pseudonymisation. It replaces matches with stable
 * tokens such as `[IBAN_1]` and can restore them locally. It is not a
 * guarantee that every personal detail is gone.
 */
class PiiMasker {
    private val email = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val iban = Regex("""\b[A-Z]{2}[0-9]{2}(?:[ ]?[A-Z0-9]{4}){2,7}(?:[ ]?[A-Z0-9]{1,4})?\b""")
    private val phone = Regex("""(?<![0-9])(?:\+49|0049|0)(?:[ \t\-/]*[0-9]){7,14}(?![0-9])""")
    private val elevenDigits = Regex("""(?<![0-9])[0-9]{11}(?![0-9])""")
    private val postcode = Regex("""(?<![0-9/])\b[0-9]{5}\b(?![0-9])""")

    fun mask(
        text: String,
        profile: UserProfile = UserProfile(),
    ): MaskResult {
        val tokens = mutableListOf<PiiToken>()
        var current = text
        current = replaceMatches(current, email, PiiKind.Email, tokens)
        current = replaceMatches(current, iban, PiiKind.Iban, tokens) { IbanCheck.isValid(it) }
        current = replaceMatches(current, elevenDigits, PiiKind.SteuerId, tokens) { SteuerId.isValid(it) }
        current = replaceMatches(current, phone, PiiKind.Phone, tokens)
        current = maskProfile(current, profile, tokens)
        current = replaceMatches(current, postcode, PiiKind.Postcode, tokens)
        return MaskResult(current, tokens.toList())
    }

    fun restore(
        masked: String,
        tokens: List<PiiToken>,
    ): String {
        var current = masked
        val ordered = tokens.sortedByDescending { it.token.length }
        for (token in ordered) {
            current = current.replace(token.token, token.original)
        }
        return current
    }

    private fun maskProfile(
        text: String,
        profile: UserProfile,
        tokens: MutableList<PiiToken>,
    ): String {
        var current = text
        val fullName = profile.fullName.trim()
        if (fullName.length >= MIN_PHRASE) {
            current = replaceLiteral(current, fullName, PiiKind.UserName, tokens)
        }
        fullName
            .split(Regex("""\s+"""))
            .filter { it.length >= MIN_PHRASE }
            .forEach { part ->
                current = replaceLiteral(current, part, PiiKind.UserName, tokens)
            }
        val street = profile.street.trim()
        if (street.length >= MIN_PHRASE) {
            current = replaceLiteral(current, street, PiiKind.UserAddress, tokens)
        }
        val postcode = profile.postcode.trim()
        val city = profile.city.trim()
        if (postcode.isNotEmpty() && city.length >= MIN_PHRASE) {
            current = replaceLiteral(current, "$postcode $city", PiiKind.UserAddress, tokens)
        }
        return current
    }

    private fun replaceLiteral(
        input: String,
        phrase: String,
        kind: PiiKind,
        tokens: MutableList<PiiToken>,
    ): String {
        val pattern =
            Regex(
                "(?<![\\p{L}\\p{N}])" + Regex.escape(phrase) + "(?![\\p{L}\\p{N}])",
                RegexOption.IGNORE_CASE,
            )
        return replaceMatches(input, pattern, kind, tokens)
    }

    private fun replaceMatches(
        input: String,
        regex: Regex,
        kind: PiiKind,
        tokens: MutableList<PiiToken>,
        accept: (String) -> Boolean = { true },
    ): String {
        val matches = regex.findAll(input).filter { accept(it.value) }.toList()
        if (matches.isEmpty()) return input
        val buffer = StringBuilder(input)
        for (match in matches.asReversed()) {
            val token = tokenFor(kind, match.value, tokens)
            buffer.replace(match.range.first, match.range.last + 1, token)
        }
        return buffer.toString()
    }

    private fun tokenFor(
        kind: PiiKind,
        original: String,
        tokens: MutableList<PiiToken>,
    ): String {
        val existing = tokens.find { it.kind == kind && it.original == original }
        if (existing != null) return existing.token
        val index = tokens.count { it.kind == kind } + 1
        val token = "[${label(kind)}_$index]"
        tokens += PiiToken(token = token, kind = kind, original = original)
        return token
    }

    private fun label(kind: PiiKind): String =
        when (kind) {
            PiiKind.Iban -> "IBAN"
            PiiKind.SteuerId -> "STEUER_ID"
            PiiKind.Phone -> "PHONE"
            PiiKind.Email -> "EMAIL"
            PiiKind.Postcode -> "POSTCODE"
            PiiKind.UserName -> "USER_NAME"
            PiiKind.UserAddress -> "USER_ADDRESS"
        }

    private companion object {
        const val MIN_PHRASE = 3
    }
}
