package de.odpisz.core.privacy

import java.math.BigInteger

object IbanCheck {
    private const val MIN_LENGTH = 15
    private const val MAX_LENGTH = 34
    private const val MOD = 97
    private val shape = Regex("""[A-Z]{2}[0-9]{2}[A-Z0-9]+""")

    fun isValid(raw: String): Boolean {
        val compact = raw.replace(" ", "").uppercase()
        if (compact.length !in MIN_LENGTH..MAX_LENGTH) return false
        if (!shape.matches(compact)) return false
        val rearranged = compact.drop(4) + compact.take(4)
        val numeric =
            buildString {
                for (ch in rearranged) {
                    if (ch.isDigit()) {
                        append(ch)
                    } else {
                        append(ch.code - 'A'.code + 10)
                    }
                }
            }
        return BigInteger(numeric).mod(BigInteger.valueOf(MOD.toLong())) == BigInteger.ONE
    }
}
