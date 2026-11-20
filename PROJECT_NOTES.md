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
- ui/HomeScreen.kt: the words you see when the app opens, and the grid under them.
- ui/SudokuBoard.kt: the 9 by 9 grid. A tap marks a square.
- ui/CellLabel.kt: the words TalkBack reads for a square.
- ui/Theme.kt: the paper colors.
- engine/Board.kt: the 9 by 9 grid, with no Android code.
- engine/Solver.kt: finds whether a puzzle has one solution, and stops early.
- engine/Singles.kt: the next naked or hidden single, in plain words, and a check for a wrong number.
- engine/Generator.kt: makes an Easy, Medium, or Hard puzzle, and today's puzzle from the date.
- game/Game.kt: one puzzle being played. Place a number, write a note, undo. Clues cannot change.
- app/src/test/.../GameTest.kt: checks place, note, undo, a wrong number, and a finished grid.
- app/src/test/.../GeneratorTest.kt: checks 20 puzzles of each level, and that today's puzzle stays the same.
- app/src/test/.../SinglesTest.kt: checks those hints and prints a full explanation.
- game/GamePackage.kt: placeholder for the game state.
- data/DataPackage.kt: placeholder for saving on the phone.
- ads/AdsPackage.kt: placeholder for the one ad.
- billing/BillingPackage.kt: placeholder for the unlock.
- app/src/test/.../BoardTest.kt: checks the solver.
- app/src/test/.../CellLabelTest.kt: checks the words for a square, including a wrong number and notes.

# Decisions
- The app is always the light paper theme. Status icons stay dark even when the phone is in dark mode. Navigation icons stay dark on Android 8 and newer. On Android 7 the navigation bar is dark so its white icons can be seen.
- compileSdk is 37, not 36. Google's current Compose BOM 2026.09.00 (Compose 1.12) requires compileSdk 37 and Android Gradle Plugin 9. targetSdk stays 36, which is the Play requirement. Source: https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
- Android Gradle Plugin 9.5.0, Gradle 9.6.0, Kotlin 2.4.10. AGP 9 already includes Kotlin, so the separate Kotlin Android plugin is not applied. Source: https://kotlinlang.org/docs/compose-compiler-migration-guide.html
- Libraries: Compose BOM 2026.09.00, Material 3 (from that BOM), activity-compose 1.13.0, lifecycle ViewModel Compose 2.11.0, lifecycle runtime Compose 2.11.0, core-ktx 1.19.1, DataStore Preferences 1.3.0, coroutines 1.11.0, JUnit 4.13.2.
- No ads library and no billing library yet.
- Naked singles are offered before hidden singles. A filled cell that disagrees with the solution is reported as a mistake, and no hint is given until that cell is fixed.
- No pairs, triples, or X-Wing. v1 hints stay singles only.
- Easy keeps 40 clues and needs no hidden single. Medium keeps 32 and needs 1 to 4. Hard keeps 28, needs 5 or more, and singles alone cannot finish it. The center clue is removed first so the count can land on that floor. A puzzle that misses its grade is thrown away.
- Clues are removed in 180-degree pairs. The generator only shuffles a known finished grid. It does not invent one.
- Today's puzzle is Medium, and the date picks the random choices. No network. February 29 is allowed only in a leap year. November 31 is rejected.
- A note can be written only in an empty cell, and not for a number already in that row, column, or box. Placing a number clears that note everywhere in the same row, column, and box. Undo puts the notes back. The notes handed back are a copy. A game will not start if the finished grid has an empty cell or a repeated number.
- The grid is left to right even when the phone language is not. Cell lines are the same dark ink as the numbers, and they stay on top of a marked square. Thickness alone marks a box. The outer corners meet. TalkBack says Clear when that square is already marked.

# Steps finished
- Step 1, Nov 16 2026: the app project exists and opens on a title screen that says Senior Sudoku. Not yet run on a phone from this machine.
- Step 2, Nov 16 2026: SDK levels, libraries, six packages, and Git are set up. Ads and billing are not added.
- Step 3, Nov 16 2026: the board and the solution counter. All 7 unit tests passed, and a debug app file builds.
- Step 4, Nov 17 2026: singles-only hints. A sample puzzle finishes in 51 naked singles, and a separate grid produces the hidden-single sentence. A later check compared the hints with a second, simpler checker. No wrong digits. Unit tests passed.
- Step 5, Nov 17 2026: the puzzle maker. Easy, Medium, Hard, and a daily puzzle that stays the same for the same date. Later checks rejected impossible dates, removed the center clue first, and compared every hint with a second reader that does not use bit masks. Every day of 2024 and 2026 makes a different puzzle. Unit tests passed.
- Step 6, Nov 17 2026: a game in progress. Place a number, write a note, undo. A clue cannot be changed. A wrong number is listed. Unit tests passed.
- Step 7, Nov 18 2026: the grid is on the opening screen. Each square takes an equal share of the screen. A tap marks that square and names its row and column. Clues are dark. The digit stays the same share of its square when the system text size is turned up. The grid does not place a number yet. Unit tests passed. Not yet run on a phone.

# What is broken right now
Nothing known in the code. Unit tests passed. It still needs a first run in Android Studio on an emulator or a phone.

# Next step
Step 8: number keys, so the marked square can take a digit.
