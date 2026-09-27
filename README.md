# SmartDiary — PDF Reader with OCR & On-Device Translation

SmartDiary (internal project name **PdfOcrReader**) is an Android app for reading PDF books with built-in OCR text recognition and on-device word/phrase translation. It is designed for language learners: tap a word or select a whole phrase (phrasal verbs included) and get an instant translation — then save it to your personal dictionary.

## Features

### 📖 PDF Reading
- Open PDF files via the system file picker (SAF)
- Page navigation (previous / next)
- Resume reading from the last opened page — progress is saved automatically
- Book library: previously opened books stay available in the start menu

### 🔍 Zoom & Navigation
- Pinch-to-zoom (1x–5x)
- Pan across the page when zoomed in
- **Translate / Move mode switch** — a floating toggle on the right edge:
  - **Translate (on)**: drag to select words or phrases
  - **Move (off)**: drag to pan the page freely

### 🌐 OCR & Translation
- On-device OCR (ML Kit Text Recognition) — works with scanned PDFs too
- Tap or drag-select a word or phrase to translate it
- English → Russian translation out of the box (on-device ML Kit model, downloaded once over Wi-Fi)

### 📚 Bookmarks & Library
- Bookmark any page with one tap
- Jump between bookmarks from the bookmarks sheet
- Library of previously opened books with saved progress

### 🗂 Personal Dictionary (TXT)
- Choose an existing `.txt` dictionary file, or create a new one right from the app
- Supported line formats:
  ```text
  phrase - translation
  phrase: translation
  phrase<TAB>translation
  ```
  Lines starting with `#` are ignored
- After translating a word/phrase, tap **Add to dictionary** to append it to your TXT file
- Duplicate entries are not added
- Browse and delete dictionary entries in the dictionary sheet

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM (ViewModel + StateFlow) |
| PDF rendering | `android.graphics.pdf.PdfRenderer` |
| OCR | ML Kit Text Recognition |
| Translation | ML Kit On-Device Translation |
| Storage | Room (SQLite) |
| Async | Kotlin Coroutines + Flow |

## Project Structure

```text
app/src/main/java/com/example/pdfocr/
├── MainActivity.kt              # Entry point
├── PdfViewerActivity.kt         # Viewer UI (Compose)
├── data/
│   ├── local/                   # Room: entities, DAOs, database
│   ├── ocr/                     # ML Kit OCR wrapper
│   ├── pdf/                     # PdfRenderer wrapper
│   ├── repository/              # Bookmark / Library / Dictionary repositories
│   ├── translate/               # ML Kit translation wrapper
│   └── ...
├── domain/
│   ├── model/                   # Domain models
│   ├── repository/              # Repository interfaces
│   └── usecase/                 # Business logic (selection, bookmarks)
└── ui/
    ├── theme/                   # Compose theme
    └── viewer/                  # ViewModel + UI state
```

## Requirements

- Android Studio (Koala or newer recommended)
- Android SDK 36 (compileSdk), min Android 10 (API 29)
- JDK 17

## Build

1. Clone the repository:
   ```bash
   git clone https://github.com/Johny5142/smartdiary.git
   cd smartdiary
   ```
2. Open the project in **Android Studio** and let Gradle sync, **or** build from the command line:
   ```bash
   ./gradlew :app:assembleDebug
   ```
3. Install the debug APK on a device/emulator:
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

## Usage

1. Launch the app and pick a PDF (or choose one from the library).
2. Wait for OCR to finish (see the `OCR…` indicator in the title).
3. Keep the floating switch on **Translate** and drag over a word or phrase.
4. Read the translation in the bottom sheet.
5. Tap **Add to dictionary** to save it to your TXT dictionary.
6. To zoom and pan, switch to **Move** or use the `−`/`+` buttons in the bottom bar.
7. Bookmark pages with the bookmark icon; your reading position is saved automatically.

## Notes

- The first translation startup downloads the on-device language model over Wi-Fi.
- Dictionary writes append to the chosen TXT file; the file must be selected first.
- OCR works on rendered page bitmaps, so it also handles scanned (non-searchable) PDFs.

## License

All rights reserved. (Add a LICENSE file if you want to publish under MIT/Apache-2.0.)
