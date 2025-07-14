package com.woosoft.translator

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import java.awt.image.BufferedImage
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.JFrame
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

enum class SubtitleDisplayMode {
    NONE,
    OCR,
    TRANSLATION,
}

class ImagePanel(private val scrollPane: JScrollPane) : JPanel() {

    private var originalImage: Image? = null
    private var scaledImage: Image? = null
    private var currentDisplayMode: ImageDisplayMode = ImageDisplayMode.FIT_TO_VIEW
    private var scale: Double = 1.0

    var selectionRect: Rectangle? = null
    private val subtitles: MutableList<SubtitleEntry> = mutableListOf()
    private var selectedSubtitle: SubtitleEntry? = null
    private var startPoint: Point? = null
    private var resizingEdge: Int = -1 // -1: none, 0: top-left, 1: top-right, 2: bottom-left, 3: bottom-right, 4: top, 5: right, 6: bottom, 7: left
    private var isMoving: Boolean = false
    private var lastMousePoint: Point? = null

    var currentSubtitleDisplayMode: SubtitleDisplayMode = SubtitleDisplayMode.TRANSLATION // Default to displaying translated text
    private val RESIZE_HANDLE_SIZE = 8
    private val EDGE_TOLERANCE = 5

    init {
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                scaleImage()
                repaint()
            }
        })

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                if (originalImage == null) return

                val imagePoint = toOriginalImagePoint(e.point)

                val clickedSubtitle = subtitles.find { it.bounds.contains(imagePoint) }

                if (SwingUtilities.isRightMouseButton(e)) {
                    if (clickedSubtitle != null) {
                        selectedSubtitle = clickedSubtitle
                        selectionRect = clickedSubtitle.bounds // Select the subtitle's bounds
                        val popupMenu = JPopupMenu()
                        val editMenuItem = JMenuItem("Edit Subtitle")
                        editMenuItem.addActionListener {
                            val topFrame = SwingUtilities.getWindowAncestor(this@ImagePanel)
                            if (topFrame is JFrame) {
                                val frameOwner = topFrame as JFrame
                                val dialog = SubtitleEditDialog(frameOwner, clickedSubtitle.translatedText) { newText ->
                                    val index = subtitles.indexOf(clickedSubtitle)
                                    if (index != -1) {
                                        subtitles[index] = clickedSubtitle.copy(translatedText = newText)
                                        repaint()
                                    }
                                }
                                dialog.isVisible = true
                            }
                        }
                        popupMenu.add(editMenuItem)
                        popupMenu.show(e.component, e.x, e.y)
                    }
                } else { // Left-click
                    if (clickedSubtitle != null) {
                        // A subtitle was clicked, activate it and set selectionRect to its bounds
                        selectedSubtitle = clickedSubtitle
                        selectionRect = clickedSubtitle.bounds

                        // Now, determine if we are starting a move or resize of this activated subtitle's selectionRect
                        resizingEdge = getResizingEdge(imagePoint)
                        if (resizingEdge != -1) {
                            startPoint = imagePoint // For resizing
                        } else if (selectionRect!!.contains(imagePoint)) {
                            isMoving = true
                            lastMousePoint = imagePoint // For moving
                        } else {
                            // Clicked on subtitle but not on its border/inside for move/resize,
                            // so treat as a click to activate, but don't start a drag operation.
                            startPoint = null
                        }
                    } else { // No subtitle was clicked
                        // Clear any previously selected subtitle
                        selectedSubtitle = null

                        // Check if we are clicking inside an existing selectionRect (not necessarily a subtitle)
                        if (selectionRect != null && selectionRect!!.contains(imagePoint)) {
                            resizingEdge = getResizingEdge(imagePoint)
                            if (resizingEdge != -1) {
                                startPoint = imagePoint
                            } else {
                                isMoving = true
                                lastMousePoint = imagePoint
                            }
                        } else {
                            // Clicked outside any subtitle and outside any existing selectionRect
                            selectionRect = null // Clear previous selection
                            startPoint = imagePoint // Start a new selection
                        }
                    }
                }
                repaint()
            }

            override fun mouseReleased(e: MouseEvent) {
                if (selectedSubtitle != null && selectionRect != null) {
                    // Update the subtitle's bounds with the final selectionRect
                    val index = subtitles.indexOf(selectedSubtitle)
                    if (index != -1) {
                        subtitles[index] = selectedSubtitle!!.copy(bounds = selectionRect!!)
                    }
                    selectedSubtitle = null // Clear selected subtitle after saving
                }
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

                val imagePoint = toOriginalImagePoint(e.point)

                if (resizingEdge != -1 && selectionRect != null) {
                    resizeSelection(imagePoint)
                } else if (isMoving && selectionRect != null && lastMousePoint != null) {
                    val dx = imagePoint.x - lastMousePoint!!.x
                    val dy = imagePoint.y - lastMousePoint!!.y
                    selectionRect!!.translate(dx, dy)
                    lastMousePoint = imagePoint
                    constrainSelectionToImageBounds()
                } else if (startPoint != null) {
                    val x = Math.min(startPoint!!.x, imagePoint.x)
                    val y = Math.min(startPoint!!.y, imagePoint.y)
                    val width = Math.abs(imagePoint.x - startPoint!!.x)
                    val height = Math.abs(imagePoint.y - startPoint!!.y)

                    selectionRect = Rectangle(x, y, width, height)
                    constrainSelectionToImageBounds()
                }
                repaint()
            }

            override fun mouseMoved(e: MouseEvent) {
                if (selectionRect != null) {
                    val imagePoint = toOriginalImagePoint(e.point)
                    cursor = getResizingCursor(imagePoint)
                } else {
                    cursor = Cursor.getDefaultCursor()
                }
            }
        })
    }

    fun setImage(image: Image?, mode: ImageDisplayMode) {
        if (originalImage !== image) {
            selectionRect = null
            subtitles.clear() // Clear subtitles when a new image is loaded
        }
        originalImage = image
        currentDisplayMode = mode
        scaleImage()
        repaint()
    }

    fun getSelectedImage(): BufferedImage? {
        if (originalImage == null || selectionRect == null) return null

        val croppedX = Math.max(0, selectionRect!!.x)
        val croppedY = Math.max(0, selectionRect!!.y)
        val croppedWidth = Math.min(selectionRect!!.width, originalImage!!.getWidth(null) - croppedX)
        val croppedHeight = Math.min(selectionRect!!.height, originalImage!!.getHeight(null) - croppedY)

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
            val imageX = (width - it.getWidth(null)) / 2
            val imageY = (height - it.getHeight(null)) / 2
            g.drawImage(it, imageX, imageY, this)

            selectionRect?.let {
                g.color = Color.RED
                (g as Graphics2D).stroke = BasicStroke(2f)
                val drawRect = Rectangle(
                    (it.x * scale).toInt() + imageX,
                    (it.y * scale).toInt() + imageY,
                    (it.width * scale).toInt(),
                    (it.height * scale).toInt()
                )
                g.drawRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height)

                // Draw resize handles
                g.fillRect(drawRect.x - RESIZE_HANDLE_SIZE / 2, drawRect.y - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Top-left
                g.fillRect(drawRect.x + drawRect.width - RESIZE_HANDLE_SIZE / 2, drawRect.y - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Top-right
                g.fillRect(drawRect.x - RESIZE_HANDLE_SIZE / 2, drawRect.y + drawRect.height - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Bottom-left
                g.fillRect(drawRect.x + drawRect.width - RESIZE_HANDLE_SIZE / 2, drawRect.y + drawRect.height - RESIZE_HANDLE_SIZE / 2, RESIZE_HANDLE_SIZE, RESIZE_HANDLE_SIZE) // Bottom-right
            }

            // Draw subtitles
            for (subtitle in subtitles) {
                val textToDisplay = when (currentSubtitleDisplayMode) {
                    SubtitleDisplayMode.NONE -> null
                    SubtitleDisplayMode.OCR -> subtitle.ocrText
                    SubtitleDisplayMode.TRANSLATION -> subtitle.translatedText
                }

                if (textToDisplay != null) {
                    val drawRect = Rectangle(
                        (subtitle.bounds.x * scale).toInt() + imageX,
                        (subtitle.bounds.y * scale).toInt() + imageY,
                        (subtitle.bounds.width * scale).toInt(),
                        (subtitle.bounds.height * scale).toInt()
                    )

                    // Draw white background
                    g.color = Color.WHITE
                    g.fillRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height)

                    // Draw black text
                    g.color = Color.BLACK
                    drawWrappedText(g as Graphics2D, textToDisplay, drawRect)
                }
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        return scaledImage?.let { Dimension(it.getWidth(null), it.getHeight(null)) } ?: super.getPreferredSize()
    }

    private fun scaleImage() {
        originalImage?.let {
            val panelWidth = scrollPane.viewport.width
            val panelHeight = scrollPane.viewport.height

            when (currentDisplayMode) {
                ImageDisplayMode.ACTUAL_SIZE -> {
                    scaledImage = it
                    scale = 1.0
                }
                ImageDisplayMode.FIT_TO_WIDTH -> {
                    if (panelWidth > 0 && it.getWidth(null) > 0) {
                        scale = panelWidth.toDouble() / it.getWidth(null)
                        val newHeight = (it.getHeight(null) * scale).toInt()
                        scaledImage = it.getScaledInstance(panelWidth, newHeight, Image.SCALE_SMOOTH)
                    } else {
                        scaledImage = it
                        scale = 1.0
                    }
                }
                ImageDisplayMode.FIT_TO_VIEW -> {
                    if (panelWidth > 0 && panelHeight > 0 && it.getWidth(null) > 0 && it.getHeight(null) > 0) {
                        val scaleX = panelWidth.toDouble() / it.getWidth(null)
                        val scaleY = panelHeight.toDouble() / it.getHeight(null)
                        scale = Math.min(scaleX, scaleY)

                        val newWidth = (it.getWidth(null) * scale).toInt()
                        val newHeight = (it.getHeight(null) * scale).toInt()
                        scaledImage = it.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
                    } else {
                        scaledImage = it
                        scale = 1.0
                    }
                }
            }
            revalidate()
        }
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
            originalImage?.let {
                val imageBounds = Rectangle(0, 0, it.getWidth(null), it.getHeight(null))
                val intersection = imageBounds.intersection(selectionRect!!)
                selectionRect = intersection
            }
        }
    }

    fun addSubtitle(subtitle: SubtitleEntry) {
        subtitles.add(subtitle)
        repaint()
    }

    fun setSubtitleDisplayMode(mode: SubtitleDisplayMode) {
        currentSubtitleDisplayMode = mode
        repaint()
    }

    private fun toOriginalImagePoint(p: Point): Point {
        scaledImage?.let {
            val imageX = (width - it.getWidth(null)) / 2
            val imageY = (height - it.getHeight(null)) / 2
            return Point(((p.x - imageX) / scale).toInt(), ((p.y - imageY) / scale).toInt())
        }
        return p
    }

    private fun drawWrappedText(g2d: Graphics2D, text: String, bounds: Rectangle) {
        if (bounds.width <= 0 || bounds.height <= 0) return

        var fontSize = 200f // Start with a large font size and reduce it
        var font: Font
        var fontMetrics: FontMetrics
        var lines: List<String>
        var textHeight: Int

        // Binary search for the largest font size that fits
        var low = 1f
        var high = 200f
        var bestFitFontSize = 1f

        while (high - low > 0.1f) { // Iterate until precision is met
            val mid = (low + high) / 2
            font = Font("Arial", Font.PLAIN, mid.toInt().coerceAtLeast(1))
            fontMetrics = g2d.getFontMetrics(font)
            lines = wrapText(text, fontMetrics, bounds.width)
            textHeight = lines.size * fontMetrics.getHeight()

            if (textHeight <= bounds.height) {
                bestFitFontSize = mid
                low = mid
            } else {
                high = mid
            }
        }

        font = Font("Arial", Font.PLAIN, bestFitFontSize.toInt().coerceAtLeast(1))
        g2d.font = font
        fontMetrics = g2d.getFontMetrics(font)
        lines = wrapText(text, fontMetrics, bounds.width)

        val totalTextHeight = lines.size * fontMetrics.getHeight()
        var y = bounds.y + (bounds.height - totalTextHeight) / 2 + fontMetrics.getAscent()

        for (line in lines) {
            val lineWidth = fontMetrics.stringWidth(line)
            val x = bounds.x + (bounds.width - lineWidth) / 2
            g2d.drawString(line, x, y)
            y += fontMetrics.getHeight()
        }
    }

    private fun wrapText(text: String, fontMetrics: FontMetrics, maxWidth: Int): List<String> {
        val lines = mutableListOf<String>()
        val words = text.split(" ")
        var currentLine = StringBuilder()

        for (word in words) {
            if (currentLine.isEmpty()) {
                currentLine.append(word)
            } else {
                val potentialLine = "$currentLine $word"
                if (fontMetrics.stringWidth(potentialLine) <= maxWidth) {
                    currentLine.append(" ").append(word)
                } else {
                    lines.add(currentLine.toString())
                    currentLine = StringBuilder(word)
                }
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
