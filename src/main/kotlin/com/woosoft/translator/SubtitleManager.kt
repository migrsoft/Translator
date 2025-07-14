package com.woosoft.translator

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

object SubtitleManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun saveSubtitles(file: File, subtitles: List<SubtitleEntry>) {
        val subtitleFileEntries = subtitles.map { SubtitleFileEntry.fromSubtitleEntry(it) }
        val jsonString = gson.toJson(subtitleFileEntries)
        file.writeText(jsonString)
    }

    fun loadSubtitles(file: File): List<SubtitleEntry> {
        if (!file.exists()) {
            return emptyList()
        }
        val jsonString = file.readText()
        val type = object : TypeToken<List<SubtitleFileEntry>>() {}.type
        val subtitleFileEntries: List<SubtitleFileEntry> = gson.fromJson(jsonString, type)
        return subtitleFileEntries.map { it.toSubtitleEntry() }
    }
}
