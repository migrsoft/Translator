package com.woosoft.translator

import java.awt.Rectangle

data class SubtitleFileEntry(
    val ocrText: String,
    val translatedText: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    fun toSubtitleEntry(): SubtitleEntry {
        return SubtitleEntry(ocrText, translatedText, Rectangle(x, y, width, height))
    }

    companion object {
        fun fromSubtitleEntry(entry: SubtitleEntry): SubtitleFileEntry {
            return SubtitleFileEntry(entry.ocrText, entry.translatedText, entry.bounds.x, entry.bounds.y, entry.bounds.width, entry.bounds.height)
        }
    }
}
