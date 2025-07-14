package com.woosoft.translator

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class OcrResultDialog(owner: JFrame, ocrText: String) : JDialog(owner, "OCR Result", true) {

    private val ocrTextArea: JTextArea

    init {
        layout = BorderLayout()
        preferredSize = Dimension(600, 400)

        ocrTextArea = JTextArea(ocrText)
        ocrTextArea.lineWrap = true
        ocrTextArea.wrapStyleWord = true
        ocrTextArea.isEditable = true
        ocrTextArea.font = ocrTextArea.font.deriveFont(16f)
        val scrollPane = JScrollPane(ocrTextArea)
        add(scrollPane, BorderLayout.CENTER)

        

        val okButton = JButton("OK")
        okButton.addActionListener {
            dispose()
        }

        val buttonPanel = JPanel()
        buttonPanel.add(okButton)
        add(buttonPanel, BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(owner)
    }
}
