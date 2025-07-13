package com.woosoft.translator

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class SubtitleEditDialog(owner: JFrame, initialText: String, private val callback: (String) -> Unit) : JDialog(owner, "Edit Subtitle", true) {

    private val subtitleTextArea: JTextArea

    init {
        layout = BorderLayout()
        preferredSize = Dimension(400, 200)

        subtitleTextArea = JTextArea(initialText)
        subtitleTextArea.lineWrap = true
        subtitleTextArea.wrapStyleWord = true
        subtitleTextArea.font = subtitleTextArea.font.deriveFont(16f)
        val scrollPane = JScrollPane(subtitleTextArea)
        add(scrollPane, BorderLayout.CENTER)

        val saveButton = JButton("Save")
        saveButton.addActionListener {
            callback(subtitleTextArea.text)
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
