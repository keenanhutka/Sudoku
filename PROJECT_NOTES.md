# App summary
Large-print, ad-light Sudoku for adults 55+. Android only. The working name is Senior Sudoku. Free with one interstitial ad after a finished puzzle, plus a CAD 5.99 one-time unlock (Remove ads + unlimited hints).

# Fixed facts
Kotlin, Jetpack Compose (Material 3), minSdk 24, targetSdk 36, DataStore, no server, offline.
Ads: AdMob GMA Next-Gen SDK + UMP. Purchases: Play Billing Library 8.0.0+ (product ID: remove_ads_unlimited_hints).
Neither ads nor billing are in the project yet.

# Package name (permanent)
com.keenanhutka.seniorsudoku

This can still change before the first Google Play upload. After that upload it cannot change.

# Files and what each one does
- settings.gradle.kts: names the project and where Gradle finds libraries.
- build.gradle.kts: lists the Android and Compose plugins for the whole project.
- gradle/libs.versions.toml: the one list of library versions.
- app/build.gradle.kts: phone settings (SDK levels) and the libraries the app uses.
- app/src/main/AndroidManifest.xml: the app name and the screen that opens first.
- app/src/main/java/com/keenanhutka/seniorsudoku/MainActivity.kt: the Android entry screen.
- ui/HomeScreen.kt: the words you see when the app opens.
- ui/Theme.kt: the paper colors.
- engine/Board.kt: the 9 by 9 grid, with no Android code.
- engine/Solver.kt: finds whether a puzzle has one solution, and stops early.
- game/GamePackage.kt: placeholder for the game state.
- data/DataPackage.kt: placeholder for saving on the phone.
- ads/AdsPackage.kt: placeholder for the one ad.
- billing/BillingPackage.kt: placeholder for the unlock.
- app/src/test/.../BoardTest.kt: checks the solver.

# Decisions
- Working name is Senior Sudoku. The package is com.keenanhutka.seniorsudoku until Keenan says otherwise.
- compileSdk is 37, not 36. Google's current Compose BOM 2026.09.00 (Compose 1.12) requires compileSdk 37 and Android Gradle Plugin 9. targetSdk stays 36, which is the Play requirement. Source: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- Android Gradle Plugin 9.5.0, Gradle 9.6.0, Kotlin 2.4.10. AGP 9 already includes Kotlin, so the separate Kotlin Android plugin is not applied. Source: https://kotlinlang.org/docs/compose-compiler-migration-guide.html
- Libraries: Compose BOM 2026.09.00, Material 3 (from that BOM), activity-compose 1.13.0, lifecycle ViewModel Compose 2.11.0, lifecycle runtime Compose 2.11.0, core-ktx 1.19.1, DataStore Preferences 1.3.0, coroutines 1.11.0, JUnit 4.13.2.
- No ads library and no billing library yet.

# Steps finished
- Step 1, Nov 16 2026: the app project exists and opens on a title screen that says Senior Sudoku. Not yet run on a phone from this machine.
- Step 2, Nov 16 2026: SDK levels, libraries, six packages, and Git are set up. Ads and billing are not added.
- Step 3, Nov 16 2026: the board and the solution counter. All 7 unit tests passed, and a debug app file builds.

# What is broken right now
Nothing known in the code. Unit tests passed. It still needs a first run in Android Studio on an emulator or a phone.

# Next step
Step 4: singles-only solver that explains the next hint in plain words.
