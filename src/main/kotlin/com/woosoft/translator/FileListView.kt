package com.woosoft.translator

import java.awt.Dimension
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.JScrollPane

fun createFileListView(): Pair<JScrollPane, DefaultListModel<String>> {
    val fileListModel = DefaultListModel<String>()
    val fileList = JList(fileListModel)
    val fileListScrollPane = JScrollPane(fileList)
    fileListScrollPane.preferredSize = Dimension(200, 0) // Set preferred width
    return Pair(fileListScrollPane, fileListModel)
}
