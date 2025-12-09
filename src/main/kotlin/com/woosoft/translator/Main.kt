package com.woosoft.translator

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.filechooser.FileNameExtensionFilter

private var lastOpenedDirectory: File? = null
private var currentCbzZipFile: ZipFile? = null // To hold the currently open CBZ file
private var currentCbzImageSubtitles: MutableMap<String, MutableList<SubtitleEntry>> = mutableMapOf() // Image name to list of subtitles
private var lastSelectedImageName: String? = null // To track the previously selected image in CBZ
private val selectedFilesList = mutableListOf<DisplayableImage>()
private var lastOcrResult: String? = null
var selectedOcrLanguageCode: String = "eng" // Default to English

interface DisplayableImage {
    val name: String
    fun getInputStream(): InputStream
}

class LocalFileImage(val file: File) : DisplayableImage {
    override val name: String = file.name
    override fun getInputStream(): InputStream = file.inputStream()
}

class CbzImage(private val zipFile: ZipFile, private val zipEntry: ZipEntry) : DisplayableImage {
    override val name: String = zipEntry.name.substringAfterLast('/')
    override fun getInputStream(): InputStream = zipFile.getInputStream(zipEntry)
}

fun main() {
    SwingUtilities.invokeLater {
        createAndShowGUI()
    }
}

fun createAndShowGUI() {
    // Create the main frame
    val frame = JFrame("Image Translator")
    frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    frame.preferredSize = Dimension(1024, 768)

    // Create the menu bar
    val menuBar = JMenuBar()
    val fileMenu = JMenu("File")
    val openMenuItem = JMenuItem("Open")
    fileMenu.add(openMenuItem)
    val openCbzMenuItem = JMenuItem("Open CBZ")
    fileMenu.add(openCbzMenuItem)
    val saveSubtitlesMenuItem = JMenuItem("Save Subtitles")
    fileMenu.add(saveSubtitlesMenuItem)
    fileMenu.addSeparator()
    val translateFileMenuItem = JMenuItem("Translate File")
    fileMenu.add(translateFileMenuItem)
    fileMenu.addSeparator()
    val exitMenuItem = JMenuItem("Exit")
    exitMenuItem.addActionListener { System.exit(0) }
    fileMenu.add(exitMenuItem)
    menuBar.add(fileMenu)

    val editMenu = JMenu("Edit")
    
    val ocrLanguageMenu = JMenu("OCR Language")
    editMenu.add(ocrLanguageMenu)

    val deleteSubtitlesMenuItem = JMenuItem("Delete All Subtitles")
    deleteSubtitlesMenuItem.addActionListener {
        val confirm = JOptionPane.showConfirmDialog(
            frame,
            "Are you sure you want to delete all subtitles for the current image?",
            "Confirm Delete",
            JOptionPane.YES_NO_OPTION
        )
        if (confirm == JOptionPane.YES_OPTION) {
            getImagePanel().subtitles.clear()
            getImagePanel().selectionRect = null // Clear any active selection
            getImagePanel().repaint()
        }
    }
    editMenu.add(deleteSubtitlesMenuItem)
    menuBar.add(editMenu)

    val ocrLanguages = mapOf(
        "English" to "eng",
        "Chinese" to "chs",
        "Greek" to "grc"
    )

    val ocrLanguageButtonGroup = ButtonGroup()
    ocrLanguages.forEach { (name, code) ->
        val menuItem = JRadioButtonMenuItem(name)
        menuItem.addActionListener {
            selectedOcrLanguageCode = code
        }
        ocrLanguageButtonGroup.add(menuItem)
        ocrLanguageMenu.add(menuItem)
    }

    // Set initial selection
    ocrLanguages.entries.firstOrNull { it.value == selectedOcrLanguageCode }?.let { entry ->
        ocrLanguageMenu.menuComponents.forEach { component ->
            if (component is JRadioButtonMenuItem && component.text == entry.key) {
                component.isSelected = true
            }
        }
    }

    val subtitleDisplayMenu = JMenu("Subtitle Display")
    editMenu.add(subtitleDisplayMenu)

    val subtitleDisplayButtonGroup = ButtonGroup()

    val noSubtitlesMenuItem = JRadioButtonMenuItem("No Subtitles")
    noSubtitlesMenuItem.addActionListener {
        getImagePanel().setSubtitleDisplayMode(SubtitleDisplayMode.NONE)
    }
    subtitleDisplayButtonGroup.add(noSubtitlesMenuItem)
    subtitleDisplayMenu.add(noSubtitlesMenuItem)

    val ocrSubtitlesMenuItem = JRadioButtonMenuItem("OCR Subtitles")
    ocrSubtitlesMenuItem.addActionListener {
        getImagePanel().setSubtitleDisplayMode(SubtitleDisplayMode.OCR)
    }
    subtitleDisplayButtonGroup.add(ocrSubtitlesMenuItem)
    subtitleDisplayMenu.add(ocrSubtitlesMenuItem)

    val translatedSubtitlesMenuItem = JRadioButtonMenuItem("Translated Subtitles")
    translatedSubtitlesMenuItem.addActionListener {
        getImagePanel().setSubtitleDisplayMode(SubtitleDisplayMode.TRANSLATION)
    }
    subtitleDisplayButtonGroup.add(translatedSubtitlesMenuItem)
    subtitleDisplayMenu.add(translatedSubtitlesMenuItem)

    // Set initial selection for subtitle display (default to translated)
    translatedSubtitlesMenuItem.isSelected = true

    frame.jMenuBar = menuBar

    // Create the file list view (left side)
    val (fileListScrollPane, fileListModel) = createFileListView()
    val fileList = (fileListScrollPane.viewport.view as JList<*>)

    // Create the image display view (right side)
    val imageDisplayScrollPane = createImageDisplayView()

    // Create the split pane
    val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, fileListScrollPane, imageDisplayScrollPane)
    splitPane.isContinuousLayout = true
    splitPane.setOneTouchExpandable(true)
    splitPane.dividerLocation = 200 // Initial divider position

    // Create the toolbar for image display modes
    val toolBar = JToolBar()
    toolBar.setFloatable(false)
    val fitToViewButton = JButton(ImageIcon(object {}.javaClass.getResource("/icons/fit_to_view.png")))
    fitToViewButton.toolTipText = "Fit to View"
    val fitToWidthButton = JButton(ImageIcon(object {}.javaClass.getResource("/icons/fit_to_width.png")))
    fitToWidthButton.toolTipText = "Fit to Width"
    val actualSizeButton = JButton(ImageIcon(object {}.javaClass.getResource("/icons/actual_size.png")))
    actualSizeButton.toolTipText = "Actual Size"
    val getSelectedImageButton = JButton(ImageIcon(object {}.javaClass.getResource("/icons/get_selected_image.png")))
    getSelectedImageButton.toolTipText = "Get Selected Image"
    val ocrButton = JButton(ImageIcon(object {}.javaClass.getResource("/icons/ocr.png")))
    ocrButton.toolTipText = "OCR Selected Image"
    val translateButton = JButton(ImageIcon(object {}.javaClass.getResource("/icons/translate.png")))
    translateButton.toolTipText = "Translate OCR Result"

    toolBar.add(fitToViewButton)
    toolBar.add(fitToWidthButton)
    toolBar.add(actualSizeButton)
    toolBar.add(Box.createHorizontalStrut(20)) // Add a 20-pixel horizontal gap
    toolBar.add(getSelectedImageButton)
    toolBar.add(ocrButton)
    toolBar.add(translateButton)
    toolBar.add(Box.createHorizontalGlue()) // Pushes everything to the left
    val imageSizeLabel = JLabel("")
    toolBar.add(imageSizeLabel)

    // Add action listener for Open menu item
    openMenuItem.addActionListener {
        currentCbzZipFile?.close() // Close any previously opened CBZ file
        currentCbzZipFile = null

        val fileChooser = JFileChooser(System.getProperty("user.dir"))
        fileChooser.fileFilter = FileNameExtensionFilter("Image Files", "jpeg", "jpg", "png", "webp")
        fileChooser.isMultiSelectionEnabled = true // Enable multi-selection

        val result = fileChooser.showOpenDialog(frame)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileListModel.clear() // Clear existing items
            selectedFilesList.clear() // Clear the list of selected files
            displayImage(null, ImageDisplayMode.FIT_TO_VIEW, imageDisplayScrollPane, imageSizeLabel) // Clear previous image

            val selectedFiles: Array<File> = fileChooser.selectedFiles
            val tempImages = selectedFiles.map { LocalFileImage(it) }.toMutableList()
            tempImages.sortWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
            tempImages.forEach {
                fileListModel.addElement(it.name)
                selectedFilesList.add(it)
            }
            selectedFiles.firstOrNull()?.parentFile?.let { lastOpenedDirectory = it }

            // Automatically load subtitles if available
            if (selectedFilesList.isNotEmpty()) {
                val firstImageFile = (selectedFilesList[0] as LocalFileImage).file
                val subtitleFile = File(firstImageFile.parentFile, firstImageFile.nameWithoutExtension + ".json")
                if (subtitleFile.exists()) {
                    val loadedSubtitles = SubtitleManager.loadSingleImageSubtitles(subtitleFile)
                    getImagePanel().subtitles.clear()
                    loadedSubtitles.forEach { getImagePanel().addSubtitle(it) }
                }
            }
        }
    }

    // Add action listener for Open CBZ menu item
    openCbzMenuItem.addActionListener {
        currentCbzZipFile?.close() // Close any previously opened CBZ file
        currentCbzZipFile = null

        val fileChooser = JFileChooser(System.getProperty("user.dir"))
        fileChooser.fileFilter = FileNameExtensionFilter("CBZ Files", "cbz")
        fileChooser.isMultiSelectionEnabled = false // Only allow single CBZ selection

        val result = fileChooser.showOpenDialog(frame)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedCbzFile = fileChooser.selectedFile
            if (selectedCbzFile != null) {
                fileListModel.clear()
                selectedFilesList.clear()
                displayImage(null, ImageDisplayMode.FIT_TO_VIEW, imageDisplayScrollPane, imageSizeLabel) // Clear previous image

                try {
                    val zipFile = ZipFile(selectedCbzFile)
                    currentCbzZipFile = zipFile // Store the ZipFile
                    val entries = zipFile.entries()
                    val imageEntries = mutableListOf<CbzImage>()
                    while (entries.hasMoreElements()) {
                        val entry = entries.nextElement()
                        if (!entry.isDirectory && (entry.name.endsWith(".jpg", true) || entry.name.endsWith(".jpeg", true) || entry.name.endsWith(".png", true) || entry.name.endsWith(".webp", true))) {
                            imageEntries.add(CbzImage(zipFile, entry))
                        }
                    }
                    // Sort image entries by name
                    imageEntries.sortWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }

                    imageEntries.forEach { cbzImage ->
                        fileListModel.addElement(cbzImage.name)
                        selectedFilesList.add(cbzImage)
                    }

                    if (selectedFilesList.isNotEmpty()) {
                        fileList.selectedIndex = 0 // Select the first image
                        displayImage(selectedFilesList[0], ImageDisplayMode.FIT_TO_WIDTH, imageDisplayScrollPane, imageSizeLabel)
                        lastSelectedImageName = selectedFilesList[0].name // Set initial selected image name
                    }
                    selectedCbzFile.parentFile?.let { lastOpenedDirectory = it }

                    // Automatically load subtitles if available for CBZ
                    val subtitleFile = File(selectedCbzFile.parentFile, selectedCbzFile.nameWithoutExtension + ".json")
                    if (subtitleFile.exists()) {
                        val loadedCbzSubtitles = SubtitleManager.loadCbzSubtitles(subtitleFile)
                        currentCbzImageSubtitles = loadedCbzSubtitles.mapValues { it.value.toMutableList() }.toMutableMap()
                        // Display subtitles for the first image if available
                        selectedFilesList.firstOrNull()?.name?.let { firstImageName ->
                            currentCbzImageSubtitles[firstImageName]?.let { subtitlesForFirstImage ->
                                getImagePanel().subtitles.clear()
                                subtitlesForFirstImage.forEach { getImagePanel().addSubtitle(it) }
                            }
                        }
                    } else {
                        currentCbzImageSubtitles.clear()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                    JOptionPane.showMessageDialog(frame, "Error opening CBZ file: ${e.message}", "CBZ Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }

    // Add action listener for Save Subtitles menu item
    saveSubtitlesMenuItem.addActionListener {
        if (selectedFilesList.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "No image open to save subtitles for.", "Save Subtitles", JOptionPane.INFORMATION_MESSAGE)
            return@addActionListener
        }

        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Save Subtitles"
        fileChooser.fileFilter = FileNameExtensionFilter("Subtitle JSON Files", "json")
        lastOpenedDirectory?.let { fileChooser.currentDirectory = it }

        if (currentCbzZipFile != null) {
            // For CBZ files, save all accumulated subtitles
            lastSelectedImageName?.let { prevImageName ->
                currentCbzImageSubtitles[prevImageName] = getImagePanel().subtitles.toMutableList()
            }
            val cbzName = File(currentCbzZipFile!!.name).name
            val suggestedFileName = cbzName.substringBeforeLast(".") + ".json"
            fileChooser.selectedFile = File(lastOpenedDirectory, suggestedFileName)

            val result = fileChooser.showSaveDialog(frame)
            if (result == JFileChooser.APPROVE_OPTION) {
                val fileToSave = fileChooser.selectedFile
                try {
                    SubtitleManager.saveCbzSubtitles(fileToSave, currentCbzImageSubtitles)
                    JOptionPane.showMessageDialog(frame, "CBZ subtitles saved successfully to ${fileToSave.name}", "Save Subtitles", JOptionPane.INFORMATION_MESSAGE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    JOptionPane.showMessageDialog(frame, "Error saving CBZ subtitles: ${e.message}", "Save Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        } else { // Single image file
            val currentSubtitles = getImagePanel().subtitles
            if (currentSubtitles.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "No subtitles to save for this image.", "Save Subtitles", JOptionPane.INFORMATION_MESSAGE)
                return@addActionListener
            }

            val selectedIndex = fileList.selectedIndex
            if (selectedIndex == -1) {
                JOptionPane.showMessageDialog(frame, "No image selected to save subtitles for.", "Save Subtitles", JOptionPane.INFORMATION_MESSAGE)
                return@addActionListener
            }

            val selectedImageFile = (selectedFilesList[selectedIndex] as LocalFileImage).file
            val suggestedFileName = selectedImageFile.nameWithoutExtension + ".json"
            fileChooser.selectedFile = File(lastOpenedDirectory, suggestedFileName)

            val result = fileChooser.showSaveDialog(frame)
            if (result == JFileChooser.APPROVE_OPTION) {
                val selectedFile = fileChooser.selectedFile
                try {
                    SubtitleManager.saveSingleImageSubtitles(selectedFile, currentSubtitles)
                    JOptionPane.showMessageDialog(frame, "Subtitles saved successfully to ${selectedFile.name}", "Save Subtitles", JOptionPane.INFORMATION_MESSAGE)
                } catch (e: Exception) {
                    e.printStackTrace()
                    JOptionPane.showMessageDialog(frame, "Error saving subtitles: ${e.message}", "Save Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }

    translateFileMenuItem.addActionListener {
        val dialog = FileTranslationDialog(frame)
        dialog.isVisible = true
    }

    // Add ListSelectionListener to fileList
    fileList.addListSelectionListener(object : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent?) {
            if (!e!!.getValueIsAdjusting()) {
                val selectedIndex = fileList.selectedIndex
                if (selectedIndex != -1) {
                    val selectedDisplayableImage = selectedFilesList[selectedIndex]

                    // If a CBZ is open, save current image's subtitles and load new image's subtitles
                    if (currentCbzZipFile != null) {
                        lastSelectedImageName?.let { prevImageName ->
                            currentCbzImageSubtitles[prevImageName] = getImagePanel().subtitles.toMutableList()
                        }
                        getImagePanel().subtitles.clear()
                        currentCbzImageSubtitles[selectedDisplayableImage.name]?.let { subtitlesForNewImage ->
                            subtitlesForNewImage.forEach { getImagePanel().addSubtitle(it) }
                        }
                        lastSelectedImageName = selectedDisplayableImage.name
                    } else {
                        // For single image files, load the corresponding subtitle file
                        getImagePanel().subtitles.clear() // Clear previous subtitles first
                        if (selectedDisplayableImage is LocalFileImage) {
                            val imageFile = selectedDisplayableImage.file
                            val subtitleFile = File(imageFile.parentFile, imageFile.nameWithoutExtension + ".json")
                            if (subtitleFile.exists()) {
                                val loadedSubtitles = SubtitleManager.loadSingleImageSubtitles(subtitleFile)
                                loadedSubtitles.forEach { getImagePanel().addSubtitle(it) }
                            }
                        }
                    }

                    displayImage(selectedDisplayableImage, ImageDisplayMode.FIT_TO_WIDTH, imageDisplayScrollPane, imageSizeLabel)
                }
            }
        }
    })

    // Add MouseListener for right-click context menu
    fileList.addMouseListener(object : java.awt.event.MouseAdapter() {
        override fun mousePressed(e: java.awt.event.MouseEvent) {
            if (SwingUtilities.isRightMouseButton(e)) {
                val selectedIndices = fileList.selectedIndices
                if (selectedIndices.isNotEmpty()) {
                    val allLocalFiles = selectedIndices.all { selectedFilesList[it] is LocalFileImage }
                    if (allLocalFiles) {
                        val popupMenu = JPopupMenu()

                        if (selectedIndices.size == 1) {
                            val renameMenuItem = JMenuItem("Rename")
                            renameMenuItem.addActionListener {
                                val selectedIndex = selectedIndices[0]
                                val selectedLocalFile = selectedFilesList[selectedIndex] as LocalFileImage
                                val originalFile = selectedLocalFile.file
                                val newName = JOptionPane.showInputDialog(frame, "Enter new name for ${originalFile.name}", originalFile.name)
                                if (!newName.isNullOrBlank() && newName != originalFile.name) {
                                    val newFile = File(originalFile.parent, newName)
                                    if (originalFile.renameTo(newFile)) {
                                        selectedFilesList[selectedIndex] = LocalFileImage(newFile)
                                        val currentSelectionName = newFile.name
                                        fileListModel.clear()
                                        selectedFilesList.sortWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
                                        selectedFilesList.forEach { fileListModel.addElement(it.name) }
                                        val newIndex = selectedFilesList.indexOfFirst { it.name == currentSelectionName }
                                        if (newIndex != -1) {
                                            fileList.selectedIndex = newIndex
                                            fileList.ensureIndexIsVisible(newIndex)
                                        }
                                    } else {
                                        JOptionPane.showMessageDialog(frame, "Failed to rename ${originalFile.name}", "Rename Error", JOptionPane.ERROR_MESSAGE)
                                    }
                                }
                            }
                            popupMenu.add(renameMenuItem)
                        }

                        val batchRenameMenuItem = JMenuItem("Batch Rename")
                        batchRenameMenuItem.addActionListener {
                            val renameDialog = RenameDialog(frame) { prefix, startNumber, width ->
                                // Perform renaming logic here
                                val selectedLocalFiles = selectedIndices.map { selectedFilesList[it] as LocalFileImage }
                                var currentNumber = startNumber
                                for (i in selectedLocalFiles.indices) {
                                    val originalFile = selectedLocalFiles[i].file
                                    val extension = originalFile.extension
                                    val newFileName = "${prefix}${String.format("%0${width}d", currentNumber)}.${extension}"
                                    val newFile = File(originalFile.parentFile, newFileName)
                                    if (originalFile.renameTo(newFile)) {
                                        // println("Renamed ${originalFile.name} to ${newFile.name}")
                                        // Update the selectedFilesList
                                        selectedFilesList[selectedIndices[i]] = LocalFileImage(newFile)
                                    } else {
                                        JOptionPane.showMessageDialog(frame, "Failed to rename ${originalFile.name}", "Rename Error", JOptionPane.ERROR_MESSAGE)
                                    }
                                    currentNumber++
                                }
                                // After renaming, clear and re-add all items to resort
                                fileListModel.clear()
                                selectedFilesList.sortWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
                                selectedFilesList.forEach {
                                    fileListModel.addElement(it.name)
                                }
                                // Re-select the first renamed file if any were renamed
                                if (selectedFilesList.isNotEmpty()) {
                                    fileList.selectedIndex = 0
                                }
                            }
                            renameDialog.isVisible = true
                        }
                        popupMenu.add(batchRenameMenuItem)

                        val deleteMenuItem = JMenuItem("Delete")
                        deleteMenuItem.addActionListener {
                            val confirm = JOptionPane.showConfirmDialog(
                                frame,
                                "Are you sure you want to delete the selected files?",
                                "Confirm Delete",
                                JOptionPane.YES_NO_OPTION
                            )
                            if (confirm == JOptionPane.YES_OPTION) {
                                val selectedLocalFiles = selectedIndices.map { selectedFilesList[it] as LocalFileImage }
                                val filesToDelete = selectedLocalFiles.map { it.file }
                                var deletedCount = 0
                                for (file in filesToDelete) {
                                    if (file.delete()) {
                                        // println("Deleted file: ${file.name}")
                                        deletedCount++
                                    } else {
                                        JOptionPane.showMessageDialog(frame, "Failed to delete ${file.name}", "Delete Error", JOptionPane.ERROR_MESSAGE)
                                    }
                                }
                                if (deletedCount > 0) {
                                    // Rebuild the list after deletion
                                    fileListModel.clear()
                                    selectedFilesList.removeAll(selectedLocalFiles)
                                    selectedFilesList.sortWith { a, b -> NaturalOrderComparator.compare(a.name, b.name) }
                                    selectedFilesList.forEach {
                                        fileListModel.addElement(it.name)
                                    }
                                    // Clear image display if the currently displayed image was deleted
                                    if (selectedFilesList.isEmpty()) {
                                        displayImage(null, ImageDisplayMode.FIT_TO_VIEW, imageDisplayScrollPane, imageSizeLabel)
                                    }
                                }
                            }
                        }
                        popupMenu.add(deleteMenuItem)
                        popupMenu.show(fileList, e.x, e.y)
                    }
                }
            }
        }
    })

    // Add action listeners for display mode buttons
    fitToViewButton.addActionListener { 
        val selectedIndex = fileList.selectedIndex
        if (selectedIndex != -1) {
            val selectedDisplayableImage = selectedFilesList[selectedIndex]
            displayImage(selectedDisplayableImage, ImageDisplayMode.FIT_TO_VIEW, imageDisplayScrollPane, imageSizeLabel)
        }
    }
    fitToWidthButton.addActionListener { 
        val selectedIndex = fileList.selectedIndex
        if (selectedIndex != -1) {
            val selectedDisplayableImage = selectedFilesList[selectedIndex]
            displayImage(selectedDisplayableImage, ImageDisplayMode.FIT_TO_WIDTH, imageDisplayScrollPane, imageSizeLabel)
        }
    }
    actualSizeButton.addActionListener { 
        val selectedIndex = fileList.selectedIndex
        if (selectedIndex != -1) {
            val selectedDisplayableImage = selectedFilesList[selectedIndex]
            displayImage(selectedDisplayableImage, ImageDisplayMode.ACTUAL_SIZE, imageDisplayScrollPane, imageSizeLabel)
        }
    }

    getSelectedImageButton.addActionListener {
        val selectedImage = getSelectedImageFromDisplay()
        if (selectedImage != null) {
            JOptionPane.showMessageDialog(frame, ImageIcon(selectedImage), "Selected Image", JOptionPane.PLAIN_MESSAGE)
        } else {
            JOptionPane.showMessageDialog(frame, "No image selected or selection is empty.", "No Selection", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    ocrButton.addActionListener {
        val selectedImage = getSelectedImageFromDisplay()
        if (selectedImage != null) {
            val languages = listOf(selectedOcrLanguageCode)
            val ocrResult = TesseractApi.ocrImage(selectedImage, mapOf("languages" to languages), 300)
            lastOcrResult = ocrResult // Store the OCR result
            val ocrDialog = OcrResultDialog(frame, ocrResult ?: "")
            ocrDialog.isVisible = true
        } else {
            JOptionPane.showMessageDialog(frame, "No image selected for OCR.", "OCR Error", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    translateButton.addActionListener {
        if (lastOcrResult != null && lastOcrResult!!.isNotBlank()) {
            val currentSelection = getImagePanel().selectionRect // Assuming imagePanel has a public getter for selectionRect
            if (currentSelection != null) {
                val translationDialog = TranslationDialog(frame, lastOcrResult!!, selectedOcrLanguageCode) {
                    translatedText ->
                    getImagePanel().addSubtitle(SubtitleEntry(lastOcrResult!!, translatedText, currentSelection))
                }
                translationDialog.isVisible = true
            } else {
                JOptionPane.showMessageDialog(frame, "No selection box available to save subtitle.", "Translation Error", JOptionPane.INFORMATION_MESSAGE)
            }
        } else {
            JOptionPane.showMessageDialog(frame, "No OCR result available for translation.", "Translation Error", JOptionPane.INFORMATION_MESSAGE)
        }
    }

    // Add ComponentListener to imageDisplayScrollPane to handle resizing
    imageDisplayScrollPane.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            redrawCurrentImage(imageDisplayScrollPane, imageSizeLabel)
        }
    })

    // Add the toolbar and split pane to the frame
    frame.add(toolBar, BorderLayout.NORTH)
    frame.add(splitPane, BorderLayout.CENTER)

    // Pack and display the frame
    frame.pack()
    frame.setLocationRelativeTo(null) // Center the window
    frame.isVisible = true
}
