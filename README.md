# Senior Sudoku

Large-print Sudoku for Android. Offline, no account. One ad after a finished puzzle, plus a one-time unlock. The build plan is in `PROJECT_NOTES.md`.

Open this folder in a current Android Studio that supports Android Gradle Plugin 9.5. Let it install Android SDK 37 if it asks. Press Sync, then Run. The phone should show Senior Sudoku and four choices: Today's puzzle, Easy, Medium, and Hard. Tap one. Then tap a square, then tap a number to write it. Erase clears a number you wrote. A printed clue stays. Home asks before you leave, because the puzzle is not saved yet. Done returns to the choices.

Unit tests: right-click `app/src/test/java/com/keenanhutka/seniorsudoku/engine/BoardTest.kt` and choose Run.
