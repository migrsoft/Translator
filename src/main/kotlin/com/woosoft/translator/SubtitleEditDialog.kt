package com.woosoft.translator

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class SubtitleEditDialog(owner: JFrame, private val ocrText: String, private val translatedText: String, private val displayMode: SubtitleDisplayMode, private val callback: (String?, String?) -> Unit) : JDialog(owner, "Edit Subtitle", true) {

    private val subtitleTextArea: JTextArea

    init {
        layout = BorderLayout()
        preferredSize = Dimension(400, 200)

        subtitleTextArea = JTextArea(when (displayMode) {
            SubtitleDisplayMode.OCR -> ocrText
            SubtitleDisplayMode.TRANSLATION -> translatedText
            else -> "" // Should not happen, but handle NONE case
        })
        subtitleTextArea.lineWrap = true
        subtitleTextArea.wrapStyleWord = true
        subtitleTextArea.font = subtitleTextArea.font.deriveFont(16f)
        val scrollPane = JScrollPane(subtitleTextArea)
        add(scrollPane, BorderLayout.CENTER)

        val saveButton = JButton("Save")
        saveButton.addActionListener {
            when (displayMode) {
                SubtitleDisplayMode.OCR -> callback(subtitleTextArea.text, null)
                SubtitleDisplayMode.TRANSLATION -> callback(null, subtitleTextArea.text)
                else -> {}
            }
            dispose()
        }
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener {
            dispose()
        }

        val buttonPanel = JPanel()
        buttonPanel.add(saveButton)
        buttonPanel.add(cancelButton)
        add(buttonPanel, BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(owner)
    }
}
