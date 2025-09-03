package com.woosoft.translator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter

class FileTranslationDialog(owner: JFrame) : JDialog(owner, "Translate Subtitle File", true) {

    private var selectedFile: File? = null
    private val selectedFileLabel = JLabel("No file selected.")
    private val progressBar = JProgressBar(0, 100)

    // Re-using the language maps from TranslationDialog
    private val targetLanguages = mapOf(
        "English" to "en",
        "Chinese" to "zh",
        "Greek" to "el"
    )
    // For source, we can use the same list.
    private val sourceLanguageComboBox = JComboBox(targetLanguages.keys.toTypedArray())
    private val targetLanguageComboBox = JComboBox(targetLanguages.keys.toTypedArray())

    init {
        layout = BorderLayout()

        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.fill = GridBagConstraints.HORIZONTAL

        // File selection
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("File:"), gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        gbc.weightx = 1.0
        panel.add(selectedFileLabel, gbc)

        gbc.gridx = 2
        gbc.gridy = 0
        gbc.weightx = 0.0
        val selectFileButton = JButton("Select File...")
        selectFileButton.addActionListener {
            selectFile()
        }
        panel.add(selectFileButton, gbc)

        // Source Language
        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(JLabel("Source Language:"), gbc)

        gbc.gridx = 1
        gbc.gridy = 1
        gbc.gridwidth = 2
        sourceLanguageComboBox.selectedItem = "English"
        panel.add(sourceLanguageComboBox, gbc)

        // Target Language
        gbc.gridx = 0
        gbc.gridy = 2
        panel.add(JLabel("Target Language:"), gbc)

        gbc.gridx = 1
        gbc.gridy = 2
        gbc.gridwidth = 2
        targetLanguageComboBox.selectedItem = "Chinese" // Different default
        panel.add(targetLanguageComboBox, gbc)

        // Progress Bar
        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 3
        progressBar.isStringPainted = true
        progressBar.isVisible = false // Initially hidden
        panel.add(progressBar, gbc)

        add(panel, BorderLayout.CENTER)

        // Buttons
        val buttonPanel = JPanel()
        val translateButton = JButton("Translate and Save")
        translateButton.addActionListener {
            translateAndSave()
        }
        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener {
            dispose()
        }
        buttonPanel.add(translateButton)
        buttonPanel.add(cancelButton)
        add(buttonPanel, BorderLayout.SOUTH)

        pack()
        setLocationRelativeTo(owner)
    }

    private fun selectFile() {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("Subtitle Files (.vtt, .srt, .txt)", "vtt", "srt", "txt")
        fileChooser.isMultiSelectionEnabled = false
        val result = fileChooser.showOpenDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.selectedFile
            selectedFileLabel.text = selectedFile?.name ?: "No file selected."
        }
    }

    private fun translateAndSave() {
        if (selectedFile == null) {
            JOptionPane.showMessageDialog(this, "Please select a file to translate.", "No File Selected", JOptionPane.WARNING_MESSAGE)
            return
        }

        val sourceLangName = sourceLanguageComboBox.selectedItem as String
        val targetLangName = targetLanguageComboBox.selectedItem as String
        val sourceLangCode = targetLanguages[sourceLangName] ?: "en"
        val targetLangCode = targetLanguages[targetLangName] ?: "en"

        if (sourceLangCode == targetLangCode) {
            JOptionPane.showMessageDialog(this, "Source and target languages cannot be the same.", "Language Error", JOptionPane.WARNING_MESSAGE)
            return
        }

        val lines = selectedFile!!.readLines()

        // Pre-scan to count blocks
        var totalBlocks = 0
        val tempBuffer = mutableListOf<String>()
        for (line in lines) {
            val isMetadata = line.isBlank() || line.matches(Regex("^\\d+$")) || line.contains("-->")
            if (isMetadata) {
                if (tempBuffer.isNotEmpty()) {
                    totalBlocks++
                    tempBuffer.clear()
                }
            } else {
                tempBuffer.add(line)
            }
        }
        if (tempBuffer.isNotEmpty()) {
            totalBlocks++
        }

        // Setup progress bar
        progressBar.minimum = 0
        progressBar.maximum = totalBlocks
        progressBar.value = 0
        progressBar.isVisible = true
        pack()

        CoroutineScope(Dispatchers.Swing).launch {
            try {
                val translatedLines = mutableListOf<String>()
                val textBuffer = mutableListOf<String>()
                var translatedBlockCount = 0

                suspend fun translateBuffer() {
                    if (textBuffer.isNotEmpty()) {
                        val textToTranslate = textBuffer.joinToString("\n")
                        val translated = withContext(Dispatchers.IO) {
                            LibreTranslateApi.translate(textToTranslate, sourceLangCode, targetLangCode)
                        }
                        translatedLines.add(translated ?: textToTranslate)
                        textBuffer.clear()
                        translatedBlockCount++
                        progressBar.value = translatedBlockCount
                    }
                }

                for (line in lines) {
                    val isMetadata = line.isBlank() || line.matches(Regex("^\\d+$")) || line.contains("-->")
                    if (isMetadata) {
                        translateBuffer()
                        translatedLines.add(line)
                    } else {
                        textBuffer.add(line)
                    }
                }
                translateBuffer() // Translate any remaining text

                val translatedText = translatedLines.joinToString("\n")
                progressBar.isVisible = false

                if (translatedText.isNotBlank()) {
                    promptToSave(translatedText)
                } else {
                    JOptionPane.showMessageDialog(this@FileTranslationDialog, "Translation resulted in empty content.", "Translation Failed", JOptionPane.ERROR_MESSAGE)
                }
            } catch (e: Exception) {
                progressBar.isVisible = false
                e.printStackTrace()
                JOptionPane.showMessageDialog(this@FileTranslationDialog, "An error occurred during translation: ${e.message}", "Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    private fun sanitizeFilename(name: String): String {
        val illegalChars = Regex("""[/\\?%*:|<>｜!&]""")
        return name.replace(illegalChars, "_")
    }

    private fun promptToSave(content: String) {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Save Translated File"
        val originalFile = selectedFile!!
        val extension = originalFile.extension
        val baseName = originalFile.nameWithoutExtension
        val sanitizedBaseName = sanitizeFilename(baseName)
        val targetLangCode = targetLanguages[targetLanguageComboBox.selectedItem as String]
        fileChooser.selectedFile = File(originalFile.parentFile, "$sanitizedBaseName.$targetLangCode.$extension")

        val result = fileChooser.showSaveDialog(this)
        if (result == JFileChooser.APPROVE_OPTION) {
            try {
                fileChooser.selectedFile.writeText(content)
                JOptionPane.showMessageDialog(this, "File saved successfully!", "Success", JOptionPane.INFORMATION_MESSAGE)
                dispose()
            } catch (e: Exception) {
                e.printStackTrace()
                JOptionPane.showMessageDialog(this, "Error saving file: ${e.message}", "Save Error", JOptionPane.ERROR_MESSAGE)
            }
        }
    }
}