package com.woosoft.translator

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import javax.swing.*

class RenameDialog(
    parent: JFrame,
    private val onRenameConfirmed: (prefix: String, startNumber: Int, width: Int) -> Unit
) : JDialog(parent, "Rename Files", true) {

    private val prefixField = JTextField(15)
    private val startNumberSpinner = JSpinner(SpinnerNumberModel(1, 1, 9999, 1))
    private val widthSpinner = JSpinner(SpinnerNumberModel(3, 1, 10, 1))

    init {
        layout = GridBagLayout()
        val gbc = GridBagConstraints()
        gbc.insets = Insets(5, 5, 5, 5)

        // Prefix
        gbc.gridx = 0
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.EAST
        add(JLabel("Prefix:"), gbc)

        gbc.gridx = 1
        gbc.gridy = 0
        gbc.anchor = GridBagConstraints.WEST
        add(prefixField, gbc)

        // Start Number
        gbc.gridx = 0
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.EAST
        add(JLabel("Start Number:"), gbc)

        gbc.gridx = 1
        gbc.gridy = 1
        gbc.anchor = GridBagConstraints.WEST
        add(startNumberSpinner, gbc)

        // Width
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.EAST
        add(JLabel("Sequence Width:"), gbc)

        gbc.gridx = 1
        gbc.gridy = 2
        gbc.anchor = GridBagConstraints.WEST
        add(widthSpinner, gbc)

        // Buttons
        val renameButton = JButton("Rename")
        renameButton.addActionListener {
            val prefix = prefixField.text
            val startNumber = startNumberSpinner.value as Int
            val width = widthSpinner.value as Int

            if (prefix.isBlank()) {
                JOptionPane.showMessageDialog(this, "Prefix cannot be empty.", "Input Error", JOptionPane.ERROR_MESSAGE)
            } else {
                onRenameConfirmed(prefix, startNumber, width)
                dispose()
            }
        }

        val cancelButton = JButton("Cancel")
        cancelButton.addActionListener {
            dispose()
        }

        val buttonPanel = JPanel()
        buttonPanel.add(renameButton)
        buttonPanel.add(cancelButton)

        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.anchor = GridBagConstraints.CENTER
        add(buttonPanel, gbc)

        pack()
        setLocationRelativeTo(parent)
    }
}
