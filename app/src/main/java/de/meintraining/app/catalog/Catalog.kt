package de.meintraining.app.catalog

import de.meintraining.app.catalog.Category.AUSDAUER
import de.meintraining.app.catalog.Category.BEINE
import de.meintraining.app.catalog.Category.HANTELN
import de.meintraining.app.catalog.Category.OBERKOERPER
import de.meintraining.app.catalog.Category.OHNE
import de.meintraining.app.catalog.Category.RUMPF
import de.meintraining.app.catalog.Muscle.ABDUKTOREN
import de.meintraining.app.catalog.Muscle.ADDUKTOREN
import de.meintraining.app.catalog.Muscle.BAUCH
import de.meintraining.app.catalog.Muscle.BEINBEUGER
import de.meintraining.app.catalog.Muscle.BIZEPS
import de.meintraining.app.catalog.Muscle.BRUST
import de.meintraining.app.catalog.Muscle.GESAESS
import de.meintraining.app.catalog.Muscle.LAT
import de.meintraining.app.catalog.Muscle.NACKEN
import de.meintraining.app.catalog.Muscle.QUADRIZEPS
import de.meintraining.app.catalog.Muscle.SCHULTERN
import de.meintraining.app.catalog.Muscle.SEITL_BAUCH
import de.meintraining.app.catalog.Muscle.TRIZEPS
import de.meintraining.app.catalog.Muscle.UNTERARME
import de.meintraining.app.catalog.Muscle.UNTERER_RUECKEN
import de.meintraining.app.catalog.Muscle.WADEN

/** All built-in exercises. Ids are stored in the database: never change one. */
object Catalog {

    private const val WEIGHT = "Gewicht"
    private const val SEAT = "Sitzhöhe"

    val exercises: List<Exercise> = listOf(
        // ---- Geräte: Beine --------------------------------------------------------------
        Exercise(
            id = "beinpresse", name = "Beinpresse", aka = "Leg Press", category = BEINE,
            primary = listOf(QUADRIZEPS, GESAESS), secondary = listOf(BEINBEUGER, WADEN),
            steps = listOf(
                "Setz dich und lehn den Rücken ganz an.",
                "Stell die Füße hüftbreit mitten auf die Platte. Die Knie sind etwa im rechten Winkel gebeugt.",
                "Drück die Platte langsam weg, bis die Beine fast gestreckt sind.",
                "Lass die Platte kontrolliert zurückkommen.",
            ),
            tip = "Knie nie ganz durchdrücken. Die Knie zeigen in dieselbe Richtung wie die Fußspitzen.",
            settings = listOf("Sitzposition", "Rückenlehne", WEIGHT),
        ),
        Exercise(
            id = "beinstrecker", name = "Beinstrecker", aka = "Leg Extension", category = BEINE,
            primary = listOf(QUADRIZEPS),
            steps = listOf(
                "Setz dich so, dass die Kniegelenke genau auf Höhe der Drehachse des Geräts liegen.",
                "Das Polster liegt vorne auf dem Schienbein, kurz über dem Fuß.",
                "Halt dich an den Griffen fest und streck die Beine langsam nach vorne.",
                "Senk die Beine kontrolliert wieder ab.",
            ),
            tip = "Bewege dich ohne Schwung. Der Po bleibt fest auf dem Sitz.",
            settings = listOf("Rückenlehne", "Polster am Schienbein", WEIGHT),
        ),
        Exercise(
            id = "beinbeuger_sitzend", name = "Beinbeuger (sitzend)", aka = "Leg Curl", category = BEINE,
            primary = listOf(BEINBEUGER), secondary = listOf(WADEN),
            steps = listOf(
                "Setz dich und lehn den Rücken an. Die Knie liegen auf Höhe der Drehachse.",
                "Die Beine liegen gestreckt auf dem unteren Polster, das obere Polster hält die Oberschenkel fest.",
                "Drück die Unterschenkel langsam nach unten und hinten.",
                "Lass die Beine kontrolliert wieder nach vorne kommen.",
            ),
            tip = "Das Oberschenkelpolster soll fest sitzen, damit du nicht vom Sitz rutschst.",
            settings = listOf("Rückenlehne", "Oberschenkelpolster", "Fußpolster", WEIGHT),
        ),
        Exercise(
            id = "beinbeuger_liegend", name = "Beinbeuger (liegend)", aka = "Lying Leg Curl", category = BEINE,
            primary = listOf(BEINBEUGER), secondary = listOf(WADEN, GESAESS),
            steps = listOf(
                "Leg dich auf den Bauch. Die Knie sind knapp hinter dem Polsterende.",
                "Das Fußpolster liegt hinten auf den Unterschenkeln, kurz über den Fersen.",
                "Halt dich an den Griffen fest und zieh die Fersen langsam Richtung Po.",
                "Streck die Beine kontrolliert wieder aus.",
            ),
            tip = "Die Hüfte bleibt die ganze Zeit auf dem Polster liegen.",
            settings = listOf("Fußpolster", WEIGHT),
        ),
        Exercise(
            id = "abduktoren", name = "Abduktoren-Maschine", aka = "Beine nach außen", category = BEINE,
            primary = listOf(ABDUKTOREN), secondary = listOf(GESAESS),
            steps = listOf(
                "Setz dich aufrecht hin, Rücken an die Lehne.",
                "Die Polster liegen außen an den Knien.",
                "Drück die Beine langsam auseinander, so weit es angenehm geht.",
                "Führ die Beine kontrolliert wieder zusammen.",
            ),
            tip = "Gut für Gleichgewicht und Hüfte. Langsam bewegen und den Oberkörper ruhig halten.",
            settings = listOf("Startposition", WEIGHT),
        ),
        Exercise(
            id = "adduktoren", name = "Adduktoren-Maschine", aka = "Beine nach innen", category = BEINE,
            primary = listOf(ADDUKTOREN),
            steps = listOf(
                "Setz dich aufrecht hin, Rücken an die Lehne.",
                "Die Polster liegen innen an den Knien. Stell die Startposition so ein, dass du nur eine leichte Dehnung spürst.",
                "Drück die Beine langsam zusammen.",
                "Öffne die Beine kontrolliert wieder.",
            ),
            tip = "Die Startposition nicht zu weit öffnen – es soll nicht ziehen.",
            settings = listOf("Startposition", WEIGHT),
        ),
        Exercise(
            id = "wadenheben", name = "Wadenheben (stehend)", aka = "Calf Raise", category = BEINE,
            primary = listOf(WADEN),
            steps = listOf(
                "Stell dich mit den Fußballen auf den Tritt, die Fersen ragen über die Kante.",
                "Die Schulterpolster liegen auf den Schultern. Halt dich an den Griffen fest.",
                "Drück dich langsam auf die Zehenspitzen hoch.",
                "Senk die Fersen kontrolliert unter die Kante ab.",
            ),
            tip = "Die Knie bleiben leicht gebeugt. Ohne Gerät geht es auch an einer Treppenstufe mit Geländer.",
            settings = listOf("Schulterpolster", WEIGHT),
        ),

        // ---- Geräte: Brust, Rücken & Arme --------------------------------------------------
        Exercise(
            id = "brustpresse", name = "Brustpresse", aka = "Chest Press", category = OBERKOERPER,
            primary = listOf(BRUST), secondary = listOf(TRIZEPS, SCHULTERN),
            steps = listOf(
                "Stell den Sitz so ein, dass die Griffe auf Brusthöhe sind.",
                "Setz dich aufrecht hin, Rücken an die Lehne, Füße fest auf dem Boden.",
                "Drück die Griffe langsam nach vorne, bis die Arme fast gestreckt sind.",
                "Führ die Griffe kontrolliert zurück, bis du eine leichte Dehnung in der Brust spürst.",
            ),
            tip = "Schultern unten lassen, nicht zu den Ohren ziehen. Ellenbogen nicht ganz durchstrecken.",
            settings = listOf(SEAT, "Griffposition", WEIGHT),
        ),
        Exercise(
            id = "butterfly", name = "Butterfly", aka = "Pec Deck mit Griffen", category = OBERKOERPER,
            primary = listOf(BRUST), secondary = listOf(SCHULTERN),
            steps = listOf(
                "Stell den Sitz so ein, dass die Griffe etwa auf Brusthöhe sind. Setz dich, Rücken an die Lehne.",
                "Drück mit einem Fuß den Fußhebel nach unten. Dadurch kommen die Griffe nach vorne zu dir.",
                "Fass die Griffe und lass den Fußhebel langsam los. Die Arme gehen dabei kontrolliert nach außen.",
                "Führ die Arme mit leicht gebeugten Ellenbogen langsam vor der Brust zusammen.",
                "Öffne die Arme langsam wieder – nur so weit, wie es für die Schultern angenehm ist.",
                "Zum Schluss den Fußhebel wieder drücken, die Griffe loslassen und den Fuß langsam lösen.",
            ),
            tip = "Der Fußhebel schont die Schultern: So startest du nicht aus der gedehnten Position und musst die Griffe am Ende nicht nach hinten ziehen lassen.",
            settings = listOf(SEAT, "Startposition der Griffe", WEIGHT),
        ),
        Exercise(
            id = "butterfly_reverse", name = "Butterfly reverse", aka = "Reverse Fly", category = OBERKOERPER,
            primary = listOf(SCHULTERN, NACKEN),
            steps = listOf(
                "Setz dich mit der Brust zum Polster, die Griffe sind auf Schulterhöhe.",
                "Greif die Griffe mit fast gestreckten Armen.",
                "Führ die Arme langsam in einem Bogen nach hinten, bis sie neben dem Körper sind.",
                "Bring die Arme kontrolliert wieder nach vorne.",
            ),
            tip = "Gut für eine aufrechte Haltung. Die Schulterblätter am Ende leicht zusammenziehen.",
            settings = listOf(SEAT, "Brustpolster", WEIGHT),
        ),
        Exercise(
            id = "latzug", name = "Latzug", aka = "Lat Pulldown", category = OBERKOERPER,
            primary = listOf(LAT), secondary = listOf(BIZEPS, NACKEN),
            steps = listOf(
                "Klemm die Oberschenkel unter das Polster.",
                "Greif die Stange etwas breiter als schulterbreit.",
                "Zieh die Stange langsam vor dem Gesicht bis zur oberen Brust.",
                "Lass die Stange kontrolliert wieder nach oben.",
            ),
            tip = "Immer vor dem Kopf ziehen, nie in den Nacken. Brust raus, leicht zurücklehnen.",
            settings = listOf("Kniepolster", WEIGHT),
        ),
        Exercise(
            id = "rudern", name = "Rudern sitzend", aka = "Seated Row", category = OBERKOERPER,
            primary = listOf(LAT, NACKEN), secondary = listOf(BIZEPS, SCHULTERN),
            steps = listOf(
                "Setz dich aufrecht hin, die Brust leicht ans Polster.",
                "Greif die Griffe mit gestreckten Armen.",
                "Zieh die Griffe langsam zum Bauch, die Ellenbogen gehen nah am Körper nach hinten.",
                "Streck die Arme kontrolliert wieder nach vorne.",
            ),
            tip = "Am Ende die Schulterblätter zusammenziehen. Der Rücken bleibt gerade. Geht genauso am Kabelzug.",
            settings = listOf(SEAT, "Brustpolster", WEIGHT),
        ),
        Exercise(
            id = "schulterpresse", name = "Schulterpresse", aka = "Shoulder Press", category = OBERKOERPER,
            primary = listOf(SCHULTERN), secondary = listOf(TRIZEPS),
            steps = listOf(
                "Stell den Sitz so ein, dass die Griffe auf Schulterhöhe sind.",
                "Setz dich aufrecht hin, Rücken an die Lehne.",
                "Drück die Griffe langsam nach oben, bis die Arme fast gestreckt sind.",
                "Senk die Griffe kontrolliert wieder auf Schulterhöhe.",
            ),
            tip = "Kein Hohlkreuz – der Rücken bleibt an der Lehne. Bei Schulterbeschwerden lieber weglassen.",
            settings = listOf(SEAT, WEIGHT),
        ),
        Exercise(
            id = "bizepsmaschine", name = "Bizepsmaschine", aka = "Bizepscurl am Gerät", category = OBERKOERPER,
            primary = listOf(BIZEPS), secondary = listOf(UNTERARME),
            steps = listOf(
                "Stell den Sitz so ein, dass die Oberarme ganz auf dem Polster liegen.",
                "Die Ellenbogen sind auf Höhe der Drehachse.",
                "Beug die Arme langsam und führ den Griff Richtung Schultern.",
                "Streck die Arme kontrolliert wieder, aber nicht ganz durch.",
            ),
            tip = "Die Oberarme bleiben die ganze Zeit auf dem Polster liegen.",
            settings = listOf(SEAT, WEIGHT),
        ),

        // ---- Geräte: Bauch & Rücken -----------------------------------------------------------
        Exercise(
            id = "rueckenstrecker", name = "Rückenstrecker", aka = "Back Extension", category = RUMPF,
            primary = listOf(UNTERER_RUECKEN), secondary = listOf(GESAESS),
            steps = listOf(
                "Setz dich hin, das Polster liegt hinten am oberen Rücken.",
                "Die Füße stehen fest auf der Fußstütze, die Arme sind vor der Brust verschränkt.",
                "Richte den Oberkörper langsam gegen das Polster auf.",
                "Geh kontrolliert wieder nach vorne.",
            ),
            tip = "Nur so weit nach hinten, bis du aufrecht sitzt – nicht ins Hohlkreuz.",
            settings = listOf(SEAT, "Fußstütze", "Startposition", WEIGHT),
        ),
        Exercise(
            id = "bauchmaschine", name = "Bauchmaschine", aka = "Ab Crunch", category = RUMPF,
            primary = listOf(BAUCH),
            steps = listOf(
                "Setz dich hin und greif die Griffe neben dem Kopf.",
                "Atme ein und spann den Bauch an.",
                "Roll den Oberkörper beim Ausatmen langsam nach vorne ein.",
                "Richte dich kontrolliert wieder auf.",
            ),
            tip = "Die Kraft kommt aus dem Bauch, nicht aus den Armen. Nicht mit den Armen ziehen.",
            settings = listOf(SEAT, "Startposition", WEIGHT),
        ),
        Exercise(
            id = "rumpfrotation", name = "Rumpfrotation", aka = "Drehmaschine", category = RUMPF,
            primary = listOf(SEITL_BAUCH), secondary = listOf(BAUCH),
            amount = 10,
            steps = listOf(
                "Setz dich aufrecht hin. Die Beine sind fixiert, die Hände halten die Griffe vor der Brust.",
                "Dreh den Oberkörper langsam zur Seite.",
                "Dreh kontrolliert zurück zur Mitte.",
                "Die Wiederholungen gelten für jede Seite. Danach die Seite wechseln.",
            ),
            tip = "Nur kleine, ruhige Drehungen. Bei Rückenbeschwerden einen kleinen Drehbereich einstellen.",
            settings = listOf(SEAT, "Drehbereich", WEIGHT),
        ),
        Exercise(
            id = "hyperextension", name = "Rückenstrecken an der Bank", aka = "Hyperextension", category = RUMPF,
            primary = listOf(UNTERER_RUECKEN), secondary = listOf(GESAESS, BEINBEUGER),
            steps = listOf(
                "Stell das Hüftpolster so ein, dass die Hüfte knapp darüber frei beweglich ist.",
                "Hak die Fersen unter dem Fußpolster ein, die Arme vor der Brust verschränken.",
                "Beug den Oberkörper langsam nach vorne unten.",
                "Richte dich auf, bis der Körper eine gerade Linie bildet.",
            ),
            tip = "Nicht über die gerade Linie hinaus nach oben gehen. Ruhig und ohne Schwung.",
            settings = listOf("Höhe Hüftpolster"),
        ),

        // ---- Hanteln & Kabelzug ---------------------------------------------------------------
        Exercise(
            id = "trizeps_kabel", name = "Trizepsdrücken am Kabel", aka = "Pushdown", category = HANTELN,
            primary = listOf(TRIZEPS),
            steps = listOf(
                "Stell dich vor den Kabelzug und greif die Stange oder das Seil.",
                "Die Ellenbogen liegen seitlich am Körper an.",
                "Drück den Griff langsam nach unten, bis die Arme gestreckt sind.",
                "Lass ihn kontrolliert bis etwa auf Brusthöhe zurück.",
            ),
            tip = "Nur die Unterarme bewegen sich. Die Ellenbogen bleiben an Ort und Stelle.",
            settings = listOf("Griff (Stange/Seil)", WEIGHT),
        ),
        Exercise(
            id = "bizepscurl", name = "Bizepscurl mit Kurzhanteln", aka = "Curl", category = HANTELN,
            primary = listOf(BIZEPS), secondary = listOf(UNTERARME),
            steps = listOf(
                "Stell dich hüftbreit hin, in jeder Hand eine Hantel, die Handflächen zeigen nach vorne.",
                "Die Ellenbogen bleiben seitlich am Körper.",
                "Beug die Arme langsam und heb die Hanteln zu den Schultern.",
                "Senk die Hanteln kontrolliert wieder ab.",
            ),
            tip = "Nicht mit dem Oberkörper schwingen. Geht auch im Sitzen.",
            settings = listOf("Gewicht je Hantel"),
        ),
        Exercise(
            id = "seitheben", name = "Seitheben sitzend", aka = "Lateral Raise mit Kurzhanteln", category = HANTELN,
            primary = listOf(SCHULTERN), secondary = listOf(NACKEN),
            steps = listOf(
                "Setz dich aufrecht ans Ende einer Bank. Die Füße stehen hüftbreit fest auf dem Boden.",
                "Die Hanteln hängen neben dem Körper, die Arme sind leicht gebeugt.",
                "Heb die Arme langsam seitlich bis auf Schulterhöhe.",
                "Senk sie kontrolliert wieder ab.",
            ),
            tip = "Im Sitzen schwingt der Oberkörper nicht mit. Nicht höher als die Schultern heben – ein leichtes Gewicht reicht völlig.",
            settings = listOf("Gewicht je Hantel"),
        ),
        Exercise(
            id = "schulterdruecken_kh", name = "Schulterdrücken mit Kurzhanteln", aka = "Shoulder Press", category = HANTELN,
            primary = listOf(SCHULTERN), secondary = listOf(TRIZEPS),
            steps = listOf(
                "Setz dich auf eine Bank mit Rückenlehne.",
                "Halte die Hanteln neben dem Kopf auf Schulterhöhe.",
                "Drück die Hanteln langsam nach oben, bis die Arme fast gestreckt sind.",
                "Senk sie kontrolliert wieder auf Schulterhöhe.",
            ),
            tip = "Der Rücken bleibt an der Lehne. Bei Schulterbeschwerden lieber weglassen.",
            settings = listOf("Gewicht je Hantel", "Rückenlehne"),
        ),
        Exercise(
            id = "kh_rudern", name = "Einarmiges Rudern", aka = "Kurzhantel-Rudern", category = HANTELN,
            primary = listOf(LAT), secondary = listOf(BIZEPS, NACKEN),
            steps = listOf(
                "Stütz dich mit einem Knie und einer Hand auf der Bank ab. Der Rücken ist gerade.",
                "Die andere Hand hält die Hantel mit gestrecktem Arm.",
                "Zieh die Hantel langsam zur Hüfte, der Ellenbogen geht nah am Körper nach hinten.",
                "Senk die Hantel kontrolliert ab. Danach die Seite wechseln.",
            ),
            tip = "Den Rücken nicht verdrehen. Blick zum Boden.",
            settings = listOf(WEIGHT),
        ),
        Exercise(
            id = "bankdruecken", name = "Bankdrücken", aka = "Bench Press", category = HANTELN,
            primary = listOf(BRUST), secondary = listOf(TRIZEPS, SCHULTERN),
            steps = listOf(
                "Leg dich auf die Bank, die Augen sind unter der Stange. Die Füße stehen fest am Boden.",
                "Greif die Stange etwas breiter als schulterbreit.",
                "Senk die Stange langsam bis kurz über die Brust.",
                "Drück sie wieder nach oben, bis die Arme fast gestreckt sind.",
            ),
            tip = "Am besten mit jemandem, der aufpasst. Alternative: die Brustpresse am Gerät.",
            settings = listOf("Gewicht gesamt"),
        ),
        Exercise(
            id = "kreuzheben_kh", name = "Kreuzheben mit Kurzhanteln", aka = "Rumänisches Kreuzheben", category = HANTELN,
            primary = listOf(BEINBEUGER, GESAESS), secondary = listOf(UNTERER_RUECKEN),
            steps = listOf(
                "Stell dich hüftbreit hin, die Hanteln vor den Oberschenkeln.",
                "Die Knie sind leicht gebeugt, der Rücken ist gerade.",
                "Schieb den Po nach hinten und senk die Hanteln dicht an den Beinen entlang.",
                "Richte dich wieder auf, indem du die Hüfte nach vorne schiebst.",
            ),
            tip = "Der Rücken bleibt immer gerade – nur so weit runter, wie das gelingt.",
            settings = listOf("Gewicht je Hantel"),
        ),

        // ---- Ohne Geräte -------------------------------------------------------------------------
        Exercise(
            id = "kettlebell_march", name = "Kettlebell Marches", aka = "Marschieren mit Kettlebell", category = HANTELN,
            primary = listOf(BAUCH, SEITL_BAUCH), secondary = listOf(QUADRIZEPS, UNTERARME),
            steps = listOf(
                "Stell dich aufrecht hin und halt die Kettlebell mit beiden Händen vor der Brust.",
                "Spann den Bauch an, die Schultern bleiben locker unten.",
                "Heb abwechselnd ein Knie bis etwa auf Hüfthöhe und setz den Fuß ruhig wieder ab.",
                "Der Oberkörper bleibt aufrecht und still – nicht nach hinten lehnen.",
            ),
            tip = "Jedes Knieheben zählt als eine Wiederholung. Mit einer leichten Kettlebell beginnen und zum Festhalten neben eine Wand stellen.",
            settings = listOf("Gewicht Kettlebell"),
        ),
        Exercise(
            id = "kniebeuge", name = "Kniebeuge", aka = "Squat", category = OHNE,
            primary = listOf(QUADRIZEPS, GESAESS), secondary = listOf(BEINBEUGER, UNTERER_RUECKEN),
            steps = listOf(
                "Stell dich etwas breiter als hüftbreit hin, die Fußspitzen zeigen leicht nach außen.",
                "Streck die Arme nach vorne, um das Gleichgewicht zu halten.",
                "Schieb den Po nach hinten und unten, als würdest du dich setzen.",
                "Drück dich über die Fersen wieder hoch.",
            ),
            tip = "Die Knie zeigen in Richtung der Zehen. Ein Stuhl hinter dir gibt Sicherheit.",
        ),
        Exercise(
            id = "ausfallschritt", name = "Ausfallschritt", aka = "Lunge", category = OHNE,
            primary = listOf(QUADRIZEPS, GESAESS), secondary = listOf(BEINBEUGER, ADDUKTOREN),
            steps = listOf(
                "Mach einen großen Schritt nach vorne, die Hände liegen an der Hüfte.",
                "Senk das hintere Knie langsam Richtung Boden.",
                "Das vordere Knie bleibt über dem Fuß.",
                "Drück dich wieder hoch. Danach das Bein wechseln.",
            ),
            tip = "Zum Festhalten neben eine Wand oder Stange stellen.",
        ),
        Exercise(
            id = "aufstehen", name = "Aufstehen vom Stuhl", aka = "Sit to Stand", category = OHNE,
            primary = listOf(QUADRIZEPS, GESAESS),
            steps = listOf(
                "Setz dich vorne auf einen festen Stuhl, die Füße hüftbreit auf dem Boden.",
                "Verschränk die Arme vor der Brust.",
                "Lehn dich nach vorne und steh ohne Hilfe der Arme auf.",
                "Setz dich langsam und kontrolliert wieder hin.",
            ),
            tip = "Eine der wichtigsten Übungen für den Alltag. Wenn nötig, am Anfang mit den Händen helfen.",
        ),
        Exercise(
            id = "wand_liegestuetz", name = "Liegestütz an der Wand", aka = "Wall Push-up", category = OHNE,
            primary = listOf(BRUST), secondary = listOf(TRIZEPS, SCHULTERN),
            steps = listOf(
                "Stell dich eine gute Armlänge vor eine Wand.",
                "Leg die Hände auf Schulterhöhe etwas breiter als schulterbreit an die Wand.",
                "Beug die Arme und bring die Brust langsam zur Wand.",
                "Drück dich wieder weg.",
            ),
            tip = "Der Körper bleibt gerade wie ein Brett. Je weiter weg die Füße, desto schwerer.",
        ),
        Exercise(
            id = "crunch", name = "Bauchpresse (Crunch)", aka = "Crunch", category = OHNE,
            primary = listOf(BAUCH),
            steps = listOf(
                "Leg dich auf den Rücken, die Beine sind angestellt.",
                "Die Hände liegen locker neben dem Kopf.",
                "Heb Kopf und Schultern langsam ein Stück vom Boden.",
                "Leg dich kontrolliert wieder ab.",
            ),
            tip = "Nicht am Kopf ziehen. Zwischen Kinn und Brust passt eine Faust.",
        ),
        Exercise(
            id = "plank", name = "Unterarmstütz", aka = "Plank", category = OHNE, kind = Kind.HOLD,
            primary = listOf(BAUCH), secondary = listOf(UNTERER_RUECKEN, SCHULTERN, GESAESS),
            steps = listOf(
                "Stütz dich auf die Unterarme, die Ellenbogen sind unter den Schultern.",
                "Streck die Beine nach hinten und stell dich auf die Zehen.",
                "Halte den Körper gerade wie ein Brett.",
                "Ruhig weiteratmen und die Zeit halten.",
            ),
            tip = "Leichter geht es mit den Knien auf dem Boden. Kein Hohlkreuz.",
        ),
        Exercise(
            id = "beckenheben", name = "Beckenheben", aka = "Glute Bridge", category = OHNE,
            primary = listOf(GESAESS), secondary = listOf(BEINBEUGER, UNTERER_RUECKEN),
            steps = listOf(
                "Leg dich auf den Rücken, die Beine sind angestellt.",
                "Die Arme liegen neben dem Körper.",
                "Heb das Becken langsam, bis Oberschenkel und Oberkörper eine Linie bilden.",
                "Senk das Becken kontrolliert wieder ab.",
            ),
            tip = "Oben kurz den Po anspannen. Nicht ins Hohlkreuz gehen.",
        ),
        Exercise(
            id = "vierfuessler", name = "Vierfüßlerstand", aka = "Bird Dog", category = OHNE,
            primary = listOf(UNTERER_RUECKEN, GESAESS), secondary = listOf(BAUCH, SCHULTERN),
            steps = listOf(
                "Geh in den Vierfüßlerstand: Hände unter den Schultern, Knie unter der Hüfte.",
                "Streck langsam einen Arm nach vorne und das gegenüberliegende Bein nach hinten.",
                "Kurz halten, dann zurück.",
                "Danach die Seiten wechseln.",
            ),
            tip = "Arm und Bein nur bis zur Waagerechten heben. Für die Knie eine weiche Matte nehmen.",
        ),

        // ---- Ausdauer -------------------------------------------------------------------------------
        Exercise(
            id = "fahrrad", name = "Fahrrad-Ergometer", aka = "Heimtrainer", category = AUSDAUER, kind = Kind.CARDIO,
            primary = listOf(QUADRIZEPS), secondary = listOf(BEINBEUGER, WADEN, GESAESS),
            steps = listOf(
                "Stell den Sattel so ein, dass das Bein unten fast gestreckt ist.",
                "Wähl eine leichte Stufe zum Aufwärmen.",
                "Tritt gleichmäßig. Du solltest dich dabei noch unterhalten können.",
            ),
            tip = "Ideal zum Aufwärmen vor dem Krafttraining.",
            settings = listOf("Sattelhöhe", "Lenker", "Stufe / Widerstand"),
        ),
        Exercise(
            id = "laufband", name = "Laufband", aka = "Gehen", category = AUSDAUER, kind = Kind.CARDIO,
            primary = listOf(QUADRIZEPS, WADEN), secondary = listOf(GESAESS, BEINBEUGER),
            steps = listOf(
                "Stell dich auf die seitlichen Trittflächen und starte das Band langsam.",
                "Steig auf das Band und halt dich anfangs an den Griffen fest.",
                "Geh in einem angenehmen Tempo. Mit etwas Steigung wird es anstrengender.",
            ),
            tip = "Die Sicherheitsleine am Kleidungsstück befestigen – sie stoppt das Band, wenn du stolperst.",
            settings = listOf("Geschwindigkeit", "Steigung"),
        ),
        Exercise(
            id = "stairmaster", name = "Stairmaster", aka = "Treppensteiger", category = AUSDAUER, kind = Kind.CARDIO,
            primary = listOf(QUADRIZEPS, GESAESS), secondary = listOf(WADEN, BEINBEUGER),
            steps = listOf(
                "Steig auf die unterste Stufe und halt dich locker an den Seitengriffen fest.",
                "Starte mit einer niedrigen Stufe – die Treppe läuft dann langsam.",
                "Steig gleichmäßig von Stufe zu Stufe, der ganze Fuß steht auf der Stufe.",
                "Bleib aufrecht und stütz dich nicht mit dem Gewicht auf die Griffe.",
            ),
            tip = "Gut zum Aufwärmen: 5 bis 10 Minuten in ruhigem Tempo. Zum Anhalten die Stopp-Taste drücken und erst dann absteigen.",
            settings = listOf("Stufe / Geschwindigkeit", "Programm"),
        ),
        Exercise(
            id = "crosstrainer", name = "Crosstrainer", aka = "Ellipsentrainer", category = AUSDAUER, kind = Kind.CARDIO,
            primary = listOf(QUADRIZEPS, GESAESS), secondary = listOf(SCHULTERN, LAT),
            steps = listOf(
                "Steig auf die Pedale und greif die beweglichen Griffe.",
                "Beweg Arme und Beine gleichmäßig.",
                "Bleib aufrecht und schau nach vorne.",
            ),
            tip = "Schont die Gelenke, weil es keine Stöße gibt.",
            settings = listOf("Stufe / Widerstand", "Programm"),
        ),
        Exercise(
            id = "rudergeraet", name = "Rudergerät", aka = "Ergometer", category = AUSDAUER, kind = Kind.CARDIO,
            primary = listOf(QUADRIZEPS, LAT), secondary = listOf(GESAESS, BIZEPS, UNTERER_RUECKEN),
            steps = listOf(
                "Schnall die Füße fest und greif den Griff.",
                "Drück dich zuerst mit den Beinen weg, dann lehn dich leicht zurück, zuletzt ziehen die Arme.",
                "Zurück in umgekehrter Reihenfolge: Arme, Oberkörper, Beine.",
            ),
            tip = "Der Rücken bleibt gerade. Ruhiges Tempo reicht.",
            settings = listOf("Stufe / Widerstand", "Fußschlaufen"),
        ),
    )

    private val byId = exercises.associateBy { it.id }

    fun get(id: String): Exercise? = byId[id]

    /** Exercises training [m]: those that train it most first. */
    fun forMuscle(m: Muscle, all: List<Exercise> = exercises): List<Exercise> =
        all.filter { m in it.primary } + all.filter { m in it.secondary && m !in it.primary }
}
