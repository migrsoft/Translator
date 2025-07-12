package com.woosoft.translator

import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

class TranslationDialog(owner: JFrame, sourceText: String) : JDialog(owner, "Translation", true) {

    private val sourceTextArea: JTextArea
    private val translatedTextArea: JTextArea

    init {
        layout = BorderLayout()
        preferredSize = Dimension(600, 600)

        sourceTextArea = JTextArea(sourceText)
        sourceTextArea.lineWrap = true
        sourceTextArea.wrapStyleWord = true
        sourceTextArea.isEditable = true
        val sourceScrollPane = JScrollPane(sourceTextArea)

        translatedTextArea = JTextArea()
        translatedTextArea.lineWrap = true
        translatedTextArea.wrapStyleWord = true
        translatedTextArea.isEditable = true
        val translatedScrollPane = JScrollPane(translatedTextArea)

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, sourceScrollPane, translatedScrollPane)
        splitPane.isContinuousLayout = true
        splitPane.setDividerLocation(0.5)
        add(splitPane, BorderLayout.CENTER)

        val translateButton = JButton("Translate")
        translateButton.addActionListener {
            val textToTranslate = sourceTextArea.text
            if (textToTranslate.isNotBlank()) {
                CoroutineScope(Dispatchers.Swing).launch {
                    // Assuming source language is Greek (grc) and target is English (eng) for now
                    val translatedText = LibreTranslateApi.translate(textToTranslate, "el", "en")
                    if (translatedText != null) {
                        translatedTextArea.text = translatedText
                    } else {
                        translatedTextArea.text = "Translation failed."
                    }
                }
            } else {
                translatedTextArea.text = "No text to translate."
            }
        }
        val buttonPanel = JPanel()
        buttonPanel.add(translateButton)
        add(buttonPanel, BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(owner)
    }
}
