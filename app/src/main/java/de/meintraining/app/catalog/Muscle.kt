package de.meintraining.app.catalog

import de.meintraining.app.figure.Part

enum class Muscle(
    val title: String,
    val latin: String,
    /** What the muscle does, in plain words. */
    val what: String,
    /** Where it helps in everyday life. */
    val everyday: String,
    /** Parts of the exercise figure that light up when this muscle works. */
    val parts: Set<Part>,
) {
    BRUST(
        "Brust", "Großer Brustmuskel",
        "Drückt die Arme nach vorne und führt sie vor dem Körper zusammen.",
        "Beim Abstützen, Schieben und Tragen – etwa beim Aufstehen aus dem Sessel oder beim Schieben des Einkaufswagens.",
        setOf(Part.TORSO),
    ),
    SCHULTERN(
        "Schultern", "Deltamuskel",
        "Hebt die Arme nach vorne, zur Seite und nach hinten.",
        "Für alles über Kopfhöhe: etwas ins Regal stellen, Fenster putzen, Haare kämmen.",
        setOf(Part.UPPER_ARM),
    ),
    BIZEPS(
        "Bizeps", "Zweiköpfiger Armmuskel",
        "Beugt den Arm im Ellenbogen.",
        "Einkaufstaschen tragen, Dinge hochheben, die Enkel auf den Arm nehmen.",
        setOf(Part.UPPER_ARM),
    ),
    TRIZEPS(
        "Trizeps", "Dreiköpfiger Armmuskel",
        "Streckt den Arm im Ellenbogen.",
        "Beim Abstützen und Hochdrücken – aus dem Sessel, aus der Badewanne oder im Bett.",
        setOf(Part.UPPER_ARM),
    ),
    UNTERARME(
        "Unterarme", "Unterarmmuskeln",
        "Bewegen Handgelenk und Finger und sorgen für einen festen Griff.",
        "Gläser öffnen, Taschen festhalten und sicher ans Treppengeländer greifen.",
        setOf(Part.FOREARM),
    ),
    NACKEN(
        "Nacken & oberer Rücken", "Kapuzenmuskel",
        "Hebt die Schultern und zieht die Schulterblätter zusammen.",
        "Für eine aufrechte Haltung und gegen Verspannungen im Nacken.",
        setOf(Part.TORSO),
    ),
    LAT(
        "Breiter Rückenmuskel", "Latissimus",
        "Zieht die Arme von oben nach unten und von vorne an den Körper.",
        "Beim Ziehen und Heben – eine schwere Tür aufziehen oder sich am Geländer hochziehen.",
        setOf(Part.TORSO),
    ),
    UNTERER_RUECKEN(
        "Unterer Rücken", "Rückenstrecker",
        "Richtet die Wirbelsäule auf und hält sie stabil.",
        "Beugt Rückenschmerzen vor und hilft beim Bücken und Heben.",
        setOf(Part.TORSO),
    ),
    BAUCH(
        "Bauch", "Gerader Bauchmuskel",
        "Beugt den Oberkörper nach vorne und stabilisiert den Rumpf.",
        "Stützt den Rücken, hilft beim Aufsetzen aus dem Liegen und beim Gleichgewicht.",
        setOf(Part.TORSO),
    ),
    SEITL_BAUCH(
        "Seitliche Bauchmuskeln", "Schräge Bauchmuskeln",
        "Drehen den Oberkörper und neigen ihn zur Seite.",
        "Beim Umdrehen, beim Schulterblick im Auto und für einen stabilen Gang.",
        setOf(Part.TORSO),
    ),
    GESAESS(
        "Gesäß", "Großer Gesäßmuskel",
        "Streckt die Hüfte. Er ist der kräftigste Muskel des Körpers.",
        "Aufstehen vom Stuhl, Treppensteigen und sicheres Gehen.",
        setOf(Part.PELVIS),
    ),
    ABDUKTOREN(
        "Hüfte außen", "Mittlerer Gesäßmuskel (Abduktoren)",
        "Bewegt das Bein zur Seite und hält das Becken beim Gehen gerade.",
        "Sehr wichtig für das Gleichgewicht – schützt vor Stürzen.",
        setOf(Part.PELVIS, Part.THIGH),
    ),
    ADDUKTOREN(
        "Oberschenkel innen", "Adduktoren",
        "Zieht die Beine zueinander.",
        "Für einen festen Stand und sicheres Gehen auf unebenem Boden.",
        setOf(Part.THIGH),
    ),
    QUADRIZEPS(
        "Oberschenkel vorne", "Vierköpfiger Oberschenkelmuskel",
        "Streckt das Knie.",
        "Aufstehen, Treppensteigen und Bergabgehen. Ein starker Oberschenkel schützt das Knie.",
        setOf(Part.THIGH),
    ),
    BEINBEUGER(
        "Oberschenkel hinten", "Beinbeuger",
        "Beugt das Knie und hilft, die Hüfte zu strecken.",
        "Für einen kräftigen Schritt und stabile Knie.",
        setOf(Part.THIGH),
    ),
    WADEN(
        "Waden", "Wadenmuskel",
        "Hebt die Ferse und drückt den Fuß beim Gehen vom Boden ab.",
        "Gehen, auf Zehenspitzen stehen und das Gleichgewicht halten.",
        setOf(Part.SHIN),
    ),
}
