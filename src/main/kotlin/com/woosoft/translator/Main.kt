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

private var lastOpenedDirectory: File? = null
private val selectedFilesList = mutableListOf<File>()

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
    fileMenu.add(JMenuItem("Exit"))
    menuBar.add(fileMenu)

    val editMenu = JMenu("Edit")
    editMenu.add(JMenuItem("Settings"))
    menuBar.add(editMenu)

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
    val fitToViewButton = JButton("Fit to View")
    val fitToWidthButton = JButton("Fit to Width")
    val actualSizeButton = JButton("Actual Size")

    toolBar.add(fitToViewButton)
    toolBar.add(fitToWidthButton)
    toolBar.add(actualSizeButton)

    // Add action listener for Open menu item
    openMenuItem.addActionListener {
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
                fileListModel.addElement(file.name)
                selectedFilesList.add(file)
            }
            selectedFiles.firstOrNull()?.parentFile?.let { lastOpenedDirectory = it }
        }
    }

    // Add ListSelectionListener to fileList
    fileList.addListSelectionListener(object : ListSelectionListener {
        override fun valueChanged(e: ListSelectionEvent?) {
            if (!e!!.getValueIsAdjusting()) {
                val selectedIndex = fileList.selectedIndex
                if (selectedIndex != -1) {
                    val selectedFile = selectedFilesList[selectedIndex]
                    displayImage(selectedFile, ImageDisplayMode.FIT_TO_VIEW, imageDisplayScrollPane)
                }
            }
        }
    })

    // Add action listeners for display mode buttons
    fitToViewButton.addActionListener { 
        val selectedIndex = fileList.selectedIndex
        if (selectedIndex != -1) {
            val selectedFile = selectedFilesList[selectedIndex]
            displayImage(selectedFile, ImageDisplayMode.FIT_TO_VIEW, imageDisplayScrollPane)
        }
    }
    fitToWidthButton.addActionListener { 
        val selectedIndex = fileList.selectedIndex
        if (selectedIndex != -1) {
            val selectedFile = selectedFilesList[selectedIndex]
            displayImage(selectedFile, ImageDisplayMode.FIT_TO_WIDTH, imageDisplayScrollPane)
        }
    }
    actualSizeButton.addActionListener { 
        val selectedIndex = fileList.selectedIndex
        if (selectedIndex != -1) {
            val selectedFile = selectedFilesList[selectedIndex]
            displayImage(selectedFile, ImageDisplayMode.ACTUAL_SIZE, imageDisplayScrollPane)
        }
    }

    // Add ComponentListener to imageDisplayScrollPane to handle resizing
    imageDisplayScrollPane.addComponentListener(object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent?) {
            redrawCurrentImage(imageDisplayScrollPane)
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