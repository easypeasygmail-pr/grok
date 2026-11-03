package de.odpisz.core.privacy

/**
 * Best-effort check for a German Steuerliche Identifikationsnummer:
 * 11 digits, no leading zero, the duplication rule on the first 10 digits,
 * and the official check digit.
 */
object SteuerId {
    private const val LENGTH = 11
    private const val HEAD_LENGTH = 10
    private const val PRODUCT_START = 10
    private const val CHECK_MOD = 11
    private const val DIGIT_BASE = 10

    fun isValid(value: String): Boolean {
        if (value.length != LENGTH || value.any { !it.isDigit() }) return false
        if (value[0] == '0') return false
        val head = value.substring(0, HEAD_LENGTH)
        if (!repetitionOk(head)) return false
        return checkDigit(head) == value[HEAD_LENGTH].digitToInt()
    }

    fun checkDigit(head: String): Int {
        var product = PRODUCT_START
        for (ch in head) {
            var sum = (ch.digitToInt() + product) % DIGIT_BASE
            if (sum == 0) sum = DIGIT_BASE
            product = (sum * 2) % CHECK_MOD
        }
        val check = CHECK_MOD - product
        return if (check == DIGIT_BASE) 0 else check
    }

    private fun repetitionOk(head: String): Boolean {
        val counts = head.groupingBy { it }.eachCount()
        if (counts.values.any { it > 3 }) return false
        val repeated = counts.values.filter { it >= 2 }
        return repeated.size == 1 && (repeated[0] == 2 || repeated[0] == 3)
    }
}
