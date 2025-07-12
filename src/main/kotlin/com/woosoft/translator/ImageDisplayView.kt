package com.woosoft.translator

import java.awt.BorderLayout
import java.awt.Image
import java.io.File
import javax.swing.BorderFactory
import javax.swing.ImageIcon
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JScrollPane

enum class ImageDisplayMode {
    FIT_TO_VIEW,
    FIT_TO_WIDTH,
    ACTUAL_SIZE
}

private val imageLabel = JLabel()
private var currentImageFile: File? = null
private var currentDisplayMode: ImageDisplayMode = ImageDisplayMode.FIT_TO_VIEW

fun createImageDisplayView(): JScrollPane {
    val imageDisplayPanel = JPanel()
    imageDisplayPanel.layout = BorderLayout()
    imageDisplayPanel.add(imageLabel, BorderLayout.CENTER)
    imageDisplayPanel.border = BorderFactory.createEtchedBorder()

    imageLabel.horizontalAlignment = JLabel.CENTER
    imageLabel.verticalAlignment = JLabel.CENTER

    val scrollPane = JScrollPane(imageDisplayPanel)
    scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
    scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

    return scrollPane
}

fun displayImage(imageFile: File, mode: ImageDisplayMode, scrollPane: JScrollPane) {
    currentImageFile = imageFile
    currentDisplayMode = mode

    if (imageFile.exists()) {
        val originalImageIcon = ImageIcon(imageFile.absolutePath)
        val originalImage = originalImageIcon.image

        val scaledImage = when (mode) {
            ImageDisplayMode.ACTUAL_SIZE -> originalImage
            ImageDisplayMode.FIT_TO_WIDTH -> {
                val panelWidth = scrollPane.viewport.width
                if (panelWidth > 0 && originalImage.getWidth(null) > 0) {
                    val newHeight = (originalImage.getHeight(null) * panelWidth.toDouble() / originalImage.getWidth(null)).toInt()
                    originalImage.getScaledInstance(panelWidth, newHeight, Image.SCALE_SMOOTH)
                } else {
                    originalImage
                }
            }
            ImageDisplayMode.FIT_TO_VIEW -> {
                val panelWidth = scrollPane.viewport.width
                val panelHeight = scrollPane.viewport.height
                if (panelWidth > 0 && panelHeight > 0 && originalImage.getWidth(null) > 0 && originalImage.getHeight(null) > 0) {
                    val imageWidth = originalImage.getWidth(null)
                    val imageHeight = originalImage.getHeight(null)

                    val scaleX = panelWidth.toDouble() / imageWidth
                    val scaleY = panelHeight.toDouble() / imageHeight
                    val scale = Math.min(scaleX, scaleY)

                    val newWidth = (imageWidth * scale).toInt()
                    val newHeight = (imageHeight * scale).toInt()
                    originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
                } else {
                    originalImage
                }
            }
        }
        imageLabel.icon = ImageIcon(scaledImage)
        imageLabel.text = null // Clear any previous "Image not found" text
        imageLabel.revalidate()
        imageLabel.repaint()
    } else {
        imageLabel.icon = null
        imageLabel.text = "Image not found: ${imageFile.name}"
    }
}

fun redrawCurrentImage(scrollPane: JScrollPane) {
    currentImageFile?.let {
        displayImage(it, currentDisplayMode, scrollPane)
    }
}