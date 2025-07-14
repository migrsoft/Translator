# Image Translator

The Image Translator is a desktop application built with Kotlin and Swing that allows users to view images, perform Optical Character Recognition (OCR) on selected regions, translate the OCR results, and manage subtitles directly on the images. It supports both individual image files and CBZ archives.

## Features

*   **Image Viewing:** Display various image formats (JPEG, JPG, PNG, WEBP).
*   **CBZ Archive Support:** Open and navigate through images within CBZ files.
*   **Image Display Modes:** Switch between "Fit to View", "Fit to Width", and "Actual Size" display modes.
*   **OCR (Optical Character Recognition):**
    *   Select a region on an image to perform OCR.
    *   View OCR results in a dedicated dialog.
    *   Configurable OCR language (English, Chinese, Greek).
*   **Translation:**
    *   Translate OCR results using a local LibreTranslate instance.
    *   Save translated text as subtitles associated with the image.
*   **Subtitle Management:**
    *   Display OCR text or translated text as subtitles on the image.
    *   Edit existing subtitles (OCR text and translated text).
    *   Move and resize subtitle bounding boxes.
    *   **Delete Individual Subtitles:** Right-click on a subtitle to delete it.
    *   **Delete All Subtitles:** Clear all subtitles for the current image via the "Edit" menu.
    *   Save and load subtitles to/from JSON files (supports both single image and CBZ-wide subtitles).
*   **File Management:**
    *   Rename multiple local image files with a sequential numbering scheme.
    *   Delete selected local image files.

## Technologies Used

*   **Kotlin:** Primary programming language.
*   **Swing:** For the graphical user interface.
*   **Gradle:** Build automation tool.
*   **Tesseract OCR:** Used for OCR functionality (requires a running Tesseract server, e.g., `tesseract-server`).
*   **LibreTranslate:** Used for translation functionality (requires a running LibreTranslate instance).
*   **OkHttp:** HTTP client for API calls to Tesseract and LibreTranslate.
*   **Gson:** Java library for JSON serialization/deserialization.

## Setup and Installation

### Prerequisites

*   Java Development Kit (JDK) 11 or higher.
*   Gradle (usually bundled with the project, so `gradlew` should work).
*   **Tesseract OCR Server:** You need a running Tesseract OCR server. A common way to set this up is using `tesseract-server`.
    *   You can find more information on setting up `tesseract-server` [here](https://github.com/tesseract-ocr/tesseract/wiki/Tesseract-OCR-Server).
    *   Ensure the server is accessible at `http://localhost:8884` (or update `TesseractApi.kt` if it's different).
*   **LibreTranslate Instance:** You need a running LibreTranslate instance.
    *   Instructions for setting up LibreTranslate can be found on their official GitHub page or documentation.
    *   Ensure the server is accessible at `http://localhost:5000` (or update `LibreTranslateApi.kt` if it's different).

### Building the Project

1.  **Clone the repository:**
    ```bash
    git clone <repository_url>
    cd Translator
    ```
2.  **Build the project using Gradle:**
    ```bash
    ./gradlew build
    ```
    This will compile the source code and create executable JARs in the `build/libs` directory.

### Running the Application

After building, you can run the application from the command line:

```bash
./gradlew run
```
Or, you can run the shadow JAR for a self-contained executable:
```bash
java -jar build/libs/Translator-all.jar
```

## Usage

1.  **Open Images/CBZ:**
    *   Go to `File` -> `Open` to select individual image files.
    *   Go to `File` -> `Open CBZ` to open a Comic Book Zip archive.
2.  **Navigate Images:** Use the file list on the left to switch between images.
3.  **Adjust Display:** Use the toolbar buttons (Fit to View, Fit to Width, Actual Size) to change how the image is displayed.
4.  **Perform OCR:**
    *   Click and drag on the image to create a selection box.
    *   Click the "OCR" button (icon with 'A' and lines) in the toolbar.
    *   The OCR result will appear in a dialog.
    *   You can change the OCR language via `Edit` -> `OCR Language`.
5.  **Translate OCR Result:**
    *   After performing OCR, click the "Translate" button (globe icon) in the toolbar.
    *   A translation dialog will appear. Select the target language and click "Translate".
    *   The translated text will be displayed.
6.  **Save Subtitles:**
    *   In the translation dialog, click "Save" to add the translated text as a subtitle to the current image.
    *   Subtitles will appear as white boxes with black text on the image.
    *   You can move and resize subtitles by dragging their bounding boxes.
7.  **Edit Subtitles:** Right-click on an existing subtitle to bring up a context menu and select "Edit Subtitle".
8.  **Delete Subtitles:**
    *   Right-click on an individual subtitle and select "Delete Subtitle".
    *   Go to `Edit` -> `Delete All Subtitles` to remove all subtitles from the current image.
9.  **Save Subtitle Files:**
    *   Go to `File` -> `Save Subtitles`.
    *   For individual images, it saves a JSON file next to the image.
    *   For CBZ files, it saves a single JSON file containing subtitles for all images in the archive.
10. **Rename/Delete Local Files:** Right-click on selected files in the file list to access "Rename" or "Delete" options.

## Screenshots

*(Consider adding screenshots here to illustrate the UI and features.)*

## Contributing

Feel free to fork the repository and submit pull requests.

## License

This project is open-source and available under the [MIT License](LICENSE).
