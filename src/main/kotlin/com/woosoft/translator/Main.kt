package com.woosoft.translator

import java.awt.BorderLayout
import java.awt.Dimension
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.zip.ZipFile
import java.io.InputStream
import java.util.zip.ZipEntry

private var lastOpenedDirectory: File? = null
private var currentCbzZipFile: ZipFile? = null // To hold the currently open CBZ file
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
    val exitMenuItem = JMenuItem("Exit")
    exitMenuItem.addActionListener { System.exit(0) }
    fileMenu.add(exitMenuItem)
    menuBar.add(fileMenu)

    val editMenu = JMenu("Edit")
    
    val ocrLanguageMenu = JMenu("OCR Language")
    editMenu.add(ocrLanguageMenu)
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

        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("Image Files", "jpeg", "jpg", "png", "webp")
        fileChooser.isMultiSelectionEnabled = true // Enable multi-selection

        lastOpenedDirectory?.let { fileChooser.currentDirectory = it }

        val result = fileChooser.showOpenDialog(frame)
        if (result == JFileChooser.APPROVE_OPTION) {
            fileListModel.clear() // Clear existing items
            selectedFilesList.clear() // Clear the list of selected files

            val selectedFiles: Array<File> = fileChooser.selectedFiles
            for (file in selectedFiles) {
                val localFileImage = LocalFileImage(file)
                fileListModel.addElement(localFileImage.name)
                selectedFilesList.add(localFileImage)
            }
            selectedFiles.firstOrNull()?.parentFile?.let { lastOpenedDirectory = it }
        }
    }

    // Add action listener for Open CBZ menu item
    openCbzMenuItem.addActionListener {
        currentCbzZipFile?.close() // Close any previously opened CBZ file
        currentCbzZipFile = null

        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("CBZ Files", "cbz")
        fileChooser.isMultiSelectionEnabled = false // Only allow single CBZ selection

        lastOpenedDirectory?.let { fileChooser.currentDirectory = it }

        val result = fileChooser.showOpenDialog(frame)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedCbzFile = fileChooser.selectedFile
            if (selectedCbzFile != null) {
                fileListModel.clear()
                selectedFilesList.clear()

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
                    imageEntries.sortBy { it.name }

                    imageEntries.forEach { cbzImage ->
                        fileListModel.addElement(cbzImage.name)
                        selectedFilesList.add(cbzImage)
                    }

                    if (selectedFilesList.isNotEmpty()) {
                        fileList.selectedIndex = 0 // Select the first image
                        displayImage(selectedFilesList[0], ImageDisplayMode.FIT_TO_WIDTH, imageDisplayScrollPane, imageSizeLabel)
                    }
                    selectedCbzFile.parentFile?.let { lastOpenedDirectory = it }
                } catch (e: Exception) {
                    e.printStackTrace()
                    JOptionPane.showMessageDialog(frame, "Error opening CBZ file: ${e.message}", "CBZ Error", JOptionPane.ERROR_MESSAGE)
                }
            }
        }
    }

    // Add ListSelectionListener to fileList
    fileList.addListSelectionListener(object : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent?) {
            if (!e!!.getValueIsAdjusting()) {
                val selectedIndex = fileList.selectedIndex
                if (selectedIndex != -1) {
                    val selectedDisplayableImage = selectedFilesList[selectedIndex]
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
                        val renameMenuItem = JMenuItem("Rename")
                        renameMenuItem.addActionListener {
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
                                        println("Renamed ${originalFile.name} to ${newFile.name}")
                                        // Update the fileListModel and selectedFilesList
                                        val oldName = fileListModel.getElementAt(selectedIndices[i])
                                        val newLocalFileImage = LocalFileImage(newFile)
                                        fileListModel.setElementAt(newLocalFileImage.name, selectedIndices[i])
                                        selectedFilesList[selectedIndices[i]] = newLocalFileImage
                                    } else {
                                        JOptionPane.showMessageDialog(frame, "Failed to rename ${originalFile.name}", "Rename Error", JOptionPane.ERROR_MESSAGE)
                                    }
                                    currentNumber++
                                }
                            }
                            renameDialog.isVisible = true
                        }
                        popupMenu.add(renameMenuItem)
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
            val translationDialog = TranslationDialog(frame, lastOcrResult!!)
            translationDialog.isVisible = true
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