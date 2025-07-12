package com.woosoft.translator

import javax.swing.JFrame
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.SwingUtilities

fun main() {
    SwingUtilities.invokeLater {
        val frame = JFrame("Translator")
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE // Closes the app when the window is closed
        frame.setSize(1024, 800) // Sets the initial size
        frame.setLocationRelativeTo(null) // Centers the frame on the screen

        // Optional: Add a window listener for specific actions (like closing)
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                println("Window is closing...")
            }
        })

        frame.isVisible = true // Makes the frame visible
    }
}