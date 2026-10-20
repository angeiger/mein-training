package de.meintraining.app.catalog

import de.meintraining.app.figure.Motion
import de.meintraining.app.figure.Motions
import de.meintraining.app.figure.Part

enum class Category(val title: String) {
    BEINE("Geräte – Beine"),
    OBERKOERPER("Geräte – Brust, Rücken & Arme"),
    RUMPF("Geräte – Bauch & Rücken"),
    HANTELN("Hanteln & Kabelzug"),
    OHNE("Ohne Geräte"),
    AUSDAUER("Ausdauer"),
    EIGENE("Eigene Übungen"),
}

/** How an exercise is counted. */
enum class Kind {
    /** Sets of repetitions. */
    REPS,

    /** Sets of holding a position for some seconds. */
    HOLD,

    /** Minutes on a cardio machine. */
    CARDIO;

    /** "12 Wiederholungen", "20 Sekunden", "10 Minuten". */
    fun amount(n: Int) = when (this) {
        REPS -> if (n == 1) "1 Wiederholung" else "$n Wiederholungen"
        HOLD -> "$n Sekunden"
        CARDIO -> if (n == 1) "1 Minute" else "$n Minuten"
    }

    fun amountShort(n: Int) = when (this) {
        REPS -> "$n Wdh."
        HOLD -> "$n Sek."
        CARDIO -> "$n Min."
    }
}

data class Exercise(
    val id: String,
    val name: String,
    val category: Category,
    val kind: Kind = Kind.REPS,
    val primary: List<Muscle>,
    val secondary: List<Muscle> = emptyList(),
    val steps: List<String> = emptyList(),
    val tip: String? = null,
    /** Machine settings worth writing down, offered as empty fields. */
    val settings: List<String> = emptyList(),
    val sets: Int = if (kind == Kind.CARDIO) 1 else 3,
    val amount: Int = when (kind) {
        Kind.REPS -> 15
        Kind.HOLD -> 20
        Kind.CARDIO -> 10
    },
    /** Other names she might know it by, shown small under the title. */
    val aka: String? = null,
    val custom: Boolean = false,
) {
    val motion: Motion? get() = Motions.all[id]

    /** Figure parts drawn in the accent colour. */
    val highlight: Set<Part> get() = primary.flatMap { it.parts }.toSet()

    val muscleSummary: String get() = primary.joinToString(", ") { it.title }
}
