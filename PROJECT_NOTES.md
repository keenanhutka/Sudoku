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
- ui/HomeScreen.kt: the home screen (Today's puzzle, Easy, Medium, Hard), the grid, the number keys, Notes, Undo, Check my numbers, and the Well done screen.
- ui/SudokuBoard.kt: the 9 by 9 grid. A tap marks a square.
- ui/NumberKeys.kt: the 1 to 9 keys and Erase. A clue cannot be changed.
- ui/CellLabel.kt: the words TalkBack reads for a square, and the Check my numbers sentence.
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
- app/src/test/.../NumberKeyTest.kt: checks that a number key works only on a marked square that is not a clue.

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
- The grid is left to right even when the phone language is not. The number keys are too. Cell lines are the same dark ink as the numbers, and they stay on top of a marked square. A marked square is filled solid brown, or solid red when its number is wrong, so it does not blend into the grid lines. Its number is paper-colored, so it stays readable. The squares in its row, column, and box have a thinner brown frame. Those frames are an even number of pixels, so they are not blurry. A number you typed is brown until that square is marked. A wrong number is red in the square and on its key. That key is outlined in red, and tapping it removes the number. That key then stays dim until a different square is marked, so a second tap cannot put the number back. Tapping that same square does not turn it back on. Undo clears the block only when that number is back. While Notes is on, that key can still take a pencil mark. The marked square itself turns red. Thickness alone marks a box. The lines and the squares share the same pixel edges, because 4dp is an odd number of pixels on some phones. Leftover pixels are spread so the three boxes stay the same size. The thin lines are an even number of pixels, so they are not blurry. The outer corners meet. TalkBack says Deselect when that square is already marked. A number key is read once, not twice. A key that is waiting keeps a dark border, so it does not fade into the paper. The keys grow with the system text size so a digit is not cut off, and they shrink on a short screen so the grid stays on screen. Turned sideways, the keys sit beside the grid. Turning the phone, or changing the text size, does not wipe the numbers you typed. Erase is its own row so the word stays whole. Notes is a button that says Notes on or Notes off. While it is on, a number key writes a small note in an empty square, and the key fills in when that note is there. Undo steps back, including notes. Check my numbers says how many numbers look wrong and does not end the game. A wrong number is still red as you type. A finished puzzle shows Well done, with Next puzzle and Done. Done closes the puzzle. The ad, if one is added later, belongs on Done, never on Next puzzle. A debug build has Fill solution so Well done can be opened quickly. Small notes sit inside the lines, so a brown frame does not cut off the corner digits. A warning that a note is already in the row, column, or box goes away when you mark a different square. TalkBack says Add note or Remove note, matching what the tap does. The Notes button says Turn notes on or Turn notes off, so it does not repeat the label as the action. Check says the puzzle is not finished when nothing is wrong but squares are still empty. TalkBack speaks that sentence when it appears. Erase stays off on an empty square, so it does not offer a tap that does nothing. Tapping the number already in a square leaves the check sentence up, because the puzzle did not change. That number key stays off, so TalkBack does not offer Enter for a tap that does nothing. A clue's key says given, not entered. A second tap on a square says Deselect, because Clear sounded like Erase. The app opens on a home screen. The choices are Today's puzzle, Easy, Medium, and Hard. Today's puzzle is the same Medium puzzle all day. Done returns to that screen. Home asks before leaving, because the puzzle is not saved yet, and TalkBack speaks that warning. Next puzzle makes a new puzzle of the same level, not the same daily puzzle again. Hint shows the next simple step and marks that square. Tapping that square again keeps the hint. Tapping a different square clears it. It turns notes off, so the next number tap places the digit. It does not type the number. A wrong number is marked too, and notes turn off so the next tap can replace it. Tapping that square again keeps it marked. Another wrong number keeps the warning. Undo keeps it too, until no wrong number is left. Check marks the first wrong square, and notes stay off until that number is fixed. The count changes when another wrong number is added or removed. The mark stays on that square while you erase it and type the right number. It moves on when that square is correct and another number is still wrong. A box hint names the square and the digit first, so that part stays on screen. A long hint scrolls, so the keys stay on screen, and the next sentence starts at the top. The hint box is a whole number of lines, so a line is not cut in half. Hint sits beside Check so the grid stays large enough to read.

# Steps finished
- Step 1, Nov 16 2026: the app project exists and opens on a title screen that says Senior Sudoku. Not yet run on a phone from this machine.
- Step 2, Nov 16 2026: SDK levels, libraries, six packages, and Git are set up. Ads and billing are not added.
- Step 3, Nov 16 2026: the board and the solution counter. All 7 unit tests passed, and a debug app file builds.
- Step 4, Nov 17 2026: singles-only hints. A sample puzzle finishes in 51 naked singles, and a separate grid produces the hidden-single sentence. A later check compared the hints with a second, simpler checker. No wrong digits. Unit tests passed.
- Step 5, Nov 17 2026: the puzzle maker. Easy, Medium, Hard, and a daily puzzle that stays the same for the same date. Later checks rejected impossible dates, removed the center clue first, and compared every hint with a second reader that does not use bit masks. Every day of 2024 and 2026 makes a different puzzle. Unit tests passed.
- Step 6, Nov 17 2026: a game in progress. Place a number, write a note, undo. A clue cannot be changed. A wrong number is listed. Unit tests passed.
- Step 7, Nov 18 2026: the grid is on the opening screen. Each square takes an equal share of the screen. A tap marks that square and names its row and column. Clues are dark. The digit stays the same share of its square when the system text size is turned up, and its line height does too, so the grid lines do not cut through it. Unit tests passed. Not yet run on a phone.
- Step 8, Nov 20 2026: number keys under the grid. A marked square takes a digit. Erase clears a digit the player wrote. A clue does not change. A wrong digit is shown in red. The keys are three across, the same size, and Erase is its own row. Unit tests passed. Not yet run on a phone.
- Step 8 continued, Nov 21 2026: Notes, Undo, Check my numbers, and the Well done screen. Fill solution shows only on a debug build. Unit tests passed. Not yet run on a phone.
- Step 9, Nov 22 2026: the home screen. Today's puzzle, Easy, Medium, and Hard. Done returns there. Unit tests passed. Not yet run on a phone.
- Step 10, Nov 22 2026: Hint. It shows the next simple step and marks that square. Notes turn off and stay off while that hint is showing, so a number key places the number instead of writing a note. Erasing the notes in that square leaves the hint up. A wrong number there leaves it up too, so Notes stay off and the keys can replace it. Tapping a different square keeps that warning. Tapping Hint again leaves the sentence up. Hint and Check look unavailable while it is showing. Check does not remove it either. If Undo brings a wrong number back, the hint is replaced by the warning and Notes stay off. A wrong number is marked too, and notes turn off so the next tap can replace it. Tapping that square again keeps it marked. Another wrong number keeps the warning. Undo keeps it too, until no wrong number is left. Check marks the first wrong square, and notes stay off until that number is fixed. The count changes when another wrong number is added or removed. The mark stays on that square while you erase it and type the right number. It moves on when that square is correct and another number is still wrong. Hint does not type the number. Unit tests passed. Not yet run on a phone.

# What is broken right now
Nothing known in the code. Unit tests passed. It still needs a first run in Android Studio on an emulator or a phone.

# Next step
The written plan in this folder does not name a step after 10.
