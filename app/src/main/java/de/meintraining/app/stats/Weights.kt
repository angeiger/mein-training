package de.meintraining.app.stats

// Pure Kotlin, so the desktop preview module can check it without a phone.

private val PAIR = Regex("""(\d+(?:[.,]\d+)?)\s*[x×*]\s*(\d+(?:[.,]\d+)?)""", RegexOption.IGNORE_CASE)
private val NUMBER = Regex("""\d+(?:[.,]\d+)?""")

// Machines often show a plate number instead of kilograms; those are not weights.
private val NOT_KILOS = Regex("stufe|platte|loch|pos", RegexOption.IGNORE_CASE)

private fun String.num() = replace(',', '.').toDoubleOrNull()

/**
 * Kilograms lifted per repetition, read from a setting she typed by hand, e.g.
 * "40 kg", "12,5", "2 x 5 kg". A "Gewicht je Hantel" counts twice, because there is
 * one dumbbell in each hand. Returns 0 when the label is not a weight, or the value
 * has no number or names a plate ("Stufe 7") rather than kilograms.
 */
fun kilograms(label: String, value: String): Double {
    if (!label.contains("gewicht", ignoreCase = true) || NOT_KILOS.containsMatchIn(value)) return 0.0
    PAIR.find(value)?.let { m ->
        val a = m.groupValues[1].num() ?: return 0.0
        val b = m.groupValues[2].num() ?: return 0.0
        return a * b
    }
    val n = NUMBER.find(value)?.value?.num() ?: return 0.0
    val perHand = label.contains("je hantel", ignoreCase = true) || value.contains("je hantel", ignoreCase = true)
    return if (perHand) n * 2 else n
}

/** "3.240 kg", "12,5 kg" */
fun formatKilograms(kg: Double): String {
    val rounded = Math.round(kg * 10) / 10.0
    return if (rounded == Math.floor(rounded)) {
        String.format(java.util.Locale.GERMANY, "%,.0f kg", rounded)
    } else {
        String.format(java.util.Locale.GERMANY, "%,.1f kg", rounded)
    }
}
