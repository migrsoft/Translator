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
        sourceTextArea.font = sourceTextArea.font.deriveFont(16f)
        val sourceScrollPane = JScrollPane(sourceTextArea)

        translatedTextArea = JTextArea()
        translatedTextArea.lineWrap = true
        translatedTextArea.wrapStyleWord = true
        translatedTextArea.isEditable = true
        translatedTextArea.font = translatedTextArea.font.deriveFont(16f)
        val translatedScrollPane = JScrollPane(translatedTextArea)

        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, sourceScrollPane, translatedScrollPane)
        splitPane.isContinuousLayout = true
        splitPane.setDividerLocation(0.5)
        splitPane.setResizeWeight(0.5)
        add(splitPane, BorderLayout.CENTER)

        val targetLanguages = mapOf(
            "English" to "en",
            "Chinese" to "zh",
            "Greek" to "el"
        )

        val targetLanguageComboBox = JComboBox(targetLanguages.keys.toTypedArray())
        targetLanguageComboBox.selectedItem = "English" // Default selection

        val translateButton = JButton("Translate")
        translateButton.addActionListener {
            val textToTranslate = sourceTextArea.text
            val selectedLanguageName = targetLanguageComboBox.selectedItem as String
            val targetLanguageCode = targetLanguages[selectedLanguageName] ?: "en" // Default to English

            if (textToTranslate.isNotBlank()) {
                CoroutineScope(Dispatchers.Swing).launch {
                    // Assuming source language is Greek (grc) for now
                    val translatedText = LibreTranslateApi.translate(textToTranslate, "el", targetLanguageCode)
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
        buttonPanel.add(targetLanguageComboBox)
        buttonPanel.add(translateButton)
        add(buttonPanel, BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(owner)
    }
}
