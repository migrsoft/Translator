package com.woosoft.translator

import java.awt.Image
import java.io.InputStream
import javax.swing.JScrollPane
import javax.imageio.ImageIO
import java.awt.image.BufferedImage
import javax.swing.JLabel

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

fun displayImage(displayableImage: DisplayableImage?, mode: ImageDisplayMode, scrollPane: JScrollPane, imageSizeLabel: JLabel) {
    currentDisplayableImage = displayableImage
    currentDisplayMode = mode

    if (displayableImage == null) {
        imagePanel.setImage(null, mode)
        scrollPane.revalidate()
        scrollPane.repaint()
        imageSizeLabel.text = ""
        return
    }

    try {
        val bufferedImage: BufferedImage = ImageIO.read(displayableImage.getInputStream())
        imagePanel.setImage(bufferedImage as Image, mode)
        scrollPane.revalidate()
        scrollPane.repaint()
        imagePanel.reScaleImage()
        imageSizeLabel.text = "${bufferedImage.width} x ${bufferedImage.height}"
        scrollPane.horizontalScrollBar.value = 0
        scrollPane.verticalScrollBar.value = 0
    } catch (e: Exception) {
        println("Error loading image: ${e.message}")
        imagePanel.setImage(null, mode) // Clear image on error
        // Optionally, display an error message to the user
    }
}

fun redrawCurrentImage(scrollPane: JScrollPane, imageSizeLabel: JLabel) {
    displayImage(currentDisplayableImage, currentDisplayMode, scrollPane, imageSizeLabel)
}

fun getSelectedImageFromDisplay(): BufferedImage? {
    return imagePanel.getSelectedImage()
}