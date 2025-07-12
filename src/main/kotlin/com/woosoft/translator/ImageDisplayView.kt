package com.woosoft.translator

import java.awt.Image
import java.io.File
import javax.swing.JScrollPane
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

private lateinit var imagePanel: ImagePanel
private var currentImageFile: File? = null
private var currentDisplayMode: ImageDisplayMode = ImageDisplayMode.FIT_TO_VIEW

fun createImageDisplayView(): JScrollPane {
    val scrollPane = JScrollPane()
    imagePanel = ImagePanel(scrollPane) // Initialize imagePanel with the scrollPane
    scrollPane.setViewportView(imagePanel)
    scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

    return scrollPane
}

fun displayImage(imageFile: File, mode: ImageDisplayMode, scrollPane: JScrollPane) {
    currentImageFile = imageFile
    currentDisplayMode = mode

    if (imageFile.exists()) {
        try {
            val bufferedImage: BufferedImage = ImageIO.read(imageFile)
            imagePanel.setImage(bufferedImage as Image, mode)
            scrollPane.revalidate()
            scrollPane.repaint()
            imagePanel.reScaleImage()
        } catch (e: Exception) {
            println("Error loading image: ${e.message}")
            imagePanel.setImage(null, mode) // Clear image on error
            // Optionally, display an error message to the user
        }
    } else {
        imagePanel.setImage(null, mode) // Clear image if not found
        // Optionally, display "Image not found" text on the panel itself or elsewhere
    }
}

fun redrawCurrentImage(scrollPane: JScrollPane) {
    currentImageFile?.let {
        displayImage(it, currentDisplayMode, scrollPane)
    }
}

fun getSelectedImageFromDisplay(): BufferedImage? {
    return imagePanel.getSelectedImage()
}