package com.woosoft.translator

import java.awt.Image
import java.io.InputStream
import javax.swing.JScrollPane
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

private lateinit var imagePanel: ImagePanel
private var currentDisplayableImage: DisplayableImage? = null
private var currentDisplayMode: ImageDisplayMode = ImageDisplayMode.FIT_TO_VIEW

fun createImageDisplayView(): JScrollPane {
    val scrollPane = JScrollPane()
    imagePanel = ImagePanel(scrollPane) // Initialize imagePanel with the scrollPane
    scrollPane.setViewportView(imagePanel)
    scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

    return scrollPane
}

fun displayImage(displayableImage: DisplayableImage, mode: ImageDisplayMode, scrollPane: JScrollPane) {
    currentDisplayableImage = displayableImage
    currentDisplayMode = mode

    try {
        val bufferedImage: BufferedImage = ImageIO.read(displayableImage.getInputStream())
        imagePanel.setImage(bufferedImage as Image, mode)
        scrollPane.revalidate()
        scrollPane.repaint()
        imagePanel.reScaleImage()
    } catch (e: Exception) {
        println("Error loading image: ${e.message}")
        imagePanel.setImage(null, mode) // Clear image on error
        // Optionally, display an error message to the user
    }
}

fun redrawCurrentImage(scrollPane: JScrollPane) {
    currentDisplayableImage?.let {
        displayImage(it, currentDisplayMode, scrollPane)
    }
}

fun getSelectedImageFromDisplay(): BufferedImage? {
    return imagePanel.getSelectedImage()
}