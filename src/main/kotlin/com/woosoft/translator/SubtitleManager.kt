package com.woosoft.translator

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import java.io.File

object SubtitleManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    fun saveSingleImageSubtitles(file: File, subtitles: List<SubtitleEntry>) {
        val subtitleFileEntries = subtitles.map { SubtitleFileEntry.fromSubtitleEntry(it) }
        val jsonString = gson.toJson(subtitleFileEntries)
        file.writeText(jsonString)
    }

    fun loadSingleImageSubtitles(file: File): List<SubtitleEntry> {
        if (!file.exists()) {
            return emptyList()
        }
        val jsonString = file.readText()
        val type = object : TypeToken<List<SubtitleFileEntry>>() {}.type
        val subtitleFileEntries: List<SubtitleFileEntry> = gson.fromJson(jsonString, type)
        return subtitleFileEntries.map { it.toSubtitleEntry() }
    }

    fun saveCbzSubtitles(file: File, cbzSubtitleData: Map<String, List<SubtitleEntry>>) {
        val subtitleMapForJson = cbzSubtitleData.mapValues { (_, subtitles) ->
            subtitles.map { SubtitleFileEntry.fromSubtitleEntry(it) }
        }
        val cbzData = CbzSubtitleData(subtitleMapForJson)
        val jsonString = gson.toJson(cbzData)
        file.writeText(jsonString)
    }

    fun loadCbzSubtitles(file: File): Map<String, List<SubtitleEntry>> {
        if (!file.exists()) {
            return emptyMap()
        }
        val jsonString = file.readText()
        val type = object : TypeToken<CbzSubtitleData>() {}.type
        val cbzData: CbzSubtitleData = gson.fromJson(jsonString, type)
        return cbzData.imageSubtitles.mapValues { (_, subtitleFileEntries) ->
            subtitleFileEntries.map { it.toSubtitleEntry() }
        }
    }
}
