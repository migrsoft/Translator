package com.woosoft.translator

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.JScrollPane

enum class ImageDisplayMode {
    FIT_TO_VIEW,
    FIT_TO_WIDTH,
    ACTUAL_SIZE
}

class ImagePanel(private val scrollPane: JScrollPane) : JPanel() {

    private var originalImage: Image? = null
    private var scaledImage: Image? = null
    private var currentDisplayMode: ImageDisplayMode = ImageDisplayMode.FIT_TO_VIEW

    private var selectionRect: Rectangle? = null
    private var startPoint: Point? = null
    private var resizingEdge: Int = -1 // -1: none, 0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right, 4: top, 5: right, 6: bottom, 7: left
    private var isMoving: Boolean = false
    private var lastMousePoint: Point? = null

    private val RESIZE_HANDLE_SIZE = 8
    private val EDGE_TOLERANCE = 5

    init {
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (originalImage == null) return

                val imageBounds = getImageBounds()
                if (imageBounds == null || !imageBounds.contains(e.point)) {
                    selectionRect = null
                    repaint()
                    return
                }

                if (selectionRect != null) {
                    resizingEdge = getResizingEdge(e.point)
                    if (resizingEdge != -1) {
                        startPoint = e.point
                    } else if (selectionRect!!.contains(e.point)) {
                        isMoving = true
                        lastMousePoint = e.point
                    } else {
                        selectionRect = null // Clicked outside existing selection
                        startPoint = e.point
                    }
                } else {
                    startPoint = e.point
                }
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                startPoint = null
                resizingEdge = -1
                isMoving = false
                lastMousePoint = null
                repaint()
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (originalImage == null) return

                if (resizingEdge != -1 && selectionRect != null) {
                    resizeSelection(e.point)
                } else if (isMoving && selectionRect != null && lastMousePoint != null) {
                    val dx = e.x - lastMousePoint!!.x
                    val dy = e.y - lastMousePoint!!.y
                    selectionRect!!.translate(dx, dy)
                    lastMousePoint = e.point
                    constrainSelectionToImageBounds()
                } else if (startPoint != null) {
                    val x = Math.min(startPoint!!.x, e.x)
                    val y = Math.min(startPoint!!.y, e.y)
                    val width = Math.abs(e.x - startPoint!!.x)
                    val height = Math.abs(e.y - startPoint!!.y)

                    selectionRect = Rectangle(x, y, width, height)
                    constrainSelectionToImageBounds()
                }
                repaint()
            }

            override fun mouseMoved(e: MouseEvent) {
                if (selectionRect != null) {
                    cursor = getResizingCursor(e.point)
                } else {
                    cursor = Cursor.getDefaultCursor()
                }
            }
        })
    }

    fun setImage(image: Image?, mode: ImageDisplayMode) {
        originalImage = image
        currentDisplayMode = mode
        scaleImage()
        repaint()
    }

    fun getSelectedImage(): BufferedImage? {
        if (originalImage == null || selectionRect == null) return null

        val imageX = (width - scaledImage!!.getWidth(null)) / 2
        val imageY = (height - scaledImage!!.getHeight(null)) / 2

        val selectionX = selectionRect!!.x - imageX
        val selectionY = selectionRect!!.y - imageY
        val selectionWidth = selectionRect!!.width
        val selectionHeight = selectionRect!!.height

        // Convert selection coordinates from scaled image to original image
        val scaleX = originalImage!!.getWidth(null).toDouble() / scaledImage!!.getWidth(null)
        val scaleY = originalImage!!.getHeight(null).toDouble() / scaledImage!!.getHeight(null)

        val originalSelectionX = (selectionX * scaleX).toInt()
        val originalSelectionY = (selectionY * scaleY).toInt()
        val originalSelectionWidth = (selectionWidth * scaleX).toInt()
        val originalSelectionHeight = (selectionHeight * scaleY).toInt()

        // Ensure the selection is within the original image bounds
        val croppedX = Math.max(0, originalSelectionX)
        val croppedY = Math.max(0, originalSelectionY)
        val croppedWidth = Math.min(originalSelectionWidth, originalImage!!.getWidth(null) - croppedX)
        val croppedHeight = Math.min(originalSelectionHeight, originalImage!!.getHeight(null) - croppedY)

        if (croppedWidth <= 0 || croppedHeight <= 0) return null

        val bufferedImage = originalImage as? BufferedImage ?: run {
            val bimage = BufferedImage(originalImage!!.getWidth(null), originalImage!!.getHeight(null), BufferedImage.TYPE_INT_ARGB)
            val g = bimage.createGraphics()
            g.drawImage(originalImage, 0, 0, null)
            g.dispose()
            bimage
        }

        return bufferedImage.getSubimage(croppedX, croppedY, croppedWidth, croppedHeight)
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        scaledImage?.let {
            val x = (width - it.getWidth(null)) / 2
            val y = (height - it.getHeight(null)) / 2
            g.drawImage(it, x, y, this)
        }

        selectionRect?.let {
            g.color = Color.RED
            (g as Graphics2D).stroke = BasicStroke(2f)
            g.drawRect(it.x, it.y, it.width, it.height)

            // Draw resize handles
            g.fillRect(it.x - RESIZE_HANDLE_SIZE / 2, it.y - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Top-left
            g.fillRect(it.x + it.width - RESIZE_HANDLE_SIZE / 2, it.y - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Top-right
            g.fillRect(it.x - RESIZE_HANDLE_SIZE / 2, it.y + it.height - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Bottom-left
            g.fillRect(it.x + it.width - RESIZE_HANDLE_SIZE / 2, it.y + it.height - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Bottom-right
        }
    }

    override fun getPreferredSize(): Dimension {
        return scaledImage?.let { Dimension(it.getWidth(null), it.getHeight(null)) } ?: super.getPreferredSize()
    }

    private fun scaleImage() {
        originalImage?.let {
            val panelWidth = scrollPane.viewport.width
            val panelHeight = scrollPane.viewport.height

            scaledImage = when (currentDisplayMode) {
                ImageDisplayMode.ACTUAL_SIZE -> it
                ImageDisplayMode.FIT_TO_WIDTH -> {
                    if (panelWidth > 0 && it.getWidth(null) > 0) {
                        val newHeight = (it.getHeight(null) * panelWidth.toDouble() / it.getWidth(null)).toInt()
                        it.getScaledInstance(panelWidth, newHeight, Image.SCALE_SMOOTH)
                    } else {
                        it
                    }
                }
                ImageDisplayMode.FIT_TO_VIEW -> {
                    if (panelWidth > 0 && panelHeight > 0 && it.getWidth(null) > 0 && it.getHeight(null) > 0) {
                        val imageWidth = it.getWidth(null)
                        val imageHeight = it.getHeight(null)

                        val scaleX = panelWidth.toDouble() / imageWidth
                        val scaleY = panelHeight.toDouble() / imageHeight
                        val scale = Math.min(scaleX, scaleY)

                        val newWidth = (imageWidth * scale).toInt()
                        val newHeight = (imageHeight * scale).toInt()
                        it.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
                    } else {
                        it
                    }
                }
            }
            revalidate()
        }
    }

    private fun getImageBounds(): Rectangle? {
        scaledImage?.let {
            val x = (width - it.getWidth(null)) / 2
            val y = (height - it.getHeight(null)) / 2
            return Rectangle(x, y, it.getWidth(null), it.getHeight(null))
        }
        return null
    }

    private fun getResizingEdge(p: Point): Int {
        selectionRect?.let {
            val handles = arrayOf(
                Rectangle(it.x - RESIZE_HANDLE_SIZE / 2, it.y - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE), // Top-left (0)
                Rectangle(it.x + it.width - RESIZE_HANDLE_SIZE / 2, it.y - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE), // Top-right (1)
                Rectangle(it.x - RESIZE_HANDLE_SIZE / 2, it.y + it.height - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE), // Bottom-left (2)
                Rectangle(it.x + it.width - RESIZE_HANDLE_SIZE / 2, it.y + it.height - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE)  // Bottom-right (3)
            )
            for (i in handles.indices) {
                if (handles[i].contains(p)) {
                    return i
                }
            }

            // Check edges
            if (p.x > it.x + RESIZE_HANDLE_SIZE / 2 && p.x < it.x + it.width - RESIZE_HANDLE_SIZE / 2) {
                if (Math.abs(p.y - it.y) < EDGE_TOLERANCE) return 4 // Top edge (4)
                if (Math.abs(p.y - (it.y + it.height)) < EDGE_TOLERANCE) return 6 // Bottom edge (6)
            }
            if (p.y > it.y + RESIZE_HANDLE_SIZE / 2 && p.y < it.y + it.height - RESIZE_HANDLE_SIZE / 2) {
                if (Math.abs(p.x - it.x) < EDGE_TOLERANCE) return 7 // Left edge (7)
                if (Math.abs(p.x - (it.x + it.width)) < EDGE_TOLERANCE) return 5 // Right edge (5)
            }
        }
        return -1
    }

    private fun getResizingCursor(p: Point): Cursor {
        val edge = getResizingEdge(p)
        return when (edge) {
            0 -> Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR) // Top-left
            1 -> Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR) // Top-right
            2 -> Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR) // Bottom-left
            3 -> Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR) // Bottom-right
            4 -> Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)  // Top edge
            5 -> Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR)  // Right edge
            6 -> Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR)  // Bottom edge
            7 -> Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)  // Left edge
            else -> Cursor.getDefaultCursor()
        }
    }

    private fun resizeSelection(currentPoint: Point) {
        selectionRect?.let {
            val newRect = Rectangle(it)
            when (resizingEdge) {
                0 -> { // Top-left
                    newRect.width += newRect.x - currentPoint.x
                    newRect.height += newRect.y - currentPoint.y
                    newRect.x = currentPoint.x
                    newRect.y = currentPoint.y
                }
                1 -> { // Top-right
                    newRect.width = currentPoint.x - newRect.x
                    newRect.height += newRect.y - currentPoint.y
                    newRect.y = currentPoint.y
                }
                2 -> { // Bottom-left
                    newRect.width += newRect.x - currentPoint.x
                    newRect.height = currentPoint.y - newRect.y
                    newRect.x = currentPoint.x
                }
                3 -> { // Bottom-right
                    newRect.width = currentPoint.x - newRect.x
                    newRect.height = currentPoint.y - newRect.y
                }
                4 -> { // Top edge
                    newRect.height += newRect.y - currentPoint.y
                    newRect.y = currentPoint.y
                }
                5 -> { // Right edge
                    newRect.width = currentPoint.x - newRect.x
                }
                6 -> { // Bottom edge
                    newRect.height = currentPoint.y - newRect.y
                }
                7 -> { // Left edge
                    newRect.width += newRect.x - currentPoint.x
                    newRect.x = currentPoint.x
                }
            }
            // Ensure width and height are not negative
            if (newRect.width < 0) {
                newRect.x += newRect.width
                newRect.width = -newRect.width
            }
            if (newRect.height < 0) {
                newRect.y += newRect.height
                newRect.height = -newRect.height
            }

            selectionRect = newRect
            constrainSelectionToImageBounds()
        }
    }

    private fun constrainSelectionToImageBounds() {
        selectionRect?.let {
            val imageBounds = getImageBounds() ?: return
            val intersection = imageBounds.intersection(it)
            selectionRect = intersection
        }
    }

    fun reScaleImage() {
        scaleImage()
        repaint()
    }
}
