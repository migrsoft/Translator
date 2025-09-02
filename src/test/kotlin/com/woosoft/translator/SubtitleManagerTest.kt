
package com.woosoft.translator

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.awt.Rectangle
import java.io.File
import kotlin.test.assertEquals

class SubtitleManagerTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun `test save and load single image subtitles`() {
        val subtitles = listOf(
            SubtitleEntry("Hello", "Hola", Rectangle(10, 20, 30, 40)),
            SubtitleEntry("World", "Mundo", Rectangle(50, 60, 70, 80))
        )
        val subtitleFile = File(tempDir, "test.json")

        SubtitleManager.saveSingleImageSubtitles(subtitleFile, subtitles)

        val loadedSubtitles = SubtitleManager.loadSingleImageSubtitles(subtitleFile)

        assertEquals(subtitles, loadedSubtitles)
    }

    @Test
    fun `test save and load cbz subtitles`() {
        val cbzSubtitles = mapOf(
            "image1.jpg" to listOf(
                SubtitleEntry("One", "Uno", Rectangle(1, 2, 3, 4))
            ),
            "image2.jpg" to listOf(
                SubtitleEntry("Two", "Dos", Rectangle(5, 6, 7, 8)),
                SubtitleEntry("Three", "Tres", Rectangle(9, 10, 11, 12))
            )
        )
        val subtitleFile = File(tempDir, "test_cbz.json")

        SubtitleManager.saveCbzSubtitles(subtitleFile, cbzSubtitles)

        val loadedCbzSubtitles = SubtitleManager.loadCbzSubtitles(subtitleFile)

        assertEquals(cbzSubtitles, loadedCbzSubtitles)
    }

    @Test
    fun `loadSingleImageSubtitles should return empty list if file does not exist`() {
        val nonExistentFile = File(tempDir, "nonexistent.json")
        val loadedSubtitles = SubtitleManager.loadSingleImageSubtitles(nonExistentFile)
        assertEquals(emptyList(), loadedSubtitles)
    }

    @Test
    fun `loadCbzSubtitles should return empty map if file does not exist`() {
        val nonExistentFile = File(tempDir, "nonexistent_cbz.json")
        val loadedSubtitles = SubtitleManager.loadCbzSubtitles(nonExistentFile)
        assertEquals(emptyMap(), loadedSubtitles)
    }
}
