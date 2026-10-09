# Mein Training

An Android app, in German, that walks someone over 60 through a gym workout:

- **Training**: several training plans; one big "Training starten" button per plan. A
  workout shows one exercise at a time, with the saved machine settings and photo, big
  circles to tick off each set, and a rest timer that vibrates and beeps when it ends.
  The screen stays on during a workout.
- **Übungen**: 37 common exercises (machines, cable, dumbbells, mat, cardio). Each has a
  looping animated figure with the working muscles in orange, a breathing cue, 3–4
  steps, a safety note, and a map of the muscles it trains. Her own exercises can be
  added for machines that are not in the list.
- **Muskeln**: front and back of a body; tap a muscle (or its name) to see what it does,
  where it helps in everyday life, and which exercises train it.
- **Machine settings**: per machine, free-text values (seat height, weight, …), a note,
  and a photo taken with the phone camera. They are shared by every plan.

Everything is stored on the phone. The app needs no internet and no account, and has
no ads. It requires **Android 8.0 or newer**.

---

## Install it on her phone

The finished app is `dist/mein-training.apk` (1.6 MB). It is not in the Play Store, so
it is installed by hand once. Allow about 10 minutes.

### 1. Copy the file to her phone

Pick whichever is easiest:

- **USB cable**: connect her phone to this PC. On the phone, pull down the notification
  shade, tap the USB notification ("Gerät wird über USB aufgeladen" or similar) and
  choose **Dateiübertragung**. In Windows Explorer, open the phone and copy
  `mein-training.apk` into its **Download** folder.
- **Google Drive / email to herself**: upload the APK, then open it on her phone and
  download it. WhatsApp also works if you send it as a *document*, not as a photo.

### 2. Install

Her phone's wording differs slightly between brands; look for these phrases.

1. On her phone, open the **Dateien** app (on Samsung: **Eigene Dateien**) and go to
   **Downloads**. Tap **mein-training.apk**.
2. Android warns: *"Aus Sicherheitsgründen ist es nicht erlaubt, auf dem Smartphone
   unbekannte Apps aus dieser Quelle zu installieren."* Tap **Einstellungen**.
3. Turn on **Dieser Quelle vertrauen** (also written *Aus dieser Quelle zulassen*), then
   go back with the back arrow.
4. Tap **Installieren**.
5. Google Play Protect may say the app is unknown or *"Unsichere App blockiert"*. This is
   expected for any app not from the Play Store. Tap **Weitere Details**, then
   **Trotzdem installieren**. If it offers to *send the app for scanning*, either choice
   is fine.
6. Tap **Öffnen**. She should see *"Guten Tag!"* and an example plan called
   **Ganzkörper (Beispiel)**.

**Verify:** start the example plan, tick "Satz 1" on the leg press, and check that a
1-minute "Pause" countdown appears at the bottom. Then tap **Beenden**.

### 3. First visit to the gym (worth doing together)

1. Rename or delete the example plan (**Plan bearbeiten**), or create her own plans
   with **Neuen Plan anlegen**, e.g. "Tag A" and "Tag B".
2. Add exercises with **Übung hinzufügen**: several can be ticked at once.
3. At each machine, tap **Geräte-Einstellungen eintragen**, take a photo where the
   seat/pin settings are visible, and type in the values. Only the first time; after
   that they appear automatically in every workout.
4. Sets, repetitions and rest time are changed per exercise with **Ändern** in the plan.
   The defaults are 2 sets × 12 repetitions with 60 seconds rest.

---

## Updating the app later

> **Important:** updates only install over the old version if they are signed with the
> same key. This app is signed with this PC's Android debug key,
> `C:\Users\andre\.android\debug.keystore`. **Back that file up.** If it is lost (new
> PC, reinstalled Windows), the next version can only be installed after uninstalling
> the old one, and uninstalling deletes all her plans, settings and photos.

1. Change what you need, raise `versionCode` (and `versionName`) in
   `app/build.gradle.kts`.
2. Build: `gradlew.bat assembleRelease` (or Android Studio: **Build → Build App
   Bundle(s) / APK(s) → Build APK(s)** with the *release* variant selected).
3. Copy `app/build/outputs/apk/release/app-release.apk` to her phone and open it as in
   step 2 above. It shows **Aktualisieren** instead of Installieren; her data stays.

Never change `applicationId` (`de.meintraining.app`) or the database file name
(`training.db`): either would install as a second, empty app.

## Building

- Gradle 8.9 (wrapper included) needs **JDK 21**, not newer. From a terminal:
  `set JAVA_HOME=C:\Users\andre\.jdks\jbr-21.0.11` then `gradlew.bat assembleRelease`.
  In Android Studio: **Settings → Build, Execution, Deployment → Build Tools → Gradle →
  Gradle JDK = 21**.
- `gradlew.bat :preview:run` renders every exercise animation (start, middle, end) to
  `build/previews/figures-*.png`; `gradlew.bat :preview:run --args="muscles"` renders
  the muscle map. Pass exercise ids to render only those. This lets you check drawings
  without a phone.

## Troubleshooting

| What you see | Cause | Fix |
|---|---|---|
| *"App nicht installiert"* / *"Paket steht in Konflikt"* when updating | New APK signed with a different key | Build on the PC with the original `debug.keystore`. Otherwise uninstall first (loses her data). |
| *"Unbekannte Apps … nicht erlaubt"* and no Einstellungen button | Some phones hide it | **Einstellungen → Apps → Dateien (or the browser) → Unbekannte Apps installieren → Zulassen** |
| Play Protect blocks without "Trotzdem installieren" | Older Play Protect screen | Tap **OK**, then in Play Store → profile → **Play Protect** → gear icon → temporarily turn off *Apps mit Play Protect scannen*, install, turn it back on |
| Gradle: *"Unsupported class file major version"* | Gradle running on a JDK newer than 21 | Set the Gradle JDK to 21 (see Building) |
| *"Auf diesem Handy wurde keine Kamera-App gefunden"* | No camera app installed or it is disabled | Use **Bild aus der Galerie wählen** instead |
| No beep at the end of the rest | Phone on silent | Vibration still works; the beep follows the notification volume |

## How it is built

Kotlin, Jetpack Compose, Room. No network permission.

- `figure/`: a small 3D stick-figure engine (inverse kinematics for arms and legs,
  orthographic camera, depth-sorted drawing) and `Motions.kt`, the movement and
  equipment of every exercise. Pure Kotlin, also used by the `preview` module.
- `catalog/`: exercises (`Catalog.kt`) and muscle groups (`Muscle.kt`). Exercise ids are
  stored in the database; never rename one.
- `musclemap/`: front/back body outline and muscle shapes, with hit-testing.
- `data/`: Room database, repository, and the photo store (photos are shrunk to
  1600 px and kept in app-private storage).
- `ui/`: screens. Workout state survives the app being closed in the background (e.g.
  while the camera is open); running timers do not.

To add an exercise: add an `Exercise` to `Catalog.kt`, and a `Motion` with the same id to
`Motions.kt` (without one, the exercise shows a placeholder instead of an animation).
