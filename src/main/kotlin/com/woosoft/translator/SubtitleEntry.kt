package com.woosoft.translator

import java.awt.Rectangle

data class SubtitleEntry(
    val ocrText: String,
    val translatedText: String,
    val bounds: Rectangle
)
